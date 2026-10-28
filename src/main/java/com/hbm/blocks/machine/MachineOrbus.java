package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.storage.TileEntityMachineOrbus;
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

/**
 * 1.7 {@code MachineOrbus}: a 4x4 footprint, five tall ({@code {4, 0, 2, 1, 2, 1}}, offset 1), drawn as one
 * tank centred on the corner the core shares with three other middle columns. Those four columns are fluid
 * proxies at the bottom and at the top layer ({@code makeExtra} at {@code y} and {@code y + 4}): the bottom
 * four take ducts from below, the top four from above. A plain click opens the barrel menu; a sneaking click
 * with a fluid identifier retypes the tank, as it did for barrels.
 */
public class MachineOrbus extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ICapabilityBlock {

    public MachineOrbus(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineOrbus(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 2, 1, 2, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * 1.7's loop also calls {@code makeExtra} on the core position at {@code y + 0} (a no-op there, the core
     * already holds the tile), and {@code getConPos} lists the core column's underside: the core's bottom
     * face is a port. The cell loop below skips the core, so its face is stated here.
     */
    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    /**
     * 1.7 {@code fillSpace} marks the core column, the one behind it ({@code dir.getOpposite()}) and the two beside
     * those ({@code dir.getRotation(UP)}) extra at the bottom and at {@code y + 4}, and all eight are ports.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ForgeDirection d2 = dir.getRotation(ForgeDirection.UP);
        ForgeDirection back = dir.getOpposite();
        visitor.cell(core.offset(back.offsetX, 0, back.offsetZ), MASK_DOWN, ROLE_FLUID);
        visitor.cell(core.offset(d2.offsetX, 0, d2.offsetZ), MASK_DOWN, ROLE_FLUID);
        visitor.cell(core.offset(back.offsetX + d2.offsetX, 0, back.offsetZ + d2.offsetZ), MASK_DOWN, ROLE_FLUID);
        visitor.cell(core.offset(0, 4, 0), MASK_UP, ROLE_FLUID);
        visitor.cell(core.offset(back.offsetX, 4, back.offsetZ), MASK_UP, ROLE_FLUID);
        visitor.cell(core.offset(d2.offsetX, 4, d2.offsetZ), MASK_UP, ROLE_FLUID);
        visitor.cell(core.offset(back.offsetX + d2.offsetX, 4, back.offsetZ + d2.offsetZ), MASK_UP, ROLE_FLUID);
    }

    @Override
    protected boolean carriesData() {
        return true;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          ItemStack stack, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.getItem() instanceof IItemFluidId ident && !level.isClientSide()
                && level.getBlockEntity(core) instanceof TileEntityMachineOrbus orbus) {
            FluidType type = ident.getType(level, core.getX(), core.getY(), core.getZ(), stack);
            orbus.tank.setTankType(type);
            orbus.setChanged();
            player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(type.getDisplayName()).append(Component.literal("!")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 0, -1, ForgeDirection.DOWN.ordinal()},
                {0, 0, 0, ForgeDirection.DOWN.ordinal()},
                {1, 0, -1, ForgeDirection.DOWN.ordinal()},
                {1, 0, 0, ForgeDirection.DOWN.ordinal()},
                {0, 4, -1, ForgeDirection.UP.ordinal()},
                {0, 4, 0, ForgeDirection.UP.ordinal()},
                {1, 4, -1, ForgeDirection.UP.ordinal()},
                {1, 4, 0, ForgeDirection.UP.ordinal()},
        }, dir);
    }

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().fluidIn().fluidOut().items()
            .fluidContacts(TileEntityBarrel.class);

    @Override
    public MachineCaps caps() { return CAPS; }
}
