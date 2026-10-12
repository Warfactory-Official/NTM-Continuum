package com.hbm.capability;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidDirectionalMK2;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.api.fluidmk2.IInfiniteFluidItem;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.PortClaims;
import dev.golem.core.fluid.FluidContainer;
import dev.golem.core.fluid.FluidExposure;
import dev.golem.core.fluid.FluidMachine;
import dev.golem.core.fluid.FluidPorts;
import dev.golem.core.fluid.FluidUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HBM's machines and container items on the loaders' standard fluid capabilities (History section 19ek-fluids), so
 * other mods' pipes, tanks and probes can see and move HBM fluids. Core's {@link FluidExposure} owns the loader
 * spellings (NeoForge 26.1/1.21.1, Forge 1.20.1, Fabric's Transfer API with droplets); this class only says what an
 * HBM machine and an HBM container are, in HBM's own terms.
 *
 * <p>The rules are HBM's, never a second set:
 * <ul>
 *   <li>A face is open exactly where a duct would connect: {@link FluidNetwork#userAtFace} is asked with the fluid
 *       actually crossing, so the machine's own {@code canConnect} (side and type), the endpoint registry and folded
 *       multiblock cells all answer as they do for a duct.</li>
 *   <li>Fluid enters through {@link IFluidReceiverMK2#transferFluid} after {@link IFluidReceiverMK2#getDemand} and
 *       leaves through {@link IFluidProviderMK2#useUpFluid} after {@link IFluidProviderMK2#getFluidAvailable}, both
 *       limited by the receiver/provider speed: a typed tank takes only its type, a barrel's mode and a pump's
 *       intake/outlet hold, and nothing is written into a tank directly.</li>
 *   <li>Pressure 0 only: a foreign pipe carries no pressure, so a pressurised tank answers neither way.</li>
 *   <li>A directional user (the flow-control pump) receives only on its intake and provides only on its outlet.</li>
 *   <li>A role is offered only at a port the machine claims it at, as for a duct: its 1.7 {@code trySubscribe} /
 *       {@code tryProvide} at the position the foreign pipe stands at, on this side ({@link PortClaims}).</li>
 * </ul>
 *
 * <p>HBM's own duct network is unchanged: it does not reach foreign tanks, and ducts are not exposed.
 */
public final class ForeignFluids {

    private ForeignFluids() {}

    private static final int PRESSURE = 0;
    private static final int FLUID = MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT;

    /** Common init, once, before the loaders' capability registration. */
    public static void register() {
        FluidExposure.exposeBlocks(PORTS, ForeignFluids::carriesFluids);
        // Registered on every HBM item: the row table builds item stacks, which NeoForge's capability registration
        // runs too early for; handles() narrows it per stack when it is asked.
        FluidExposure.exposeItems(CONTAINERS, item -> item instanceof IFillableItem || isHbm(item));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Machines
    // ------------------------------------------------------------------------------------------------------------

    /** HBM blocks that can be or front a fluid machine: block entities and folded multiblock connector cells. */
    private static boolean carriesFluids(Block block) {
        if (!(block instanceof EntityBlock) && !(block instanceof IFluidConnectorBlockMK2)) return false;
        return Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    public static final FluidPorts PORTS = ForeignFluids::machineAt;

    /**
     * The HBM fluid machine behind {@code pos}, seen through {@code side}, or null. A face is offered where some tank
     * of the machine could connect: a typed tank whose type connects on this side, or an untyped tank (which may be
     * typed later). A sideless query (a probe) sees the tanks and moves nothing.
     */
    public static @Nullable FluidMachine machineAt(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        BlockEntity be = candidate(level, pos, side);
        if (!(be instanceof IFluidUserMK2)) return null;
        // the user this face reaches, as for a duct (a machine with several users, IFluidMultiUserMK2)
        IFluidUserMK2 user = com.hbm.api.fluidmk2.IFluidMultiUserMK2.resolve(be, pos, side);
        if (user == null) return null;
        FluidTankNTM[] tanks = user.getAllTanks();
        if (tanks == null || tanks.length == 0) return null;
        Face face = new Face(level, pos.immutable(), side, be, user);
        if (side == null) return face;
        for (FluidTankNTM tank : tanks) {
            FluidType type = tank.getTankType();
            if (type == Fluids.NONE || face.connects(type)) return face;
        }
        return null;
    }

    /** The same three routes as the duct's own face probe, without the fluid: endpoint registry, block entity, folded cell. */
    private static @Nullable BlockEntity candidate(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        BlockEntity declared = EndpointRegistry.of(level).resolve(level, pos.asLong(), side, FLUID);
        if (isUser(declared)) return declared;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IFluidUserMK2) return isUser(be) ? be : null;
        if (side == null) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IFluidConnectorBlockMK2)) return null;
        BlockEntity core = MachineCore.at(level, pos, state);
        return isUser(core) ? core : null;
    }

    private static boolean isUser(@Nullable BlockEntity be) {
        return be instanceof IFluidUserMK2 && !(be instanceof IFluidPipeMK2) && !be.isRemoved();
    }

    private static boolean contains(FluidTankNTM[] tanks, FluidTankNTM tank) {
        if (tanks == null) return false;
        for (FluidTankNTM candidate : tanks) if (candidate == tank) return true;
        return false;
    }

    /** One face of one HBM machine. Everything is read live; nothing is cached but the owner. */
    private static final class Face implements FluidMachine {

        private final ServerLevel level;
        private final BlockPos pos;
        private final @Nullable Direction side;
        private final BlockEntity be;
        private final IFluidUserMK2 user;

        Face(ServerLevel level, BlockPos pos, @Nullable Direction side, BlockEntity be, IFluidUserMK2 user) {
            this.level = level;
            this.pos = pos;
            this.side = side;
            this.be = be;
            this.user = user;
        }

        /** Whether a duct carrying {@code type} would connect to this machine through this face. */
        boolean connects(FluidType type) {
            return side != null && type != Fluids.NONE && FluidNetwork.userAtFace(level, pos, side, type) == be;
        }

        private @Nullable ForgeDirection forgeSide() {
            return side == null ? null : ForgeDirection.getOrientation(side);
        }

        /**
         * The receiver this face is a port of for {@code type}, when it wants some: 1.7's {@code trySubscribe} at the
         * position a duct (here the foreign pipe) stands at, on this side ({@link PortClaims}), as the duct network asks.
         * The demand is read first because the role is only asked about with something to move.
         */
        private @Nullable IFluidReceiverMK2 receiver(FluidType type) {
            if (!(user instanceof IFluidReceiverMK2 receiver)) return null;
            if (user instanceof IFluidDirectionalMK2 directional && (side == null || !directional.receivesOn(forgeSide()))) return null;
            if (side == null || Math.min(receiver.getDemand(type, PRESSURE), receiver.getReceiverSpeed(type, PRESSURE)) <= 0L) return null;
            if (!PortClaims.allows(level, pos.relative(side).asLong(), forgeSide(), user, type, PortClaims.SUBSCRIBE)) return null;
            return receiver;
        }

        /** The provider this face is a port of for {@code type}, when it holds some: 1.7's {@code tryProvide} there. */
        private @Nullable IFluidProviderMK2 provider(FluidType type) {
            if (!(user instanceof IFluidProviderMK2 provider)) return null;
            if (user instanceof IFluidDirectionalMK2 directional && (side == null || !directional.providesOn(forgeSide()))) return null;
            if (side == null || Math.min(provider.getFluidAvailable(type, PRESSURE), provider.getProviderSpeed(type, PRESSURE)) <= 0L) return null;
            if (!PortClaims.allows(level, pos.relative(side).asLong(), forgeSide(), user, type, PortClaims.PROVIDE)) return null;
            return provider;
        }

        private @Nullable FluidTankNTM tank(int index) {
            FluidTankNTM[] tanks = user.getAllTanks();
            return tanks == null || index < 0 || index >= tanks.length ? null : tanks[index];
        }

        @Override public Object owner() { return be; }

        @Override
        public boolean valid() {
            return !be.isRemoved() && user.isLoaded();
        }

        @Override
        public int tanks() {
            FluidTankNTM[] tanks = user.getAllTanks();
            return tanks == null ? 0 : tanks.length;
        }

        @Override
        public Fluid fluid(int index) {
            FluidTankNTM tank = tank(index);
            if (tank == null || tank.getFill() <= 0) return net.minecraft.world.level.material.Fluids.EMPTY;
            return Fluids.toVanilla(tank.getTankType());
        }

        @Override
        public long amount(int index) {
            FluidTankNTM tank = tank(index);
            return tank == null || tank.getTankType() == Fluids.NONE ? 0L : Math.max(0, tank.getFill());
        }

        @Override
        public long capacity(int index) {
            FluidTankNTM tank = tank(index);
            return tank == null ? 0L : Math.max(0, tank.getMaxFill());
        }

        @Override
        public boolean accepts(int index, Fluid fluid) {
            FluidTankNTM tank = tank(index);
            FluidType type = Fluids.fromVanilla(fluid);
            if (tank == null || type == Fluids.NONE || tank.getTankType() != type || tank.getPressure() != PRESSURE) return false;
            IFluidReceiverMK2 receiver = receiver(type);
            if (receiver == null || !connects(type)) return false;
            return !(receiver instanceof IFluidStandardReceiverMK2 standard) || contains(standard.getReceivingTanks(), tank);
        }

        @Override
        public boolean provides(int index, Fluid fluid) {
            FluidTankNTM tank = tank(index);
            FluidType type = Fluids.fromVanilla(fluid);
            if (tank == null || type == Fluids.NONE || tank.getTankType() != type || tank.getPressure() != PRESSURE) return false;
            IFluidProviderMK2 provider = provider(type);
            if (provider == null || !connects(type)) return false;
            return !(provider instanceof IFluidStandardSenderMK2 standard) || contains(standard.getSendingTanks(), tank);
        }

        @Override
        public long demand(Fluid fluid) {
            FluidType type = Fluids.fromVanilla(fluid);
            IFluidReceiverMK2 receiver = receiver(type);
            if (receiver == null || !valid() || !connects(type)) return 0L;
            return Math.max(0L, Math.min(receiver.getDemand(type, PRESSURE), receiver.getReceiverSpeed(type, PRESSURE)));
        }

        @Override
        public long available(Fluid fluid) {
            FluidType type = Fluids.fromVanilla(fluid);
            IFluidProviderMK2 provider = provider(type);
            if (provider == null || !valid() || !connects(type)) return 0L;
            return Math.max(0L, Math.min(provider.getFluidAvailable(type, PRESSURE), provider.getProviderSpeed(type, PRESSURE)));
        }

        @Override
        public long receive(Fluid fluid, long mb) {
            FluidType type = Fluids.fromVanilla(fluid);
            IFluidReceiverMK2 receiver = receiver(type);
            if (receiver == null || type == Fluids.NONE || mb <= 0L) return 0L;
            return mb - receiver.transferFluid(type, PRESSURE, mb);
        }

        @Override
        public long give(Fluid fluid, long mb) {
            FluidType type = Fluids.fromVanilla(fluid);
            IFluidProviderMK2 provider = provider(type);
            if (provider == null || type == Fluids.NONE || mb <= 0L) return 0L;
            long given = Math.min(mb, provider.getFluidAvailable(type, PRESSURE));
            if (given > 0L) provider.useUpFluid(type, PRESSURE, given);
            return Math.max(0L, given);
        }

        @Override
        public void changed() {
            be.setChanged();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Container items
    // ------------------------------------------------------------------------------------------------------------

    private static volatile @Nullable Set<Item> rowItems;

    /**
     * The container items: every {@link IFillableItem} except the infinite sources (1.7 had no bottomless item a
     * foreign pipe could drain), the port's per-fluid bucket, and the 1.7 {@code FluidContainerRegistry} rows on HBM
     * items (full items that drain whole, and the empty canister / gas tank / lead tank / disperser / gland that fill
     * into their family).
     */
    private static boolean isContainerItem(Item item) {
        if (item instanceof IFillableItem) return !(item instanceof IInfiniteFluidItem);
        if (item instanceof ItemFluidBucket) return true;
        return rowItems().contains(item);
    }

    private static Set<Item> rowItems() {
        Set<Item> items = rowItems;
        if (items == null) {
            items = new HashSet<>();
            for (FluidContainerRegistry.ForeignRow row : FluidContainerRegistry.INSTANCE.foreignRows()) {
                items.add(row.full());
                if (!row.empty().isEmpty() && isHbm(row.empty().getItem())) items.add(row.empty().getItem());
            }
            for (FluidContainerRegistry.ForeignFill fill : FluidContainerRegistry.INSTANCE.foreignFills()) items.add(fill.empty());
            rowItems = items;
        }
        return items;
    }

    private static boolean isHbm(Item item) {
        return Constants.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    private static FluidContainerRegistry.@Nullable ForeignRow fullRow(ItemStack one) {
        for (FluidContainerRegistry.ForeignRow row : FluidContainerRegistry.INSTANCE.foreignRows()) {
            if (one.is(row.full())) return row;
        }
        return null;
    }

    /** An empty HBM item that a fixed row fills ({@code rod_zirnox_empty} with 2,000 mB tritium, ...). */
    private static FluidContainerRegistry.@Nullable ForeignRow emptyRow(ItemStack one, FluidType type) {
        for (FluidContainerRegistry.ForeignRow row : FluidContainerRegistry.INSTANCE.foreignRows()) {
            if (row.fluid() == type && !row.empty().isEmpty() && isHbm(row.empty().getItem()) && one.is(row.empty().getItem())) return row;
        }
        return null;
    }

    private static FluidContainerRegistry.@Nullable ForeignFill familyFill(ItemStack one, FluidType type) {
        for (FluidContainerRegistry.ForeignFill fill : FluidContainerRegistry.INSTANCE.foreignFills()) {
            if (one.is(fill.empty()) && fill.admits().test(type) && fill.family().acceptsFluid(type, new ItemStack(fill.family()))) return fill;
        }
        return null;
    }

    private static boolean isEmptyContainer(ItemStack one) {
        for (FluidContainerRegistry.ForeignFill fill : FluidContainerRegistry.INSTANCE.foreignFills()) if (one.is(fill.empty())) return true;
        for (FluidContainerRegistry.ForeignRow row : FluidContainerRegistry.INSTANCE.foreignRows()) {
            if (!row.empty().isEmpty() && isHbm(row.empty().getItem()) && one.is(row.empty().getItem())) return true;
        }
        return false;
    }

    public static final FluidContainer CONTAINERS = new FluidContainer() {

        @Override
        public boolean handles(ItemStack stack) {
            return !stack.isEmpty() && isContainerItem(stack.getItem());
        }

        @Override
        public Fluid fluid(ItemStack one) {
            return Fluids.toVanilla(heldType(one));
        }

        private FluidType heldType(ItemStack one) {
            if (one.getItem() instanceof IFillableItem fillable && !(one.getItem() instanceof IInfiniteFluidItem)) {
                FluidType type = fillable.getFirstFluidType(one);
                return type == null || fillable.getFill(one) <= 0 ? Fluids.NONE : type;
            }
            if (one.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(one);
            FluidContainerRegistry.ForeignRow row = fullRow(one);
            return row == null ? Fluids.NONE : row.fluid();
        }

        @Override
        public long amount(ItemStack one) {
            FluidType type = heldType(one);
            if (type == Fluids.NONE) return 0L;
            if (one.getItem() instanceof IFillableItem fillable) return Math.max(0, fillable.getFill(one));
            if (one.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.VOLUME;
            FluidContainerRegistry.ForeignRow row = fullRow(one);
            return row == null ? 0L : row.amount();
        }

        @Override
        public long capacity(ItemStack one, Fluid fluid) {
            FluidType held = heldType(one);
            FluidType type = fluid == net.minecraft.world.level.material.Fluids.EMPTY ? held : Fluids.fromVanilla(fluid);
            if (one.getItem() instanceof IFillableItem fillable) {
                if (type == Fluids.NONE) return one.getItem() instanceof ItemFluidTank tank ? tank.capacity : 0L;
                if (held != Fluids.NONE && held != type) return 0L;
                if (!fillable.acceptsFluid(type, one)) return amount(one);
                ItemStack probe = one.copy();
                int room = Integer.MAX_VALUE / 2;
                return amount(one) + (room - fillable.tryFill(type, room, probe));
            }
            if (held != Fluids.NONE) return held == type ? amount(one) : 0L;
            if (type == Fluids.NONE) return 0L;
            FluidContainerRegistry.ForeignRow row = emptyRow(one, type);
            if (row != null) return row.amount();
            FluidContainerRegistry.ForeignFill fill = familyFill(one, type);
            return fill == null ? 0L : fill.family().capacity;
        }

        @Override
        public @Nullable Moved fill(ItemStack one, Fluid fluid, long maxMb) {
            FluidType type = Fluids.fromVanilla(fluid);
            if (type == Fluids.NONE || maxMb <= 0L) return null;
            int max = FluidUnits.saturate(maxMb);
            if (one.getItem() instanceof IFillableItem fillable) {
                if (!fillable.acceptsFluid(type, one)) return null;
                ItemStack result = one.copy();
                int moved = max - fillable.tryFill(type, max, result);
                return moved > 0 ? new Moved(result, moved) : null;
            }
            if (!isEmptyContainer(one)) return null;
            // 1.7 FluidContainerRegistry rows are whole: an empty container fills only by its full amount at once.
            FluidContainerRegistry.ForeignRow row = emptyRow(one, type);
            if (row != null) return max >= row.amount() ? new Moved(new ItemStack(row.full()), row.amount()) : null;
            FluidContainerRegistry.ForeignFill fill = familyFill(one, type);
            if (fill == null || max < fill.family().capacity) return null;
            return new Moved(fill.family().make(type, fill.family().capacity), fill.family().capacity);
        }

        @Override
        public @Nullable Moved drain(ItemStack one, Fluid fluid, long maxMb) {
            FluidType type = Fluids.fromVanilla(fluid);
            if (type == Fluids.NONE || maxMb <= 0L) return null;
            int max = FluidUnits.saturate(maxMb);
            if (one.getItem() instanceof IFillableItem fillable) {
                if (!fillable.providesFluid(type, one)) return null;
                ItemStack result = one.copy();
                int moved = fillable.tryEmpty(type, max, result);
                return moved > 0 ? new Moved(result, moved) : null;
            }
            if (one.getItem() instanceof ItemFluidBucket) {
                if (ItemFluidBucket.getFluid(one) != type || max < ItemFluidBucket.VOLUME) return null;
                return new Moved(new ItemStack(Items.BUCKET), ItemFluidBucket.VOLUME);
            }
            FluidContainerRegistry.ForeignRow row = fullRow(one);
            if (row == null || row.fluid() != type || max < row.amount()) return null;
            return new Moved(row.empty().copy(), row.amount());
        }
    };

    /** For tests: the container rows exposed besides {@link IFillableItem}s. */
    public static List<Item> rowItemsForTest() {
        return List.copyOf(rowItems());
    }
}
