package net.alex.guzhenrenworld.biome;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 区域群系表的测试：源群系各进其组，在两组或不在任何组时失败；塔尖格取塔尖群系，除非本格群系被保留。
 */
class BiomeTableTest {

    private static final ResourceKey<Biome> PLAINS = key("plains");
    private static final ResourceKey<Biome> RIVER = key("river");
    private static final ResourceKey<Biome> SPIRES = key("spires");
    private static final Map<ResourceKey<Biome>, List<String>> GROUPS = Map.of(
            PLAINS, List.of("minecraft:plains", "minecraft:meadow"),
            RIVER, List.of("minecraft:river"));

    @Test
    void aSourceBiomeTakesItsGroupsBiome() {
        BiomeTable table = BiomeTable.of("Test", GROUPS);
        assertEquals(PLAINS, table.getBiome(point(0.0F), ResourceLocation.parse("minecraft:meadow")));
        assertEquals(RIVER, table.getBiome(point(0.9F), ResourceLocation.parse("minecraft:river")));
    }

    @Test
    void anUnmappedOrTwiceMappedSourceFails() {
        BiomeTable table = BiomeTable.of("Test", GROUPS);
        assertThrows(IllegalStateException.class,
                () -> table.getBiome(point(0.0F), ResourceLocation.parse("minecraft:desert")));
        assertThrows(IllegalStateException.class, () -> BiomeTable.of("Test", Map.of(
                PLAINS, List.of("minecraft:plains"), RIVER, List.of("minecraft:plains"))));
    }

    @Test
    void aTableWithoutSpiresLeavesTheSpireBandAsMapped() {
        BiomeTable table = BiomeTable.of("Test", GROUPS);
        assertEquals(PLAINS, table.getBiome(point(0.9F), ResourceLocation.parse("minecraft:plains")));
    }

    @Test
    void theErrorNamesTheRegionAndTheSource() {
        BiomeTable table = BiomeTable.of("Test Region", GROUPS);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> table.getBiome(point(0.0F), ResourceLocation.parse("minecraft:desert")));
        assertEquals("no Test Region biome for minecraft:desert", failure.getMessage());
    }

    @Test
    void spireCellsTakeTheSpireBiomeExceptForTheKeptOnes() {
        BiomeTable table = BiomeTable.withSpires("Test", GROUPS, Set.of(RIVER), SPIRES);
        ResourceLocation plains = ResourceLocation.parse("minecraft:plains");
        assertEquals(PLAINS, table.getBiome(point(BiomeTable.SPIRE_EROSION - 0.01F), plains));
        assertEquals(SPIRES, table.getBiome(point(BiomeTable.SPIRE_EROSION), plains));
        assertEquals(RIVER, table.getBiome(point(0.9F), ResourceLocation.parse("minecraft:river")));
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("guworld", path));
    }

    private static Climate.ParameterPoint point(float erosion) {
        Climate.Parameter any = Climate.Parameter.span(-1.0F, 1.0F);
        return Climate.parameters(any, any, any, Climate.Parameter.point(erosion), any, any, 0.0F);
    }
}
