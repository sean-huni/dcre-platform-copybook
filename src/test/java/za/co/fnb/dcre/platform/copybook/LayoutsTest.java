package za.co.fnb.dcre.platform.copybook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LayoutsTest {

    // The attested sample lines from SPEC-DATA-MODEL section 3 (values only).
    static final String HEADER =
        "0002DCRERF2026071112000002             202607111200 00000000000030FNBRF01                            20260711                                                            ";
    static final String DETAIL =
        "01FNBRF00001-62681769356319023       62681769356319023      CT0000000001  ZAR000000000117710250205     FNB REVOLVING FACILITY             62681769356319023      DDA RCUR";
    // V3 = the V2 body (169) plus a trailing mandate_ref(35), left-aligned space-filled.
    static final String MANDATE_REF = "MND0000000042";
    static final String DETAIL_V3_LINE = DETAIL + pad(MANDATE_REF, 35);

    private static String pad(final String value, final int width) {
        return value + " ".repeat(width - value.length());
    }

    @Test
    void headerFieldsSliceToAttestedValues() {
        assertEquals(109, Layouts.HEADER.length());
        assertEquals("0002", Layouts.HEADER.slice(HEADER, "record_type"));
        assertEquals("DCRE", Layouts.HEADER.slice(HEADER, "sender_id"));
        assertEquals("RF", Layouts.HEADER.slice(HEADER, "file_type"));
        assertEquals("20260711120000", Layouts.HEADER.slice(HEADER, "created_ts"));
        assertEquals("02", Layouts.HEADER.slice(HEADER, "layout_version"));
        assertEquals("FNBRF01", Layouts.HEADER.slice(HEADER, "destination_id").strip());
        assertEquals(30, Integer.parseInt(Layouts.HEADER.slice(HEADER, "tx_count").strip()));
    }

    @Test
    void detailV2SlicesRoleNamedFields() {
        assertEquals(169, Layouts.DETAIL_V2.length());
        assertEquals(DETAIL.length(), Layouts.DETAIL_V2.length());
        assertEquals("01", Layouts.DETAIL_V2.slice(DETAIL, "record_type"));
        assertEquals("62681769356319023", Layouts.DETAIL_V2.slice(DETAIL, "creditor_account").strip());
        assertEquals("ZAR", Layouts.DETAIL_V2.slice(DETAIL, "currency"));
        assertEquals("62681769356319023", Layouts.DETAIL_V2.slice(DETAIL, "debtor_account").strip());
        assertEquals("DDA RCUR", Layouts.DETAIL_V2.slice(DETAIL, "acc_type_seq"));
    }

    @Test
    void detailV3AppendsMandateRefAndKeepsV2Body() {
        assertEquals(204, Layouts.DETAIL_V3.length());
        assertEquals(DETAIL_V3_LINE.length(), Layouts.DETAIL_V3.length());
        // trailing mandate_ref slices to the attested value
        assertEquals(MANDATE_REF, Layouts.DETAIL_V3.slice(DETAIL_V3_LINE, "mandate_ref").strip());
        // the V2 body is a byte-exact prefix: every V2 field slices identically under V3
        assertEquals("01", Layouts.DETAIL_V3.slice(DETAIL_V3_LINE, "record_type"));
        assertEquals("62681769356319023", Layouts.DETAIL_V3.slice(DETAIL_V3_LINE, "creditor_account").strip());
        assertEquals("ZAR", Layouts.DETAIL_V3.slice(DETAIL_V3_LINE, "currency"));
        assertEquals("62681769356319023", Layouts.DETAIL_V3.slice(DETAIL_V3_LINE, "debtor_account").strip());
        assertEquals("DDA RCUR", Layouts.DETAIL_V3.slice(DETAIL_V3_LINE, "acc_type_seq"));
        assertEquals("CT0000000001", Layouts.DETAIL_V3.slice(DETAIL_V3_LINE, "contract_ref").strip());
    }

    @Test
    void olderLayoutsUnchangedByV3() {
        // V1/V2 lengths are frozen so older files still parse after V3 lands
        assertEquals(161, Layouts.DETAIL_V1.length());
        assertEquals(169, Layouts.DETAIL_V2.length());
        assertEquals("62681769356319023", Layouts.DETAIL_V2.slice(DETAIL, "debtor_account").strip());
    }

}
