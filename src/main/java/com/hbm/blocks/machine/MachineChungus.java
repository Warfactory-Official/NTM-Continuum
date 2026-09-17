package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityChungus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The 5x4x14 Leviathan steam turbine multiblock. */
public class MachineChungus extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    /** The housing ridge that sits one layer above the main body. */
    private static final int[] RIDGE = {4, -4, 0, 3, 1, 1};
    /** The shaft running back from the body toward the generator. */
    private static final int[] SHAFT = {3, 0, 6, -1, 1, 1};
    /** The generator at the far end of the shaft. */
    private static final int[] GENERATOR = {2, 0, 10, -7, 1, 1};

    /**
     * The steam intake nozzle, one block past the front of the body and two up. It is the only cell of
     * the machine that no footprint box covers, so the geometry states it on its own.
     */
    private static final int INTAKE_FORWARD = 4;

    public MachineChungus(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityChungus(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 3, 2, 2};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    /**
     * Four boxes and one loose cell. The ridge, the shaft and the generator are footprints in their own
     * right; the steam intake nozzle sits one block past the front of the body and two up, which no
     * footprint covers, so it is stated on its own.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, RIDGE, facing, visitor);
        MultiblockHandlerXR.visitBox(core, SHAFT, facing, visitor);
        MultiblockHandlerXR.visitBox(core, GENERATOR, facing, visitor);
        visitor.cell(core.relative(facing, INTAKE_FORWARD).above(2), MASK_SOUTH, ROLE_FLUID);

        Direction side = facing.getClockWise();
        visitor.cell(core.relative(side, 2), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.relative(side, -2), MASK_EAST, ROLE_FLUID);
        visitor.cell(core.relative(facing, -10), MASK_NORTH, ROLE_POWER);
    }

    /** The intake, the generator outlet and the two side inlets are the machine's ports. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction side = facing.getClockWise();
        visitor.passiveCell(core.relative(facing, INTAKE_FORWARD).above(2), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(facing, -10), MASK_ALL, PASSIVE_POWER_IN);
        visitor.passiveCell(core.relative(side, 2), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(side, -2), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockHandlerXR.checkSpace(level, core, SHAFT, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core, GENERATOR, clicked, facing)
                && level.getBlockState(core.relative(facing, INTAKE_FORWARD).above(2)).canBeReplaced();
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityChungus chungus)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        ForgeDirection dir = chungus.getFacing();
        ForgeDirection turn = dir.getRotation(ForgeDirection.DOWN);
        int nearX = core.getX() + dir.offsetX + turn.offsetX * 2;
        int farX = core.getX() + dir.offsetX * 2 + turn.offsetX * 2;
        int nearZ = core.getZ() + dir.offsetZ + turn.offsetZ * 2;
        int farZ = core.getZ() + dir.offsetZ * 2 + turn.offsetZ * 2;

        if ((pos.getX() == nearX || pos.getX() == farX) && (pos.getZ() == nearZ || pos.getZ() == farZ)
                && pos.getY() < core.getY() + 2) {
            if (!level.isClientSide()) {
                if (!chungus.operational) {
                    level.playSound(null, pos, ModSounds.CHUNGUS_LEVER.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    chungus.onLeverPull();
                } else {
                    player.sendSystemMessage(
                            Component.literal("Cannot change compressor setting while operational!")
                                    .withStyle(ChatFormatting.RED));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityChungus chungus
                && IEnergyConnectorBlock.isPort(chungus.getPowerPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return (CoolingRecipes.get(type) != null || type == Fluids.SPENTSTEAM)
                && MultiblockSurface.coreEntity(world, pos) instanceof TileEntityChungus chungus
                && IEnergyConnectorBlock.isPort(chungus.getConPos(), pos, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityChungus chungus)) return null;

        List<String> text = new ArrayList<>();
        FluidType inputType = chungus.tanks[0].getTankType();
        if (inputType != Fluids.NONE) {
            text.add("§a-> §r" + inputType.getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", chungus.tanks[0].getFill()) + "/"
                    + String.format(Locale.US, "%,d", chungus.tanks[0].getMaxFill()) + "mB");
        }
        FluidType outputType = chungus.tanks[1].getTankType();
        if (outputType != Fluids.NONE) {
            text.add("§c<- §r" + outputType.getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", chungus.tanks[1].getFill()) + "/"
                    + String.format(Locale.US, "%,d", chungus.tanks[1].getMaxFill()) + "mB");
        }
        text.add("§c<- §r" + Library.getShortNumber(chungus.powerBuffer) + "HE");

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

}
