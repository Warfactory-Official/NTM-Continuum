package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.hbm.tileentity.machine.TileEntityMachineHephaestus;

/**
 * The geothermal heat exchanger. 1.7 {@code MachineHephaestus}: a 3x12x3 column (dims {@code {11,0,1,1,1,1}},
 * offset 1) whose eight axis-neighbours of the core at y and y+11 are the fluid ports. Right-clicking with a
 * fluid identifier retypes the input tank; there is no menu.
 */
public class MachineHephaestus extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineHephaestus(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHephaestus(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{11, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** 1.7 {@code fillSpace}: {@code makeExtra} on the four axis neighbours at y and again at y+11. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dy : new int[]{0, 11}) {
            visitor.cell(core.offset(1, dy, 0), MASK_EAST, ROLE_FLUID);
            visitor.cell(core.offset(-1, dy, 0), MASK_WEST, ROLE_FLUID);
            visitor.cell(core.offset(0, dy, 1), MASK_SOUTH, ROLE_FLUID);
            visitor.cell(core.offset(0, dy, -1), MASK_NORTH, ROLE_FLUID);
        }
    }

    /** 1.7 stamps {@code TileEntityProxyCombo().fluid()} on every extra. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int dy : new int[]{0, 11}) {
            visitor.passiveCell(core.offset(1, dy, 0), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(-1, dy, 0), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(0, dy, 1), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(0, dy, -1), MASK_ALL, PASSIVE_FLUID_IN);
        }
    }

    /**
     * 1.7 {@code onBlockActivated}: on the server, a non-sneaking click holding a fluid identifier retypes the
     * input tank and reports the change; any other click is passed through. The identifier is an item in the
     * hand, so the held-item entry point is the one that sees it.
     */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(stack.getItem() instanceof IItemFluidId identifier)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineHephaestus heatex)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
            heatex.input.setTankType(type);
            heatex.setChanged();
            player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(type.getDisplayName()).append(Component.literal("!")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineHephaestus heatex)) return null;

        List<String> text = new ArrayList<>();
        text.add(String.format(Locale.US, "%,d", heatex.bufferedHeat) + " TU");

        FluidTankNTM[] tanks = heatex.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            FluidTankNTM tank = tanks[i];
            text.add((i == 0 ? "§a-> " : "§c<- ") + "§r" + tank.getTankType().getDisplayNameString() + ": "
                    + tank.getFill() + "/" + tank.getMaxFill() + "mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** The heat exchanger takes whatever the identifier named; the duct answer is the tile's. */
    @Override
    public boolean acceptsFluid(FluidType type) {
        return true;
    }
}
