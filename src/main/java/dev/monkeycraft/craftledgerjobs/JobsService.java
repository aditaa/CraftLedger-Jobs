package dev.monkeycraft.craftledgerjobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public final class JobsService {
    private final Ledger ledger;
    private final JobPayoutLimiter payoutLimiter;

    public JobsService(Ledger ledger) {
        this.ledger = ledger;
        this.payoutLimiter = new JobPayoutLimiter(ledger.jobPayouts());
    }

    public JoinResult join(ServerPlayer player, String jobId) {
        if (!ledger.jobsConfig().enabled) {
            return JoinResult.JOBS_DISABLED;
        }
        String normalized = jobId.toLowerCase(Locale.ROOT);
        if (!ledger.jobsConfig().jobs.containsKey(normalized)) {
            return JoinResult.UNKNOWN_JOB;
        }
        String currentJob = ledger.players().job(player);
        if (normalized.equals(currentJob)) {
            return JoinResult.ALREADY_IN_JOB;
        }
        if (currentJob != null && !ledger.jobsConfig().allowSwitching) {
            return JoinResult.SWITCHING_DISABLED;
        }
        ledger.players().setJob(player, normalized);
        ledger.transactions().write("job_join", player, 0, normalized);
        return JoinResult.SUCCESS;
    }

    public void leave(ServerPlayer player) {
        if (!ledger.jobsConfig().enabled) {
            return;
        }
        String oldJob = ledger.players().job(player);
        ledger.players().clearJob(player);
        ledger.transactions().write("job_leave", player, 0, oldJob == null ? "" : oldJob);
    }

    public String listJobs(String currentJob, int page) {
        if (!ledger.jobsConfig().enabled) {
            return "Jobs are disabled.";
        }
        return JobViews.list(ledger.jobsConfig(), currentJob, page);
    }

    public String info(String jobId, int page) {
        if (!ledger.jobsConfig().enabled) {
            return "Jobs are disabled.";
        }
        return JobViews.info(ledger.jobsConfig(), ledger.common(), jobId, page);
    }

    public String info(String jobId) {
        return info(jobId, 1);
    }

    public void handleBlockBreak(Object event) {
        if (!(invoke(event, "getPlayer") instanceof ServerPlayer player)) {
            return;
        }
        if (!ledger.jobsConfig().enabled) {
            return;
        }
        String jobId = ledger.players().job(player);
        if (jobId == null) {
            return;
        }
        JobsConfig.JobDefinition job = ledger.jobsConfig().jobs.get(jobId);
        if (job == null) {
            return;
        }
        if (!(invoke(event, "getState") instanceof BlockState state)) {
            return;
        }
        ResourceLocation blockId = RegistryIds.blockId(state.getBlock());
        if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
            return;
        }
        String detail = blockId.toString();
        Double currencyPayout = job.blockBreak.get(detail);
        Integer xpPayout = job.blockBreakXp.get(detail);
        Object level = invoke(event, "getLevel", "getWorld");
        Object pos = invoke(event, "getPos");
        if (hasConfiguredPayout(currencyPayout, xpPayout)
                && ledger.jobsConfig().trackPlacedBlocks
                && level instanceof ServerLevel serverLevel
                && pos instanceof BlockPos blockPos
                && ledger.placedBlocks().consume(serverLevel, blockPos)) {
            return;
        }
        pay(player, jobId, currencyPayout, xpPayout, "job_block_break", detail);
    }

    public void handleBlockPlace(Object event) {
        if (!ledger.jobsConfig().enabled || !ledger.jobsConfig().trackPlacedBlocks) {
            return;
        }
        if (!(invoke(event, "getEntity", "getPlayer") instanceof Player)) {
            return;
        }
        Object level = invoke(event, "getLevel", "getWorld");
        Object pos = invoke(event, "getPos");
        if (level instanceof ServerLevel serverLevel && pos instanceof BlockPos blockPos) {
            ledger.placedBlocks().record(serverLevel, blockPos, ledger.jobsConfig().maxTrackedPlacedBlocks);
        }
    }

    public void handleLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!ledger.jobsConfig().enabled) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        String jobId = ledger.players().job(player);
        if (jobId == null) {
            return;
        }
        JobsConfig.JobDefinition job = ledger.jobsConfig().jobs.get(jobId);
        if (job == null) {
            return;
        }
        ResourceLocation entityId = RegistryIds.entityTypeId(event.getEntity().getType());
        String detail = entityId.toString();
        pay(player, jobId, job.entityKill.get(detail), job.entityKillXp.get(detail), "job_entity_kill", detail);
    }

    private void pay(ServerPlayer player, String jobId, Double currencyPayout, Integer xpPayout, String type, String detail) {
        int level = ledger.players().jobProgress(player, jobId).level();
        double multiplier = ledger.jobsConfig().progressionEnabled ? JobProgression.multiplier(level, ledger.jobsConfig()) : 1.0D;
        double baseCurrency = ledger.common().currencyEnabled() && currencyPayout != null && EconomyRules.isPositiveFinite(currencyPayout) ? currencyPayout : 0.0D;
        double currency = baseCurrency * multiplier;
        int xp = xpPayout == null ? 0 : Math.max(0, (int) Math.round(xpPayout * multiplier));
        if (currency <= 0 && xp <= 0) {
            return;
        }
        if (currency > 0 && !ledger.canDeposit(player, currency)) {
            return;
        }
        String payoutKey = type + ":" + detail;
        if (!payoutLimiter.allow(player.getUUID(), payoutKey, currency, ledger.jobsConfig())) {
            return;
        }
        if (currency > 0 && !ledger.deposit(player, currency)) {
            return;
        }
        if (xp > 0) {
            player.giveExperiencePoints(xp);
        }
        ledger.transactions().write(type, player, currency, detail + payoutDetail(currency, xp));
        PlayerStore.JobProgress progress = awardProgress(player, jobId, currency, xp);
        if (ledger.jobsConfig().notifyPayouts) {
            TextUtil.send(player, TextUtil.success(ledger.messages().format("job.payout", "payout", payoutMessage(currency, xp) + progressMessage(jobId, progress))));
        }
    }

    private static Object invoke(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try the next method name used by another Minecraft/Forge generation.
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalStateException("Failed to read event method " + methodName, ex);
            }
        }
        return null;
    }

    private static boolean hasConfiguredPayout(Double currencyPayout, Integer xpPayout) {
        return currencyPayout != null || xpPayout != null;
    }

    private String payoutMessage(double currency, int xp) {
        if (currency > 0 && xp > 0) {
            return ledger.common().format(currency) + " and " + xp + " XP";
        }
        if (currency > 0) {
            return ledger.common().format(currency);
        }
        return xp + " XP";
    }

    private PlayerStore.JobProgress awardProgress(ServerPlayer player, String jobId, double currency, int xp) {
        if (!ledger.jobsConfig().progressionEnabled || ledger.jobsConfig().jobXpPerPayout <= 0) {
            return ledger.players().jobProgress(player, jobId);
        }
        PlayerStore.JobProgress progress = ledger.players().addJobExperience(player, jobId, ledger.jobsConfig().jobXpPerPayout, ledger.jobsConfig());
        if (progress.leveled()) {
            ledger.transactions().write("job_level_up", player, 0, jobId + " level " + progress.level());
        }
        return progress;
    }

    private String progressMessage(String jobId, PlayerStore.JobProgress progress) {
        if (!ledger.jobsConfig().progressionEnabled) {
            return "";
        }
        String leveled = progress.leveled() ? ", " + jobId + " level " + progress.level() : "";
        return " (job XP +" + String.format(java.util.Locale.ROOT, "%.1f", progress.gainedXp()) + leveled + ")";
    }

    private static String payoutDetail(double currency, int xp) {
        if (xp <= 0) {
            return "";
        }
        if (currency <= 0) {
            return " xp " + xp;
        }
        return " xp " + xp;
    }

    public enum JoinResult {
        SUCCESS,
        JOBS_DISABLED,
        UNKNOWN_JOB,
        ALREADY_IN_JOB,
        SWITCHING_DISABLED
    }
}
