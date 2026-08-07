package za.co.fnb.dcre.platform.copybook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A declarative fixed-width record layout: a record length (LRECL) plus the
 * field table that cuts it. Family-agnostic on purpose, collections, payments
 * and mandates are all cut by the same toolkit, so the layout tables differ
 * while the slicing does not.
 *
 * <p>Slices are returned RAW and UNTRIMMED. A trailing space is data: A-83 is a
 * mandate MsgId whose trailing sequence is space-padded in the copybook header
 * and zero-padded in the filename, and a trimming reader cannot see that at
 * all. Callers that want a business value strip it themselves, at the point
 * where they know stripping is safe.
 *
 * <p>A field table may be empty, which declares a pure record-length gate:
 * the reader still fails closed on a short record, and the fields are cut
 * elsewhere.
 */
public final class CopybookLayout {

    private final int lrecl;
    private final Map<String, FieldSpec> fields;

    private CopybookLayout(int lrecl, Map<String, FieldSpec> fields) {
        this.lrecl = lrecl;
        this.fields = fields;
    }

    public static CopybookLayout of(int lrecl, List<FieldSpec> fields) {
        if (lrecl <= 0) {
            throw new IllegalArgumentException("record length must be positive, got " + lrecl);
        }
        Map<String, FieldSpec> byName = new LinkedHashMap<>();
        for (FieldSpec field : fields) {
            if (field.end() > lrecl) {
                throw new IllegalArgumentException("field " + field.name() + " ends at " + field.end()
                        + ", past the declared " + lrecl + "-byte record");
            }
            if (byName.put(field.name(), field) != null) {
                throw new IllegalArgumentException("duplicate field " + field.name());
            }
        }
        return new CopybookLayout(lrecl, Map.copyOf(byName));
    }

    /** Declared record length; the reader rejects any record shorter than it. */
    public int lrecl() {
        return lrecl;
    }

    /** The field's columns, raw and untrimmed. */
    public String slice(String line, String field) {
        FieldSpec spec = fields.get(field);
        if (spec == null) {
            throw new IllegalArgumentException("unknown field " + field);
        }
        return line.substring(spec.offset(), spec.end());
    }
}
