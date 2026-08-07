package za.co.fnb.dcre.platform.copybook;

/**
 * A record shorter than its layout's declared LRECL. Typed so a caller can
 * decide which record's shortness it is reporting (a short first record is a
 * malformed header; a short later one is a ragged body) without matching on
 * message text. Stays an {@link IllegalArgumentException}: a short record is a
 * malformed argument to the read, not an I/O fault.
 */
public class ShortRecordException extends IllegalArgumentException {

    private final long recordIndex;
    private final int actualLength;
    private final int declaredLength;

    public ShortRecordException(long recordIndex, int actualLength, int declaredLength) {
        super("record " + recordIndex + " is " + actualLength + " bytes, shorter than the declared "
                + declaredLength + "-byte layout");
        this.recordIndex = recordIndex;
        this.actualLength = actualLength;
        this.declaredLength = declaredLength;
    }

    /** 0-based position of the offending record in the file. */
    public long recordIndex() {
        return recordIndex;
    }

    public int actualLength() {
        return actualLength;
    }

    public int declaredLength() {
        return declaredLength;
    }
}
