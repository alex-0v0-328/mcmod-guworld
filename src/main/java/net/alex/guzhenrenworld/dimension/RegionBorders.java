package net.alex.guzhenrenworld.dimension;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.slf4j.Logger;

/**
 * Gives each finite region its own square world border around 0, 0, of the side {@link #SIZES} lists.
 *
 * <p>Vanilla treats the border as the overworld's alone: every other level's border follows the overworld's through
 * a delegate listener, the overworld's saved settings are applied to all of them only after every level has loaded,
 * {@code /worldborder} changes the overworld's, and players are sent the overworld's border whatever level they stand
 * in. So {@link RegionBorderEvents} calls in here:
 * <ul>
 *   <li>{@link #start} once the server has started, after vanilla is done: it applies the region sizes, logs each
 *   one, and adds a listener after vanilla's on the overworld border that applies them again and resends them
 *   whenever an overworld change has run through the delegates and reached players.</li>
 *   <li>{@link #sendBorder} after vanilla has sent the overworld's border on login, dimension change and respawn: a
 *   player in a region gets that region's border instead; a player leaving a region gets the overworld's from vanilla
 *   as usual.</li>
 * </ul>
 * Damage, warning distance and warning time stay the overworld's, as vanilla delegates them. The sizes are
 * never saved: they come from code on every start, so a changed size takes effect on the next start.
 *
 * <p>⚠ Region borders are server-wide constants: a {@code /worldborder} command changes the overworld's
 * border only, and the regions keep theirs.
 *
 * @author Alex
 * @version 1.0.0
 * @see SouthernBorder
 * @since 1.0.0
 */

public final class RegionBorders {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceKey<Level>, Double> SIZES =
            Map.of(SouthernBorder.LEVEL, SouthernBorder.BORDER_SIZE);

    private RegionBorders() {}

    public static void start(MinecraftServer server) {
        for (ServerLevel level : applySizes(server)) {
            ResourceKey<Level> key = level.dimension();
            LOGGER.info("World border of {} set to {} blocks", key.location(), SIZES.get(key));
        }
        server.overworld().getWorldBorder().addListener(new Reapplier(server));
    }

    public static void sendBorder(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!SIZES.containsKey(serverPlayer.level().dimension())) return;
        WorldBorder border = serverPlayer.serverLevel().getWorldBorder();
        serverPlayer.connection.send(new ClientboundInitializeBorderPacket(border));
    }

    private static List<ServerLevel> applySizes(MinecraftServer server) {
        List<ServerLevel> applied = new ArrayList<>();
        SIZES.forEach((key, size) -> {
            ServerLevel level = server.getLevel(key);
            if (level == null) return;
            WorldBorder border = level.getWorldBorder();
            border.setCenter(0.0, 0.0);
            border.setSize(size);
            applied.add(level);
        });
        return applied;
    }

    private static void reapply(MinecraftServer server) {
        for (ServerLevel level : applySizes(server)) level.players().forEach(RegionBorders::sendBorder);
    }

    private record Reapplier(MinecraftServer server) implements BorderChangeListener {

        @Override
        public void onBorderSizeSet(WorldBorder border, double size) { reapply(server); }

        @Override
        public void onBorderSizeLerping(WorldBorder border, double oldSize, double newSize, long time) {
            reapply(server);
        }

        @Override
        public void onBorderCenterSet(WorldBorder border, double x, double z) { reapply(server); }

        @Override
        public void onBorderSetWarningTime(WorldBorder border, int warningTime) {}

        @Override
        public void onBorderSetWarningBlocks(WorldBorder border, int warningBlocks) {}

        @Override
        public void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock) {}

        @Override
        public void onBorderSetDamageSafeZOne(WorldBorder border, double damageSafeZone) {}
    }
}
