package net.alex.guzhenrenworld.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.alex.guzhenrenworld.secretrealm.SoulsunderClearance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 世界生成 Mixin，让荡魂山山心净空圆里的地表结构起点作废，规则见 {@link SoulsunderClearance}。
 * <ul>
 *   <li>⚠ 不用事件：NeoForge 21.1.265 没有否决结构起点的事件，结构集的 {@code exclusion_zone} 只能挂在被排斥一方。
 *       </li>
 *   <li>{@code createStructures} 整个方法包在 try/finally 里设、清 {@code SoulsunderClearance.STATE}：生成抛异常时线程
 *       局部变量也会清掉，不让工作线程一直攥着上一份结构状态（种子、群系源、注册表）。</li>
 * </ul>
 */

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @WrapMethod(method = "createStructures")
    private void guworld$rememberState(RegistryAccess registries, ChunkGeneratorStructureState state,
            StructureManager structures, ChunkAccess chunk, StructureTemplateManager templates,
            Operation<Void> original) {
        SoulsunderClearance.STATE.set(state);
        try {
            original.call(registries, state, structures, chunk, templates);
        } finally {
            SoulsunderClearance.STATE.remove();
        }
    }

    @WrapOperation(method = "tryGenerateStructure", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/StructureStart;isValid()Z"))
    private boolean guworld$keepSoulsunderClear(StructureStart start, Operation<Boolean> original,
            @Local(argsOnly = true) RegistryAccess registries) {
        return original.call(start) && !SoulsunderClearance.crowds(SoulsunderClearance.STATE.get(), registries, start);
    }
}
