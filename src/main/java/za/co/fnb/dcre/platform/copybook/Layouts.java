package za.co.fnb.dcre.platform.copybook;

import java.util.List;

/**
 * Ported field-for-field from the fixture toolkit's layout tables
 * (be/python/dcre/fnb_dcre_ctv_toolkit/generate_dcre_copybook.py), the
 * dev-normative source under R-35. Header content 109; V1 161 (synthetic,
 * fails closed in production per A-2); V2 169; V3 204 (V2 + a trailing
 * mandate_ref(35), the canonical collection-to-mandate link, M10). Role-based
 * names per R-32.
 */
public final class Layouts {

    private Layouts() {
    }

    public static final FixedWidthLayout HEADER = new FixedWidthLayout(List.of(
            new LayoutField("record_type", 4),
            new LayoutField("sender_id", 4),
            new LayoutField("file_type", 2),
            new LayoutField("created_ts", 14),
            new LayoutField("layout_version", 2),
            new LayoutField("filler_1", 13),
            new LayoutField("created_short", 13),
            new LayoutField("tx_count", 14),
            new LayoutField("destination_id", 7),
            new LayoutField("filler_2", 28),
            new LayoutField("business_date", 8)));

    private static List<LayoutField> v1Fields() {
        return List.of(
            new LayoutField("record_type", 2),
            new LayoutField("end_to_end", 35),
            new LayoutField("creditor_account", 23),
            new LayoutField("contract_ref", 14),
            new LayoutField("currency", 3),
            new LayoutField("amount", 15),
            new LayoutField("branch_code", 11),
            new LayoutField("debtor_name", 35),
            new LayoutField("debtor_account", 23));
    }

    public static final FixedWidthLayout DETAIL_V1 = new FixedWidthLayout(v1Fields());

    private static List<LayoutField> v2Fields() {
        return concat(v1Fields(), new LayoutField("acc_type_seq", 8));
    }

    public static final FixedWidthLayout DETAIL_V2 = new FixedWidthLayout(v2Fields());

    public static final FixedWidthLayout DETAIL_V3 = new FixedWidthLayout(
            concat(v2Fields(), new LayoutField("mandate_ref", 35)));

    private static List<LayoutField> concat(List<LayoutField> base, LayoutField extra) {
        java.util.ArrayList<LayoutField> all = new java.util.ArrayList<>(base);
        all.add(extra);
        return all;
    }

    static {
        assert HEADER.length() == 109;
        assert DETAIL_V1.length() == 161;
        assert DETAIL_V2.length() == 169;
        assert DETAIL_V3.length() == 204;
    }
}
