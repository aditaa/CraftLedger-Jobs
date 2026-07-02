package dev.monkeycraft.craftledgerjobs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;

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

    public void handleBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
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
        ResourceLocation blockId = RegistryIds.blockId(event.getState().getBlock());
        if (event.getState().getBlock() instanceof CropBlock crop && !crop.isMaxAge(event.getState())) {
            return;
        }
        String detail = blockId.toString();
        Double currencyPayout = job.blockBreak.get(detail);
        Integer xpPayout = job.blockBreakXp.get(detail);
        if (hasConfiguredPayout(currencyPayout, xpPayout)
                && ledger.jobsConfig().trackPlacedBlocks
                && event.getLevel() instanceof ServerLevel level
                && ledger.placedBlocks().consume(level, event.getPos())) {
            return;
        }
        pay(player, currencyPayout, xpPayout, "job_block_break", detail);
    }

    public void handleBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!ledger.jobsConfig().enabled || !ledger.jobsConfig().trackPlacedBlocks) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel level) {
            ledger.placedBlocks().record(level, event.getPos(), ledger.jobsConfig().maxTrackedPlacedBlocks);
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
        pay(player, job.entityKill.get(detail), job.entityKillXp.get(detail), "job_entity_kill", detail);
    }

    private void pay(ServerPlayer player, Double currencyPayout, Integer xpPayout, String type, String detail) {
        double currency = ledger.common().currencyEnabled() && currencyPayout != null && EconomyRules.isPositiveFinite(currencyPayout) ? currencyPayout : 0.0D;
        int xp = xpPayout == null ? 0 : Math.max(0, xpPayout);
        if (currency <= 0 && xp <= 0) {
            return;
        }
        if (currency > 0 && !ledger.players().canDeposit(player, currency)) {
            return;
        }
        String payoutKey = type + ":" + detail;
        if (!payoutLimiter.allow(player.getUUID(), payoutKey, currency, ledger.jobsConfig())) {
            return;
        }
        if (currency > 0 && !ledger.players().deposit(player, currency)) {
            return;
        }
        if (xp > 0) {
            player.giveExperiencePoints(xp);
        }
        ledger.transactions().write(type, player, currency, detail + payoutDetail(currency, xp));
        if (ledger.jobsConfig().notifyPayouts) {
            player.sendSystemMessage(TextUtil.success(ledger.messages().format("job.payout", "payout", payoutMessage(currency, xp))));
        }
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
