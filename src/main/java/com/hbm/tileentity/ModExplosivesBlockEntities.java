package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.tileentity.bomb.TileEntityBombMulti;
import com.hbm.tileentity.bomb.TileEntityCharge;
import com.hbm.tileentity.bomb.TileEntityCrashedBomb;
import com.hbm.tileentity.bomb.TileEntityDetCord;
import com.hbm.tileentity.bomb.TileEntityLandmine;

/** Feature-local block-entity types for the conventional explosives. */
public final class ModExplosivesBlockEntities {
    private ModExplosivesBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityLandmine>> LANDMINE =
            BLOCK_ENTITIES.register("landmine", () -> BlockEntities.serverTicking(TileEntityLandmine::new,
                    TileEntityLandmine::serverTick,
                    ModExplosivesBlocks.mine_ap.get(), ModExplosivesBlocks.mine_he.get(), ModExplosivesBlocks.mine_shrap.get(),
                    ModExplosivesBlocks.mine_fat.get(), ModExplosivesBlocks.mine_naval.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCharge>> CHARGE =
            BLOCK_ENTITIES.register("charge", () -> BlockEntities.serverTicking(TileEntityCharge::new,
                    TileEntityCharge::serverTick,
                    ModExplosivesBlocks.charge_dynamite.get(), ModExplosivesBlocks.charge_miner.get(),
                    ModExplosivesBlocks.charge_c4.get(), ModExplosivesBlocks.charge_semtex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDetCord>> DET_CORD =
            BLOCK_ENTITIES.register("det_cord", () -> BlockEntities.type(TileEntityDetCord::new,
                    ModExplosivesBlocks.det_cord.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrashedBomb>> CRASHED_BOMB =
            BLOCK_ENTITIES.register("crashed_bomb", () -> BlockEntities.serverTicking(TileEntityCrashedBomb::new,
                    TileEntityCrashedBomb::serverTick,
                    ModExplosivesBlocks.crashed_bomb_balefire.get(), ModExplosivesBlocks.crashed_bomb_conventional.get(),
                    ModExplosivesBlocks.crashed_bomb_nuke.get(), ModExplosivesBlocks.crashed_bomb_salted.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBombMulti>> BOMB_MULTI =
            BLOCK_ENTITIES.register("bomb_multi", () -> BlockEntities.type(TileEntityBombMulti::new,
                    ModExplosivesBlocks.bomb_multi.get()));

    public static void init() {}
}
