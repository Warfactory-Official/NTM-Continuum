package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityDiFurnace;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Fuel-fired DiFurnace block, with the old active variant represented by {@link #LIT}. */
public class MachineDiFurnace extends dev.golem.core.world.CompatBlock
        implements TickingBlock, WorldlyContainerHolder {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Selects the tall front/side textures when the separate extension is installed above. */
    public static final BooleanProperty EXT = BooleanProperty.create("ext");

    public MachineDiFurnace(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(EXT, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDiFurnace(pos, state);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityDiFurnace furnace ? furnace : null;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityDiFurnace furnace
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(furnace) : 0;
    }

    public int comparatorOutput(BlockState state, Level level, BlockPos pos) {
        return onAnalogOutputSignal(state, level, pos);
    }

    /** The old onBlockAdded facing correction, retained for existing builds placed against solid blocks. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) return;

        Direction facing = state.getValue(FACING);
        BlockState north = level.getBlockState(pos.north());
        BlockState south = level.getBlockState(pos.south());
        BlockState west = level.getBlockState(pos.west());
        BlockState east = level.getBlockState(pos.east());

        if (facing == Direction.NORTH && Compat.isSolidRender(north, level, pos.north())
                && !Compat.isSolidRender(south, level, pos.south())) facing = Direction.SOUTH;
        else if (facing == Direction.SOUTH && Compat.isSolidRender(south, level, pos.south())
                && !Compat.isSolidRender(north, level, pos.north())) facing = Direction.NORTH;
        else if (facing == Direction.WEST && Compat.isSolidRender(west, level, pos.west())
                && !Compat.isSolidRender(east, level, pos.east())) facing = Direction.EAST;
        else if (facing == Direction.EAST && Compat.isSolidRender(east, level, pos.east())
                && !Compat.isSolidRender(west, level, pos.west())) facing = Direction.WEST;

        if (facing != state.getValue(FACING)) level.setBlock(pos, state.setValue(FACING, facing), 2);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + random.nextDouble() * 6.0D / 16.0D + 0.2D;
        double z = pos.getZ() + 0.5D;
        double sideRandom = random.nextFloat() * 0.5D - 0.25D;
        double xRandom = random.nextFloat() * 0.375D - 0.1875D;
        double zRandom = random.nextFloat() * 0.375D - 0.1875D;
        double smokeY = pos.getY() + (state.getValue(EXT) ? 2D : 1D);
        double sideOffset = 0.52D;

        if (random.nextDouble() < 0.1D) {
            level.playLocalSound(pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                    1.0F, 1.0F, false);
        }

        switch (state.getValue(FACING)) {
            case WEST -> {
                level.addParticle(ParticleTypes.FLAME, x - sideOffset, y, z + sideRandom, 0D, 0D, 0D);
                level.addParticle(ParticleTypes.SMOKE, x + xRandom, smokeY, z + zRandom, 0D, 0D, 0D);
            }
            case EAST -> {
                level.addParticle(ParticleTypes.FLAME, x + sideOffset, y, z + sideRandom, 0D, 0D, 0D);
                level.addParticle(ParticleTypes.SMOKE, x + xRandom, smokeY, z + zRandom, 0D, 0D, 0D);
            }
            case NORTH -> {
                level.addParticle(ParticleTypes.FLAME, x + sideRandom, y, z - sideOffset, 0D, 0D, 0D);
                level.addParticle(ParticleTypes.SMOKE, x + xRandom, smokeY, z + zRandom, 0D, 0D, 0D);
            }
            case SOUTH -> {
                level.addParticle(ParticleTypes.FLAME, x + sideRandom, y, z + sideOffset, 0D, 0D, 0D);
                level.addParticle(ParticleTypes.SMOKE, x + xRandom, smokeY, z + zRandom, 0D, 0D, 0D);
            }
            default -> { }
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, EXT);
    }

    public static boolean hasExtension(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(DiFurnaceContent.MACHINE_DIFURNACE_EXTENSION.get());
    }

    /** Changes only state properties, so the block entity survives the old active/inactive transition. */
    public static void updateBlockState(boolean lit, boolean extension, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(LIT) || !state.hasProperty(EXT)) return;
        BlockState updated = state.setValue(LIT, lit).setValue(EXT, extension);
        if (updated != state) level.setBlock(pos, updated, 2);
    }
}
