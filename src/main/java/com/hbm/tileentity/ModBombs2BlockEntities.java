package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.tileentity.bomb.TileEntityNukeBalefire;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;
import com.hbm.tileentity.bomb.TileEntityNukeFleija;
import com.hbm.tileentity.bomb.TileEntityNukePrototype;
import com.hbm.tileentity.bomb.TileEntityNukeSolinium;

/** Block-entity types for the special bombs. The custom nuke and the balefire bomb tick on the server. */
public final class ModBombs2BlockEntities {

    private ModBombs2BlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityNukeCustom>> NUKE_CUSTOM =
            REG.blockEntity("nuke_custom", () -> BlockEntities.serverTicking(
                    TileEntityNukeCustom::new, TileEntityNukeCustom::serverTick, ModBombs2Blocks.nuke_custom.get()));
    public static final RegistryObject<BlockEntityType<TileEntityNukeFleija>> NUKE_FLEIJA =
            REG.blockEntity("nuke_fleija",
                    () -> BlockEntities.type(TileEntityNukeFleija::new, ModBombs2Blocks.nuke_fleija.get()));
    public static final RegistryObject<BlockEntityType<TileEntityNukeSolinium>> NUKE_SOLINIUM =
            REG.blockEntity("nuke_solinium",
                    () -> BlockEntities.type(TileEntityNukeSolinium::new, ModBombs2Blocks.nuke_solinium.get()));
    public static final RegistryObject<BlockEntityType<TileEntityNukePrototype>> NUKE_PROTOTYPE =
            REG.blockEntity("nuke_prototype",
                    () -> BlockEntities.type(TileEntityNukePrototype::new, ModBombs2Blocks.nuke_prototype.get()));
    public static final RegistryObject<BlockEntityType<TileEntityNukeBalefire>> NUKE_FSTBMB =
            REG.blockEntity("nuke_fstbmb", () -> BlockEntities.serverTicking(
                    TileEntityNukeBalefire::new, TileEntityNukeBalefire::serverTick, ModBombs2Blocks.nuke_fstbmb.get()));

    public static void init() {}
}
