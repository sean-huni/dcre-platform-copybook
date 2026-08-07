package za.co.fnb.dcre.platform.copybook;

/**
 * Chooses the layout that cuts ONE record. A DCRE copybook file is
 * multi-record-type: a collection request book carries a 109-content header
 * followed by V1/V2/V3 detail records of three different LRECLs, and a mandate
 * instruction book carries its own header plus 285-byte instructions. Binding a
 * single layout to a whole file therefore cannot express the format, which is
 * why the reader asks per record instead.
 *
 * <p>The discriminator is the format's own, not the reader's. Line LENGTH is
 * the usual one, which is what {@code SpineWriter.layoutFor} and
 * {@code MandateEntryWriter.layoutFor} already do. RECORD INDEX matters too: a
 * generator pads the header out to the detail LRECL while a real file does not,
 * so a padded 169-byte header is length-identical to a V2 detail and only its
 * position at index 0 tells them apart.
 *
 * <p>Fail closed. A record the format does not recognise is a malformed file,
 * so an implementation throws its own domain exception rather than returning a
 * default layout; the reader rejects a {@code null} return for the same reason.
 */
@FunctionalInterface
public interface LayoutResolver {

    /**
     * @param recordIndex 0-based position of the record in the file
     * @param line        the decoded record, raw
     * @return the layout that cuts this record, never {@code null}
     */
    FixedWidthLayout layoutFor(long recordIndex, String line);
}
