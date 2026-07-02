package dev.monkeycraft.craftledgerjobs;

public final class JobProgression {
    private JobProgression() {
    }

    public static double multiplier(int level, JobsConfig config) {
        int safeLevel = Math.max(1, level);
        double multiplier = 1.0D + ((safeLevel - 1) * config.payoutMultiplierPerLevel);
        return Double.isFinite(multiplier) && multiplier > 0 ? multiplier : 1.0D;
    }

    public static double requiredXpForNextLevel(int level, JobsConfig config) {
        int safeLevel = Math.max(1, level);
        double required = config.baseJobXpRequired * Math.pow(config.jobXpGrowth, safeLevel - 1);
        return Double.isFinite(required) && required > 0 ? required : config.baseJobXpRequired;
    }

    public static Progress apply(int currentLevel, double currentXp, double gainedXp, JobsConfig config) {
        int level = Math.max(1, currentLevel);
        double xp = Math.max(0.0D, Double.isFinite(currentXp) ? currentXp : 0.0D);
        double gained = Math.max(0.0D, Double.isFinite(gainedXp) ? gainedXp : 0.0D);
        int maxLevel = Math.max(1, config.maxJobLevel);
        if (level >= maxLevel) {
            return new Progress(maxLevel, 0.0D, gained, false);
        }

        xp += gained;
        boolean leveled = false;
        while (level < maxLevel) {
            double required = requiredXpForNextLevel(level, config);
            if (xp < required) {
                break;
            }
            xp -= required;
            level++;
            leveled = true;
        }
        if (level >= maxLevel) {
            level = maxLevel;
            xp = 0.0D;
        }
        return new Progress(level, xp, gained, leveled);
    }

    public record Progress(int level, double xp, double gainedXp, boolean leveled) {
    }
}
