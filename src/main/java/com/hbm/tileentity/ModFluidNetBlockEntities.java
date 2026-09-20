package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.network.FluidDuctBox;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityBarrelDeco;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import com.hbm.tileentity.network.TileEntityFluidDuctVariant;
import com.hbm.tileentity.network.TileEntityFluidPump;
import com.hbm.tileentity.network.TileEntityFluidSwitch;
import com.hbm.tileentity.machine.TileEntityFoundryTank;
import com.hbm.tileentity.network.TileEntityPipeAnchor;
import com.hbm.tileentity.network.TileEntityPipeExhaust;
import com.hbm.tileentity.network.TileEntityPipeGauge;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.ModFluidNetBlocks;

/** Block-entity types for the fluid-network family. */
public final class ModFluidNetBlockEntities {

    private ModFluidNetBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    /** The three neo skins (one renderer: pipe_neo.obj). */
    public static final RegistryObject<BlockEntityType<TileEntityFluidDuctVariant>> DUCT =
            BLOCK_ENTITIES.register("fluid_duct_neo", () -> BlockEntities.type(TileEntityFluidDuctVariant::new,
                    ModFluidNetBlocks.fluid_duct_neo.get(), ModFluidNetBlocks.fluid_duct_neo_silver.get(), ModFluidNetBlocks.fluid_duct_neo_colored.get()));

    /** The fifteen box ducts (one renderer: the boxduct faces). */
    public static final RegistryObject<BlockEntityType<TileEntityFluidDuctVariant>> BOX =
            BLOCK_ENTITIES.register("fluid_duct_box", () -> BlockEntities.type(TileEntityFluidDuctVariant::new, boxDucts()));

    /** The paintable cube duct (renderer: the fluid-coloured port overlay). */
    public static final RegistryObject<BlockEntityType<TileEntityFluidDuctVariant>> PAINTABLE =
            BLOCK_ENTITIES.register("fluid_duct_paintable", () -> BlockEntities.type(TileEntityFluidDuctVariant::new,
                    ModFluidNetBlocks.fluid_duct_paintable.get()));

    /** The five exhaust box ducts (box renderer, exhaust skin). */
    public static final RegistryObject<BlockEntityType<TileEntityPipeExhaust>> EXHAUST =
            BLOCK_ENTITIES.register("fluid_duct_exhaust", () -> BlockEntities.type(TileEntityPipeExhaust::new, exhaustDucts()));

    /** The paintable exhaust cube (no renderer: 1.7 drew its overlay only once painted). */
    public static final RegistryObject<BlockEntityType<TileEntityPipeExhaust>> PAINTABLE_EXHAUST =
            BLOCK_ENTITIES.register("fluid_duct_paintable_block_exhaust", () -> BlockEntities.type(TileEntityPipeExhaust::new,
                    ModFluidNetBlocks.fluid_duct_paintable_block_exhaust.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPipeGauge>> GAUGE =
            BLOCK_ENTITIES.register("fluid_duct_gauge", () -> BlockEntities.serverTicking(TileEntityPipeGauge::new,
                    TileEntityPipeGauge::serverTick, ModFluidNetBlocks.fluid_duct_gauge.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPipeAnchor>> ANCHOR =
            BLOCK_ENTITIES.register("pipe_anchor", () -> BlockEntities.type(TileEntityPipeAnchor::new, ModFluidNetBlocks.pipe_anchor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidSwitch>> SWITCH =
            BLOCK_ENTITIES.register("fluid_switch", () -> BlockEntities.type(TileEntityFluidSwitch::new, ModFluidNetBlocks.fluid_switch.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidCounterValve>> COUNTER_VALVE =
            BLOCK_ENTITIES.register("fluid_counter_valve", () -> BlockEntities.serverTicking(TileEntityFluidCounterValve::new,
                    TileEntityFluidCounterValve::serverTick, ModFluidNetBlocks.fluid_counter_valve.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidPump>> PUMP =
            BLOCK_ENTITIES.register("fluid_pump", () -> BlockEntities.serverTicking(TileEntityFluidPump::new,
                    TileEntityFluidPump::serverTick, ModFluidNetBlocks.fluid_pump.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBarrel>> BARREL =
            BLOCK_ENTITIES.register("barrel", () -> BlockEntities.serverTicking(TileEntityBarrel::new, TileEntityBarrel::serverTick,
                    ModFluidNetBlocks.barrel_plastic.get(), ModFluidNetBlocks.barrel_steel.get(),
                    ModFluidNetBlocks.barrel_tcalloy.get(), ModFluidNetBlocks.barrel_antimatter.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBarrelDeco>> BARREL_DECO =
            BLOCK_ENTITIES.register("barrel_deco", () -> BlockEntities.type(TileEntityBarrelDeco::new,
                    ModFluidNetBlocks.barrel_corroded.get(), ModFluidNetBlocks.red_barrel.get(), ModFluidNetBlocks.pink_barrel.get(),
                    ModFluidNetBlocks.yellow_barrel.get(), ModFluidNetBlocks.vitrified_barrel.get(),
                    ModFluidNetBlocks.lox_barrel.get(), ModFluidNetBlocks.taint_barrel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFoundryTank>> FOUNDRY_TANK =
            BLOCK_ENTITIES.register("foundry_tank", () -> BlockEntities.serverTicking(TileEntityFoundryTank::new,
                    TileEntityFoundryTank::serverTick, ModFluidNetBlocks.foundry_tank.get()));

    private static Block[] boxDucts() {
        List<Block> blocks = new ArrayList<>();
        for (int m = 0; m < FluidDuctBox.MATERIALS.length; m++) {
            for (int s = 0; s < 5; s++) blocks.add(ModFluidNetBlocks.fluid_duct_box[m][s].get());
        }
        return blocks.toArray(new Block[0]);
    }

    private static Block[] exhaustDucts() {
        List<Block> blocks = new ArrayList<>();
        for (int s = 0; s < 5; s++) blocks.add(ModFluidNetBlocks.fluid_duct_exhaust[s].get());
        return blocks.toArray(new Block[0]);
    }

    public static void init() {}
}
