package za.co.fnb.dcre.platform.copybook;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MandateLayoutsTest {

    /** Field -> value in layout order; padded per width into one fixed line. */
    private static final Map<String, String> HEADER_VALUES = orderedMap(new String[][]{
            {"record_type", "0002"},
            {"sender_id", "DCRE"},
            {"file_type", "MB"},
            {"created_ts", "20260722080000"},
            {"layout_version", "01"},
            {"filler_1", ""},
            {"created_short", "202607220800"},
            {"entry_count", "42"},
            {"destination_id", "FNBCC01"},
            {"filler_2", ""},
            {"business_date", "20260722"}});

    private static final Map<String, String> DETAIL_VALUES = orderedMap(new String[][]{
            {"record_type", "01"},
            {"sequence", "000001"},
            {"action_code", "CRE"},
            {"mandate_ref", "FNBM00000000000001"},
            {"contract_ref", "CT0000000001"},
            {"creditor_account", "62681769356319023"},
            {"debtor_account", "62110001234567890"},
            {"debtor_branch", "250655"},
            {"debtor_name", "FNB REVOLVING FACILITY"},
            {"currency", "ZAR"},
            {"max_collection_amount", "000000000117710"},
            {"frequency", "MNTH"},
            {"collection_day", "25"},
            {"start_date", "20260801"},
            {"expiry_date", "20270801"},
            {"mndt_req_id", "FNBCC01-MRQ-000000000000001"}});

    @Test
    void headerMirrorsCollections109Lrecl() {
        assertEquals(109, MandateLayouts.HEADER.length());
    }

    @Test
    void detailTotalsTheAssertedSyntheticWidth() {
        assertEquals(285, MandateLayouts.DETAIL.length());
    }

    @Test
    void headerSliceRoundTripsEveryField() {
        final String line = render(MandateLayouts.HEADER, HEADER_VALUES);
        assertEquals(MandateLayouts.HEADER.length(), line.length());
        HEADER_VALUES.forEach((field, value) ->
                assertEquals(value, MandateLayouts.HEADER.slice(line, field).strip(), field));
    }

    @Test
    void detailSliceRoundTripsEveryField() {
        final String line = render(MandateLayouts.DETAIL, DETAIL_VALUES);
        assertEquals(MandateLayouts.DETAIL.length(), line.length());
        DETAIL_VALUES.forEach((field, value) ->
                assertEquals(value, MandateLayouts.DETAIL.slice(line, field).strip(), field));
    }

    @Test
    void detailPinsTheToolkitMirroredWidths() {
        // Widths mirrored from generate_dcre_mandates.py DDL; the rest are SYNTHETIC (A-61).
        final String line = render(MandateLayouts.DETAIL, DETAIL_VALUES);
        assertEquals(35, MandateLayouts.DETAIL.slice(line, "mandate_ref").length());
        assertEquals(14, MandateLayouts.DETAIL.slice(line, "contract_ref").length());
        assertEquals(3, MandateLayouts.DETAIL.slice(line, "currency").length());
        assertEquals(3, MandateLayouts.DETAIL.slice(line, "action_code").length());
        assertEquals(8, MandateLayouts.DETAIL.slice(line, "start_date").length());
        assertEquals(8, MandateLayouts.DETAIL.slice(line, "expiry_date").length());
        assertEquals(35, MandateLayouts.DETAIL.slice(line, "mndt_req_id").length());
    }

    @Test
    void unknownFieldFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> MandateLayouts.DETAIL.slice(" ".repeat(285), "end_to_end"));
    }

    /** Renders values into one fixed-width line by left-padding each field with spaces. */
    private static String render(final FixedWidthLayout layout, final Map<String, String> values) {
        final StringBuilder line = new StringBuilder();
        values.forEach((field, value) -> {
            final int width = layout.slice(" ".repeat(layout.length()), field).length();
            if (value.length() > width) {
                throw new IllegalArgumentException("test value too wide for %s".formatted(field));
            }
            line.append(value).append(" ".repeat(width - value.length()));
        });
        return line.toString();
    }

    private static Map<String, String> orderedMap(final String[][] entries) {
        final Map<String, String> map = new LinkedHashMap<>();
        for (final String[] entry : entries) {
            map.put(entry[0], entry[1]);
        }
        return map;
    }
}
