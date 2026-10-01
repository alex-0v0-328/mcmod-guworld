package net.alex.guzhenrenworld;

import net.alex.guzhenrenworld.client.dimension.TreasureYellowHeavenEffects;
import net.alex.guzhenrenworld.registry.WorldDimensions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Client-only entry point, so that nothing which would crash a dedicated server sits in
 * {@link GuWorld}.
 *
 * <p>Annotated {@code @Mod(dist = Dist.CLIENT)}. It registers each dimension's special effects on the
 * mod bus under the id its dimension type names in {@code effects}.
 *
 * @author Alex
 * @version 1.0.0
 * @see GuWorld
 * @since 1.0.0
 */

@Mod(value = GuWorld.MOD_ID, dist = Dist.CLIENT)
public class GuWorldClient {

    public GuWorldClient(IEventBus modEventBus) {
        modEventBus.addListener(GuWorldClient::onRegisterDimensionSpecialEffects);
    }

    private static void onRegisterDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(WorldDimensions.TREASURE_YELLOW_HEAVEN_EFFECTS, new TreasureYellowHeavenEffects());
    }
}
