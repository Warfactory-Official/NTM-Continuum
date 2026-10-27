package com.hbm.blocks.network;

import com.hbm.blocks.generic.ItemStateBlock;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.block.IToolable;
import com.hbm.tileentity.network.DuctPaint;
import com.hbm.tileentity.network.TileEntityPipePaintable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code FluidDuctPaintable}: a full-cube coated duct, a steel cube with a fluid-coloured port overlay on every
 * face. A solid block right-clicked onto it paints it (the cube wears that block), the screwdriver strips the paint,
 * the defuser hides the port overlay over the paint, and the copy tool carries the paint. Networking is the plain
 * duct's (the connection flags only feed the look).
 */
public class FluidDuctPaintable extends BlockFluidDuct implements ITooltipProvider, IToolable {

    /** 1.7 metadata 0: the port overlay over the paint; the defuser turns it off. */
    public static final BooleanProperty OVERLAY = BooleanProperty.create("overlay");

    public FluidDuctPaintable(Properties properties) {
        super(properties);
        if (paintable()) registerDefaultState(defaultBlockState().setValue(OVERLAY, true));
    }

    /** Whether the cube takes paint; the gauge, its 1.7 sibling, does not. Asked while the state is built. */
    protected boolean paintable() {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if (paintable()) builder.add(OVERLAY);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPipePaintable(pos, state);
    }

    /** 1.7: a solid block's item paints an unpainted cube; anything else is the duct's own use (a fluid identifier). */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (paintable() && stack.getItem() instanceof BlockItem item && BlockCablePaintable.allowedPaint(item.getBlock(), this)
                && level.getBlockEntity(pos) instanceof DuctPaint.Holder duct && duct.paint().get() == null) {
            if (!level.isClientSide()) duct.paint().set((BlockEntity) duct, ItemStateBlock.of(item, stack));
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand,
                           ToolType tool) {
        if (!paintable()) return false;
        if (tool == ToolType.SCREWDRIVER && level.getBlockEntity(pos) instanceof DuctPaint.Holder duct
                && duct.paint().get() != null) {
            if (!level.isClientSide()) duct.paint().set((BlockEntity) duct, null);
            return true;
        }
        if (tool == ToolType.DEFUSER) {
            BlockState state = level.getBlockState(pos);
            if (!level.isClientSide()) level.setBlock(pos, state.setValue(OVERLAY, !state.getValue(OVERLAY)), 3);
            return true;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** The coated ducts are cube models: 1.7 pass 0 the block icon or the paint, pass 1 the overlay. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** A full cube never holds water (the parent's waterlogging is for the thin pipe). */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(WATERLOGGED, false);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }
}
