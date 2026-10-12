package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.registry.NamedRegistry;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Console-facing column classification (1.7 {@code RBMKConsole.ColumnType}); offsets byte-identical to the oracle's
 * atlas rows. A registry of records keyed by 1.7's constant name (what the console peripheral reports): an addon
 * column (NTM: Space's burner) registers its own type with its icon, and the console shows it as a plain column with
 * heat, or as the column data its factory builds. The console sends a column's type as its registration index, a
 * runtime number that is never saved.
 */
public final class RBMKColumnType {

    public static final NamedRegistry<RBMKColumnType> REGISTRY = new NamedRegistry<>("RBMK column type", RBMKColumnType::name);

    public static final RBMKColumnType BLANK = register("BLANK", 0);
    public static final RBMKColumnType FUEL = register("FUEL", 10);
    public static final RBMKColumnType FUEL_SIM = register("FUEL_SIM", 90);
    public static final RBMKColumnType CONTROL = register("CONTROL", 20);
    public static final RBMKColumnType CONTROL_AUTO = register("CONTROL_AUTO", 30);
    public static final RBMKColumnType BOILER = register("BOILER", 40);
    public static final RBMKColumnType MODERATOR = register("MODERATOR", 50);
    public static final RBMKColumnType ABSORBER = register("ABSORBER", 60);
    public static final RBMKColumnType REFLECTOR = register("REFLECTOR", 70);
    public static final RBMKColumnType OUTGASSER = register("OUTGASSER", 80);
    public static final RBMKColumnType BREEDER = register("BREEDER", 100);
    public static final RBMKColumnType STORAGE = register("STORAGE", 110);
    public static final RBMKColumnType COOLER = register("COOLER", 120);
    public static final RBMKColumnType HEATEX = register("HEATEX", 130);

    private final String name;
    /** The icon's u on the console sheet (row v = 172, 10x10). */
    public final int offset;
    /** The console sheet the icon is on; null for HBM's console texture. */
    public final @Nullable Identifier sheet;
    /** The console data of such a column; null for a plain column (heat only). */
    public final @Nullable Function<RBMKColumnType, RBMKColumn> column;

    private RBMKColumnType(String name, int offset, @Nullable Identifier sheet, @Nullable Function<RBMKColumnType, RBMKColumn> column) {
        this.name = name;
        this.offset = offset;
        this.sheet = sheet;
        this.column = column;
    }

    private static RBMKColumnType register(String name, int offset) {
        return REGISTRY.register(new RBMKColumnType(name, offset, null, null));
    }

    /**
     * Registers an addon column type: its icon at ({@code offset}, 172) on {@code sheet} (256x256), and the console
     * data its columns carry ({@code column}, or null for heat only). Call at mod construction, on both sides.
     */
    public static RBMKColumnType register(String name, Identifier sheet, int offset,
                                          @Nullable Function<RBMKColumnType, RBMKColumn> column) {
        return REGISTRY.register(new RBMKColumnType(name, offset, sheet, column));
    }

    public static List<RBMKColumnType> values() {
        return REGISTRY.values();
    }

    /** The registration index (1.7's ordinal for HBM's fourteen); runtime only, never saved. */
    public int ordinal() {
        return REGISTRY.values().indexOf(this);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
