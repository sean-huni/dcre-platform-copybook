package za.co.fnb.dcre.platform.copybook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The reader is exercised against the RECOVERED collections tables, not an
 * invented toy layout: a collection request book is a 109-content header
 * followed by detail records of three different LRECLs. Every test below would
 * be unwritable against a reader that bound ONE layout to a whole file.
 */
class CopybookReaderTest {

    private static final String AMOUNT_RAW = "000000011771025";
    private static final String ACCOUNT = "62681769356319023";

    /** The collections file format: record 0 is the header, later records are
     *  details discriminated by LRECL. Fails closed on anything else. */
    private static final LayoutResolver COLLECTIONS = (index, line) -> {
        if (index == 0) {
            return Layouts.HEADER;
        }
        return switch (line.length()) {
            case 204 -> Layouts.DETAIL_V3;
            case 169 -> Layouts.DETAIL_V2;
            case 161 -> Layouts.DETAIL_V1;
            default -> throw new IllegalStateException(
                    "detail LRECL " + line.length() + " matches no layout");
        };
    };

    @Test
    void selectsTheLayoutPerRecordSoAHeaderAndItsDetailsCutDifferently() throws Exception {
        Path file = write(header(109), detailV2("FNBRF00001-E2E"), detailV3("FNBRF00002-E2E"));

        List<FixedWidthRecord> records = CopybookReader.read(file, COLLECTIONS);

        assertThat(records).hasSize(3);
        assertThat(records.get(0).layout()).isSameAs(Layouts.HEADER);
        assertThat(records.get(0).field("destination_id").strip()).isEqualTo("FNBRF01");
        assertThat(records.get(1).layout()).isSameAs(Layouts.DETAIL_V2);
        assertThat(records.get(1).field("end_to_end").strip()).isEqualTo("FNBRF00001-E2E");
        assertThat(records.get(2).layout()).isSameAs(Layouts.DETAIL_V3);
        assertThat(records.get(2).field("mandate_ref").strip()).isEqualTo("MND-0001");
    }

    @Test
    void aPaddedHeaderIsLengthIdenticalToADetailSoOnlyItsIndexTellsThemApart() throws Exception {
        // Generator files pad the header out to the detail LRECL; real files do
        // not. A 169-byte header and a V2 detail are indistinguishable by length,
        // which is why the resolver is handed the record INDEX as well as the line.
        Path file = write(header(169), detailV2("FNBRF00001-E2E"));

        List<FixedWidthRecord> records = CopybookReader.read(file, COLLECTIONS);

        assertThat(records.get(0).line()).hasSameSizeAs(records.get(1).line());
        assertThat(records.get(0).layout()).isSameAs(Layouts.HEADER);
        assertThat(records.get(0).field("layout_version")).isEqualTo("02");
        assertThat(records.get(0).field("record_type")).isEqualTo("0002");
        assertThat(Layouts.DETAIL_V2.slice(records.get(0).line(), "record_type"))
                .as("the DETAIL table reads different columns of the very same bytes, so a"
                        + " length-only resolver would silently mis-slice a padded header")
                .isEqualTo("00");
    }

    @Test
    void fieldsAreRawAndUntrimmed() throws Exception {
        Path file = write(header(109), detailV2("SHORT-REF"));

        List<FixedWidthRecord> records = CopybookReader.read(file, COLLECTIONS);

        assertThat(records.get(1).field("end_to_end"))
                .as("a trailing space is DATA (A-83): the mandate MsgId space-vs-zero"
                        + " padding defect is invisible to a reader that trims")
                .isEqualTo("SHORT-REF" + " ".repeat(35 - "SHORT-REF".length()));
    }

