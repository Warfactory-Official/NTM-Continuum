package com.hbm.blocks.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFoundryOutlet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Foundry outlet (1.12.2 {@code FoundryOutlet}) — a filtered, redstone-closable tap that takes flow
 * from a channel behind it and re-pours 4 blocks down. Right-click with scraps sets the material
 * filter; empty-hand right-click clears it (the invert toggles are deferred with IToolable). Drawn by
 * the BER (the old {@code foundry_outlet.obj}).
 */
public class FoundryOutlet extends dev.golem.core.world.CompatBlock implements EntityBlock, ICrucibleAcceptor, ILookOverlay {

    public FoundryOutlet(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    /** The 1.12.2 per-facing hitboxes: a half-height nub offset toward the channel side. */
    private static final VoxelShape SHAPE_WEST = Shapes.box(0.625, 0, 0.3125, 1, 0.5, 0.6875);
    private static final VoxelShape SHAPE_EAST = Shapes.box(0, 0, 0.3125, 0.375, 0.5, 0.6875);
    private static final VoxelShape SHAPE_NORTH = Shapes.box(0.3125, 0, 0.625, 0.6875, 0.5, 1);
    private static final VoxelShape SHAPE_SOUTH = Shapes.box(0.3125, 0, 0, 0.6875, 0.5, 0.375);

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 1.12.2: placer.getHorizontalFacing().getOpposite() — the outlet faces the player.
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // OBJ drawn by the BER
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFoundryOutlet(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : FoundryChannel.createTickerHelper(type, ModBlockEntities.FOUNDRY_OUTLET.get(), TileEntityFoundryOutlet::serverTick);
    }

    protected TileEntityFoundryOutlet getTE(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityFoundryOutlet te ? te : null;
    }

    /** 1.12.2 interactions: scraps set the filter, any other right-click toggles the redstone invert;
     * sneak-right-click with an empty hand clears the filter (replaces the old screwdriver). */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        MaterialStack mats = ItemScraps.getMats(stack);
        if (mats != null) {
            if (!level.isClientSide()) {
                TileEntityFoundryOutlet te = getTE(level, pos);
                if (te != null) {
                    te.filter = mats.material;
                    te.setChanged();
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            TileEntityFoundryOutlet te = getTE(level, pos);
            if (te != null) {
                if (player.isShiftKeyDown()) {
                    // screwdriver replacement: clear the filter + the filter invert
                    te.filter = null;
                    te.invertFilter = false;
                } else {
                    te.invertRedstone = !te.invertRedstone;
                }
                te.setChanged();
            }
        }
        return InteractionResult.SUCCESS;
    }

    // --- ICrucibleAcceptor: delegate to the TE (flow only) ---

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) { return false; }

    @Override
    public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) { return stack; }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) {
        TileEntityFoundryOutlet te = getTE(world, pos);
        return te != null && te.canAcceptPartialFlow(world, pos, side, stack);
    }

    @Override
    public MaterialStack flow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) {
        TileEntityFoundryOutlet te = getTE(world, pos);
        return te == null ? stack : te.flow(world, pos, side, stack);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        TileEntityFoundryOutlet te = getTE(level, pos);
        if (te == null) return null;
        List<String> text = new ArrayList<>();
        text.add(te.isClosed() ? "§cClosed" : "§aOpen");
        if (te.filter != null) {
            text.add("&[" + te.filter.moltenColor + "&]Filter: " + te.filter.getDisplayName());
        }
        if (te.invertFilter) text.add("§eFilter inverted");
        if (te.invertRedstone) text.add("§2Redstone inverted");
        return new ILookOverlay.Data(this.getName().getString(), 0xFF4000, 0x401000, text);
    }
}
