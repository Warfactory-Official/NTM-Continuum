package com.hbm.capability;

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
 * <p>The fluid bits are declared but carry no face gate here, unlike upstream: Continuum's fluid
 * network is still {@code FluidNetMK2} over UNINOS and has no {@code FluidFace} to gate on. Keeping
 * the bits means the domain mask is the same shape when the fluid net follows.
 */
public final class MachineCaps {

    private static final BiPredicate<BlockEntity, Direction> ALL = (be, side) -> true;

    /** Exposes nothing. Stated rather than omitted, so "no caps" reads as a decision. */
    public static final MachineCaps NONE = new MachineCaps(null);

    public static final int POWER_IN = 1;
    public static final int POWER_OUT = 1 << 1;
    public static final int FLUID_IN = 1 << 2;
    public static final int FLUID_OUT = 1 << 3;
    public static final int ITEMS = 1 << 4;
    public static final int ITEMS_AT_CELLS = 1 << 5;
    public static final int FE = 1 << 6;

    final boolean powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe;
    final boolean selfProvided;
    private final BiPredicate<BlockEntity, Direction> faces;
    private final @Nullable RegistryObject<? extends BlockEntityType<?>> beType;

    private MachineCaps(@Nullable RegistryObject<? extends BlockEntityType<?>> beType) {
        this(beType, false, false, false, false, false, false, false, false, ALL);
    }

    private MachineCaps(@Nullable RegistryObject<? extends BlockEntityType<?>> beType, boolean powerIn,
                        boolean powerOut, boolean fluidIn, boolean fluidOut, boolean items,
                        boolean itemsAtCells, boolean fe, boolean selfProvided,
                        BiPredicate<BlockEntity, Direction> faces) {
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
    }

    public static MachineCaps of(RegistryObject<? extends BlockEntityType<?>> beType) {
        return new MachineCaps(beType);
    }

    /** For a machine whose every exposure is block-keyed, so no block entity type is needed to name it. */
    public static MachineCaps blockKeyed() {
        return new MachineCaps(null);
    }

    public MachineCaps powerIn() {
        return new MachineCaps(beType, true, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe, selfProvided, faces);
    }

    public MachineCaps powerOut() {
        return new MachineCaps(beType, powerIn, true, fluidIn, fluidOut, items, itemsAtCells, fe, selfProvided, faces);
    }

    public MachineCaps fluidIn() {
        return new MachineCaps(beType, powerIn, powerOut, true, fluidOut, items, itemsAtCells, fe, selfProvided, faces);
    }

    public MachineCaps fluidOut() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, true, items, itemsAtCells, fe, selfProvided, faces);
    }

    /** Sided inventory at the core block entity only — a GUI inventory with no proxy cells. */
    public MachineCaps items() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, true, itemsAtCells, fe, selfProvided, faces);
    }

    /** Sided inventory at every connector cell, which is 1.7's {@code TileEntityProxyCombo().inventory()}. */
    public MachineCaps itemsAtCells() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, true, fe, selfProvided, faces);
    }

    /** Foreign-energy exposure. NeoForge and Forge only; Fabric ships no standard energy capability. */
    public MachineCaps fe() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, true, selfProvided, faces);
    }

    /**
     * The declared domains register no default mapper: the block states side-gated ones in
     * {@link ICapabilityBlock#declareExtraCaps} instead. The bits are declared all the same, because
     * the endpoint registry owns every machine of ours and the probe behind it answers for foreign
     * handlers.
     */
    public MachineCaps selfProvided() {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe, true, faces);
    }

    /**
     * The faces this machine accepts, applied by the capability itself so that resolving and
     * connecting are one question. Stated here rather than on the handler because a folded machine's
     * faces come from its mask, and a rule on the handler would be a second answer nobody reads.
     *
     * <p>Evaluated per lookup, so a rule may read the block state or live state.
     */
    public <BE extends BlockEntity> MachineCaps faces(Class<BE> type, BiPredicate<BE, Direction> gate) {
        return new MachineCaps(beType, powerIn, powerOut, fluidIn, fluidOut, items, itemsAtCells, fe,
                selfProvided, (be, side) -> gate.test(type.cast(be), side));
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
