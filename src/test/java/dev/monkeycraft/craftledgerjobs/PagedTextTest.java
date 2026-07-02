package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagedTextTest {
    @Test
    void emptyRowsUseFriendlyMessage() {
        assertEquals("No shop buy items configured.", PagedText.format("Shop buy items", List.of(), 1, 8));
    }

    @Test
    void clampsRequestedPageToAvailableRange() {
        String output = PagedText.format("Rows", List.of("one", "two", "three"), 99, 2);

        assertEquals("""
                Rows (page 2/2):
                three""", output);
    }

    @Test
    void usesAtLeastOneRowPerPage() {
        String output = PagedText.format("Rows", List.of("one", "two"), 1, 0);

        assertEquals("""
                Rows (page 1/2):
                one""", output);
    }
}
