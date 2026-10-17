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
4. 进游戏看南疆地貌区里的河床和湖底：应该是沙、砾石、粘土，崖壁仍是岩带。
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
3. **《世界》**：如果页面里列了 Gu World 的类名，也按上面更新。

## 三、待办

1. 审查发现、本分支没做的：
   - 性能与 JSON 体积（改动会改变生成的 JSON，要逐项比较）：
     - 地貌权重为 0 时，`FOOT` 和 `TOP` 重复计算同一条链。
     - `variant` 和 `HeightfieldRouter.getWall` 被内联了多份。
     - 河流距离重复构建扭曲噪声：大河 5 次、溪流 10 次，应为 3 次。
   - `Landforms.getMix` 的七个手写累加器可以合并成一个加权和（浮点舍入会变，生成的 JSON 也会变）。
   - `HeightfieldRouter` 的 Javadoc 说顶部 slide "与原版一致"，实际不是。
   - 命名：
     - 地貌区权重在不同处叫 `cliffs`、`weight`、`zones`、`zoneWeight`，建议统一成 `zoneWeight`。
     - 起伏轴有 `elevation`、`relief`、`erosion` 三个名字。
     - `feature.SpiritSpringFeature`、`biome.BiomeTable` 的类名重复了包名（命名规则 4）。
   - `BiomeLayout.RIVER_WEIRDNESS` 归属：它缩放的是河岸，应该放在 `Rivers`。
   - 散落的魔数（y 50..320、300/340、72/90、66/74）可以由已有常量推导。
   - `RegionBorders` 两处应用循环可以合并（日志行为要保留）。
2. `runData` 日志里有两条 ERROR：移植自 Terralith 的 `guworld:sakura/birch`、`sakura/birch_sparse` 在 `guworld:peach_grove` 里缺 `BiomeFilter.biome()`。这是 Terralith 原数据的样子，本分支没动；要不要在移植脚本里补上，留给 Alex 定。
3. 语言表可以改成和 Guzhenren 一样的三列单表。
