package com.hbm.blocks.machine;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
public class MachineOrbus extends BlockMultiblockMachine implements IFluidConnectorBlockMK2 {

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
     * 1.7 {@code fillSpace}: the core column, the one behind it ({@code dir.getOpposite()}) and the two beside
     * those ({@code dir.getRotation(UP)}), each marked extra at the bottom and at {@code y + 4}.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        ForgeDirection dir = ForgeDirection.getOrientation(facing);
        ForgeDirection d2 = dir.getRotation(ForgeDirection.UP);
        ForgeDirection back = dir.getOpposite();
        for (int i = 0; i < 5; i += 4) {
            int mask = i == 0 ? MASK_DOWN : MASK_UP;
            if (i != 0) visitor.cell(core.above(i), mask, ROLE_FLUID);
            visitor.cell(core.offset(back.offsetX, i, back.offsetZ), mask, ROLE_FLUID);
            visitor.cell(core.offset(d2.offsetX, i, d2.offsetZ), mask, ROLE_FLUID);
            visitor.cell(core.offset(back.offsetX + d2.offsetX, i, back.offsetZ + d2.offsetZ), mask, ROLE_FLUID);
        }
    }

    /**
     * 1.7 {@code getDrops} returned {@code IPersistentNBT.getDrops} of the core found from whichever part was broken,
     * so the tank's contents came along from every cell. A cell here is the machine's own block with no block entity
     * (its loot table is limited to the core), so a cell broken by a player drops the core's loot, rolled with the
     * core's block entity, exactly as {@code BlockMultiblockCell} does for shared cells.
     */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.onPlayerWillDestroy(level, pos, state, player);
        if (!(level instanceof ServerLevel server) || !state.getValue(CELL)
                || playerPreventsBlockDrops(player) || !player.hasCorrectToolForDrops(state)) return;
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return;
        BlockState coreState = level.getBlockState(core);
        if (coreState.getBlock() != this) return;
        Block.dropResources(coreState, server, pos, server.getBlockEntity(core), player,
                player.getMainHandItem());
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
}
