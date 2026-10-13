package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityPipeAnchor;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code FluidPipeAnchor}: a steel stub mounted on a block face, conducting through that face
 * only. {@link #FACING} is the face it was placed against (1.7 metadata = clicked side); the open side is
 * its opposite. The block itself is a plain cube-model block (1.7 render type -1, {@code block_steel}).
 *
 * <p>Linked with the pipe wrench to other anchors up to 10 m away ({@link TileEntityPipeAnchor}), drawn by
 * {@code RenderPipeAnchor}.
 */
public class FluidPipeAnchor extends BlockFluidDuct {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public FluidPipeAnchor(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 1.7 render type -1: RenderPipeAnchor draws the OBJ anchor and its pipelines
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        return state.setValue(FACING, context.getClickedFace());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPipeAnchor(pos, state);
    }

    /** The 1.7 bounds: a 8×8×8 core stretched to the mounted face. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        float min = 0.25F, max = 0.75F;
        ForgeDirection dir = ForgeDirection.getOrientation(state.getValue(FACING)).getOpposite();
        float minX = dir == ForgeDirection.WEST ? 0F : min;
        float maxX = dir == ForgeDirection.EAST ? 1F : max;
        float minY = dir == ForgeDirection.DOWN ? 0F : min;
        float maxY = dir == ForgeDirection.UP ? 1F : max;
        float minZ = dir == ForgeDirection.NORTH ? 0F : min;
        float maxZ = dir == ForgeDirection.SOUTH ? 1F : max;
        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 1.7 {@code changeTypeRecursively} on an anchor followed only its mounted side; the parent's
     * sneak-spread walks all six neighbours, so the anchor retypes itself and its one neighbour instead.
     */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId ident)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityPipeAnchor anchor) {
            FluidType type = ident.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            if (type != anchor.getFluidType()) {
                FluidType prev = anchor.getFluidType();
                anchor.setFluidType(type);
                level.setBlock(pos, state, 3);
                if (player.isShiftKeyDown()) {
                    ForgeDirection open = anchor.openSide();
                    BlockPos next = pos.offset(open.offsetX, open.offsetY, open.offsetZ);
                    if (level.getBlockEntity(next) instanceof TileEntityFluidDuctNT) {
                        BlockFluidDuct.spreadType(level, next, type, prev, 63);
                    }
                    // 1.7 changeTypeRecursively also followed every linked pipeline
                    for (BlockPos linked : List.copyOf(anchor.getConnected())) {
                        if (level.getBlockEntity(linked) instanceof TileEntityPipeAnchor other && other.getFluidType() == prev) {
                            BlockFluidDuct.spreadType(level, linked, type, prev, 63);
                        }
                    }
                }
                player.sendSystemMessage(Component.literal("Duct fluid: ").append(type.getDisplayName())
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