    @Test
    void decodesIso88591ByteTransparentlySoOffsetsDoNotDrift() throws Exception {
        String detail = detailV2("FNBRF00001-E2E").replace("DEBTOR NAME", "DEBTOR NÉME");
        Path file = Files.createTempFile("cb", ".txt");
        Files.write(file, (header(109) + "\n" + detail + "\n").getBytes(StandardCharsets.ISO_8859_1));

        List<FixedWidthRecord> records = CopybookReader.read(file, COLLECTIONS);

        assertThat(records.get(1).line()).hasSize(169);
        assertThat(records.get(1).field("debtor_name").strip()).isEqualTo("DEBTOR NÉME");
        assertThat(records.get(1).field("acc_type_seq"))
                .as("one byte is one char, so the LAST field still starts on its own column;"
                        + " a UTF-8 decode of 0xC3 0x89 would shift everything after it")
                .isEqualTo("DDA RCUR");
    }

    @Test
    void rejectsARecordShorterThanItsOwnLayout() throws Exception {
        Path file = write(header(109).substring(0, 80));

        assertThatThrownBy(() -> CopybookReader.read(file, COLLECTIONS))
                .isInstanceOf(ShortRecordException.class)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("109");
    }

    @Test
    void judgesTheShortGatePerRecordSoAValidHeaderDoesNotExcuseARaggedDetail() throws Exception {
        // The mandate instruction book has ONE detail layout, so its resolver is
        // positional rather than length-discriminated: a ragged detail reaches the
        // short gate and is judged against 285, never against the header's 109.
        LayoutResolver book = (index, line) -> index == 0 ? MandateLayouts.HEADER : MandateLayouts.DETAIL;
        Path file = write(" ".repeat(MandateLayouts.HEADER.length()),
                " ".repeat(MandateLayouts.DETAIL.length() - 1));

        assertThatThrownBy(() -> CopybookReader.read(file, book))
                .isInstanceOfSatisfying(ShortRecordException.class, e -> {
                    assertThat(e.recordIndex()).isEqualTo(1);
                    assertThat(e.actualLength()).isEqualTo(284);
                    assertThat(e.declaredLength())
                            .as("gated at the DETAIL length it resolved to, not at the header's 109")
                            .isEqualTo(285);
                });
    }

    @Test
    void acceptsARecordLongerThanItsLayoutAndCutsItByTheLayoutsColumns() throws Exception {
        // A padded header and a real one must ingest identically.
        List<FixedWidthRecord> padded = CopybookReader.read(write(header(169)), COLLECTIONS);
        List<FixedWidthRecord> real = CopybookReader.read(write(header(109)), COLLECTIONS);

        assertThat(padded.get(0).field("tx_count")).isEqualTo(real.get(0).field("tx_count"));
        assertThat(padded.get(0).field("business_date")).isEqualTo(real.get(0).field("business_date"));
    }

    @Test
    void forEachRecordStreamsInOrderAndReturnsTheCountWithoutHoldingTheFile() throws Exception {
        Path file = write(header(109), detailV2("FIRST"), detailV2("SECOND"), detailV3("THIRD"));

        List<String> seen = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicLong live = new java.util.concurrent.atomic.AtomicLong();
        long count = CopybookReader.forEachRecord(file, COLLECTIONS, record -> {
            live.incrementAndGet();
            seen.add(record.index() + ":" + record.line().length());
        });

        assertThat(count).isEqualTo(4);
        assertThat(seen)
                .as("records arrive in file order, one at a time")
                .containsExactly("0:109", "1:169", "2:169", "3:204");
        assertThat(live.get()).isEqualTo(4);
    }

