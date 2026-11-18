package net.alex.guzhenrenworld.dimension;

import net.alex.guzhenrenworld.GuWorld;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * {@link RegionBorders} 的 NeoForge 事件监听入口。{@link #onServerStarted} 在服务端启动且原版将主世界存档边界同步至各维
 * 度后触发，用于覆写四域边界；{@link #onPlayerLoggedIn}、{@link #onPlayerChangedDimension} 与 {@link #onPlayerRespawn}
 * 在原版向客户端发送主世界边界包后触发，为处于四域内的玩家补发本域边界包。
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class RegionBorderEvents {

    private RegionBorderEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) { RegionBorders.start(event.getServer()); }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        RegionBorders.sendBorder(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        RegionBorders.sendBorder(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        RegionBorders.sendBorder(event.getEntity());
    }
}
