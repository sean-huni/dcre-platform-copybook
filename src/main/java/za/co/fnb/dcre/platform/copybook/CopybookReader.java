package za.co.fnb.dcre.platform.copybook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a whole fixed-width copybook file into records.
 *
 * <p>Decoding is ISO_8859_1, byte-transparent: one byte = one char, so offsets
 * and slice positions never drift on non-UTF-8 bytes and a legacy byte such as
 * 0xE9 survives the read intact. Strict UTF-8 decoding anywhere in the pipeline
 * crashes or mangles it.
 *
 * <p>Fails closed on a record shorter than the layout's declared LRECL, naming
 * the record. A LONGER record is accepted and sliced by the layout's columns:
 * generator files pad the header out to the detail LRECL while real files do
 * not, and both must ingest identically.
 */
public final class CopybookReader {

    private CopybookReader() {
    }

    public static List<FixedWidthRecord> read(Path file, CopybookLayout layout) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        List<FixedWidthRecord> records = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.length() < layout.lrecl()) {
                throw new ShortRecordException(index, line.length(), layout.lrecl());
            }
            records.add(new FixedWidthRecord(index, line, layout));
        }
        return List.copyOf(records);
    }
}
