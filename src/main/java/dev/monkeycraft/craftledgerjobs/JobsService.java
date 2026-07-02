package dev.monkeycraft.craftledgerjobs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
        String oldJob = ledger.players().job(player);
        ledger.players().clearJob(player);
        ledger.transactions().write("job_leave", player, 0, oldJob == null ? "" : oldJob);
    }

    public String listJobs(String currentJob, int page) {
        return JobViews.list(ledger.jobsConfig(), currentJob, page);
    }

    public String info(String jobId, int page) {
        return JobViews.info(ledger.jobsConfig(), ledger.common(), jobId, page);
    }

    public String info(String jobId) {
        return info(jobId, 1);
    }

    public void handleBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
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
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (event.getState().getBlock() instanceof CropBlock crop && !crop.isMaxAge(event.getState())) {
            return;
        }
        Double payout = job.blockBreak.get(blockId.toString());
        pay(player, payout, "job_block_break", blockId.toString());
    }

    public void handleLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
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
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
        Double payout = job.entityKill.get(entityId.toString());
        pay(player, payout, "job_entity_kill", entityId.toString());
    }

    private void pay(ServerPlayer player, Double payout, String type, String detail) {
        if (payout == null || !EconomyRules.isPositiveFinite(payout)) {
            return;
        }
        if (!ledger.players().canDeposit(player, payout)) {
            return;
        }
        String payoutKey = type + ":" + detail;
        if (!payoutLimiter.allow(player.getUUID(), payoutKey, payout, ledger.jobsConfig())) {
            return;
        }
        if (!ledger.players().deposit(player, payout)) {
            return;
        }
        ledger.transactions().write(type, player, payout, detail);
        if (ledger.jobsConfig().notifyPayouts) {
            player.sendSystemMessage(TextUtil.success("Job payout: " + ledger.common().format(payout)));
        }
    }

    public enum JoinResult {
        SUCCESS,
        UNKNOWN_JOB,
        ALREADY_IN_JOB,
        SWITCHING_DISABLED
    }
}
