package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityBarrelDeco;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code BlockFluidBarrel}: the five storage drums ({@code barrel_plastic} 12,000 mB,
 * {@code barrel_corroded} 6,000, {@code barrel_steel} 16,000, {@code barrel_tcalloy} 24,000,
 * {@code barrel_antimatter} 16,000). A click opens the barrel's menu; a sneak-click with a fluid
 * identifier sets the fluid. The corroded barrel is decoration in 1.7 (no tile, no menu) and stays so.
 * A non-empty barrel keeps its fluid when picked up (see {@link TileEntityBarrel#collectCarriedData}).
 * Drawn by the {@code barrel.obj} renderer, with a connector on every side a duct of its fluid touches.
 */
public class BlockFluidBarrel extends dev.golem.core.world.CompatBlock implements TickingBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.125, 0, 0.125, 0.875, 1, 0.875);

    public final int capacity;
    public final String texture;
    public final boolean corroded;

    public BlockFluidBarrel(Properties properties, int capacity, String texture, boolean corroded) {
        super(properties);
        this.capacity = capacity;
        this.texture = texture;
        this.corroded = corroded;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return corroded ? new TileEntityBarrelDeco(pos, state) : new TileEntityBarrel(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (corroded) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && stack.getItem() instanceof IItemFluidId ident) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityBarrel barrel) {
                FluidType type = ident.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                barrel.tank.setTankType(type);
                barrel.setChanged();
                level.updateNeighborsAt(pos, this);
                if (level instanceof ServerLevel server) com.hbm.uninos.graph.LevelNodeGraph.invalidateEndpointsAt(server, pos);
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(type.getDisplayName()).append(Component.literal("!")));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (corroded) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS; // 1.7: sneaking without an identifier does nothing but consumes the click
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileEntityBarrel barrel) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, barrel, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) com.hbm.uninos.graph.LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityBarrel barrel ? barrel.getComparatorPower() : 0;
    }
}
