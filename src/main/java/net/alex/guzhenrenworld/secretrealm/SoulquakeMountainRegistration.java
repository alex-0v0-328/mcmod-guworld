package net.alex.guzhenrenworld.secretrealm;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 荡魂山的代码注册入口：以 {@link SoulquakeMountain#ID} 注册结构类型 {@link #STRUCTURE_TYPE} 与结构片类型
 * {@link #PIECE_TYPE}。
 * <ul>
 *   <li>两者是本类持有的单例，注册时交出同一实例，{@link SoulquakeMountainStructure#type} 与结构片构造器直接引用它们。
 *       </li>
 *   <li>⚠ 结构片类型 ID 写进存档的结构起点；改名后已生成区块里的荡魂山起点读不回来。</li>
 * </ul>
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class SoulquakeMountainRegistration {

    public static final StructureType<SoulquakeMountainStructure> STRUCTURE_TYPE =
            () -> SoulquakeMountainStructure.CODEC;
    public static final StructurePieceType PIECE_TYPE =
            (StructurePieceType.ContextlessType) SoulquakeMountainPiece::new;

    private SoulquakeMountainRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.STRUCTURE_TYPE, SoulquakeMountain.ID, () -> STRUCTURE_TYPE);
        event.register(Registries.STRUCTURE_PIECE, SoulquakeMountain.ID, () -> PIECE_TYPE);
    }
}
