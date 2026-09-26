package com.hbm.inventory.material;

import com.hbm.api.material.Material;
import com.hbm.api.material.SmeltingBehavior;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.LinkedHashSet;
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
public final class NTMMaterial implements Material {

    /** Item-naming key: {@code iron} for {@code hbm:iron}, else {@code <namespace>_<path>}; {@link Mats#byName} key. */
    public final String name;
    /** Persisted identity. */
    public final net.minecraft.resources.Identifier id;
    /** Session index = registration order; network buffers only. */
    public final int index;

    /** The legacy constructor defaults, used by the three coke materials. */
    public int solidColorLight = 0xFF4A00;
    public int solidColorDark = 0x802000;
    public int moltenColor = 0xFF4A00;

    /** How this material behaves in a crucible. Only {@code SMELTABLE} melt pools can be poured/cast. */
    public SmeltingBehavior smeltable = SmeltingBehavior.NOT_SMELTABLE;
    /** What this material melts into ({@code this} by default; e.g. coal melts into carbon). */
    public NTMMaterial smeltsInto = this;
    /** Conversion ratio into {@link #smeltsInto}: {@code amount * convOut / convIn}. */
    public int convIn = 1;
    public int convOut = 1;

    /** Shapes this material is auto-generated in (gates the autogen item's creative variants). */
    public final Set<MaterialShapes> autogen = new LinkedHashSet<>();

    private String displayName;

    /** A tag-only aggregate: in no registry, no index. */
    public static NTMMaterial group(String name) {
        return new NTMMaterial(dev.golem.core.Ids.of(com.hbm.Constants.MOD_ID, name), name, -1);
    }

    NTMMaterial(net.minecraft.resources.Identifier id, String name, int index) {
        this.id = id;
        this.name = name;
        this.index = index;
    }

    @Override public net.minecraft.resources.Identifier id() { return id; }
    @Override public int index() { return index; }
    @Override public String name() { return name; }
    @Override public int solidColorLight() { return solidColorLight; }
    @Override public int solidColorDark() { return solidColorDark; }
    @Override public int moltenColor() { return moltenColor; }
    @Override public SmeltingBehavior smeltable() { return smeltable; }
    @Override public NTMMaterial smeltsInto() { return smeltsInto; }
    @Override public int convIn() { return convIn; }
    @Override public int convOut() { return convOut; }

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

    /** Sets the human-readable name when the canonical tag suffix is not the legacy display name. */
    public NTMMaterial displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public NTMMaterial setAutogen(MaterialShapes... shapes) {
        for (MaterialShapes s : shapes) if (s != null) autogen.add(s);
        return this;
    }

    /** Title-cased display name, e.g. {@code combine_steel → "Combine Steel"}. */
    @Override
    public String getDisplayName() {
        if (displayName != null) return displayName;
        StringBuilder sb = new StringBuilder();
        for (String part : name.toLowerCase(Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    /** The {@code c:<plural>/<name>} item tag for this material in the given shape. */
    public TagKey<Item> tag(MaterialShapes shape) {
        return TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", shape.tagPlural + "/" + name));
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
}
