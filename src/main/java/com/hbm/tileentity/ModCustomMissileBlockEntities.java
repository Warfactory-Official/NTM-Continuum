package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.tileentity.bomb.TileEntityCompactLauncher;
import com.hbm.tileentity.bomb.TileEntityCustomMissileLauncher;
import com.hbm.tileentity.bomb.TileEntityLaunchPadLarge;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import com.hbm.tileentity.machine.TileEntityMachineMissileAssembly;
import com.hbm.tileentity.machine.TileEntityMultiblock;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types of the custom missile system and the two extra launch pads. */
public final class ModCustomMissileBlockEntities {

    private ModCustomMissileBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityMultiblock>> STRUCT_LAUNCHER_CORE =
            REG.blockEntity("struct_launcher_core", () -> BlockEntities.serverTicking(TileEntityMultiblock::new,
                    TileEntityMultiblock::serverTick, ModCustomMissileBlocks.struct_launcher_core.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMultiblock>> STRUCT_LAUNCHER_CORE_LARGE =
            REG.blockEntity("struct_launcher_core_large", () -> BlockEntities.serverTicking(TileEntityMultiblock::new,
                    TileEntityMultiblock::serverTick, ModCustomMissileBlocks.struct_launcher_core_large.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMissileAssembly>> MACHINE_MISSILE_ASSEMBLY =
            REG.blockEntity("machine_missile_assembly", () -> BlockEntities.serverTicking(
                    TileEntityMachineMissileAssembly::new, TileEntityMachineMissileAssembly::serverTick,
                    ModCustomMissileBlocks.machine_missile_assembly.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCompactLauncher>> COMPACT_LAUNCHER =
            REG.blockEntity("compact_launcher", () -> BlockEntities.ticking(
                    TileEntityCompactLauncher::new, TileEntityCustomMissileLauncher::clientTick,
                    TileEntityCustomMissileLauncher::serverTick,
                    ModCustomMissileBlocks.compact_launcher.get()));
    public static final RegistryObject<BlockEntityType<TileEntityLaunchTable>> LAUNCH_TABLE =
            REG.blockEntity("launch_table", () -> BlockEntities.ticking(
                    TileEntityLaunchTable::new, TileEntityCustomMissileLauncher::clientTick,
                    TileEntityCustomMissileLauncher::serverTick,
                    ModCustomMissileBlocks.launch_table.get()));
    public static final RegistryObject<BlockEntityType<TileEntityLaunchPadLarge>> LAUNCH_PAD_LARGE =
            REG.blockEntity("launch_pad_large", () -> BlockEntities.ticking(
                    TileEntityLaunchPadLarge::new, TileEntityLaunchPadLarge::clientTick,
                    TileEntityLaunchPadLarge::serverTick, ModCustomMissileBlocks.launch_pad_large.get()));
    public static final RegistryObject<BlockEntityType<TileEntityLaunchPadRusted>> LAUNCH_PAD_RUSTED =
            REG.blockEntity("launch_pad_rusted", () -> BlockEntities.serverTicking(
                    TileEntityLaunchPadRusted::new, TileEntityLaunchPadRusted::serverTick,
                    ModCustomMissileBlocks.launch_pad_rusted.get()));

    public static void init() {}
}
