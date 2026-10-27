package com.hbm.capability;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidMultiUserMK2;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.uninos.graph.EndpointRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Native, role-specific machine contacts. These queries describe configured contact, independent of stored
 * charge, tank fill, pressure, transfer budgets and whether a network subscription has run this tick.
 * Network distribution retains its own TilePort subscription and demand checks.
 */
public final class NativeContacts {
    private NativeContacts() {}

    public static @Nullable IEnergyHandlerMK2 powerAt(ServerLevel level, BlockPos pos,
                                                     @Nullable Direction side, CapRole role) {
        if (role != CapRole.POWER_IN && role != CapRole.POWER_OUT) {
            throw new IllegalArgumentException("Not a power role: " + role);
        }
        var owner = ownerAt(level, pos, side, role.bit);
        if (!(owner instanceof IEnergyHandlerMK2 handler)) return null;
        if (role == CapRole.POWER_IN && !(handler instanceof IEnergyReceiverMK2)
                || role == CapRole.POWER_OUT && !(handler instanceof IEnergyProviderMK2)) return null;
        if (!opens(owner, pos, side, role)) return null;
        var direction = side == null ? ForgeDirection.UNKNOWN : ForgeDirection.getOrientation(side);
        return handler.canConnect(direction) ? handler : null;
    }

    public static @Nullable IFluidUserMK2 fluidAt(ServerLevel level, BlockPos pos,
                                                 @Nullable Direction side, @Nullable FluidType fluid, CapRole role) {
        var user = fluidUserAt(level, pos, side, fluid, role, true, true);
        if (user == null) return null;
        var direction = side == null ? ForgeDirection.UNKNOWN : ForgeDirection.getOrientation(side);
        return user.exposesFluidContact(fluid, direction, role == CapRole.FLUID_IN) ? user : null;
    }

    /** Capability lookup with the raw caller-supplied FluidFace fluid, without a storage-demand filter. */
    public static @Nullable IFluidUserMK2 fluidKindAt(ServerLevel level, BlockPos pos,
                                                     @Nullable Direction side, @Nullable FluidType fluid, CapRole role) {
        return fluidUserAt(level, pos, side, fluid, role, true, false);
    }

    /** The absent-context lookup differs from a provided FluidFace whose side happens to be null. */
    public static @Nullable IFluidUserMK2 fluidKindAt(ServerLevel level, BlockPos pos, CapRole role) {
        return fluidUserAt(level, pos, null, null, role, false, false);
    }

    private static @Nullable IFluidUserMK2 fluidUserAt(ServerLevel level, BlockPos pos,
                                                      @Nullable Direction side, @Nullable FluidType fluid, CapRole role,
                                                      boolean hasContext, boolean resourceQuery) {
        if (role != CapRole.FLUID_IN && role != CapRole.FLUID_OUT) {
            throw new IllegalArgumentException("Not a fluid role: " + role);
        }
        var owner = ownerAt(level, pos, side, role.bit);
        if (!(owner instanceof IFluidUserMK2) || owner instanceof IFluidPipeMK2) return null;
        if (!opens(owner, pos, side, role)) return null;
        if (hasContext && !(resourceQuery ? MachineCaps.acceptsResource(owner, side, fluid, role)
                : MachineCaps.accepts(owner, side, fluid, role))) return null;
        var user = IFluidMultiUserMK2.resolve(owner, pos, side);
        if (user == null) return null;
        if (role == CapRole.FLUID_IN && !(user instanceof IFluidReceiverMK2)
                || role == CapRole.FLUID_OUT && !(user instanceof IFluidProviderMK2)) return null;
        return user;
    }

    /** The same live endpoint index and folded-core resolver used by the native networks. */
    private static @Nullable BlockEntity ownerAt(ServerLevel level, BlockPos pos, @Nullable Direction side, int role) {
        var declared = EndpointRegistry.of(level).resolve(level, pos.asLong(), side, role);
        if (declared != null && !declared.isRemoved()) return declared;
        var direct = level.getBlockEntity(pos);
        if (direct != null && !direct.isRemoved()) return direct;
        var core = MachineCore.at(level, pos, level.getBlockState(pos));
        return core != null && !core.isRemoved() ? core : null;
    }

    private static boolean opens(BlockEntity owner, BlockPos pos, @Nullable Direction side, CapRole role) {
        if (!MachineCaps.supportsRole(owner, role)) return false;
        var state = owner.getBlockState();
        if ((role == CapRole.POWER_IN || role == CapRole.POWER_OUT) && !MachineCaps.accepts(owner, side, role)) return false;
        if (side == null) return pos.equals(owner.getBlockPos());
        if (!(state.getBlock() instanceof BlockMultiblockCore block)) return true;
        var core = owner.getBlockPos();
        var facing = state.getValue(BlockMultiblockCore.FACING);
        var dx = pos.getX() - core.getX();
        var dy = pos.getY() - core.getY();
        var dz = pos.getZ() - core.getZ();
        return block.isOpenAt(facing, dx, dy, dz, side, role.passivePlane, role);
    }
}
