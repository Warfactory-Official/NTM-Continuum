package com.hbm.blocks.machine;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
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

/** Industrial boiler: a five-port, four-block-tall fluid heater. */
public class MachineHeatBoilerIndustrial extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay, ITooltipProvider {
    public MachineHeatBoilerIndustrial(Properties properties) { super(properties); }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeatBoilerIndustrial(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide()) {
                BlockPos core = MultiblockSurface.coreOfAny(level, pos);
                if (core == null || !(level.getBlockEntity(core) instanceof TileEntityHeatBoilerIndustrial boiler)) {
                    return InteractionResult.FAIL;
                }
                FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                if (HeatRecipes.hasBoilerRecipe(type)) {
                    boiler.tanks[0].setTankType(type);
                    boiler.setChanged();
                    player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                            .append(type.getDisplayName()).append(Component.literal("!")));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityHeatBoilerIndustrial boiler)) return null;
        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", boiler.heat) + " TU");
        text.add("§a-> §r" + boiler.tanks[0].getTankType().getDisplayNameString() + ": "
                + String.format("%,d", boiler.tanks[0].getFill()) + "/" + String.format("%,d", boiler.tanks[0].getMaxFill()) + " mB");
        text.add("§c<- §r" + boiler.tanks[1].getTankType().getDisplayNameString() + ": "
                + String.format("%,d", boiler.tanks[1].getFill()) + "/" + String.format("%,d", boiler.tanks[1].getMaxFill()) + " mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** Four water inlets around the waist and the steam outlet four above. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
        visitor.cell(core.above(4), MASK_UP);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.above(4), MASK_ALL, PASSIVE_FLUID_IN);
    }
}