    @Test
    void forEachRecordAppliesTheSameGatesAsRead() throws Exception {
        Path shortHeader = write(header(109).substring(0, 80));

        assertThatThrownBy(() -> CopybookReader.forEachRecord(shortHeader, COLLECTIONS, r -> { }))
                .isInstanceOf(ShortRecordException.class)
                .hasMessageContaining("109");
        assertThatThrownBy(() -> CopybookReader.forEachRecord(write(header(109)), (i, l) -> null, r -> { }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no layout resolved for record 0");
    }

    @Test
    void readAndForEachRecordAgreeRecordForRecord() throws Exception {
        Path file = write(header(169), detailV2("FIRST"), detailV3("SECOND"));

        List<FixedWidthRecord> viaRead = CopybookReader.read(file, COLLECTIONS);
        List<FixedWidthRecord> viaStream = new java.util.ArrayList<>();
        long count = CopybookReader.forEachRecord(file, COLLECTIONS, viaStream::add);

        assertThat(count).isEqualTo(viaRead.size());
        assertThat(viaStream).isEqualTo(viaRead);
    }

    @Test
    void failsClosedWhenTheResolverHasNoLayoutForARecord() throws Exception {
        Path file = write(header(109));

        assertThatThrownBy(() -> CopybookReader.read(file, (index, line) -> null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no layout resolved for record 0");
    }

    @Test
    void toStringCarriesTheIndexAndLengthAndNoAccountData() throws Exception {
        Path file = write(header(109), detailV2("FNBRF00001-E2E"));

        FixedWidthRecord detail = CopybookReader.read(file, COLLECTIONS).get(1);

        assertThat(detail.toString()).isEqualTo("FixedWidthRecord[index=1, length=169]");
        assertThat(detail.toString())
                .as("a bank-file record must not print debtor name, account or amount:"
                        + " one log.debug downstream lands them durably in the collector")
                .doesNotContain(ACCOUNT, "DEBTOR NAME", AMOUNT_RAW)
                .doesNotContain(detail.line());
    }

    // ---- fixtures, rendered FROM the layout tables so no column is hand-counted ----

    private static Path write(String... lines) throws Exception {
        Path file = Files.createTempFile("cb", ".txt");
        Files.write(file, String.join("\n", lines).concat("\n").getBytes(StandardCharsets.ISO_8859_1));
        return file;
    }

    private static String header(int paddedTo) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("record_type", "0002");
        values.put("sender_id", "DCRE");
        values.put("file_type", "RF");
        values.put("created_ts", "20260711120000");
        values.put("layout_version", "02");
        values.put("filler_1", "");
        values.put("created_short", "202607111200");
        values.put("tx_count", "30");
        values.put("destination_id", "FNBRF01");
        values.put("filler_2", "");
        values.put("business_date", "20260711");
        String line = render(Layouts.HEADER, values);
        return line + " ".repeat(paddedTo - line.length());
    }

    private static String detailV2(String endToEnd) {
        return render(Layouts.DETAIL_V2, detailValues(endToEnd));
    }

    private static String detailV3(String endToEnd) {
        Map<String, String> values = detailValues(endToEnd);
        values.put("mandate_ref", "MND-0001");
        return render(Layouts.DETAIL_V3, values);
    }

    private static Map<String, String> detailValues(String endToEnd) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("record_type", "01");
        values.put("end_to_end", endToEnd);
        values.put("creditor_account", ACCOUNT);
        values.put("contract_ref", "CT0000000001");
        values.put("currency", "ZAR");
        values.put("amount", AMOUNT_RAW);
        values.put("branch_code", "250205");
        values.put("debtor_name", "DEBTOR NAME");
        values.put("debtor_account", ACCOUNT);
        values.put("acc_type_seq", "DDA RCUR");
        return values;
    }

    /** Lays every field of the layout, in declaration order, into its own columns.
     *  The map must carry every field: a missing one would silently shift the rest. */
    private static String render(FixedWidthLayout layout, Map<String, String> values) {
        StringBuilder line = new StringBuilder();
        String blank = " ".repeat(layout.length());
        values.forEach((field, value) -> {
            int width = layout.slice(blank, field).length();
            if (value.length() > width) {
                throw new IllegalArgumentException("test value too wide for " + field);
            }
            line.append(value).append(" ".repeat(width - value.length()));
        });
        if (line.length() != layout.length()) {
            throw new IllegalStateException("fixture is " + line.length() + " of "
                    + layout.length() + " columns: a field is missing from the value map");
        }
        return line.toString();
    }
}
