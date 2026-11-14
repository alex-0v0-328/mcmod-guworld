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
 * 宝黄天[Treasure Yellow Heaven]的客户端天空效果。
 *
 * <ul>
 *   <li>仅客户端订阅（{@code Dist.CLIENT}），专用服务端不加载；{@link #onRegisterDimensionSpecialEffects} 在模组总线以
 *       {@link TreasureYellowHeaven#EFFECTS}（维度类型 {@code effects} 所填 ID）注册。</li>
 *   <li>{@code NORMAL} 天空留原版云、日月、昼夜，天和雾取群系黄色：{@code FogRenderer} 把 {@code fog_color} 传给
 *       {@link #getBrightnessDependentFogColor}，黄色只在 {@link TreasureYellowHeaven} 群系调，本类只按日光调暗。</li>
 * </ul>
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
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
