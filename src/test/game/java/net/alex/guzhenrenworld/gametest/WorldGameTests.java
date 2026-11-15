package net.alex.guzhenrenworld.gametest;

import java.util.List;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.alex.guzhenrenworld.feature.SpiritSpringBasin;
import net.alex.guzhenrenworld.feature.SpiritSpringGround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 元泉[Spirit Spring]世界生成运行时检查。{@link SpiritSpringBasin#placeStructure} 铺方解石池底与中心柱、清空一圈、
 * 放主模组元泉液体源，保留格不动；{@link SpiritSpringGround#findSurfaceGround} 接受干燥平地，拒绝与地面齐平的水、
 * 冰封水坑，15×15 内地面起伏 3 格接受、4 格拒绝；群系修改器把已放置地物挂到陆地群系 {@code TOP_LAYER_MODIFICATION} 步，
 * 不挂海洋。选址测试在 17×17 模板正中露天铺 15×15 石地板：平整检查读此范围高度图，模板更小则四周屏障墙会被当成地面。
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorldGameTests {

    private static final BlockPos GROUND = new BlockPos(4, 1, 4);
    private static final BlockPos SITE = new BlockPos(8, 1, 8);
    private static final int FLOOR_RADIUS = 7;
    private static final ResourceKey<PlacedFeature> SPIRIT_SPRING =
            ResourceKey.create(Registries.PLACED_FEATURE, GuWorld.id("spirit_spring"));

    private WorldGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void springStructureLaysBasinAndSource(GameTestHelper helper) {
        BlockPos keep = GROUND.offset(-3, 0, -3);
        helper.setBlock(keep, Blocks.DIRT);

        SpiritSpringBasin.placeStructure(helper.getLevel(), helper.absolutePos(GROUND));

        helper.assertBlockPresent(Blocks.CALCITE, GROUND.below());
        helper.assertBlockPresent(Blocks.CALCITE, GROUND);
        helper.assertBlockPresent(Blocks.AIR, GROUND.north());
        helper.assertBlockPresent(Blocks.DIRT, keep);
        helper.assertBlockPresent(GuzhenrenBlocks.SPIRIT_SPRING.get(), GROUND.above());
        helper.assertTrue(helper.getBlockState(GROUND.above()).getFluidState().isSource(), "spring is not a source");
        helper.succeed();
    }

    @GameTest(template = "empty17x9x17", skyAccess = true)
    public static void springRefusesIceAndWater(GameTestHelper helper) {
        layFloor(helper);
        helper.assertTrue(isSiteFound(helper), "dry level ground is refused");

        helper.setBlock(SITE.east(3), Blocks.WATER);
        helper.assertFalse(isSiteFound(helper), "water level with the ground is accepted");

        helper.setBlock(SITE.east(3), Blocks.STONE);
        helper.setBlock(SITE.below(), Blocks.WATER);
        helper.setBlock(SITE, Blocks.ICE);
        helper.assertFalse(isSiteFound(helper), "a frozen pond is accepted");
        helper.succeed();
    }

    @GameTest(template = "empty17x9x17", skyAccess = true)
    public static void springTakesARiseOfThreeAtMost(GameTestHelper helper) {
        layFloor(helper);
        BlockPos corner = SITE.offset(FLOOR_RADIUS, 0, FLOOR_RADIUS);
        for (int offsetY = 1; offsetY <= 3; offsetY++) {
            helper.setBlock(corner.above(offsetY), Blocks.STONE);
        }
        helper.assertTrue(isSiteFound(helper), "a rise of 3 blocks is refused");

        helper.setBlock(corner.above(4), Blocks.STONE);
        helper.assertFalse(isSiteFound(helper), "a rise of 4 blocks is accepted");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void springHangsOnLandBiomesOnly(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);

        helper.assertTrue(hasSpring(biomes.getHolderOrThrow(Biomes.PLAINS)), "plains lack the spring");
        helper.assertFalse(hasSpring(biomes.getHolderOrThrow(Biomes.OCEAN)), "the ocean carries the spring");
        helper.succeed();
    }

    private static void layFloor(GameTestHelper helper) {
        for (int offsetX = -FLOOR_RADIUS; offsetX <= FLOOR_RADIUS; offsetX++) {
            for (int offsetZ = -FLOOR_RADIUS; offsetZ <= FLOOR_RADIUS; offsetZ++) {
                helper.setBlock(SITE.offset(offsetX, 0, offsetZ), Blocks.STONE);
            }
        }
    }

    private static boolean isSiteFound(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(SITE);
        return SpiritSpringGround.findSurfaceGround(helper.getLevel(), center.getX(), center.getZ()) != null;
    }

    private static boolean hasSpring(Holder<Biome> biome) {
        List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
        int step = GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal();
        return step < steps.size() && steps.get(step).stream().anyMatch(feature -> feature.is(SPIRIT_SPRING));
    }
}
