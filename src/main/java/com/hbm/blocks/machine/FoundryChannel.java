package com.hbm.blocks.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import dev.golem.core.registry.TickingBlock;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Foundry channel (1.12.2 {@code FoundryChannel}): the molten-metal gutter. The blockstate carries the
 * old 4-bit connection mask (E=1, W=2, S=4, N=8: neighbouring channels, the mold caster, and outlets
 * facing this block), which drives both the JSON model variant and the shape. Right-click with a shovel
 * empties the content to scraps. Molten level is drawn by the channel BER.
 */
public class FoundryChannel extends CompatBlock implements TickingBlock, ICrucibleAcceptor {

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    public FoundryChannel(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(META, getConnectionMeta(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState) {
        return state.setValue(META, getConnectionMeta(level, pos));
    }

    private int getConnectionMeta(LevelReader world, BlockPos pos) {
        int meta = 0;
        if (canConnectTo(world, pos, Direction.EAST)) meta |= 1;
        if (canConnectTo(world, pos, Direction.WEST)) meta |= 2;
        if (canConnectTo(world, pos, Direction.SOUTH)) meta |= 4;
        if (canConnectTo(world, pos, Direction.NORTH)) meta |= 8;
        return meta;
    }

    public boolean canConnectTo(BlockGetter world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = world.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        if (neighborBlock instanceof FoundryOutlet) {
            return neighborState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    && neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == direction;
        }

        return neighborBlock instanceof FoundryChannel || neighborBlock instanceof FoundryMold;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        int meta = state.getValue(META);
        double minX = (meta & 2) != 0 ? 0 : 0.3125D;
        double maxX = (meta & 1) != 0 ? 1 : 0.6875D;
        double minZ = (meta & 8) != 0 ? 0 : 0.3125D;
        double maxZ = (meta & 4) != 0 ? 1 : 0.6875D;
        return Shapes.box(minX, 0, minZ, maxX, 0.5D, maxZ);
    }

    // --- BlockEntity + ticker ---

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFoundryChannel(pos, state);
    }

    /** Shovel right-click empties the content to scraps. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityFoundryChannel cast && cast.amount > 0 && cast.type != null) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(cast.type, cast.amount));
                if (!player.getInventory().add(scrap)) {
                    ItemScraps.spawnMats(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new MaterialStack(cast.type, cast.amount));
                }
                cast.amount = 0;
                cast.type = null;
                cast.propagateMaterial(null);
                cast.setChanged();
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    // --- ICrucibleAcceptor: delegate to the TE ---

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        return world.getBlockEntity(pos) instanceof TileEntityFoundryChannel te && te.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityFoundryChannel te)) return stack;
        return te.pour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) {
        return world.getBlockEntity(pos) instanceof TileEntityFoundryChannel te && te.canAcceptPartialFlow(world, pos, side, stack);
    }

    @Override
    public MaterialStack flow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityFoundryChannel te)) return stack;
        return te.flow(world, pos, side, stack);
    }
}
