# 交接：分支 `claude/blissful-tesla-4gax8y`

本文件只服务于这条分支的交接：按第二节把 wiki 更新完、第一节的本地检查通过后，删除本文件。它不是规则，规则仍以 `CLAUDE.md` 为准。Guzhenren 的同名分支有自己的一份 `HANDOFF.md`，两边一起处理。

这条分支包含 `claude/pensive-bell-rwd5he` 的全部云端重构，以及本分支自己的改动。原先写在 `CLAUDE.md` 里的 "Pending local check" 段已经挪到本文件。

## 一、本地先跑的检查

云端已通过：

- 编译通过。
- `runData` 逐字节一致：
  - `pensive-bell` 那批重构，是从空的 `src/generated` 重新生成的。
  - 本分支的搬包和 key 精简。
- 南疆河床的规则改动只改了 `noise_settings/southern_border.json`。结构比较确认，只是在地貌岩带外面多包了一层"柱子是干的"条件，同时去掉了表土里重复的水检查。
- 和 Guzhenren 一起开服冒烟：三个维度加载，南疆强制加载 81 个区块，生成时没有报错。
- 复刻群系按实体类型归类刷怪：`runData` 只改了 `bamboo_jungle.json`（豹猫从 monster 移到 creature）；开服日志里不再有 MONSTER 分类的警告。
- 第二轮：
  - 语言表合并、命名统一、常量推导、`RegionBorders` 合并循环，`runData` 都逐字节一致。
  - 只省计算的三处改动（见第二节第 2 条）改变了 JSON 的结构，不改数值：
    - `top.json` 地貌区内的分支与原来整条 TOP 链逐节点相同。
    - 其余是把内联的子树换成对新注册函数的引用，新函数的内容就是原来内联的那棵子树。

云端跑不了：L2 JUnit、GameTest、客户端。本地按顺序做：

1. 两个仓库都检出 `claude/blissful-tesla-4gax8y`。
2. 运行 `python3 -B ../mcmod-guzhenren/tools/check.py --project guworld`。
3. 本地测试要跟进的改名和移动：
   - `pensive-bell` 那批：
     - `SouthernBorder.SEA_LEVEL` 改为 `Rivers.SEA_LEVEL`。
     - 形状函数改为接收区域中线：`getShapes` 现在在 `LandformShapes` 上。
     - `Replicas`、`Ports`、南疆群系类、`TreasureYellowHeaven`、`SouthernBorder` 上的 `addTranslations` 已删除。
   - 本分支，南疆各类移进 `dimension.southernborder` 并去掉前缀：
     - `SouthernBorderTerrain` → `Terrain`
     - `SouthernBorderRivers` → `Rivers`，嵌套 record `Rivers` → `Rivers.Network`
     - `SouthernBorderMountains` → `Mountains`
     - `SouthernBorderLandforms` → `Landforms`
     - `SouthernBorderShapes` → `LandformShapes`
     - `SouthernBorderBiomes` → `BiomeLayout`
     - `SouthernBorderBiomeSettings` → `BiomeSettings`
     - `SouthernBorderSurface` → `Surface`
     - `SouthernBorder` 类名不变，也移到这个包。
   - `DensityShapes`、`HeightfieldRouter` 移到 `terrain` 包，并改为公开类。
   - `Rivers`、`Landforms` 改为包私有。
   - noise 和 density function 的 key 改由 `SouthernBorder.noiseKey`、`functionKey`、`landformNoiseKey`、`landformFunctionKey` 统一生成，各类的私有副本已删除。
   - `Replicas` 的刷怪表不再原样照搬原版：每条刷怪项放到实体类型自己的分类下。测试里如果拿复刻群系和原版逐分类比较刷怪，竹林要按这个规则比。
   - 第二轮：
     - `EnUsLanguageProvider`、`ZhCnLanguageProvider`、`ZhTwLanguageProvider` 合并进 `datagen.lang.Translations(output, Language)`。
     - `BiomeLayout.RIVER_WEIRDNESS` 移到 `Rivers.RIVER_WEIRDNESS`。
     - `Landforms.Cliffs.weight()` 改为 `zoneWeight()`，`Landforms.getWeight` 改为 `getZoneWeight`。
     - `DensityShapes.getSoftCap` 新增；`RegionBorders.applySize` 改为 `applySizes`。
     - 新注册的 density function：`southern_border/river_warp_x`、`river_warp_z`、`stream_a`、`stream_b`、`variant`。测试里如果数过函数个数或按 id 列表比较，要加上它们。
4. 进游戏看南疆地貌区里的河床和湖底：应该是沙、砾石、粘土，崖壁仍是岩带。顺便看一眼地形和河流与上一版一致（第二轮只改了计算方式）。
5. 把结果报告给 Alex，等 Alex 合并。

## 二、wiki 修改建议

云端看不到 wiki，下面按页面主题写，请对应到实际页面。

1. **《南疆》**：地貌区（丹霞、张石岩崖、砂岩柱、喀斯特）的岩带只铺在上方没有水的柱子上。河床和湖底走移植群系与原版规则，表层是沙、砾石、粘土，下方是石头。紧邻水面的崖壁上方没有水，岩带不受影响。
2. **《Gu World 维护说明》**
   - 包结构：
     - `dimension` 放 `TreasureYellowHeaven` 和 `RegionBorders`。
     - `dimension.southernborder` 放南疆区域的全部部件。
     - `terrain` 放与区域无关的 `HeightfieldRouter`、`DensityShapes`。
   - 依赖方向为 `dimension → dimension.southernborder → terrain`。
   - 南疆的 noise 和 density function key 都由区域入口类生成。
   - 测试一节里提到的类名按第一节第 3 条更新。
   - 复刻群系：气候、颜色、音乐、地物沿用原版；不雕刻洞穴；刷怪项按实体类型自己的分类放（原版竹林把豹猫放在 monster 下，复刻版放在 creature 下，不占敌对生物上限）。
   - 语言表：一个 `Translations` 类，按语言各跑一次；每行并排写英文和简中，繁中由 `TraditionalGlyphs` 自动转换。
   - 南疆的计算共享：
     - 地貌区外（区权重恰为 0）TOP 直接读 FOOT。
     - 两个扭曲噪声和各条河的中心采样都注册成共享函数，一列只算一次。
     - variant 噪声也注册成一份共享函数。
     - 原则：一个函数在多处读，就注册成共享函数，因为只有共享的 holder 才共用缓存。
   - 命名：地貌区权重统一叫 `zoneWeight`；"relief" 只指群系表的分带轴，南疆的这根轴叫 elevation，原版路由里的槽位叫 erosion。
3. **《世界》**：如果页面里列了 Gu World 的类名，也按上面更新。

## 三、待办

1. `feature.SpiritSpringFeature`、`biome.BiomeTable` 的类名重复了包名（命名规则 4），在本地用 IDEA 重命名，测试一起跟着改。
2. `runData` 日志里有两条 ERROR：移植自 Terralith 的 `guworld:sakura/birch`、`sakura/birch_sparse` 在 `guworld:peach_grove` 里缺 `BiomeFilter.biome()`。这是 Terralith 原数据的样子，本分支没动；要不要在移植脚本里补上，留给 Alex 定。
3. Alex 定为暂不做的两项：`Landforms.getMix` 合成一个加权和（浮点舍入会变）、群系高度带边界改由 y 90/150/220/280 推导（边界会微移）。`HeightfieldRouter.getWall` 内联的几份互斥，每个采样点只算一份，不用改。
