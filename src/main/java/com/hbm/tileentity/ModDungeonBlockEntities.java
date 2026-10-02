package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.generic.BlockLoot;
import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.blocks.generic.BlockRebar;
import com.hbm.blocks.generic.BlockSkeletonHolder;
import com.hbm.blocks.generic.DungeonSpawner;
import com.hbm.blocks.generic.LogicBlock;
import com.hbm.blocks.generic.TrappedBrick;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.tileentity.deco.TileEntityTrappedBrick;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types of the dungeon family, under 1.7 {@code TileMappings}' names. */
public final class ModDungeonBlockEntities {

    private ModDungeonBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    /** Only the detector traps carry one (1.7 {@code createNewTileEntity}). */
    public static final RegistryObject<BlockEntityType<TileEntityTrappedBrick>> TRAPPED_BRICK =
            REG.blockEntity("tileentity_trapped_brick", () -> BlockEntities.serverTicking(
                    TileEntityTrappedBrick::new, TileEntityTrappedBrick::serverTick,
                    ModDungeonBlocks.TRAPS.stream().map(RegistryObject::get)
                            .filter(b -> b instanceof TrappedBrick.Detector).toArray(Block[]::new)));
    public static final RegistryObject<BlockEntityType<BlockPedestal.TileEntityPedestal>> PEDESTAL =
            REG.blockEntity("tileentity_ntm_pedestal", () -> BlockEntities.serverTicking(
                    BlockPedestal.TileEntityPedestal::new, BlockPedestal.TileEntityPedestal::serverTick,
                    ModDungeonBlocks.pedestal.get()));
    public static final RegistryObject<BlockEntityType<BlockSkeletonHolder.TileEntitySkeletonHolder>> SKELETON_HOLDER =
            REG.blockEntity("tileentity_ntm_skeleton", () -> BlockEntities.type(
                    BlockSkeletonHolder.TileEntitySkeletonHolder::new, ModDungeonBlocks.skeleton_holder.get()));
    public static final RegistryObject<BlockEntityType<BlockLoot.TileEntityLoot>> LOOT =
            REG.blockEntity("tileentity_ntm_loot", () -> BlockEntities.type(
                    BlockLoot.TileEntityLoot::new, ModDungeonBlocks.deco_loot.get()));
    public static final RegistryObject<BlockEntityType<TileEntityLanternBehemoth>> LANTERN_BEHEMOTH =
            REG.blockEntity("tileentity_lantern_behemoth", () -> BlockEntities.serverTicking(
                    TileEntityLanternBehemoth::new, TileEntityLanternBehemoth::serverTick,
                    ModDungeonBlocks.lantern_behemoth.get()));
    public static final RegistryObject<BlockEntityType<DungeonSpawner.TileEntityDungeonSpawner>> DUNGEON_SPAWNER =
            REG.blockEntity("tileentity_ntm_dungeon_spawner", () -> BlockEntities.serverTicking(
                    DungeonSpawner.TileEntityDungeonSpawner::new, DungeonSpawner.TileEntityDungeonSpawner::serverTick,
                    ModDungeonBlocks.dungeon_spawner.get()));
    public static final RegistryObject<BlockEntityType<LogicBlock.TileEntityLogicBlock>> LOGIC_BLOCK =
            REG.blockEntity("tileentity_ntm_logic_block", () -> BlockEntities.serverTicking(
                    LogicBlock.TileEntityLogicBlock::new, LogicBlock.TileEntityLogicBlock::serverTick,
                    ModDungeonBlocks.logic_block.get(), ModDungeonBlocks.logic_block_invis.get()));
    public static final RegistryObject<BlockEntityType<BlockRebar.TileEntityRebar>> REBAR =
            REG.blockEntity("tileentity_rebar", () -> BlockEntities.serverTicking(
                    BlockRebar.TileEntityRebar::new, BlockRebar.TileEntityRebar::serverTick,
                    ModDungeonBlocks.rebar.get()));

    public static void init() {}
}
