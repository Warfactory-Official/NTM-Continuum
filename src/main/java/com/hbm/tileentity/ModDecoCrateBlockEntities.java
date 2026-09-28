package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.tileentity.deco.TileEntityBobble;
import com.hbm.tileentity.deco.TileEntityCanCrate;
import com.hbm.tileentity.deco.TileEntityEmitter;
import com.hbm.tileentity.deco.TileEntityGlyphidSpawner;
import com.hbm.tileentity.deco.TileEntityPartEmitter;
import com.hbm.tileentity.deco.TileEntityPlushie;
import com.hbm.tileentity.deco.TileEntitySnowglobe;
import com.hbm.tileentity.deco.TileEntityVendingMachine;
import com.hbm.tileentity.machine.TileEntityBroadcaster;
import com.hbm.tileentity.machine.TileEntityRadioRec;
import com.hbm.tileentity.machine.TileEntityRadiobox;
import com.hbm.tileentity.machine.storage.TileEntityCrateTungsten;
import com.hbm.tileentity.machine.storage.TileEntityFileCabinet;
import com.hbm.tileentity.machine.storage.TileEntitySafe;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types of the storage crates, decorative props and simple blocks ({@link ModDecoCrateBlocks}). */
public final class ModDecoCrateBlockEntities {

    private ModDecoCrateBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityCanCrate>> CRATE_CAN =
            BLOCK_ENTITIES.register("crate_can", () -> BlockEntities.type(TileEntityCanCrate::new,
                    ModDecoCrateBlocks.crate_can.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCrateTungsten>> CRATE_TUNGSTEN =
            BLOCK_ENTITIES.register("crate_tungsten", () -> BlockEntities.ticking(TileEntityCrateTungsten::new,
                    TileEntityCrateTungsten::clientTick, TileEntityCrateTungsten::serverTick,
                    ModDecoCrateBlocks.crate_tungsten.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySafe>> SAFE =
            BLOCK_ENTITIES.register("safe", () -> BlockEntities.type(TileEntitySafe::new, ModDecoCrateBlocks.safe.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFileCabinet>> FILE_CABINET =
            BLOCK_ENTITIES.register("filing_cabinet", () -> BlockEntities.ticking(TileEntityFileCabinet::new,
                    TileEntityFileCabinet::tick, TileEntityFileCabinet::tick, ModDecoCrateBlocks.filing_cabinet.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBobble>> BOBBLE =
            BLOCK_ENTITIES.register("bobblehead", () -> BlockEntities.type(TileEntityBobble::new,
                    ModDecoCrateBlocks.bobblehead.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPlushie>> PLUSHIE =
            BLOCK_ENTITIES.register("plushie", () -> BlockEntities.clientTicking(TileEntityPlushie::new,
                    TileEntityPlushie::clientTick, ModDecoCrateBlocks.plushie.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySnowglobe>> SNOWGLOBE =
            BLOCK_ENTITIES.register("snowglobe", () -> BlockEntities.type(TileEntitySnowglobe::new,
                    ModDecoCrateBlocks.snowglobe.get()));
    public static final RegistryObject<BlockEntityType<TileEntityVendingMachine>> VENDING_MACHINE =
            BLOCK_ENTITIES.register("vending_machine", () -> BlockEntities.type(TileEntityVendingMachine::new,
                    ModDecoCrateBlocks.vending_machine.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBroadcaster>> BROADCASTER =
            BLOCK_ENTITIES.register("broadcaster_pc", () -> BlockEntities.ticking(TileEntityBroadcaster::new,
                    TileEntityBroadcaster::clientTick, TileEntityBroadcaster::serverTick,
                    ModDecoCrateBlocks.broadcaster_pc.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadiobox>> RADIOBOX =
            BLOCK_ENTITIES.register("radiobox", () -> BlockEntities.serverTicking(TileEntityRadiobox::new,
                    TileEntityRadiobox::serverTick, ModDecoCrateBlocks.radiobox.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioRec>> RADIOREC =
            BLOCK_ENTITIES.register("radiorec", () -> BlockEntities.serverTicking(TileEntityRadioRec::new,
                    TileEntityRadioRec::serverTick, ModDecoCrateBlocks.radiorec.get()));
    public static final RegistryObject<BlockEntityType<TileEntityEmitter>> EMITTER =
            BLOCK_ENTITIES.register("deco_emitter", () -> BlockEntities.serverTicking(TileEntityEmitter::new,
                    TileEntityEmitter::serverTick, ModDecoCrateBlocks.deco_emitter.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPartEmitter>> PART_EMITTER =
            BLOCK_ENTITIES.register("part_emitter", () -> BlockEntities.serverTicking(TileEntityPartEmitter::new,
                    TileEntityPartEmitter::serverTick, ModDecoCrateBlocks.part_emitter.get()));
    public static final RegistryObject<BlockEntityType<TileEntityGlyphidSpawner>> GLYPHID_SPAWNER =
            BLOCK_ENTITIES.register("glyphid_spawner", () -> BlockEntities.serverTicking(TileEntityGlyphidSpawner::new,
                    TileEntityGlyphidSpawner::serverTick, ModDecoCrateBlocks.glyphid_spawner.get()));

    public static void init() {}
}
