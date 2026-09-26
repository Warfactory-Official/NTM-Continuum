package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower;
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

/**
 * Fractioning Tower block — 1:1 port of the 1.12.2 {@code MachineFractionTower}: a 3-tall 3×3
 * the folded core (dims {@code {2,0,1,1,1,1}}, offset 1) with the 4 side cells marked as extras
 * (fluid ports). Right-click with a fluid identifier on the <b>bottom</b> segment types the input tank
 * (towers stacked 3 apart share the type); empty-hand right-click prints the tank contents.
 */
public class MachineFractionTower extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineFractionTower(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineFractionTower(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide()) {
                BlockPos core = MultiblockSurface.coreOfAny(level, pos);
                if (core == null) return InteractionResult.FAIL;
                if (!(level.getBlockEntity(core) instanceof TileEntityMachineFractionTower frac)) return InteractionResult.FAIL;

                if (level.getBlockEntity(core.offset(0, -3, 0)) instanceof TileEntityMachineFractionTower) {
                    player.sendSystemMessage(Component.translatable("chat.fractioning.onlybottom"));
                } else {
                    FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                    frac.tanks[0].setTankType(type);
                    frac.setChanged();
                    player.sendSystemMessage(Component.translatable("chat.fractioning.changedto",
                            type.getDisplayName()).withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineFractionTower frac)) return null;

        List<String> text = new ArrayList<>();
        for (FluidTankNTM tank : frac.tanks) {
            text.add(tank.getTankType().getDisplayNameString() + ": " + String.format("%,d", tank.getFill()) + "/" + String.format("%,d", tank.getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /** Four inlets around the base of the tower. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_FLUID_IN);
    }
}
