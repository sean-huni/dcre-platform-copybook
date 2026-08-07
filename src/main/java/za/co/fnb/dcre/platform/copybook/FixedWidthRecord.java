package za.co.fnb.dcre.platform.copybook;

/**
 * One decoded copybook record: its 0-based position in the file, the raw
 * decoded line, and the layout that cuts it. The index is the record's
 * position, never a shared counter, so a sequence derived from it is
 * deterministic under partitioned or restarted ingest.
 */
public record FixedWidthRecord(long index, String line, FixedWidthLayout layout) {

    /** The named field, RAW and UNTRIMMED: a trailing space is data (A-83). */
    public String field(String name) {
        return layout.slice(line, name);
    }

    /**
     * Index and length ONLY. The generated record {@code toString} prints every
     * component, and {@code line} on a collections detail record is
     * debtor_name, debtor_account and amount, on a mandate record it is the
     * debtor's name, account and branch. This is a bank-file reader: one
     * {@code log.debug(record)} anywhere downstream would put account numbers
     * into the log collector, where they are durable and searchable. The raw
     * line is still reachable through {@link #line()} by a caller that means it.
     */
    @Override
    public String toString() {
        return "FixedWidthRecord[index=" + index + ", length=" + line.length() + "]";
    }
}
