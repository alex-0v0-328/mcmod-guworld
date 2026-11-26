package net.alex.guzhenrenworld.dimension;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandFoxImmortal;
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
 * 有限维度的独立方形世界边界管理器，以 {@code (0, 0)} 为中心、边长按维度取（{@link #SIZES}）：四域（
 * {@link Regions#ALL}）各 {@link Region#BORDER_SIZE} 格，狐仙福地 {@link BlissfulLandFoxImmortal#BORDER_SIZE} 格。
 * <ul>
 *   <li>原版世界边界默认绑定主世界：其余维度通过委托监听器被动跟随主世界边界，{@code /worldborder} 指令也仅修改主世界，
 *       且玩家在任何维度登录、切换维度或重生时原版均下发主世界边界包；因此需由 {@link RegionBorderEvents} 在原版流程之
 *       后介入覆写。</li>
 *   <li>{@link #start} 在服务端启动完成后将各维度的服务端边界设为固定中心与边长，并在主世界边界的委托监听器之后追加
 *       {@link Reapplier}，使得每当主世界边界变更穿透到各维度后立刻重新覆写这些边界并向维度内玩家重发数据包；
 *       {@link #sendBorder}则在玩家登录、切换维度或重生后为处于这些维度内的玩家下发当前维度的边界包。</li>
 *   <li>边界伤害、安全区与警告距离仍沿用主世界的委托配置；边界尺寸不写入存档，始终以代码常量为准。</li>
 *   <li>⚠ 边界为服务端代码常量，执行 {@code /worldborder} 仅会改变主世界，这些维度的边界会被 {@link Reapplier} 立即恢
 *       复。</li>
 * </ul>
 */

public final class RegionBorders {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceKey<Level>, Double> SIZES = getSizes();

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
        for (Map.Entry<ResourceKey<Level>, Double> size : SIZES.entrySet()) {
            ServerLevel level = server.getLevel(size.getKey());
            if (level == null) continue;
            WorldBorder border = level.getWorldBorder();
            border.setCenter(0.0, 0.0);
            border.setSize(size.getValue());
            applied.add(level);
        }
        return applied;
    }

    private static Map<ResourceKey<Level>, Double> getSizes() {
        Map<ResourceKey<Level>, Double> sizes = new LinkedHashMap<>();
        Regions.ALL.forEach(region -> sizes.put(region.level(), Region.BORDER_SIZE));
        sizes.put(BlissfulLandFoxImmortal.LEVEL, BlissfulLandFoxImmortal.BORDER_SIZE);
        return Map.copyOf(sizes);
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
