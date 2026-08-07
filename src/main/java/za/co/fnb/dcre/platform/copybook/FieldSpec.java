package za.co.fnb.dcre.platform.copybook;

/**
 * One field's place in a copybook record: absolute {@code offset} into the
 * record and its raw {@code length}, both in bytes (one byte = one char under
 * the reader's ISO_8859_1 decode). Offsets are declared, never derived from
 * rendered whitespace, so a field whose value happens to be blank still
 * occupies its columns.
 */
public record FieldSpec(String name, int offset, int length) {

    public FieldSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("field name is required");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("field " + name + " has negative offset " + offset);
        }
        if (length <= 0) {
            throw new IllegalArgumentException("field " + name + " has non-positive length " + length);
        }
    }

    /** Exclusive end column of this field. */
    public int end() {
        return offset + length;
    }
}
