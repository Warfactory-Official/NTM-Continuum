package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.tileentity.machine.TileEntityDiFurnaceRTG;
import com.hbm.tileentity.machine.TileEntityMachineAnnihilator;
import com.hbm.tileentity.machine.TileEntityMachineExcavator;
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper;
import com.hbm.tileentity.machine.TileEntityRtgFurnace;

/** Feature-local block-entity types for the production machines (group B). */
public final class ModProduction2BlockEntities {
    private ModProduction2BlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineExcavator>> EXCAVATOR =
            BLOCK_ENTITIES.register("machine_excavator", () -> BlockEntities.ticking(
                    TileEntityMachineExcavator::new, TileEntityMachineExcavator::clientTick,
                    TileEntityMachineExcavator::serverTick, ModProduction2Blocks.MACHINE_EXCAVATOR.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineOreSlopper>> ORE_SLOPPER =
            BLOCK_ENTITIES.register("machine_ore_slopper", () -> BlockEntities.ticking(
                    TileEntityMachineOreSlopper::new, TileEntityMachineOreSlopper::clientTick,
                    TileEntityMachineOreSlopper::serverTick, ModProduction2Blocks.MACHINE_ORE_SLOPPER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineAnnihilator>> ANNIHILATOR =
            BLOCK_ENTITIES.register("machine_annihilator", () -> BlockEntities.serverTicking(
                    TileEntityMachineAnnihilator::new, TileEntityMachineAnnihilator::serverTick,
                    ModProduction2Blocks.MACHINE_ANNIHILATOR.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRtgFurnace>> RTG_FURNACE =
            BLOCK_ENTITIES.register("machine_rtg_furnace_off", () -> BlockEntities.serverTicking(
                    TileEntityRtgFurnace::new, TileEntityRtgFurnace::serverTick,
                    ModProduction2Blocks.MACHINE_RTG_FURNACE.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDiFurnaceRTG>> DIFURNACE_RTG =
            BLOCK_ENTITIES.register("machine_difurnace_rtg_off", () -> BlockEntities.serverTicking(
                    TileEntityDiFurnaceRTG::new, TileEntityDiFurnaceRTG::serverTick,
                    ModProduction2Blocks.MACHINE_DIFURNACE_RTG.get()));

    public static void init() {}
}
