package net.alex.guzhenrenworld.client.dimension;

import net.alex.guzhenrenworld.datagen.DatapackProvider;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Client-side visual effects for the Treasure Yellow Heaven dimension.
 *
 * <p>Keeps vanilla clouds, sun, moon and the day-night cycle by using the NORMAL sky type, while the
 * sky and fog stay the dimension biome's yellow. Vanilla hands
 * {@link #getBrightnessDependentFogColor} the biome's {@code fog_color} (FogRenderer), so the yellow is
 * tuned in one place, the biome in {@link DatapackProvider}, and
 * this class only dims it with the daylight.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public class TreasureYellowHeavenEffects extends DimensionSpecialEffects {

    public TreasureYellowHeavenEffects() {
        super(192.0F, false, SkyType.NORMAL, false, false);
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
