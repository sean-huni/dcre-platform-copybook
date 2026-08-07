package za.co.fnb.dcre.platform.copybook;

/**
 * One field of a copybook record: its name and its WIDTH in bytes. There is no
 * offset here on purpose. The layout derives every offset by laying the fields
 * out in declaration order, so a gap or an overlap between two fields cannot be
 * expressed at all. A representation carrying absolute offsets can express both,
 * and a one-column slip in a hand-typed offset is silent forever.
 *
 * <p>One byte is one char under the reader's ISO_8859_1 decode, so a width is a
 * byte count and a column count at the same time.
 */
public record LayoutField(String name, int width) {

    public LayoutField {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("field name is required");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("field " + name + " has non-positive width " + width);
        }
    }
}
