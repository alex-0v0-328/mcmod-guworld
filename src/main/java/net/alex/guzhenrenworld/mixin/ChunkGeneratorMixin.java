package net.alex.guzhenrenworld.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.alex.guzhenrenworld.secretrealm.SoulquakeClearance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 世界生成 Mixin，让荡魂山山心净空圆里的地表结构起点作废，规则见 {@link SoulquakeClearance}。
 * <ul>
 *   <li>⚠ 不用事件：NeoForge 21.1.265 没有否决结构起点的事件，结构集的 {@code exclusion_zone} 只能挂在被排斥一方。</li>
 * </ul>
 */

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(method = "createStructures", at = @At("HEAD"))
    private void guworld$rememberState(RegistryAccess registries, ChunkGeneratorStructureState state,
            StructureManager structures, ChunkAccess chunk, StructureTemplateManager templates, CallbackInfo info) {
        SoulquakeClearance.STATE.set(state);
    }

    @Inject(method = "createStructures", at = @At("RETURN"))
    private void guworld$forgetState(RegistryAccess registries, ChunkGeneratorStructureState state,
            StructureManager structures, ChunkAccess chunk, StructureTemplateManager templates, CallbackInfo info) {
        SoulquakeClearance.STATE.remove();
    }

    @WrapOperation(method = "tryGenerateStructure", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/StructureStart;isValid()Z"))
    private boolean guworld$keepSoulquakeClear(StructureStart start, Operation<Boolean> original,
            @Local(argsOnly = true) RegistryAccess registries) {
        return original.call(start) && !SoulquakeClearance.crowds(SoulquakeClearance.STATE.get(), registries, start);
    }
}
