package dev.monkeycraft.craftledgerjobs;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;

@Mod(CraftLedgerJobs.MOD_ID)
public final class CraftLedgerJobs {
    public static final String MOD_ID = "craftledger_jobs";
    public static final Logger LOGGER = LogUtils.getLogger();

    private final Ledger ledger = new Ledger();

    public CraftLedgerJobs() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ledger.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ledger.stop();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CraftLedgerCommands.register(event.getDispatcher(), ledger);
    }

    @SubscribeEvent
    public void onRegisterPermissionNodes(PermissionGatherEvent.Nodes event) {
        CraftLedgerPermissions.register(event);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ledger.players().get(player);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        ledger.jobs().handleBlockBreak(event);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        ledger.jobs().handleLivingDeath(event);
    }
}
