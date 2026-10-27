package com.hbm.capability;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * What a machine exposes to automation, declared by its own block class beside the connection geometry.
 *
 * <p>This is how the node graph finds endpoints. UNINOS had machines push themselves into a network
 * every tick and time them out three seconds later; the graph asks the block what it is instead, so
 * a machine in an unloaded chunk is a fact about the world rather than a subscription that lapsed.
 *
 * <p>Fluid contacts carry their own live side rule, independent of power and inventory. Native contact
 * queries apply it before the machine's configured fluid-type rule and folded geometry.
 */
public final class MachineCaps {

    private static final FaceGate ALL = (be, side, role) -> true;
    private static final FluidRule<BlockEntity> ALL_FLUID = (be, fluid, side) -> true;

    /** Exposes nothing. Stated rather than omitted, so "no caps" reads as a decision. */
    public static final MachineCaps NONE = new MachineCaps(null);

    public static final int POWER_IN = 1;
    public static final int POWER_OUT = 1 << 1;
    public static final int FLUID_IN = 1 << 2;
    public static final int FLUID_OUT = 1 << 3;
    public static final int ITEMS = 1 << 4;
    public static final int ITEMS_AT_CELLS = 1 << 5;
    public static final int FE = 1 << 6;

    /**
     * The fusion links, which are domains rather than caps: no block declares them and the builder below
     * takes no argument for them. They name what a machine claims at a <b>position</b>: a fusion beam
     * lands on a coordinate that need not hold a block at all, so the in/out halves are spelled
     * separately, because which end of a link a machine is on is the whole of what the plasma and
     * klystron nets carry.
     */
    public static final int PLASMA_IN = 1 << 7;
    public static final int PLASMA_OUT = 1 << 8;
    public static final int KLYSTRON_IN = 1 << 9;
    public static final int KLYSTRON_OUT = 1 << 10;

    /**
     * Every face open, as a {@code 1 << Direction.ordinal()} mask. Upstream keeps this on
     * {@code BlockMultiblockCore} beside its cell visitor; Continuum's multiblock core has no such
     * visitor, and a face mask belongs with the class that owns face semantics anyway.
     */
    public static final int FACE_MASK_ALL = 0b111111;

    final boolean powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe;
    final boolean selfProvided;
    private final FaceGate faces;
    private final FluidRule<BlockEntity> fluidFaces;
    private final @Nullable RegistryObject<? extends BlockEntityType<?>> beType;

    private MachineCaps(@Nullable RegistryObject<? extends BlockEntityType<?>> beType) {
        this(beType, false, false, false, false, false, false, false, false, ALL, ALL_FLUID);
    }

    private MachineCaps(@Nullable RegistryObject<? extends BlockEntityType<?>> beType, boolean powerIn,
                        boolean powerOut, boolean fluidIn, boolean fluidOut, boolean items,
                        boolean itemsAtCells, boolean fe, boolean selfProvided,
                        FaceGate faces,
                        FluidRule<BlockEntity> fluidFaces) {
        this.beType = beType;
        this.powerIn = powerIn;
        this.powerOut = powerOut;
        this.fluidIn = fluidIn;
        this.fluidOut = fluidOut;
        this.items = items;
        this.itemsAtCells = itemsAtCells;
        this.fe = fe;
        this.selfProvided = selfProvided;
        this.faces = faces;
        this.fluidFaces = fluidFaces;
    }

    public static MachineCaps of(RegistryObject<? extends BlockEntityType<?>> beType) {
        return new MachineCaps(beType);
    }

    /** For a machine whose every exposure is block-keyed, so no block entity type is needed to name it. */
    public static MachineCaps blockKeyed() {
        return new MachineCaps(null);
    }

    public MachineCaps powerIn() {
        return new MachineCaps(beType, true, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe, selfProvided, faces, fluidFaces);
    }

    public MachineCaps powerOut() {
        return new MachineCaps(beType, powerIn, true, fluidIn, fluidOut, items, itemsAtCells, fe, selfProvided, faces, fluidFaces);
    }

    public MachineCaps fluidIn() {
        return new MachineCaps(beType, powerIn, powerOut, true, fluidOut, items, itemsAtCells, fe, selfProvided, faces, fluidFaces);
    }

