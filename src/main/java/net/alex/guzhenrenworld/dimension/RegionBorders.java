package net.alex.guzhenrenworld.dimension;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/**
 * 各有限域（{@link Regions#ALL}）的方形世界边界：中心 0, 0，边长 {@link Region#BORDER_SIZE}。
 *
 * <ul>
 *   <li>原版边界只属主世界：其余维度经委托监听器跟随主世界，存档设置待全部维度加载后才应用，{@code /worldborder}
 *       只改主世界，玩家在任何维度收到的都是主世界边界；故 {@link RegionBorderEvents} 在原版之后调进本类。</li>
 *   <li>{@link #start}：服务器启动、原版完成后应用各域尺寸并记日志，在主世界边界上原版监听器之后加监听器，主世界变动经
 *       委托传到玩家后重新应用并重发；{@link #sendBorder}：原版在登录、换维度、重生时发出主世界边界后调用；域内玩家改收
 *       本域边界，离域玩家照收原版的主世界边界。</li>
 *   <li>伤害、警告距离、警告时间沿用主世界（原版本就委托）；尺寸不存档，每次启动取自代码，改后下次启动生效。</li>
 *   <li>⚠ 域边界是全服常量：{@code /worldborder} 只改主世界，各域不变。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @see Regions
 * @since 1.0.0
 */

public final class RegionBorders {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceKey<Level>> LEVELS = Regions.ALL.stream().map(Region::level)
            .collect(Collectors.toUnmodifiableSet());

    private RegionBorders() {}

    public static void start(MinecraftServer server) {
        for (ServerLevel level : applySizes(server)) {
            ResourceKey<Level> key = level.dimension();
            LOGGER.info("World border of {} set to {} blocks", key.location(), Region.BORDER_SIZE);
        }
        server.overworld().getWorldBorder().addListener(new Reapplier(server));
    }

    public static void sendBorder(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!LEVELS.contains(serverPlayer.level().dimension())) return;
        WorldBorder border = serverPlayer.serverLevel().getWorldBorder();
        serverPlayer.connection.send(new ClientboundInitializeBorderPacket(border));
    }

    private static List<ServerLevel> applySizes(MinecraftServer server) {
        List<ServerLevel> applied = new ArrayList<>();
        for (ResourceKey<Level> key : LEVELS) {
            ServerLevel level = server.getLevel(key);
            if (level == null) continue;
            WorldBorder border = level.getWorldBorder();
            border.setCenter(0.0, 0.0);
            border.setSize(Region.BORDER_SIZE);
            applied.add(level);
        }
        return applied;
    }

    private static void reapply(MinecraftServer server) {
        for (ServerLevel level : applySizes(server)) level.players().forEach(RegionBorders::sendBorder);
    }

    private record Reapplier(MinecraftServer server) implements BorderChangeListener {

        @Override
        public void onBorderSizeSet(@NotNull WorldBorder border, double size) { reapply(server); }

        @Override
        public void onBorderSizeLerping(@NotNull WorldBorder border, double oldSize, double newSize, long time) {
            reapply(server);
        }

        @Override
        public void onBorderCenterSet(@NotNull WorldBorder border, double x, double z) { reapply(server); }

        @Override
        public void onBorderSetWarningTime(@NotNull WorldBorder border, int warningTime) {}

        @Override
        public void onBorderSetWarningBlocks(@NotNull WorldBorder border, int warningBlocks) {}

        @Override
        public void onBorderSetDamagePerBlock(@NotNull WorldBorder border, double damagePerBlock) {}

        @Override
        public void onBorderSetDamageSafeZOne(@NotNull WorldBorder border, double damageSafeZone) {}
    }
}
