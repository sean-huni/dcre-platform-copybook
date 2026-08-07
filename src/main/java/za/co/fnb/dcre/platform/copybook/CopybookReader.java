package za.co.fnb.dcre.platform.copybook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    public static List<FixedWidthRecord> read(Path file, LayoutResolver resolver) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        List<FixedWidthRecord> records = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
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
            records.add(new FixedWidthRecord(index, line, layout));
        }
        return List.copyOf(records);
    }
}
