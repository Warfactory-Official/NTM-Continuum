package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.bomb.BlockVolcano;
import com.hbm.blocks.generic.BlockChlorineSeal;
import com.hbm.blocks.generic.BlockFissure;
import com.hbm.blocks.generic.BlockGeysir;
import com.hbm.blocks.generic.BlockVent;

/** Block-entity types for the terrain family: fissure, volcano core, geyser, vent and chlorine seal. */
public final class ModTerrainBlockEntities {

    private ModTerrainBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<BlockFissure.TileEntityFissure>> FISSURE =
            REG.blockEntity("fissure", () -> BlockEntities.serverTicking(
                    BlockFissure.TileEntityFissure::new, BlockFissure.TileEntityFissure::serverTick,
                    ModTerrainBlocks.ore_volcano.get()));

    public static final RegistryObject<BlockEntityType<TileEntityVolcanoCore>> VOLCANO_CORE =
            REG.blockEntity("volcano_core", () -> {
                List<Block> cores = new ArrayList<>();
                for (RegistryObject<BlockVolcano> core : ModTerrainBlocks.volcanoCores()) cores.add(core.get());
                return BlockEntities.serverTicking(TileEntityVolcanoCore::new, TileEntityVolcanoCore::serverTick,
                        cores.toArray(new Block[0]));
            });

    public static final RegistryObject<BlockEntityType<BlockGeysir.TileEntityGeysir>> GEYSIR =
            REG.blockEntity("geysir", () -> BlockEntities.serverTicking(
                    BlockGeysir.TileEntityGeysir::new, BlockGeysir.TileEntityGeysir::serverTick,
                    ModTerrainBlocks.geysir_chlorine.get(), ModTerrainBlocks.geysir_nether.get()));

    public static final RegistryObject<BlockEntityType<BlockVent.TileEntityVent>> VENT =
            REG.blockEntity("vent", () -> BlockEntities.serverTicking(
                    BlockVent.TileEntityVent::new, BlockVent.TileEntityVent::serverTick,
                    ModTerrainBlocks.vent_chlorine.get(), ModTerrainBlocks.vent_cloud.get(), ModTerrainBlocks.vent_pink_cloud.get()));

    public static final RegistryObject<BlockEntityType<BlockChlorineSeal.TileEntityChlorineSeal>> CHLORINE_SEAL =
            REG.blockEntity("chlorine_seal", () -> BlockEntities.serverTicking(
                    BlockChlorineSeal.TileEntityChlorineSeal::new, BlockChlorineSeal.TileEntityChlorineSeal::serverTick,
                    ModTerrainBlocks.vent_chlorine_seal.get()));

    public static void init() {}
}
