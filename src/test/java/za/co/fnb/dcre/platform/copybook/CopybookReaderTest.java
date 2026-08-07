package za.co.fnb.dcre.platform.copybook;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CopybookReaderTest {

    private static final CopybookLayout LAYOUT = CopybookLayout.of(20, List.of(
            new FieldSpec("kind", 0, 4),
            new FieldSpec("ref", 4, 12),
            new FieldSpec("pad", 16, 4)));

    @Test
    void readsFieldsByOffsetWithoutTrimming() throws Exception {
        Path f = Files.createTempFile("cb", ".txt");
        Files.writeString(f, "MHDRFNB1MB000001    \n");

        List<FixedWidthRecord> rows = CopybookReader.read(f, LAYOUT);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).field("kind")).isEqualTo("MHDR");
        assertThat(rows.get(0).field("ref"))
                .as("raw and untrimmed: a trailing space is DATA. The mandate MsgId"
                        + " space-vs-zero padding defect (A-83) is invisible if the"
                        + " reader trims")
                .isEqualTo("FNB1MB000001");
    }

    @Test
    void rejectsALineShorterThanTheDeclaredRecordLength() throws Exception {
        Path f = Files.createTempFile("cb", ".txt");
        Files.writeString(f, "MHDRSHORT\n");

        assertThatThrownBy(() -> CopybookReader.read(f, LAYOUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }
}
