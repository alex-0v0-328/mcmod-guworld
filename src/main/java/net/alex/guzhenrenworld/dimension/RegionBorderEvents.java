package net.alex.guzhenrenworld.dimension;

import net.alex.guzhenrenworld.GuWorld;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * {@link RegionBorders} 的事件转交类。{@link #onServerStarted} 在原版把主世界存档边界应用到各维度后触发；
 * {@link #onPlayerLoggedIn}、{@link #onPlayerChangedDimension}、{@link #onPlayerRespawn} 在原版发出主世界边界后触发。
 *
 * @author Alex
 * @version 1.0.0
 * @see RegionBorders
 * @since 1.0.0
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
