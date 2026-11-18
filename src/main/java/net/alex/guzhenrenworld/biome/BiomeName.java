package net.alex.guzhenrenworld.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 生物群系的英文与简体中文手写显示名记录。各域仅列出各自独有的群系显示名，由语言表生成器（
 * {@code datagen.lang.Translations}）统一汇总写出，繁体中文由简体中文自动转换生成。
 */

public record BiomeName(ResourceKey<Biome> biome, String english, String simplified) {}
