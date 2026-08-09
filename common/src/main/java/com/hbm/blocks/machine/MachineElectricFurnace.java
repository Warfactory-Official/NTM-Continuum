package com.hbm.blocks.machine;

import com.hbm.platform.Services;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Electric Furnace — a single horizontal-facing, BlockEntity-backed machine. It smelts vanilla furnace
 * recipes using HE drawn from adjacent power networks (its {@link TileEntityMachineElectricFurnace}
 * implements {@code IEnergyReceiverMK2}, which a red cable connects to directly).
 *
 * <p>The 1.12.2 original used two separate blocks ({@code _off}/{@code _on}); here that becomes a single
 * block with a {@link #LIT} property (the TE flips it while smelting), driving the lit front texture in
 * the blockstate. Right-click opens the menu. The lit "flame particle" effect is deferred (§18).
 */
public class MachineElectricFurnace extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public MachineElectricFurnace(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineElectricFurnace(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_ELECTRIC_FURNACE.get(), TileEntityMachineElectricFurnace::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    /** Smoke + flame puffing from the front face while smelting (LIT) — ported from the 1.12.2
     * {@code randomDisplayTick} of the {@code machine_electric_furnace_on} block. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;

        double f = pos.getX() + 0.5;
        double f1 = pos.getY() + random.nextFloat() * 6.0 / 16.0;
        double f2 = pos.getZ() + 0.5;
        double f3 = 0.52;
        double f4 = random.nextFloat() * 0.6 - 0.3;

        double px, pz;
        switch (state.getValue(FACING)) {
            case WEST -> { px = f - f3; pz = f2 + f4; }
            case EAST -> { px = f + f3; pz = f2 + f4; }
            case NORTH -> { px = f + f4; pz = f2 - f3; }
            default -> { px = f + f4; pz = f2 + f3; } // SOUTH
        }
        level.addParticle(ParticleTypes.SMOKE, px, f1, pz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, px, f1, pz, 0.0, 0.0, 0.0);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            Services.PLATFORM.openMenu(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}
