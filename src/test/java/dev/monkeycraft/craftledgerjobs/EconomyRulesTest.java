package dev.monkeycraft.craftledgerjobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyRulesTest {
    @Test
    void positiveFiniteAmountsAreRequiredForMoneyMovement() {
        assertTrue(EconomyRules.isPositiveFinite(0.01D));

        assertFalse(EconomyRules.isPositiveFinite(0));
        assertFalse(EconomyRules.isPositiveFinite(-1));
        assertFalse(EconomyRules.isPositiveFinite(Double.NaN));
        assertFalse(EconomyRules.isPositiveFinite(Double.POSITIVE_INFINITY));
    }

    @Test
    void balanceMathRejectsInvalidAmountsAndOverflow() {
        assertEquals(10.0D, EconomyRules.addToBalance(10.0D, -1));
        assertEquals(10.0D, EconomyRules.addToBalance(10.0D, Double.NaN));
        assertEquals(10.0D, EconomyRules.addToBalance(10.0D, Double.POSITIVE_INFINITY));
        assertEquals(Double.MAX_VALUE, EconomyRules.addToBalance(Double.MAX_VALUE, Double.MAX_VALUE));

        assertEquals(7.5D, EconomyRules.addToBalance(5.0D, 2.5D));
        assertTrue(EconomyRules.canAddToBalance(5.0D, 2.5D));
        assertFalse(EconomyRules.canAddToBalance(Double.MAX_VALUE, Double.MAX_VALUE));
    }

    @Test
    void subtractClampsToZero() {
        assertEquals(7.0D, EconomyRules.subtractFromBalance(10.0D, 3.0D));
        assertEquals(0.0D, EconomyRules.subtractFromBalance(10.0D, 99.0D));
        assertEquals(10.0D, EconomyRules.subtractFromBalance(10.0D, Double.NaN));
    }

    @Test
    void withdrawRequiresEnoughBalance() {
        assertTrue(EconomyRules.canWithdraw(10.0D, 10.0D));
        assertFalse(EconomyRules.canWithdraw(10.0D, 10.01D));
        assertFalse(EconomyRules.canWithdraw(10.0D, Double.NaN));
    }
}
