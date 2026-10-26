package com.hbm.blocks.generic;

import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.blocks.ModDecoContent;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.tool.ItemRebarPlacer;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModDungeonBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import com.hbm.uninos.networkproviders.RebarNetwork;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code BlockRebar}: a cage of twelve steel bars (the block model), placed in bulk by the rebar placer and
 * filled with liquid concrete from the fluid network. 1.7's {@code concrete_liquid} fluid block was never
 * registered; what actually runs is this: a rebar next to a duct carrying {@code CONCRETE} is a fluid receiver,
 * and it hands every delivery to the lowest layer of the rebar cage it belongs to (all rebar connected face to face,
 * 1.7's {@code RebarNetwork}). That layer fills evenly, at most 50 mB per rebar per delivery, and a rebar that
 * reaches 1,000 mB sets into the concrete the placer was loaded with (or reinforced concrete if that is not a valid
 * concrete). The rising concrete is drawn by {@code RenderRebarFill}. Waterloggable (port addition, see
 * {@link Waterlogging}); a rebar that sets into concrete displaces its water.
 */
public class BlockRebar extends CompatBlock implements TickingBlock, SimpleWaterloggedBlock {

    public BlockRebar(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(Waterlogging.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Waterlogging.WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Waterlogging.place(defaultBlockState(), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Waterlogging.fluid(state);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        Waterlogging.tick(state, level, ticks, pos);
        return state;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRebar(pos, state);
    }

    /** The cage's twelve 2x2 bars, the same boxes as the {@code rebarModel()} declaration in {@code ModDungeonBlocks}. */
    private static final VoxelShape BARS = bars();

    private static VoxelShape bars() {
        VoxelShape shape = Shapes.empty();
        double[] at = {3, 11};
        for (double a : at) for (double b : at) {
            shape = Shapes.or(shape, Block.box(a, 0, b, a + 2, 16, b + 2), Block.box(0, a, b, 16, a + 2, b + 2),
                    Block.box(a, b, 0, a + 2, b + 2, 16));
        }
        return shape.optimize();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /*
     * Collision is the bars (a port deviation): 1.7's full cube kept a waterlogged rebar's
     * water in, since vanilla fluid only leaves through faces the collision does not cover. The outline and the
     * support shape stay the 1.7 full cube.
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BARS;
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityRebar rebar)) return;
        rebar.hasConnection = false;
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof IFluidPipeMK2) {
                rebar.hasConnection = true;
                return;
            }
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        RebarNetwork.invalidate();
    }

    public static class TileEntityRebar extends TileEntityLoadedBase implements IFluidReceiverMK2 {

        /** The concrete this rebar sets into: the placer's loaded stack (1.7 stored the block and its metadata). */
        public ItemStack concrete = ItemStack.EMPTY;
        public int progress;
        public int prevProgress;
        public boolean hasConnection = false;

        public TileEntityRebar(BlockPos pos, BlockState state) {
            super(ModDungeonBlockEntities.REBAR.get(), pos, state);
        }

        public TileEntityRebar setup(ItemStack concrete) {
            this.concrete = concrete.copyWithCount(1);
            setChanged();
            return this;
        }

        @Override
        public void clearRemoved() {
            super.clearRemoved();
            RebarNetwork.invalidate();
        }

        @Override
        public boolean canConnect(FluidType type, ForgeDirection dir) {
            return dir == ForgeDirection.UP && type == Fluids.CONCRETE;
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRebar rebar) {
            if (rebar.hasConnection) {
                if (rebar.fluidInPorts == null) {
                    rebar.fluidInPorts = new TilePort[] {new TilePort().setupOwner(rebar).setupType(FluidNetProvider.of(Fluids.CONCRETE))
                            .setupPositions(pos.immutable()).setupConnections(new DirPos(pos.getX(), pos.getY() + 1, pos.getZ(), Library.POS_Y))};
                }
                rebar.fluidInPorts[0].update(level);
                rebar.fluidInPorts[0].checkSubscribe(level);
            } else if (rebar.fluidInPorts != null) {
                rebar.fluidInPorts[0].disableIfPresent(level);
                rebar.fluidInPorts = null;
            }

            if (rebar.prevProgress != rebar.progress) {
                rebar.setChanged();
                rebar.prevProgress = rebar.progress;
            }

            if (rebar.progress >= 1_000) {
                level.setBlock(pos, rebar.setState(), 3);
                return;
            }

            rebar.networkPackNT();
        }

        /** What the rebar becomes once full: the loaded concrete if it is a valid one, else reinforced concrete. */
        BlockState setState() {
            if (!concrete.isEmpty() && ItemRebarPlacer.isValidConk(concrete) && concrete.getItem() instanceof BlockItem item) {
                Block block = item.getBlock();
                if (block instanceof BlockConcreteColored colored) return colored.stateForItem(concrete);
                if (block instanceof BlockConcreteColoredExt ext) return ext.stateForItem(concrete);
                return block.defaultBlockState();
            }
            return ModDecoContent.concrete_rebar.get().defaultBlockState();
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(progress);
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            super.deserialize(buf);
            this.progress = buf.readInt();
        }

        @Override
        protected void loadData(DataIn nbt) {
            super.loadData(nbt);
            this.progress = nbt.getIntOr("progress", 0);
            this.hasConnection = nbt.getBooleanOr("hasConnection", false);
            this.concrete = nbt.read("concrete", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }

        @Override
        protected void saveData(DataOut nbt) {
            super.saveData(nbt);
            nbt.putInt("progress", this.progress);
            nbt.putBoolean("hasConnection", this.hasConnection);
            if (!concrete.isEmpty()) nbt.store("concrete", ItemStack.CODEC, concrete);
        }

        @Override
        public FluidTankNTM[] getAllTanks() {
            FluidTankNTM tank = new FluidTankNTM(Fluids.CONCRETE, 1_000);
            tank.setFill(progress);
            return new FluidTankNTM[]{tank};
        }

        @Override
        public long transferFluid(FluidType type, int pressure, long amount) {
            if (type != Fluids.CONCRETE || level == null) return amount;

            List<TileEntityRebar> lowestLinks = new ArrayList<>();
            int lowestY = Integer.MAX_VALUE;
            int progress = 0;
            int capacity = 0;

            for (BlockPos link : RebarNetwork.linksOf(level, worldPosition)) {
                int y = link.getY();
                if (y < lowestY) {
                    lowestY = y;
                    progress = 0;
                    capacity = 0;
                    lowestLinks.clear();
                }
                if (y == lowestY) {
                    if (!(level.getBlockEntity(link) instanceof TileEntityRebar rebar)) continue;
                    progress += rebar.progress;
                    capacity += 1_000;
                    lowestLinks.add(rebar);
                }
            }

            if (capacity > 0 && !lowestLinks.isEmpty()) {
                int maxSpeed = 50;
                int maxAccept = (int) Math.min(Math.min(capacity - progress, amount), maxSpeed * lowestLinks.size());
                int target = Math.min((progress + maxAccept) / lowestLinks.size(), 1_000);

                for (TileEntityRebar rebar : lowestLinks) {
                    if (rebar.progress >= target) continue;
                    int delta = target - rebar.progress;
                    if (delta > amount) continue;
                    rebar.progress += delta;
                    amount -= delta;
                }
            }
            return amount;
        }

        @Override
        public long getDemand(FluidType type, int pressure) {
            return type == Fluids.CONCRETE && pressure == 0 ? 10_000L : 0L;
        }
    }
}
