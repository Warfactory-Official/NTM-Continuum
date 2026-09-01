package com.hbm.blocks.machine;

import com.hbm.api.block.IBlowable;
import com.hbm.api.block.IToolable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** The six-way redstone fan with the legacy obstruction, force, particle, and tool-mode behavior. */
public class MachineFan extends dev.golem.core.world.CompatBlock implements EntityBlock, IToolable {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final Supplier<? extends BlockEntityType<TileEntityFan>> type;

    public MachineFan(Properties properties, Supplier<? extends BlockEntityType<TileEntityFan>> type) {
        super(properties.noOcclusion());
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFan(type.get(), pos, state);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        return player == null ? defaultBlockState()
                : defaultBlockState().setValue(FACING, IntakeFanDirections.fromEntity(context.getClickedPos(), player));
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof TileEntityFan fan) {
            fan.setIndirectlyPowered(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool == ToolType.SCREWDRIVER) {
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getOpposite()), 3);
            return true;
        }

        if (tool == ToolType.HAND_DRILL) {
            if (level.getBlockEntity(pos) instanceof TileEntityFan fan) {
                fan.falloff = !fan.falloff;
                fan.markChanged();
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.translatable(
                            "tile.fan." + (fan.falloff ? "falloffOn" : "falloffOff")));
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 0.5F);
                }
            }
            return true;
        }

        if (tool == ToolType.DEFUSER) {
            if (level.getBlockEntity(pos) instanceof TileEntityFan fan) {
                fan.suck = !fan.suck;
                fan.markChanged();
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.translatable(
                            "tile.fan." + (fan.suck ? "suckOn" : "suckOff")));
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 0.5F);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> actualType) {
        return createTickerHelper(actualType, type.get(), TileEntityFan::tick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    /** Legacy nested type retained so the old block/entity relationship stays visible to integrations. */
    public static class TileEntityFan extends TileEntityLoadedBase {

        public float spin;
        public float prevSpin;
        public boolean falloff = true;
        public boolean suck;
        private boolean indirectlyPowered;
        private boolean powerStateInitialized;

        public TileEntityFan(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        private void setIndirectlyPowered(boolean powered) {
            indirectlyPowered = powered;
            powerStateInitialized = true;
        }

        private void refreshPowerState(Level level) {
            if (!powerStateInitialized) setIndirectlyPowered(level.hasNeighborSignal(worldPosition));
        }

        public static void tick(Level level, BlockPos pos, BlockState state, TileEntityFan fan) {
            fan.refreshPowerState(level);
            fan.prevSpin = fan.spin;

            if (fan.indirectlyPowered) {
                // The legacy TE read metadata from the world on every update; do not trust a cached BE state.
                Direction direction = level.getBlockState(pos).getValue(FACING);
                int effectiveRange = 0;

                for (int distance = 1; distance <= FanPhysics.RANGE; distance++) {
                    BlockPos targetPos = pos.relative(direction, distance);
                    BlockState targetState = level.getBlockState(targetPos);
                    boolean blowable = targetState.getBlock() instanceof IBlowable;
                    if (Compat.isSolidRender(targetState, level, targetPos) || blowable) {
                        if (!level.isClientSide() && blowable) {
                            ((IBlowable) targetState.getBlock()).applyFan(level, targetPos,
                                    ForgeDirection.getOrientation(direction), distance);
                        }
                        break;
                    }
                    effectiveRange = distance;
                }

                net.minecraft.world.phys.AABB area = FanPhysics.affectedBox(pos, direction, effectiveRange);
                for (Entity entity : level.getEntitiesOfClass(Entity.class, area)) {
                    double distance = Math.sqrt(entity.distanceToSqr(pos.getX() + 0.5D,
                            pos.getY() + 0.5D, pos.getZ() + 0.5D));
                    double coefficient = FanPhysics.coefficient(distance, fan.falloff, fan.suck);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(
                            direction.getStepX() * coefficient,
                            direction.getStepY() * coefficient,
                            direction.getStepZ() * coefficient));
                }

                if (level.isClientSide() && level.getRandom().nextInt(30) == 0) {
                    double speed = fan.suck ? -0.2D : 0.2D;
                    level.addParticle(ParticleTypes.CLOUD,
                            pos.getX() + 0.5D + direction.getStepX() * 0.5D,
                            pos.getY() + 0.5D + direction.getStepY() * 0.5D,
                            pos.getZ() + 0.5D + direction.getStepZ() * 0.5D,
                            direction.getStepX() * speed,
                            direction.getStepY() * speed,
                            direction.getStepZ() * speed);
                }

                fan.spin += 30F;
            }

            if (fan.spin >= 360F) {
                fan.prevSpin -= 360F;
                fan.spin -= 360F;
            }

            if (!level.isClientSide()) fan.networkPackNT();
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            super.serialize(buf);
            buf.writeBoolean(falloff);
            buf.writeBoolean(suck);
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            super.deserialize(buf);
            falloff = buf.readBoolean();
            suck = buf.readBoolean();
        }

        @Override
        protected void saveData(DataOut output) {
            super.saveData(output);
            output.putBoolean("falloff", falloff);
            output.putBoolean("suck", suck);
        }

        @Override
        protected void loadData(DataIn input) {
            super.loadData(input);
            falloff = input.getBooleanOr("falloff", true);
            suck = input.getBooleanOr("suck", false);
        }
    }
}