    public MachineCaps fluidOut() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, true, items, itemsAtCells, fe, selfProvided, faces, fluidFaces);
    }

    /** Sided inventory at the core block entity only: a GUI inventory with no proxy cells. */
    public MachineCaps items() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, true, itemsAtCells, fe, selfProvided, faces, fluidFaces);
    }

    /** Sided inventory at every connector cell, which is 1.7's {@code TileEntityProxyCombo().inventory()}. */
    public MachineCaps itemsAtCells() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, true, fe, selfProvided, faces, fluidFaces);
    }

    /** Foreign-energy exposure. NeoForge and Forge only; Fabric ships no standard energy capability. */
    public MachineCaps fe() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, true, selfProvided, faces, fluidFaces);
    }

    /**
     * The declared domains register no default mapper: the block states side-gated ones in
     * {@link ICapabilityBlock#declareExtraCaps} instead. The bits are declared all the same, because
     * the endpoint registry owns every machine of ours and the probe behind it answers for foreign
     * handlers.
     */
    public MachineCaps selfProvided() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe, true, faces, fluidFaces);
    }

    /**
     * The non-fluid faces this machine accepts, applied by the capability itself so that resolving and
     * connecting are one question. Stated here rather than on the handler because a folded machine's
     * faces come from its mask, and a rule on the handler would be a second answer nobody reads.
     *
     * <p>Evaluated per lookup, so a rule may read the block state or live state.
     */
    public <BE extends BlockEntity> MachineCaps faces(Class<BE> type, BiPredicate<BE, Direction> gate) {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe,
                selfProvided, (be, side, role) -> gate.test(type.cast(be), side), fluidFaces);
    }

    /** Separates input and output faces without changing a machine's priority or speed contract. */
    public <BE extends BlockEntity> MachineCaps roleFaces(Class<BE> type, RoleGate<BE> gate) {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe,
                selfProvided, (be, side, role) -> gate.test(type.cast(be), side, role), fluidFaces);
    }

    @FunctionalInterface
    public interface RoleGate<BE extends BlockEntity> {
        boolean test(BE owner, Direction side, CapRole role);
    }

    @FunctionalInterface
    private interface FaceGate extends BiPredicate<BlockEntity, Direction> {
        boolean test(BlockEntity owner, Direction side, CapRole role);
        @Override
        default boolean test(BlockEntity owner, Direction side) {
            return test(owner, side, CapRole.POWER_IN) || test(owner, side, CapRole.POWER_OUT);
        }
    }

    /** A side rule specific to fluid contacts, evaluated against the live owner on every query. */
    public <BE extends BlockEntity> MachineCaps fluidFaces(Class<BE> type, BiPredicate<BE, Direction> gate) {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe,
                selfProvided, faces, (be, fluid, side) -> gate.test(type.cast(be), side));
    }

    public FluidRule<BlockEntity> fluidGate() {
        return fluidFaces;
    }

    /** A live fluid-type and face rule, independent of energy and inventory faces. */
    public <BE extends BlockEntity> MachineCaps fluidTypes(Class<BE> type, FluidRule<BE> gate) {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe,
                selfProvided, faces, (be, fluid, side) -> gate.test(type.cast(be), fluid, side));
    }

    /** Uses the concrete native handler's configured fluid contacts, never fill or transfer demand. */
    public <BE extends BlockEntity & IFluidConnectorMK2> MachineCaps fluidContacts(Class<BE> type) {
        return fluidTypes(type, (be, fluid, side) -> fluid == null
                || be.canConnect(fluid, side == null ? ForgeDirection.UNKNOWN : ForgeDirection.getOrientation(side)));
    }

    @FunctionalInterface
    public interface FluidRule<BE extends BlockEntity> {
        boolean test(BE owner, @Nullable FluidType fluid, @Nullable Direction side);
    }

    /** Apply declared live rules; position geometry and the handler's fluid types remain separate checks. */
    public static boolean accepts(BlockEntity owner, @Nullable Direction side, CapRole role) {
        if (side == null) return true;
        return accepts(owner, side, null, role);
    }

    public static boolean accepts(BlockEntity owner, @Nullable Direction side, @Nullable FluidType fluid, CapRole role) {
        if (!(owner.getBlockState().getBlock() instanceof ICapabilityBlock declaring)) return true;
        var caps = declaring.caps();
        return role == CapRole.FLUID_IN || role == CapRole.FLUID_OUT
                ? caps.fluidFaces.test(owner, fluid, side) : side == null || caps.faces.test(owner, side, role);
    }

    /** Actual resources may satisfy a declared chemical-kind rule; storage joins still check exact identity. */
    public static boolean acceptsResource(BlockEntity owner, @Nullable Direction side, @Nullable FluidType fluid, CapRole role) {
        if (accepts(owner, side, fluid, role)) return true;
        if (fluid == null) return false;
        var kind = Fluids.kindOf(fluid);
        return kind != fluid && accepts(owner, side, kind, role);
    }

    /** Apply an explicit role declaration while retaining native interface routing for undeclared blocks. */
    public static boolean supportsRole(BlockEntity owner, CapRole role) {
        return !(owner.getBlockState().getBlock() instanceof ICapabilityBlock declaring)
                || (declaring.caps().declaredBits() & role.bit) != 0;
    }

    public BiPredicate<BlockEntity, Direction> faceGate() {
        return faces;
    }

    /**
     * What the machine at this block entity declares. The role is the declaration's, never the
     * object's: one handler type carries both halves, so a net asks what the owner said it does
     * rather than what it is.
     */
    public static int declaredDomains(@Nullable BlockEntity owner) {
        if (owner == null) return 0;
        return owner.getBlockState().getBlock() instanceof ICapabilityBlock declaring
                ? declaring.caps().declaredBits() : 0;
    }

    /** The flag set, so a folded multiblock's cell can re-apply its owner's declaration at query time. */
    public int declaredBits() {
        return (powerIn ? POWER_IN : 0) | (powerOut ? POWER_OUT : 0) | (fluidIn ? FLUID_IN : 0)
                | (fluidOut ? FLUID_OUT : 0) | (items ? ITEMS : 0) | (itemsAtCells ? ITEMS_AT_CELLS : 0)
                | (fe ? FE : 0);
    }

    public boolean isSelfProvided() {
        return selfProvided;
    }

    public RegistryObject<? extends BlockEntityType<?>> beType() {
        if (beType == null) {
            throw new IllegalStateException("a cap keyed on the block entity type was declared without one");
        }
        return beType;
    }

    public @Nullable RegistryObject<? extends BlockEntityType<?>> beTypeOrNull() {
        return beType;
    }
}
