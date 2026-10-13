package com.hbm.blocks.generic;

import com.hbm.items.block.ItemBlockDecoVariant;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import com.hbm.tileentity.machine.storage.TileEntityFileCabinet;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code BlockDecoContainer} as the filing cabinet ({@code DecoCabinetEnum} GREEN/STEEL): a
 * {@code BlockDecoModel} that holds a {@link TileEntityFileCabinet} and is drawn by its renderer.
 *
 * <p>1.7 packed the four-way rotation into metadata bits 2..3 ("0 North, 1 South, 2 West, 3 East" - the side the
 * drawers face) and the type into bits 0..1; here they are {@link #FACING} and {@link #TYPE}. The bounds are
 * {@code (3, 0, 0)..(13, 16, 12)} pixels for a south-facing cabinet, turned with it. A right-click opens the
 * drawers if the lock allows, a padlock or key kit is handed to the item, and a sneaking click does nothing.
 */
public class BlockDecoContainer extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<CabinetType> TYPE = EnumProperty.create("type", CabinetType.class);

    private static final VoxelShape NORTH = Block.box(3, 0, 4, 13, 16, 16);
    private static final VoxelShape SOUTH = Block.box(3, 0, 0, 13, 16, 12);
    private static final VoxelShape WEST = Block.box(4, 0, 3, 16, 16, 13);
    private static final VoxelShape EAST = Block.box(0, 0, 3, 12, 16, 13);

    public BlockDecoContainer(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, CabinetType.GREEN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFileCabinet(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        int type = context.getItemInHand().getItem() instanceof ItemBlockDecoVariant item
                ? item.variant(context.getItemInHand()) : 0;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(TYPE, CabinetType.of(type));
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof TileEntityLockableBase) { // annoying accommodations for the filing cabinet
            if (TileEntityLockableBase.LOCK_TOOLS.test(player.getMainHandItem())) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!player.isShiftKeyDown() && ((TileEntityLockableBase) entity).canAccess(player)
                    && entity instanceof TileEntityCrateBase crate && player instanceof ServerPlayer serverPlayer) {
                Menus.openWithPos(serverPlayer, crate, pos);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private ItemStack stackFor(BlockState state) {
        int type = state.getValue(TYPE).ordinal();
        return asItem() instanceof ItemBlockDecoVariant item ? item.of(type) : new ItemStack(this);
    }

    /** 1.7 {@code damageDropped(meta & 3)}: the cabinet keeps its finish. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackFor(state));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return stackFor(state);
    }

    public enum CabinetType implements StringRepresentable {
        GREEN("green"),
        STEEL("steel");

        public static final CabinetType[] VALUES = values();
        private final String name;

        CabinetType(String name) {
            this.name = name;
        }

        public static CabinetType of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** Structure rotation and mirroring turn the placed facing, as vanilla's directional blocks do. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
