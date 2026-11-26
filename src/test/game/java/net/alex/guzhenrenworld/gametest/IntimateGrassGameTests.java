package net.alex.guzhenrenworld.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.GuzhenrenBlocks;
import net.alex.guzhenrenworld.dimension.Regions;
import net.alex.guzhenrenworld.dimension.blissfulland.BlissfulLandBiomes;
import net.alex.guzhenrenworld.dimension.region.Region;
import net.alex.guzhenrenworld.dimension.southernborder.SouthernBorder;
import net.alex.guzhenrenworld.feature.IntimateGrass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 野生知心草世界生成运行时检查。配置地物在半草半石的 15×15 地板上只把两格高知心草放到草方块上，石地一株不长；群系修改
 * 器把放置地物挂到主世界陆地、南疆陆地与狐仙福地陆地群系的 {@code VEGETAL_DECORATION} 步，不挂海洋，也不挂
 * 别域独有的陆地群系。
 * 放置用固定种子的随机源，结果确定。
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IntimateGrassGameTests {

    private static final BlockPos SITE = new BlockPos(8, 1, 8);
    private static final int FLOOR_RADIUS = 7;
    private static final long PATCH_SEED = 20261118L;

    private IntimateGrassGameTests() {}

    @GameTest(template = "empty17x9x17", skyAccess = true)
    public static void patchGrowsOnGrassBlocksOnly(GameTestHelper helper) {
        for (int offsetX = -FLOOR_RADIUS; offsetX <= FLOOR_RADIUS; offsetX++) {
            for (int offsetZ = -FLOOR_RADIUS; offsetZ <= FLOOR_RADIUS; offsetZ++) {
                helper.setBlock(SITE.offset(offsetX, 0, offsetZ), offsetX < 0 ? Blocks.STONE : Blocks.GRASS_BLOCK);
            }
        }
        ServerLevel level = helper.getLevel();
        ConfiguredFeature<?, ?> patch = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(IntimateGrass.CONFIGURED_FEATURE);

        patch.place(level, level.getChunkSource().getGenerator(), RandomSource.create(PATCH_SEED),
                helper.absolutePos(SITE.above()));

        Block grass = GuzhenrenBlocks.INTIMATE_GRASS.get();
        int plants = 0;
        for (int offsetX = -FLOOR_RADIUS; offsetX <= FLOOR_RADIUS; offsetX++) {
            for (int offsetZ = -FLOOR_RADIUS; offsetZ <= FLOOR_RADIUS; offsetZ++) {
                BlockPos lower = SITE.offset(offsetX, 1, offsetZ);
                BlockState state = helper.getBlockState(lower);
                if (!state.is(grass)) continue;

                plants++;
                helper.assertTrue(offsetX >= 0, "a plant stands on stone at " + lower);
                helper.assertTrue(state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER,
                        "the ground cell holds an upper half at " + lower);
                BlockState upper = helper.getBlockState(lower.above());
                helper.assertTrue(upper.is(grass) && upper.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER,
                        "the plant lacks its upper half at " + lower);
            }
        }
        helper.assertTrue(plants > 0, "the patch places no plant on grass");
        helper.succeed();
    }

    @GameTest(template = "empty9x9x9")
    public static void patchHangsOnMainlandSouthernBorderAndBlissfulLandOnly(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        helper.assertTrue(hasPatch(biomes.getHolderOrThrow(Biomes.PLAINS)), "plains lack the patch");
        helper.assertFalse(hasPatch(biomes.getHolderOrThrow(Biomes.OCEAN)), "the ocean carries the patch");

        Set<ResourceKey<Biome>> southern = new HashSet<>(SouthernBorder.REGION.biomes().land());
        southern.addAll(BlissfulLandBiomes.GRASSLAND);
        for (ResourceKey<Biome> biome : southern) {
            helper.assertTrue(hasPatch(biomes.getHolderOrThrow(biome)), biome.location() + " lacks the patch");
        }
        for (Region region : Regions.ALL) {
            for (ResourceKey<Biome> biome : region.biomes().land()) {
                if (southern.contains(biome)) continue;

                helper.assertFalse(hasPatch(biomes.getHolderOrThrow(biome)),
                        biome.location() + " of " + region.name() + " carries the patch");
            }
        }
        helper.succeed();
    }

    private static boolean hasPatch(Holder<Biome> biome) {
        List<HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
        int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
        return step < steps.size()
                && steps.get(step).stream().anyMatch(feature -> feature.is(IntimateGrass.PLACED_FEATURE));
    }
}
