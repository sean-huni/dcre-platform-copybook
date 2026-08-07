package za.co.fnb.dcre.platform.copybook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative fixed-width record layout. Offsets derive from the field table
 * alone, never from rendered whitespace (register provenance rule) and never
 * from a hand-typed absolute column: laying the fields out in declaration order
 * makes a gap or an overlap impossible to express, so the class of defect where
 * one field's offset is a column adrift cannot occur.
 *
 * <p>Slices come back RAW and UNTRIMMED. A trailing space is data: A-83 is a
 * mandate MsgId whose trailing sequence is space-padded in the copybook header
 * and zero-padded in the filename, and a trimming reader cannot see that defect
 * at all. Callers strip at the point where they know stripping is safe.
 */
public final class FixedWidthLayout {

    private final Map<String, int[]> offsets = new LinkedHashMap<>();
    private final int length;

    public FixedWidthLayout(List<LayoutField> fields) {
        int pos = 0;
        for (LayoutField f : fields) {
            if (offsets.put(f.name(), new int[]{pos, pos + f.width()}) != null) {
                // A repeated name would silently overwrite the earlier field's
                // columns, so every slice of it would read the later occurrence.
                throw new IllegalArgumentException("duplicate field " + f.name());
            }
            pos += f.width();
        }
        this.length = pos;
    }

    public String slice(String line, String field) {
        int[] o = offsets.get(field);
        if (o == null) {
            throw new IllegalArgumentException("unknown field " + field);
        }
        return line.substring(o[0], o[1]);
    }

    /** The record length this table cuts: the sum of every field's width. */
    public int length() {
        return length;
    }
}
