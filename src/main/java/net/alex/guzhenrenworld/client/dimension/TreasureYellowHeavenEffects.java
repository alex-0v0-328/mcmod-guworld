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
 * Client-side visual effects for the Treasure Yellow Heaven dimension.
 *
 * <p>Annotated {@code @EventBusSubscriber(value = Dist.CLIENT)}, so nothing which would crash a dedicated
 * server loads there: {@link #onRegisterDimensionSpecialEffects} registers an instance on the mod bus under
 * {@link TreasureYellowHeaven#EFFECTS}, the id the dimension type names in {@code effects}.
 *
 * <p>Keeps vanilla clouds, sun, moon and the day-night cycle by using the NORMAL sky type, while the
 * sky and fog stay the dimension biome's yellow. Vanilla hands
 * {@link #getBrightnessDependentFogColor} the biome's {@code fog_color} (FogRenderer), so the yellow is
 * tuned in one place, the biome in {@link TreasureYellowHeaven}, and this class only dims it with the
 * daylight.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID, value = Dist.CLIENT)
public class TreasureYellowHeavenEffects extends DimensionSpecialEffects {

    public TreasureYellowHeavenEffects() {
        super(192.0F, false, SkyType.NORMAL, false, false);
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
