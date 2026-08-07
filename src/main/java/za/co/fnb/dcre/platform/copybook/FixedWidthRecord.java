package za.co.fnb.dcre.platform.copybook;

/**
 * One decoded copybook record: its 0-based position in the file, the raw
 * decoded line, and the layout that cuts it. The index is the record's
 * position, never a shared counter, so a sequence derived from it is
 * deterministic under partitioned or restarted ingest.
 */
public record FixedWidthRecord(long index, String line, CopybookLayout layout) {

    /** The named field, RAW and UNTRIMMED: a trailing space is data (A-83). */
    public String field(String name) {
        return layout.slice(line, name);
    }
}
