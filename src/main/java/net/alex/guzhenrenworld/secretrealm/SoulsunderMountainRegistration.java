package net.alex.guzhenrenworld.secretrealm;

import net.alex.guzhenrenworld.GuWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 荡魂山的代码注册入口：以 {@link SoulsunderMountain#ID} 注册结构类型 {@link #STRUCTURE_TYPE} 与结构片类型
 * {@link #PIECE_TYPE}。
 * <ul>
 *   <li>两者是本类持有的单例，注册时交出同一实例，{@link SoulsunderMountainStructure#type} 与结构片构造器直接引用它们。
 *       </li>
 *   <li>⚠ 结构片类型 ID 写进存档的结构起点；改名后已生成区块里的荡魂山起点读不回来。</li>
 *   <li>停服时清空结构片的耗时累加表（{@link SoulsunderMountainPiece#clearProgress}）。</li>
 * </ul>
 */

@EventBusSubscriber(modid = GuWorld.MOD_ID)
public final class SoulsunderMountainRegistration {

    public static final StructureType<SoulsunderMountainStructure> STRUCTURE_TYPE =
            () -> SoulsunderMountainStructure.CODEC;
    public static final StructurePieceType PIECE_TYPE =
            (StructurePieceType.ContextlessType) SoulsunderMountainPiece::new;

    private SoulsunderMountainRegistration() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.STRUCTURE_TYPE, SoulsunderMountain.ID, () -> STRUCTURE_TYPE);
        event.register(Registries.STRUCTURE_PIECE, SoulsunderMountain.ID, () -> PIECE_TYPE);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SoulsunderMountainPiece.clearProgress();
    }
}
