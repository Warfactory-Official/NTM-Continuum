package com.hbm.items;

/**
 * The metadata-subtype enums from the 1.12.2 {@code ItemEnums} — in 1.21 each variant becomes its own
 * registered item (see {@link ModIntermediateItems}). <b>Partial port:</b> only the enums whose items are
 * ported so far (the recipe intermediates); grow it as more {@code ItemEnumMulti} items land.
 */
public final class ItemEnums {

    private ItemEnums() {}

    /** Ore chunks (centrifuge/shredder ore-processing outputs). */
    public enum EnumChunkType {
        RARE, MALACHITE, CRYOLITE, MOONSTONE;
        public static final EnumChunkType[] VALUES = values();
    }

    /** Circuits / electronics — the ubiquitous machine-recipe intermediate. */
    public enum EnumCircuitType {
        VACUUM_TUBE, CAPACITOR, CAPACITOR_TANTALIUM, PCB, SILICON, CHIP, CHIP_BISMOID, ANALOG, BASIC,
        ADVANCED, CAPACITOR_BOARD, BISMOID, CONTROLLER_CHASSIS, CONTROLLER, CONTROLLER_ADVANCED, QUANTUM,
        CHIP_QUANTUM, CONTROLLER_QUANTUM, ATOMIC_CLOCK, NUMITRON;
        public static final EnumCircuitType[] VALUES = values();
    }

    /** Oil tars / waxes (refinery, solidifier and later coker/pyro byproducts). */
    public enum EnumTarType {
        CRUDE, CRACK, COAL, WOOD, WAX, PARAFFIN;
        public static final EnumTarType[] VALUES = values();
    }

    /** Coke variants (coker output + coal/lignite briquette smelting). */
    public enum EnumCokeType {
        COAL, LIGNITE, PETROLEUM;
        public static final EnumCokeType[] VALUES = values();
    }

    /** "Expensive mode" structural parts (assembly-machine intermediates). */
    public enum EnumExpensiveType {
        STEEL_PLATING, HEAVY_FRAME, CIRCUIT, LEAD_PLATING, FERRO_PLATING, COMPUTER, BRONZE_TUBES, PLASTIC,
        GOLD_DUST, DEGENERATE_MATTER;
        public static final EnumExpensiveType[] VALUES = values();
    }
}
