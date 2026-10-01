# gu-world

[蛊真人](https://github.com/alex-0v0-328/guzhenren-mcmod) 的配套模组：全部维度、地形与群系都在这里。必须与蛊真人一起安装。

> 开发阶段，玩法内容尚未定型，本文不列具体内容。

## 环境与依赖

|               |                                        |
|---------------|----------------------------------------|
| Minecraft     | `1.21.1`                               |
| NeoForge      | `21.1.x`                               |
| Java          | `21`                                   |
| mod id / 包名 | `gu_world` · `net.alex.guzhenrenworld` |
| 必需依赖      | 蛊真人（`guzhenren`）                  |

精确版本以 `gradle.properties` 与 `build.gradle` 为准。

## 构建与运行

本项目编译时不依赖蛊真人。`runData` 与开发客户端运行时从兄弟目录 `../guzhenren` 加载它：它构建好的 jar，以及它 `run/mods/` 下的 Epic Fight 与 GeckoLib，所以要先在那边构建。两个模组一起的联调在蛊真人项目里进行。

```text
gradlew.bat build              # 编译、打包
gradlew.bat runData            # 重新生成数据
```

其他系统用 `./gradlew`。`runData` 的产物 `src/generated/resources` 属于源码集，provider 改动后需重新生成并提交。

## 许可

模组本体版权所有，保留所有权利。`LICENSE.txt` 是继承自 NeoForge MDK 模板的 MIT 协议，**不覆盖模组代码**。
