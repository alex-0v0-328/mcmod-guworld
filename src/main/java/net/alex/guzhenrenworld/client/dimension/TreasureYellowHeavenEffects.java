package net.alex.guzhenrenworld.client.dimension;

import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.TreasureYellowHeaven;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 宝黄天的客户端维度天空与雾气视觉效果。
 * <ul>
 *   <li>仅在客户端加载（{@code Dist.CLIENT}），由 {@link #onRegisterDimensionSpecialEffects} 在模组总线上绑定至维度类型
 *       配置的 {@link TreasureYellowHeaven#EFFECTS} 标识。</li>
 *   <li>使用 {@code NORMAL} 天空类型以保留原版云层、日月与昼夜交替；天空与雾气的黄色基调统一在
 *       {@link TreasureYellowHeaven}群系音效与颜色中定义，{@link #getBrightnessDependentFogColor} 仅负责按当前日光亮度
 *       等比压暗雾色。</li>
 * </ul>
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID, value = Dist.CLIENT)
public class TreasureYellowHeavenEffects extends DimensionSpecialEffects {

    private static final float CLOUD_HEIGHT = 192.0F;
    private static final boolean HAS_GROUND = false;
    private static final boolean FORCE_BRIGHT_LIGHTMAP = false;
    private static final boolean CONSTANT_AMBIENT_LIGHT = false;

    public TreasureYellowHeavenEffects() {
        super(CLOUD_HEIGHT, HAS_GROUND, SkyType.NORMAL, FORCE_BRIGHT_LIGHTMAP, CONSTANT_AMBIENT_LIGHT);
    }

    @SubscribeEvent
    public static void onRegisterDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(TreasureYellowHeaven.EFFECTS, new TreasureYellowHeavenEffects());
    }

    @Override
    public @NotNull Vec3 getBrightnessDependentFogColor(@NotNull Vec3 fogColor, float brightness) {
        return fogColor.scale(brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }
}
