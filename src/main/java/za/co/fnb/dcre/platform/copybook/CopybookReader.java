package za.co.fnb.dcre.platform.copybook;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reads a whole fixed-width copybook file into records, selecting each record's
 * layout through a {@link LayoutResolver}. The layout is chosen PER RECORD
 * because a DCRE file is multi-record-type: header and detail records differ,
 * and the detail records themselves differ between layout versions.
 *
 * <p>Decoding is ISO_8859_1, byte-transparent: one byte is one char, so offsets
 * and slice positions never drift on non-UTF-8 bytes and a legacy byte such as
 * 0xE9 survives the read intact. Strict UTF-8 decoding anywhere in the pipeline
 * crashes or mangles it.
 *
 * <p>Fails closed on a record shorter than its own layout's length, naming the
 * record. A LONGER record is accepted and sliced by that layout's columns:
 * generator files pad the header out to the detail LRECL while real files do
 * not, and both must ingest identically.
 */
public final class CopybookReader {

    private CopybookReader() {
    }

    /**
     * Streams the file, handing each record to {@code sink} in file order, and
     * returns the record COUNT.
     *
     * <p>Holds ONE record at a time. These files are large, which is the whole
     * reason the partitioned readers exist, so a caller that needs only a
     * couple of records and the count must not be forced to materialise the
     * file plus a wrapper per line. Prefer this over {@link #read}.
     */
    public static long forEachRecord(Path file, LayoutResolver resolver, Consumer<FixedWidthRecord> sink)
            throws IOException {
        long index = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sink.accept(record(index, line, resolver));
                index++;
            }
        }
        return index;
    }

    /**
     * The whole file as a list. Convenient, and it holds every record at once:
     * use {@link #forEachRecord} unless the caller genuinely needs random
     * access to all of them.
     */
    public static List<FixedWidthRecord> read(Path file, LayoutResolver resolver) throws IOException {
        List<FixedWidthRecord> records = new ArrayList<>();
        forEachRecord(file, resolver, records::add);
        return List.copyOf(records);
    }

    private static FixedWidthRecord record(long index, String line, LayoutResolver resolver) {
        FixedWidthLayout layout = resolver.layoutFor(index, line);
        if (layout == null) {
            // Fail closed: a resolver with no answer means the format does not
            // recognise the record, which is a malformed file, not a default.
            throw new IllegalArgumentException(
                    "no layout resolved for record " + index + " of " + line.length() + " bytes");
        }
        if (line.length() < layout.length()) {
            throw new ShortRecordException(index, line.length(), layout.length());
        }
        return new FixedWidthRecord(index, line, layout);
    }
}
