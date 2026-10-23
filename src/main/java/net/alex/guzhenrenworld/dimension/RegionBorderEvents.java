package net.alex.guzhenrenworld.dimension;

import net.alex.guzhenrenworld.GuWorld;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Hands the server and player events {@link RegionBorders} acts on over to it.
 *
 * <p>Annotated {@code @EventBusSubscriber}. {@link #onServerStarted} fires once vanilla has applied the overworld's
 * saved border to every level; {@link #onPlayerLoggedIn}, {@link #onPlayerChangedDimension} and
 * {@link #onPlayerRespawn} fire after vanilla has sent the overworld's border.
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
