package com.hbm.handler.ae2;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import dev.golem.core.autocraft.CraftingMachine;
import dev.golem.core.autocraft.CraftingMachines;
import dev.golem.core.storage.ItemStore;
import dev.golem.core.storage.ItemStores;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.TileEntityProxyCell;

/**
 * Applied Energistics 2: pattern providers push jobs into the recipe machines 1.12.2 opened to it, and storage buses
 * read the mass storage unit and the large arc furnace as 1.7 did. A multiblock is reached through its core or any cell
 * that was a proxy, but not through a face that touches the same machine.
 */
public final class AE2CompatHandler {

    private AE2CompatHandler() {}

    /** Common init, once, before the loaders' capability registration. */
    public static void init() {
        CraftingMachines.exposeBlocks((level, pos, state, be, side) -> {
            BlockEntity owner = owner(level, pos, state, be, side);
            return owner == null ? null : machineOf(owner);
        }, AE2CompatHandler::carries);
        ItemStores.exposeBlocks(AE2CompatHandler::storeAt, AE2CompatHandler::carries);
    }

    private static boolean carries(Block block) {
        if (!(block instanceof EntityBlock) && !(block instanceof BlockMultiblockCore)
                && !(block instanceof BlockMultiblockCell)) return false;
        return Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    /** The block entity a network reaches at {@code pos}: its own, or a proxy cell's core. */
    public static @Nullable BlockEntity owner(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be,
                                              @Nullable Direction side) {
        if (be != null && !(be instanceof TileEntityProxyCell)) return be.isRemoved() ? null : be;
        if (!MultiblockSurface.isFoldedCell(state)) return null;
        BlockPos core = MultiblockSurface.coreOfCell(level, pos);
        BlockEntity owner = core == null ? null : level.getBlockEntity(core);
        if (owner == null || owner.isRemoved() || !isProxyCell(level, pos)) return null;
        if (side != null && core.equals(MultiblockSurface.coreOfAny(level, pos.relative(side)))) return null;
        return owner;
    }

    public static @Nullable ItemStore storeAt(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be,
                                              @Nullable Direction side) {
        BlockEntity owner = owner(level, pos, state, be, side);
        if (owner instanceof TileEntityMassStorage msu) return new MassStorageMEInventory(msu);
        if (owner instanceof TileEntityMachineArcFurnaceLarge afl) return new ArcFurnaceLargeMEInventory(afl);
        return null;
    }

    private static boolean isProxyCell(ServerLevel level, BlockPos pos) {
        if (MultiblockSurface.carriesPassive(level, pos, BlockMultiblockCore.PASSIVE_ANY)) return true;
        BlockState state = level.getBlockState(pos);
        for (Direction face : Direction.values()) {
            if (MultiblockSurface.isOpen(level, pos, state, face)) return true;
        }
        return false;
    }

    public static @Nullable CraftingMachine machineOf(BlockEntity be) {
        if (be instanceof TileEntityMachineAssemblyMachine m) return new NTMCraftingMachine(m, m.assemblerModule, 1);
        if (be instanceof TileEntityMachineChemicalPlant m) return new NTMCraftingMachine(m, m.chemplantModule, 1);
        if (be instanceof TileEntityMachinePrecAss m) return new NTMCraftingMachine(m, m.assemblerModule, 1);
        if (be instanceof TileEntityMachineRockMill m) return new NTMCraftingMachine(m, m.rockMillModule, 1);
        if (be instanceof TileEntityMachinePUREX m) return new NTMCraftingMachine(m, m.purexModule, 1);
        if (be instanceof TileEntityFusionTorus m) {
            return new NTMCraftingMachine(m, m.fusionModule, TileEntityFusionTorus.SLOT_BLUEPRINT);
        }
        if (be instanceof TileEntityFusionPlasmaForge m) {
            return new NTMCraftingMachine(m, m.plasmaModule, TileEntityFusionPlasmaForge.SLOT_BLUEPRINT);
        }
        return null;
    }
}
