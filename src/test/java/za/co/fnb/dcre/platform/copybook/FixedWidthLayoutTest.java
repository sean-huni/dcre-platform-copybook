package za.co.fnb.dcre.platform.copybook;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The representation is the point: widths in declaration order, offsets derived.
 * Gaps and overlaps are not rejected by a validator here, they are UNEXPRESSIBLE,
 * which is the property an absolute-offset field table cannot have.
 */
class FixedWidthLayoutTest {

    private static final FixedWidthLayout LAYOUT = new FixedWidthLayout(List.of(
            new LayoutField("kind", 4),
            new LayoutField("ref", 12),
            new LayoutField("pad", 4)));

    @Test
    void derivesEachOffsetFromThePrecedingWidthsSoTheColumnsAreContiguous() {
        String line = "MHDRFNB1MB000001    ";

        assertThat(LAYOUT.length()).isEqualTo(20);
        assertThat(LAYOUT.slice(line, "kind")).isEqualTo("MHDR");
        assertThat(LAYOUT.slice(line, "ref")).isEqualTo("FNB1MB000001");
        assertThat(LAYOUT.slice(line, "pad")).isEqualTo("    ");
        assertThat(LAYOUT.slice(line, "kind") + LAYOUT.slice(line, "ref") + LAYOUT.slice(line, "pad"))
                .as("no gap and no overlap: the slices reconstruct the record exactly")
                .isEqualTo(line);
    }

    @Test
    void insertingAFieldShiftsEveryLaterFieldInsteadOfLeavingAHole() {
        FixedWidthLayout widened = new FixedWidthLayout(List.of(
                new LayoutField("kind", 4),
                new LayoutField("inserted", 3),
                new LayoutField("ref", 12),
                new LayoutField("pad", 4)));

        assertThat(widened.length()).isEqualTo(23);
        assertThat(widened.slice("MHDRXYZFNB1MB000001    ", "ref")).isEqualTo("FNB1MB000001");
    }

    @Test
    void rejectsADuplicateFieldName() {
        assertThatThrownBy(() -> new FixedWidthLayout(List.of(
                new LayoutField("ref", 4), new LayoutField("ref", 8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate field ref");
    }

    @Test
    void rejectsANonPositiveWidth() {
        assertThatThrownBy(() -> new LayoutField("ref", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-positive width");
    }

    @Test
    void rejectsABlankFieldName() {
        assertThatThrownBy(() -> new LayoutField("  ", 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field name is required");
    }

    @Test
    void failsClosedOnAnUnknownField() {
        assertThatThrownBy(() -> LAYOUT.slice(" ".repeat(20), "nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown field nope");
    }
}
