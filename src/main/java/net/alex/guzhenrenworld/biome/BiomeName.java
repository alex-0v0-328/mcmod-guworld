package net.alex.guzhenrenworld.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 群系的英文、简体中文两种手写显示名。语言表 provider（{@code datagen.lang.Translations}）写出，繁体由简体列转换；
 * 各域列出只有自己放置的群系名，provider 只汇总。
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public record BiomeName(ResourceKey<Biome> biome, String english, String simplified) {}
