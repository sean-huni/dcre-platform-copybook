package za.co.fnb.dcre.platform.copybook;

import java.util.List;

/**
 * SYNTHETIC-CONTRACT (A-61): the OnHost mandate instruction book copybook is
 * NOT recovered; every width below is invented-contemporary until the real
 * layout is attested. HEADER mirrors the collections 109-LRECL header shape
 * (Layouts.HEADER); DETAIL mirrors generate_dcre_mandates.py column widths
 * where the toolkit defines them (mandate_ref 35, contract_ref 14,
 * creditor/debtor account 32, debtor_branch 16, debtor_name 70, currency 3,
 * frequency 4) and picks sensible fixed widths for the rest. Detail 285.
 * Role-based names per R-32; amounts are amount-raw scale-2 (MoneyText).
 */
public final class MandateLayouts {

    private MandateLayouts() {
    }

    /** SYNTHETIC (A-61): field-for-field mirror of the collections header; count field renamed. */
    public static final FixedWidthLayout HEADER = new FixedWidthLayout(List.of(
            new LayoutField("record_type", 4),
            new LayoutField("sender_id", 4),
            new LayoutField("file_type", 2),
            new LayoutField("created_ts", 14),
            new LayoutField("layout_version", 2),
            new LayoutField("filler_1", 13),
            new LayoutField("created_short", 13),
            new LayoutField("entry_count", 14),
            new LayoutField("destination_id", 7),
            new LayoutField("filler_2", 28),
            new LayoutField("business_date", 8)));

    /**
     * SYNTHETIC (A-61) detail record. action_code is the record operation:
     * CRE (create, pain.009) | AMD (amend, pain.010) | CAN (cancel, pain.011).
     * Widths: toolkit-mirrored where marked; the rest synthetic picks
     * (sequence 6, dates yyyyMMdd 8, amount 15 as the collections amount,
     * collection_day 2, mndt_req_id 35 as the ISO 20022 MndtReqId max).
     */
    public static final FixedWidthLayout DETAIL = new FixedWidthLayout(List.of(
            new LayoutField("record_type", 2),                  // synthetic (collections detail shape)
            new LayoutField("sequence", 6),                     // synthetic, zero-padded row number
            new LayoutField("action_code", 3),                  // synthetic: CRE|AMD|CAN
            new LayoutField("mandate_ref", 35),                 // toolkit DDL VARCHAR(35)
            new LayoutField("contract_ref", 14),                // toolkit DDL VARCHAR(14)
            new LayoutField("creditor_account", 32),            // toolkit DDL VARCHAR(32)
            new LayoutField("debtor_account", 32),              // toolkit DDL VARCHAR(32)
            new LayoutField("debtor_branch", 16),               // toolkit DDL VARCHAR(16)
            new LayoutField("debtor_name", 70),                 // toolkit DDL VARCHAR(70)
            new LayoutField("currency", 3),                     // toolkit DDL CHAR(3)
            new LayoutField("max_collection_amount", 15),       // synthetic, amount-raw scale-2
            new LayoutField("frequency", 4),                    // toolkit DDL VARCHAR(4)
            new LayoutField("collection_day", 2),               // synthetic, 01-31
            new LayoutField("start_date", 8),                   // synthetic, yyyyMMdd
            new LayoutField("expiry_date", 8),                  // synthetic, yyyyMMdd or blank
            new LayoutField("mndt_req_id", 35)));               // synthetic, MndtReqId max 35

    static {
        assert HEADER.length() == 109;
        assert DETAIL.length() == 285;
    }
}
