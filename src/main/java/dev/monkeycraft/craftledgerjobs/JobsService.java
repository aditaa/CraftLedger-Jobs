package dev.monkeycraft.craftledgerjobs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Map;

public final class JobsService {
    private final Ledger ledger;

    public JobsService(Ledger ledger) {
        this.ledger = ledger;
    }

    public boolean join(ServerPlayer player, String jobId) {
        String normalized = jobId.toLowerCase();
        if (!ledger.jobsConfig().jobs.containsKey(normalized)) {
            return false;
        }
        ledger.players().setJob(player, normalized);
        ledger.transactions().write("job_join", player, 0, normalized);
        return true;
    }

    public void leave(ServerPlayer player) {
        String oldJob = ledger.players().job(player);
        ledger.players().clearJob(player);
        ledger.transactions().write("job_leave", player, 0, oldJob == null ? "" : oldJob);
    }

    public String listJobs() {
        StringBuilder builder = new StringBuilder("Jobs:");
        for (Map.Entry<String, JobsConfig.JobDefinition> entry : ledger.jobsConfig().jobs.entrySet()) {
            builder.append("\n").append(entry.getKey()).append(" - ").append(entry.getValue().displayName);
        }
        return builder.toString();
    }

    public String info(String jobId) {
        JobsConfig.JobDefinition job = ledger.jobsConfig().jobs.get(jobId.toLowerCase());
        if (job == null) {
            return "Unknown job: " + jobId;
        }
        StringBuilder builder = new StringBuilder(job.displayName).append(" payouts:");
        job.blockBreak.forEach((id, amount) -> builder.append("\nBreak ").append(id).append(": ").append(ledger.common().format(amount)));
        job.entityKill.forEach((id, amount) -> builder.append("\nKill ").append(id).append(": ").append(ledger.common().format(amount)));
        return builder.toString();
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
        if (payout == null || payout <= 0) {
            return;
        }
        ledger.players().deposit(player, payout);
        ledger.transactions().write(type, player, payout, detail);
        player.sendSystemMessage(TextUtil.success("Job payout: " + ledger.common().format(payout)));
    }
}
