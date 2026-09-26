package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The five-long legacy gas turbine multiblock. */
public class MachineTurbineGas extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineTurbineGas(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineTurbineGas(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 1, 4, 5}; }
    @Override public int getOffset() { return 1; }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineTurbineGas turbine
                && com.hbm.lib.Ports.isPort(new com.hbm.lib.DirPos[]{turbine.getPowerPos()}, pos, dir.toDirection());
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityMachineTurbineGas turbine
                && turbine.isFluidPort(pos, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineTurbineGas turbine)) return null;

        ForgeDirection facing = turbine.getFacing();
        List<String> text = new ArrayList<>();

        if (hitCheck(facing, turbine.getBlockPos(), -1, -1, 0, pos)
                || hitCheck(facing, turbine.getBlockPos(), 1, -1, 0, pos)) {
            text.add("§2-> §r" + turbine.tanks[0].getTankType().getDisplayNameString());
            text.add("§2-> §r" + turbine.tanks[1].getTankType().getDisplayNameString());
        }

        if (hitCheck(facing, turbine.getBlockPos(), -1, 4, 0, pos)
                || hitCheck(facing, turbine.getBlockPos(), 1, 4, 0, pos)) {
            text.add("§2-> §r" + turbine.tanks[2].getTankType().getDisplayNameString());
        }

        if (hitCheck(facing, turbine.getBlockPos(), 0, 5, 1, pos)) {
            text.add("§4-> §r" + turbine.tanks[3].getTankType().getDisplayNameString());
        }

        if (hitCheck(facing, turbine.getBlockPos(), 0, -4, 1, pos)) {
            text.add("§4-> §rPower");
        }

        if (!text.isEmpty()) {
            return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
        }
        return null;
    }

    private static boolean hitCheck(ForgeDirection facing, BlockPos core, int facingOffset, int rotationOffset,
                                    int yOffset, BlockPos hit) {
        ForgeDirection turn = facing.getRotation(ForgeDirection.DOWN);
        int x = core.getX() + facing.offsetX * facingOffset + turn.offsetX * rotationOffset;
        int y = core.getY() + yOffset;
        int z = core.getZ() + facing.offsetZ * facingOffset + turn.offsetZ * rotationOffset;
        return x == hit.getX() && y == hit.getY() && z == hit.getZ();
    }

    /** Four anchors along the flanks and two more a step up, all riding the facing. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(-dx + rx, 0, -dz + rz), MASK_NORTH);
        visitor.cell(core.offset(dx + rx, 0, dz + rz), MASK_SOUTH);
        visitor.cell(core.offset(-dx + rx * -4, 0, -dz + rz * -4), MASK_NORTH);
        visitor.cell(core.offset(dx + rx * -4, 0, dz + rz * -4), MASK_SOUTH);
        visitor.cell(core.offset(rx * 4, 1, rz * 4), MASK_WEST);
        visitor.cell(core.offset(rx * -5, 1, rz * -5), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;

        visitor.passiveCell(core.offset(-dx + rx, 0, -dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx + rx, 0, dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-dx + rx * -4, 0, -dz + rz * -4), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx + rx * -4, 0, dz + rz * -4), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rx * 4, 1, rz * 4), MASK_ALL, domains);
        visitor.passiveCell(core.offset(rx * -5, 1, rz * -5), MASK_ALL, domains);
    }
}
