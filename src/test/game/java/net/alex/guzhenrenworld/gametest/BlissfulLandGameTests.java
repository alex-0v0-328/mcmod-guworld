package net.alex.guzhenrenworld.gametest;

import java.util.function.Predicate;
import net.alex.guzhenrenworld.GuWorld;
import net.alex.guzhenrenworld.dimension.blissfulland.BoundedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 福地疆域边上的装饰视图（{@code BoundedLevel}）检查：GameTest 服务端不加载福地维度，所以在测试场地里划一条边线
 * {@link #EDGE}（相对 x 不小于它算界外），用原版的地物经视图放置。
 * <ul>
 *   <li>泉：泉眼在边线内侧一列，四面石头、只有界外一面是空的；经视图放置时泉判成没有出口、不放，界外照旧是空气；不经视
 *       图（对照）时泉放得出来。</li>
 *   <li>橡树：种在边线内第二列的泥土上（紧贴边线时原版的树干留空检查读到界外石头，树不长），树冠伸到边线，经视图放置后
 *       界外一格也没写。</li>
 *   <li>视图本身：界外读作石头、无流体，写入返回失败；往边线内侧那一列写水，写进去的是石头。</li>
 * </ul>
 */

@GameTestHolder(GuWorld.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlissfulLandGameTests {

    private static final int EDGE = 5;
    private static final BlockPos SPRING = new BlockPos(EDGE - 1, 3, 4);
    private static final BlockPos SAPLING = new BlockPos(EDGE - 2, 2, 1);

    private BlissfulLandGameTests() {}

    @GameTest(template = "empty9x9x9")
    public static void edgeDecorationKeepsTheVoidEmpty(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int edge = helper.absolutePos(new BlockPos(EDGE, 0, 0)).getX();
        Predicate<BlockPos> inside = pos -> pos.getX() < edge;
        Predicate<BlockPos> rim = pos -> pos.getX() == edge - 1;
        WorldGenLevel bounded = BoundedLevel.wrap(level, inside, rim, Blocks.STONE.defaultBlockState());
        BlockPos outside = helper.absolutePos(SPRING.east());
        helper.assertTrue(bounded.getBlockState(outside).is(Blocks.STONE) && bounded.getFluidState(outside).isEmpty()
                && !bounded.isEmptyBlock(outside), "the view does not read the outside as stone");
        helper.assertFalse(bounded.setBlock(outside, Blocks.DIRT.defaultBlockState(), 3),
                "the view writes outside");
        BlockPos pool = helper.absolutePos(SPRING.above(3));
        bounded.setBlock(pool, Blocks.WATER.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pool).is(Blocks.STONE), "water stands on the rim");
        level.setBlock(pool, Blocks.AIR.defaultBlockState(), 3);

        for (int y = SPRING.getY() - 1; y <= SPRING.getY() + 1; y++) {
            for (int z = SPRING.getZ() - 1; z <= SPRING.getZ() + 1; z++) {
                for (int x = SPRING.getX() - 1; x <= SPRING.getX(); x++) helper.setBlock(x, y, z, Blocks.STONE);
            }
        }
        BlockPos spring = helper.absolutePos(SPRING);
        helper.assertFalse(place(helper, bounded, "spring_water", spring), "a spring pours over the edge");
        helper.assertTrue(level.getBlockState(spring).is(Blocks.STONE) && level.getBlockState(outside).isAir(),
                "the edge spring changed the blocks");
        helper.assertTrue(place(helper, level, "spring_water", spring)
                && level.getFluidState(spring).is(Fluids.WATER), "the control spring does not pour");

        helper.setBlock(SAPLING.below(), Blocks.DIRT);
        helper.assertTrue(place(helper, bounded, "oak", helper.absolutePos(SAPLING)), "no oak grows at the edge");
        int leaked = 0;
        for (int y = 1; y < 9; y++) {
            for (int z = 0; z < 9; z++) {
                for (int x = EDGE; x < 9; x++) {
                    BlockState state = helper.getBlockState(new BlockPos(x, y, z));
                    if (!state.isAir() && !state.is(Blocks.WATER)) leaked++;
                }
            }
        }
        helper.assertTrue(leaked == 0, leaked + " blocks of the edge oak lie outside");
        helper.succeed();
    }

    private static boolean place(GameTestHelper helper, WorldGenLevel level, String feature, BlockPos pos) {
        ServerLevel server = helper.getLevel();
        ConfiguredFeature<?, ?> configured = server.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                        ResourceLocation.withDefaultNamespace(feature)));
        return configured.place(level, server.getChunkSource().getGenerator(), RandomSource.create(1L), pos);
    }
}
