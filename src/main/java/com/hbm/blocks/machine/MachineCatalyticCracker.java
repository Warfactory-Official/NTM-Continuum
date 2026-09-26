package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockFootprint;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Catalytic Cracker block — 1:1 port of the 1.12.2 {@code MachineCatalyticCracker}: a large steam
 * tower (dims {@code {0,0,3,3,2,3}}, offset 3; 4 body fills + 8 makeExtra ports). No GUI — fluid-ID
 * right-click retypes the input, empty-hand look-overlay prints the 5 tanks. */
public class MachineCatalyticCracker extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineCatalyticCracker(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCatalyticCracker(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 3, 3, 2, 3}; }
    @Override public int getOffset() { return 3; }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide()) {
                BlockPos core = MultiblockSurface.coreOfAny(level, pos);
                if (core == null) return InteractionResult.FAIL;
                if (!(level.getBlockEntity(core) instanceof TileEntityMachineCatalyticCracker cracker)) return InteractionResult.FAIL;

                FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                cracker.tanks[0].setTankType(type);
                cracker.setChanged();
                player.sendSystemMessage(Component.translatable("chat.fractioning.changedto",
                        type.getDisplayName()).withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineCatalyticCracker cracker)) return null;

        List<String> text = new ArrayList<>();
        for (FluidTankNTM tank : cracker.tanks) {
            text.add(tank.getTankType().getDisplayNameString() + ": " + String.format("%,d", tank.getFill()) + "/" + String.format("%,d", tank.getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /** 1.7: the tower shaft, the chimney and the two wings beyond getDimensions. */
    private static final int[][] EXTRA_BOXES = {
            {4, -1, 3, -1, 1, 1},
            {13, 0, 0, 3, 2, 1},
            {14, -13, -1, 2, 1, 0},
            {3, -1, 2, 3, -1, 3}};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] box : EXTRA_BOXES) {
            if (!MultiblockFootprint.checkSpace(level, core, box, clicked, facing)) return false;
        }
        return true;
    }

    /**
     * Six of upstream's eight ports. The pair at {@code +dir*2 - rot*4} and {@code -dir*2 + rot*3}
     * each carries the face of the opposite flank, so each names a cell one step outside the machine
     * -- 1.7 asks {@code isPort} of a block that is not the cracker and never gets an answer -- so
     * they connect nothing there either, and the table says what the machine really has.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : EXTRA_BOXES) MultiblockFootprint.visitBox(core, box, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing, 3).relative(rot), MASK_SOUTH);
        visitor.cell(core.relative(facing, 3).relative(rot, -2), MASK_SOUTH);
        visitor.cell(core.relative(facing, -3).relative(rot), MASK_NORTH);
        visitor.cell(core.relative(facing, -3).relative(rot, -2), MASK_NORTH);
        visitor.cell(core.relative(facing, 2).relative(rot, 2), MASK_WEST);
        visitor.cell(core.relative(facing, -2).relative(rot, -3), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(facing, 3).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 3).relative(rot, -2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -3).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -3).relative(rot, -2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 2).relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 2).relative(rot, -3), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -2).relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -2).relative(rot, -3), MASK_ALL, domains);
    }
}
