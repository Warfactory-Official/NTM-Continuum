package com.hbm.items.special;

import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.special.ItemAutogen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.hbm.inventory.material.Mats.*;

/** The six density channels and their exact legacy material-output tables. */
public enum BedrockOreType {
    // primary, sulfuric byproducts, solvent byproducts, high-performance-solvent byproducts
    LIGHT_METAL("light", "Light Metal",
            o(MAT_IRON, 9), o(MAT_COPPER, 9),
            o(MAT_TITANIUM, 6), o(MAT_BAUXITE, 9), o(MAT_CRYOLITE, 3),
            o(MAT_CHLOROCALCITE, 5), o(MAT_LITHIUM, 5), o(MAT_SODIUM, 3),
            o(MAT_CHLOROCALCITE, 6), o(MAT_LITHIUM, 6), o(MAT_SODIUM, 6)),
    HEAVY_METAL("heavy", "Heavy Metal",
            o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9),
            o(MAT_GOLD, 2), o(MAT_GOLD, 2), o(MAT_BERYLLIUM, 3),
            o(MAT_TUNGSTEN, 9), o(MAT_LEAD, 9), o(MAT_GOLD, 5),
            o(MAT_BISMUTH, 2), o(MAT_TANTALIUM, 2), o(MAT_GOLD, 6)),
    RARE_EARTH("rare", "Rare Earth",
            o(MAT_COBALT, 5), o(MAT_RAREEARTH, 5),
            o(MAT_BORON, 5), o(MAT_LANTHANIUM, 3), o(MAT_NIOBIUM, 4),
            o(MAT_NEODYMIUM, 3), o(MAT_STRONTIUM, 3), o(MAT_ZIRCONIUM, 3),
            o(MAT_NIOBIUM, 5), o(MAT_NEODYMIUM, 5), o(MAT_STRONTIUM, 3)),
    ACTINIDE("actinide", "Actinide",
            o(MAT_URANIUM, 4), o(MAT_THORIUM, 4),
            o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2),
            o(MAT_RADIUM, 2), o(MAT_RADIUM, 2), o(MAT_POLONIUM, 2),
            o(MAT_TECHNETIUM, 1), o(MAT_TECHNETIUM, 1), o(MAT_U238, 1)),
    NON_METAL("nonmetal", "Non-Metal",
            o(MAT_COAL, 9), o(MAT_SULFUR, 9),
            o(MAT_LIGNITE, 9), o(MAT_KNO, 6), o(MAT_FLUORITE, 6),
            o(MAT_PHOSPHORUS, 5), o(MAT_FLUORITE, 6), o(MAT_SULFUR, 6),
            o(MAT_CHLOROCALCITE, 6), o(MAT_SILICON, 2), o(MAT_SILICON, 2)),
    CRYSTALLINE("crystal", "Crystalline",
            o(MAT_REDSTONE, 9), o(MAT_CINNABAR, 4),
            o(MAT_SODALITE, 9), o(MAT_ASBESTOS, 6), o(MAT_DIAMOND, 3),
            o(MAT_CINNABAR, 3), o(MAT_ASBESTOS, 5), o(MAT_EMERALD, 3),
            o(MAT_BORAX, 3), o(MAT_MOLYSITE, 3), o(MAT_SODALITE, 9));

    public static final BedrockOreType[] VALUES = values();

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

    BedrockOreType(String suffix, String displayName,
                   BedrockOreOutput primary1, BedrockOreOutput primary2,
                   BedrockOreOutput byproductAcid1, BedrockOreOutput byproductAcid2,
                   BedrockOreOutput byproductAcid3, BedrockOreOutput byproductSolvent1,
                   BedrockOreOutput byproductSolvent2, BedrockOreOutput byproductSolvent3,
                   BedrockOreOutput byproductRad1, BedrockOreOutput byproductRad2,
                   BedrockOreOutput byproductRad3) {
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

    private static BedrockOreOutput o(NTMMaterial material, int amount) {
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

    public List<BedrockOreOutput> outputs() {
        return List.of(primary1, primary2,
                byproductAcid1, byproductAcid2, byproductAcid3,
                byproductSolvent1, byproductSolvent2, byproductSolvent3,
                byproductRad1, byproductRad2, byproductRad3);
    }

    public record BedrockOreOutput(NTMMaterial material, int amount) {}
}
