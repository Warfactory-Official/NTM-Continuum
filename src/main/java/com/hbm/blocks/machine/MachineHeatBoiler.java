package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Heat Boiler block (old id {@code heat_boiler}), 1:1 port of the 1.12.2 {@code MachineHeatBoiler}:
 * a 4-tall 3×3 the folded core (dims {@code {3,0,1,1,1,1}}, offset 1) with two side ports and
 * the top port marked as extras. Right-click with a fluid identifier types the input tank; the look
 * overlay prints heat + both tanks (the boiler has no GUI).
 */
public class MachineHeatBoiler extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay, ITooltipProvider {

    public MachineHeatBoiler(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeatBoiler(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** Fluid identifier right-click types the input tank (old onBlockActivated). */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide()) {
                BlockPos core = MultiblockSurface.coreOfAny(level, pos);
                if (core == null) return InteractionResult.FAIL;
                if (!(level.getBlockEntity(core) instanceof TileEntityHeatBoiler boiler)) return InteractionResult.FAIL;

                FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                boiler.tanks[0].setTankType(type);
                boiler.setChanged();
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(type.getDisplayName())
                        .append(Component.literal("!")));
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityHeatBoiler boiler)) return null;

        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", boiler.heat) + " TU");
        for (FluidTankNTM tank : boiler.tanks) {
            text.add(tank.getTankType().getDisplayNameString() + ": " + String.format("%,d", tank.getFill()) + "/" + String.format("%,d", tank.getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /**
     * 1.7 {@code MachineHeatBoiler.getDrops}: a boiler that has blown its top ({@code hasExploded}) breaks into
     * 4 steel ingots and 8 copper plates instead of dropping itself.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        TileEntityHeatBoiler boiler = null;
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof TileEntityHeatBoiler direct) {
            boiler = direct;
        } else {
            Vec3 origin = params.getOptionalParameter(
                    LootContextParams.ORIGIN);
            BlockPos core = origin == null ? null : MultiblockSurface.coreOfAny(params.getLevel(), BlockPos.containing(origin));
            if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntityHeatBoiler found) boiler = found;
        }
        if (boiler != null && boiler.hasExploded) {
            return List.of(new ItemStack(ModItems.ingot_steel.get(), 4),
                    new ItemStack(ModItems.plate_copper.get(), 8));
        }
        return super.getDrops(state, params);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    /** Two side ports that ride the facing, and the steam outlet three above. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot), MASK_WEST);
        visitor.cell(core.relative(rot.getOpposite()), MASK_EAST);
        visitor.cell(core.above(3), MASK_UP);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(core.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(rot.getOpposite()), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.above(3), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {-1, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 3, 0, ForgeDirection.UP.ordinal()},
        }, dir);
    }

    @Override
    public double[][] getAABBExtras() {
        return new double[][] {
                {0, 0, 0.5, -0.3, 0.4, -0.4, 1},
        };
    }
}
