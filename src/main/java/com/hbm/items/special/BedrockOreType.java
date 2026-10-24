package com.hbm.items.special;

import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.special.ItemAutogen;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;

import com.hbm.api.registry.NamedRegistry;

import java.util.List;

import static com.hbm.inventory.material.Mats.*;

/**
 * A bedrock ore type: 1.7's six density channels and their exact material-output tables, as a registry of records
 * keyed by the name the items and saves carry ({@link #suffix}: {@code "light"}, {@code "heavy"}, ...).
 *
 * <p>{@link #VALUES} stays 1.7's six, in 1.7 order: they are what the overworld's noise ({@code ItemBedrockOreBase}),
 * the ore slopper and HBM's processing chain loop over, so an addon's type never shifts HBM's numbers. An addon (NTM:
 * Space's per-body ores) registers its own types with {@link #register}, registers their graded items in its own
 * namespace and maps them with {@code ModBedrockOreItems.add}, and adds its chain recipes as data; {@link #values()}
 * lists every type.
 */
public final class BedrockOreType {

    /** Every bedrock ore type by name, HBM's six first. */
    public static final NamedRegistry<BedrockOreType> REGISTRY = new NamedRegistry<>("bedrock ore type", t -> t.name);

    // primary, sulfuric byproducts, solvent byproducts, high-performance-solvent byproducts
    public static final BedrockOreType LIGHT_METAL = register(new BedrockOreType("light", "Light Metal",
            o(MAT_IRON, 9), o(MAT_COPPER, 9),
            o(MAT_ILMENITE, 6), o(MAT_BAUXITE, 9), o(MAT_CRYOLITE, 3),
            o(MAT_CHLOROCALCITE, 5), o(MAT_LITHIUM, 5), o(MAT_SODIUM, 3),
            o(MAT_CHLOROCALCITE, 6), o(MAT_LITHIUM, 6), o(MAT_SODIUM, 6)));
    public static final BedrockOreType HEAVY_METAL = register(new BedrockOreType("heavy", "Heavy Metal",
            o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9),
            o(MAT_GOLD, 2), o(MAT_GOLD, 2), o(MAT_BERYLLIUM, 3),
            o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9), o(MAT_GOLD, 5),
            o(MAT_BISMUTH, 2), o(MAT_TANTALIUM, 2), o(MAT_GOLD, 6)));
    public static final BedrockOreType RARE_EARTH = register(new BedrockOreType("rare", "Rare Earth",
            o(MAT_COBALT, 5), o(MAT_RAREEARTH, 5),
            o(MAT_BORON, 5), o(MAT_LANTHANIUM, 3), o(MAT_NIOBIUM, 4),
            o(MAT_NEODYMIUM, 3), o(MAT_STRONTIUM, 3), o(MAT_ZIRCONIUM, 3),
            o(MAT_NIOBIUM, 5), o(MAT_NEODYMIUM, 5), o(MAT_STRONTIUM, 3)));
    public static final BedrockOreType ACTINIDE = register(new BedrockOreType("actinide", "Actinide",
            o(MAT_URANIUM, 4), o(MAT_THORIUM, 4),
            o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2),
            o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2),
            o(MAT_TECHNETIUM, 1), o(MAT_TECHNETIUM, 1), o(MAT_U238, 1)));
    public static final BedrockOreType NON_METAL = register(new BedrockOreType("nonmetal", "Non-Metal",
            o(MAT_COAL, 9), o(MAT_SULFUR, 9),
            o(MAT_LIGNITE, 9), o(MAT_KNO, 6), o(MAT_FLUORITE, 6),
            o(MAT_PHOSPHORUS, 5), o(MAT_FLUORITE, 6), o(MAT_SULFUR, 6),
            o(MAT_CHLOROCALCITE, 6), o(MAT_SILICON, 2), o(MAT_SILICON, 2)));
    public static final BedrockOreType CRYSTALLINE = register(new BedrockOreType("crystal", "Crystalline",
            o(MAT_REDSTONE, 9), o(MAT_CINNABAR, 4),
            o(MAT_SODALITE, 9), o(MAT_ASBESTOS, 6), o(MAT_DIAMOND, 3),
            o(MAT_CINNABAR, 3), o(MAT_ASBESTOS, 5), o(MAT_EMERALD, 3),
            o(MAT_BORAX, 3), o(MAT_MOLYSITE, 3), o(MAT_SODALITE, 9)));

    /** 1.7's six, in 1.7 order ({@code BedrockOreType.values()}). */
    public static final BedrockOreType[] VALUES = {LIGHT_METAL, HEAVY_METAL, RARE_EARTH, ACTINIDE, NON_METAL, CRYSTALLINE};

    /**
     * The registry name: HBM's six are named by their suffix ({@code "light"}), an addon's carry its namespace
     * ({@code "ntmspace:duna_light"}).
     */
    public final String name;
    /** The art and translation suffix ({@code item.bedrock_ore.type.<suffix>.name}); several types may share one. */
    public final String suffix;
    public final String displayName;
    public final BedrockOreOutput primary1;
    public final BedrockOreOutput primary2;
    public final BedrockOreOutput byproductAcid1;
    public final BedrockOreOutput byproductAcid2;
    public final BedrockOreOutput byproductAcid3;
    public final BedrockOreOutput byproductSolvent1;
    public final BedrockOreOutput byproductSolvent2;
    public final BedrockOreOutput byproductSolvent3;
    public final BedrockOreOutput byproductRad1;
    public final BedrockOreOutput byproductRad2;
    public final BedrockOreOutput byproductRad3;

    public BedrockOreType(String suffix, String displayName,
                   BedrockOreOutput primary1, BedrockOreOutput primary2,
                   BedrockOreOutput byproductAcid1, BedrockOreOutput byproductAcid2,
                   BedrockOreOutput byproductAcid3, BedrockOreOutput byproductSolvent1,
                   BedrockOreOutput byproductSolvent2, BedrockOreOutput byproductSolvent3,
                   BedrockOreOutput byproductRad1, BedrockOreOutput byproductRad2,
                   BedrockOreOutput byproductRad3) {
        this(suffix, suffix, displayName, primary1, primary2, byproductAcid1, byproductAcid2, byproductAcid3,
                byproductSolvent1, byproductSolvent2, byproductSolvent3, byproductRad1, byproductRad2, byproductRad3);
    }

    /**
     * A type with one output per channel (hbm-space's {@code CelestialBedrockOreType}: primary, sulfuric, solvent and
     * high-performance-solvent byproduct), registered as {@code name}. The second and third slots stay null.
     */
    public BedrockOreType(String name, String suffix, String displayName, BedrockOreOutput primary,
                          BedrockOreOutput byproductAcid, BedrockOreOutput byproductSolvent, BedrockOreOutput byproductRad) {
        this(name, suffix, displayName, primary, null, byproductAcid, null, null, byproductSolvent, null, null,
                byproductRad, null, null);
    }

    private BedrockOreType(String name, String suffix, String displayName,
                   BedrockOreOutput primary1, BedrockOreOutput primary2,
                   BedrockOreOutput byproductAcid1, BedrockOreOutput byproductAcid2,
                   BedrockOreOutput byproductAcid3, BedrockOreOutput byproductSolvent1,
                   BedrockOreOutput byproductSolvent2, BedrockOreOutput byproductSolvent3,
                   BedrockOreOutput byproductRad1, BedrockOreOutput byproductRad2,
                   BedrockOreOutput byproductRad3) {
        this.name = name;
        this.suffix = suffix;
        this.displayName = displayName;
        this.primary1 = primary1;
        this.primary2 = primary2;
        this.byproductAcid1 = byproductAcid1;
        this.byproductAcid2 = byproductAcid2;
        this.byproductAcid3 = byproductAcid3;
        this.byproductSolvent1 = byproductSolvent1;
        this.byproductSolvent2 = byproductSolvent2;
        this.byproductSolvent3 = byproductSolvent3;
        this.byproductRad1 = byproductRad1;
        this.byproductRad2 = byproductRad2;
        this.byproductRad3 = byproductRad3;
    }

    /** Registers a type (an addon's name is {@code "<namespace>_<path>"}-safe, it becomes part of item ids). */
    public static BedrockOreType register(BedrockOreType type) {
        return REGISTRY.register(type);
    }

    /** Every registered type, HBM's six first. */
    public static List<BedrockOreType> values() {
        return REGISTRY.values();
    }

    /** The type named {@code name}, or null. */
    public static BedrockOreType byName(String name) {
        return REGISTRY.get(name);
    }

    /** The registration index: 1.7's ordinal for HBM's six. A runtime index, never saved. */
    public int ordinal() {
        return REGISTRY.values().indexOf(this);
    }

    public String name() {
        return name;
    }

    /** Shared 1.7 suffixes use HBM's translation; an addon's own suffix uses its registration namespace. */
    public net.minecraft.network.chat.Component typeName() {
        boolean shared = java.util.Arrays.stream(VALUES).anyMatch(type -> type.suffix.equals(suffix));
        String namespace = shared || !name.contains(":") ? com.hbm.Constants.MOD_ID : name.substring(0, name.indexOf(':'));
        return net.minecraft.network.chat.Component.translatableWithFallback(
                "item." + namespace + ".bedrock_ore.type." + suffix, displayName);
    }

    @Override
    public String toString() {
        return name;
    }

    public static BedrockOreOutput o(NTMMaterial material, int amount) {
        return new BedrockOreOutput(material, amount);
    }

    /** Exact old fragment construction: ceil(table amount * multiplier), capped to one stack. */
    public static ItemStack fragment(BedrockOreOutput output, double multiplier) {
        ItemAutogen item = ModAutogenItems.get(MaterialShapes.FRAGMENT, output.material());
        if (item == null) {
            throw new IllegalStateException("Missing bedrock fragment item for " + output.material().name);
        }
        return new ItemStack(item, Math.min((int) Math.ceil(output.amount() * multiplier), 64));
    }

    /** The actual smeltable output in molten-material quanta, as ItemBedrockOreNew.toFluid computed it. */
    public static Mats.MaterialStack molten(@org.jspecify.annotations.Nullable BedrockOreOutput output, double multiplier) {
        if (output == null) return null;
        var material = output.material();
        return material.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE
                ? new Mats.MaterialStack(material,
                        (int) Math.ceil(MaterialShapes.FRAGMENT.quantity * output.amount() * multiplier))
                : null;
    }

    /** Every output slot that is set, in table order (a four-output type has four). */
    public List<BedrockOreOutput> outputs() {
        return Stream.of(primary1, primary2,
                byproductAcid1, byproductAcid2, byproductAcid3,
                byproductSolvent1, byproductSolvent2, byproductSolvent3,
                byproductRad1, byproductRad2, byproductRad3).filter(Objects::nonNull).toList();
    }

    public record BedrockOreOutput(NTMMaterial material, int amount) {}
}
