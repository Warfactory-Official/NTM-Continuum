package com.hbm.inventory.material;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * A material in HBM's material system — the modern tag-system port of {@code NTMMaterial}. Identified by a
 * snake-case {@link #name} (matching the ported item naming, e.g. {@code combine_steel}); combining it with
 * a {@link MaterialShapes shape} yields a recipe ingredient ({@link AStack}):
 *
 * <ul>
 *   <li>Shapes with real, taggable items (ingot/plate/dust/nugget/gem/block) → a {@link TagStack} on the
 *       {@code c:<plural>/<name>} common item tag (cross-mod, the old {@code "ingotIron"} equivalent).</li>
 *   <li>HBM "autogen" shapes (cast/welded plate, shell, pipe, fine/dense wire, …) → a {@link MaterialStack}
 *       on the single {@link com.hbm.items.special.ItemAutogen} item for that shape, matched by its material
 *       data component (these aren't taggable per-material since they're one item recoloured per material).</li>
 * </ul>
 *
 * <p>The {@link #solidColorLight} drives the autogen icon tint. The crucible half of the 1.12.2 class —
 * {@link SmeltingBehavior} plus the {@link #smeltsInto}/{@link #convIn}/{@link #convOut} conversion triple
 * (how a material behaves when melted, e.g. coal → carbon at 2:1) — is ported for the foundry system;
 * traits remain deferred.
 */
public final class NTMMaterial {

    /** Snake-case id, matching the ported item naming (e.g. {@code iron}, {@code combine_steel}). */
    public final String name;
    /** The original numeric id, kept for reference / ordering. */
    public final int id;

    public int solidColorLight = 0xFFFFFF;
    public int solidColorDark = 0x505050;
    public int moltenColor = 0xFFFFFF;

    /** How this material behaves in a crucible. Only {@code SMELTABLE} melt pools can be poured/cast. */
    public SmeltingBehavior smeltable = SmeltingBehavior.NOT_SMELTABLE;
    /** What this material melts into ({@code this} by default; e.g. coal melts into carbon). */
    public NTMMaterial smeltsInto = this;
    /** Conversion ratio into {@link #smeltsInto}: {@code amount * convOut / convIn}. */
    public int convIn = 1;
    public int convOut = 1;

    /** Shapes this material is auto-generated in (gates the autogen item's creative variants). */
    public final Set<MaterialShapes> autogen = EnumSet.noneOf(MaterialShapes.class);

    public NTMMaterial(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public NTMMaterial smelting(SmeltingBehavior behavior) {
        this.smeltable = behavior;
        return this;
    }

    public NTMMaterial conv(NTMMaterial mat, int in, int out) {
        this.smeltsInto = mat;
        this.convIn = in;
        this.convOut = out;
        return this;
    }

    public NTMMaterial colors(int solidLight, int solidDark, int molten) {
        this.solidColorLight = solidLight;
        this.solidColorDark = solidDark;
        this.moltenColor = molten;
        return this;
    }

    public NTMMaterial setAutogen(MaterialShapes... shapes) {
        for (MaterialShapes s : shapes) if (s != null) autogen.add(s);
        return this;
    }

    /** Title-cased display name, e.g. {@code combine_steel → "Combine Steel"}. */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        for (String part : name.toLowerCase(Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    /** The {@code c:<plural>/<name>} item tag for this material in the given shape. */
    public TagKey<Item> tag(MaterialShapes shape) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", shape.tagPlural + "/" + name));
    }

    // --- tag-backed shapes (real items + cross-mod c: tags) ---

    public AStack nugget()  { return new TagStack(tag(MaterialShapes.NUGGET)); }
    public AStack billet()  { return new TagStack(tag(MaterialShapes.BILLET)); }
    public AStack ingot()   { return new TagStack(tag(MaterialShapes.INGOT)); }
    public AStack gem()     { return new TagStack(tag(MaterialShapes.GEM)); }
    public AStack crystal() { return new TagStack(tag(MaterialShapes.CRYSTAL)); }
    public AStack dust()    { return new TagStack(tag(MaterialShapes.DUST)); }
    public AStack plate()   { return new TagStack(tag(MaterialShapes.PLATE)); }
    public AStack block()   { return new TagStack(tag(MaterialShapes.BLOCK)); }
    public AStack ore()     { return new TagStack(tag(MaterialShapes.ORE)); }

    // --- HBM autogen shapes (one item per shape, matched by the material data component) ---

    public AStack plateCast()   { return new MaterialStack(this, MaterialShapes.CASTPLATE); }
    public AStack plateWelded() { return new MaterialStack(this, MaterialShapes.WELDEDPLATE); }
    public AStack shell()       { return new MaterialStack(this, MaterialShapes.SHELL); }
    public AStack pipe()        { return new MaterialStack(this, MaterialShapes.PIPE); }
    public AStack wire()        { return new MaterialStack(this, MaterialShapes.WIRE); }
    public AStack wireDense()   { return new MaterialStack(this, MaterialShapes.DENSEWIRE); }
    public AStack bolt()        { return new MaterialStack(this, MaterialShapes.BOLT); }
    public AStack heavyComp()   { return new MaterialStack(this, MaterialShapes.HEAVY_COMPONENT); }

    @Override
    public String toString() {
        return "NTMMaterial[" + name + "]";
    }

    /** 1.12.2 {@code NTMMaterial.SmeltingBehavior}, verbatim. */
    public enum SmeltingBehavior {
        NOT_SMELTABLE,  // anything that can't be smelted or otherwise doesn't belong in a smelter, like diamond
        VAPORIZES,      // can't be smelted because the material would skadoodle
        BREAKS,         // can't be smelted because the material doesn't survive the temperatures
        SMELTABLE,      // mostly metal
        ADDITIVE        // stuff like coal which isn't smeltable but can be put in a crucible anyway
    }
}
