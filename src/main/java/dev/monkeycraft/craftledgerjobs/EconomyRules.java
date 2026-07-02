package dev.monkeycraft.craftledgerjobs;

final class EconomyRules {
    private EconomyRules() {
    }

    static boolean isPositiveFinite(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

    static double nonNegativeFiniteOrZero(double amount) {
        return Double.isFinite(amount) && amount > 0 ? amount : 0;
    }

    static double addToBalance(double balance, double amount) {
        if (!isPositiveFinite(amount)) {
            return balance;
        }
        double next = balance + amount;
        return Double.isFinite(next) ? next : balance;
    }

    static boolean canAddToBalance(double balance, double amount) {
        return isPositiveFinite(amount) && Double.isFinite(balance + amount);
    }

    static double subtractFromBalance(double balance, double amount) {
        if (!isPositiveFinite(amount)) {
            return balance;
        }
        return Math.max(0, balance - amount);
    }

    static boolean canWithdraw(double balance, double amount) {
        return isPositiveFinite(amount) && balance >= amount;
    }
}
