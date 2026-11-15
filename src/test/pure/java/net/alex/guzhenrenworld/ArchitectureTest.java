package net.alex.guzhenrenworld;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import java.util.Arrays;
import java.util.Set;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 主代码类的包依赖规则：各域包（{@code dimension} 下除 {@code region} 外的直接子包）互不依赖，{@code dimension.region}
 * 不依赖它们；共用部件（{@code biome}）与水系不依赖 {@code dimension}；只有数据生成提供器碰 Minecraft 数据生成；
 * 公共代码不碰客户端类；二级包间无环，根包类（{@code dimension} 等建在其上）自成一片。
 */
@AnalyzeClasses(packages = "net.alex.guzhenrenworld", importOptions = ArchitectureTest.MainOutputOnly.class)
class ArchitectureTest {

    private static final String ROOT_PACKAGE = "net.alex.guzhenrenworld";
    private static final String ROOT = ROOT_PACKAGE + ".";
    private static final String DIMENSION = ROOT + "dimension.";
    private static final String SHARED_REGION_PACKAGE = DIMENSION + "region";
    private static final Set<String> DATA_GENERATION_CLASSES = Set.of(
            "net.minecraft.data.worldgen.BootstrapContext",
            "net.minecraft.data.worldgen.SurfaceRuleData",
            "net.minecraft.data.worldgen.biome.OverworldBiomes",
            "net.minecraft.data.worldgen.features.VegetationFeatures",
            "net.minecraft.data.worldgen.placement.PlacementUtils",
            "net.minecraft.data.worldgen.placement.VegetationPlacements",
            "net.minecraft.data.registries.VanillaRegistries");
    private static final DescribedPredicate<JavaClass> CLIENT_ONLY = resideInAPackage(ROOT + "client..")
            .as("are client-only");
    private static final DescribedPredicate<JavaClass> DATA_GENERATION_HELPERS = DescribedPredicate.describe(
            "are data generation helpers", javaClass -> DATA_GENERATION_CLASSES.contains(javaClass.getName()));
    private static final SliceAssignment REGIONS = new SliceAssignment() {

        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            String region = getRegion(javaClass.getPackageName());
            return region == null ? SliceIdentifier.ignore() : SliceIdentifier.of(region);
        }

        @Override
        public String getDescription() { return "the region packages"; }
    };
    private static final SliceAssignment SECOND_LEVEL_PACKAGES = new SliceAssignment() {

        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            String packageName = javaClass.getPackageName();
            if (packageName.equals(ROOT_PACKAGE)) return SliceIdentifier.of("(root)");
            if (!packageName.startsWith(ROOT)) return SliceIdentifier.ignore();

            String[] parts = packageName.substring(ROOT.length()).split("\\.");
            return SliceIdentifier.of(String.join(".", Arrays.copyOf(parts, Math.min(2, parts.length))));
        }

        @Override
        public String getDescription() { return "second-level packages"; }
    };

    @ArchTest
    static final ArchRule regionsDoNotDependOnEachOther = slices().assignedFrom(REGIONS).should().notDependOnEachOther()
            .because("a region shares only what lives in dimension.region and biome; a fifth region is a new package "
                    + "and one line in Regions");
    @ArchTest
    static final ArchRule theSharedRegionPackageKnowsNoRegionAndNothingElseInDimension = noClasses()
            .that().resideInAPackage(SHARED_REGION_PACKAGE + "..")
            .should().dependOnClassesThat(resideInAPackage(DIMENSION.substring(0, DIMENSION.length() - 1) + "..")
                    .and(not(resideInAPackage(SHARED_REGION_PACKAGE + ".."))))
            .because("a region depends on dimension.region; the other way round would close a package cycle");
    @ArchTest
    static final ArchRule biomeAndWaterwaysStayBelowDimension = noClasses()
            .that().resideInAnyPackage(ROOT + "biome..", ROOT + "waterways..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + "dimension..")
            .because("the regions wrap their terrain in the waterways and fill the shared biome plumbing, never the "
                    + "other way round");
    @ArchTest
    static final ArchRule nothingDependsOnDatagen = noClasses().that().resideOutsideOfPackage(ROOT + "datagen..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + "datagen..")
            .because("datagen is a leaf that runs only under runData");
    @ArchTest
    static final ArchRule onlyDatagenReachesDataProviders = noClasses()
            .that().resideOutsideOfPackage(ROOT + "datagen..")
            .should().dependOnClassesThat(resideInAPackage("net.minecraft.data..").and(not(DATA_GENERATION_HELPERS)))
            .because("only the providers write data; the helpers are the bootstrap context and the vanilla biome and "
                    + "feature builders every function package registers its entries with, and the vanilla registry "
                    + "lookup that biome.Ports reads under runData");
    @ArchTest
    static final ArchRule commonCodeNeverReachesClientClasses = noClasses().that(not(CLIENT_ONLY).as("are common"))
            .should().dependOnClassesThat(CLIENT_ONLY.or(resideInAPackage("net.minecraft.client.."))
                    .or(resideInAPackage("com.mojang.blaze3d.."))
                    .or(resideInAPackage("net.neoforged.neoforge.client..")))
            .because("a dedicated server must load every common class without client classes");
    @ArchTest
    static final ArchRule secondLevelPackagesFormNoCycle = slices().assignedFrom(SECOND_LEVEL_PACKAGES)
            .should().beFreeOfCycles();

    private static String getRegion(String packageName) {
        if (!packageName.startsWith(DIMENSION)) return null;

        String region = packageName.substring(DIMENSION.length()).split("\\.")[0];
        return region.equals("region") ? null : region;
    }

    /** 只导入主代码输出：pureTest 类路径还带着同名包下它自己的类。 */
    static final class MainOutputOnly implements ImportOption {

        @Override
        public boolean includes(Location location) {
            return location.contains("/classes/java/main/");
        }
    }
}
