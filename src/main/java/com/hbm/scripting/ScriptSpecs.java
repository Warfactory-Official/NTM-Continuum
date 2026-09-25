package com.hbm.scripting;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import dev.golem.core.Ids;
import dev.golem.core.world.Compat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What scripts write for a stack, parsed.
 *
 * <pre>
 * item        "hbm:plate_steel"   "3x hbm:plate_steel"   an ItemStack
 * items       "hbm:plate_steel" | "#c:plates/steel"         every member, for tables keyed by item
 * ingredient  item form | "#c:plates/steel" | "2x #c:plates/steel" | an AStack
 * fluid       "water" | "1000x water" | "1000x water@2"   (name as in Fluids, any case)
 * molten      "72x iron"                                    quanta: nugget 8, ingot 72, block 648
 * block       "minecraft:stone" | "#minecraft:logs"         blocks; tags resolve against the live world
 * </pre>
 *
 * Items resolve against the live registry, so only an op running after registration may parse them.
 */
public final class ScriptSpecs {

    private ScriptSpecs() {}

    private record Counted(int count, String id) {}

    private static Counted counted(String spec) {
        String s = spec.trim();
        int x = s.indexOf("x ");
        if (x > 0 && s.substring(0, x).chars().allMatch(Character::isDigit)) {
            return new Counted(Integer.parseInt(s.substring(0, x)), s.substring(x + 2).trim());
        }
        return new Counted(1, s);
    }

    public static Item item(Object raw) {
        String id = String.valueOf(raw instanceof Spec wrapped ? wrapped.raw : raw);
        var key = Ids.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalArgumentException("Unknown item " + id);
        return BuiltInRegistries.ITEM.getValue(key);
    }

    public static List<Item> items(Object raw) {
        String spec = String.valueOf(raw instanceof Spec wrapped ? wrapped.raw : raw);
        if (!spec.startsWith("#")) return List.of(item(spec));
        List<Item> items = new ArrayList<>();
        for (ItemStack stack : Compat.itemsInTag(tag(spec))) items.add(stack.getItem());
        return items;
    }

    public static ItemStack stack(Object spec) {
        if (spec instanceof Spec wrapped) spec = wrapped.raw;
        if (spec instanceof ItemStack stack) return stack.copy();
        Counted c = counted(String.valueOf(spec));
        return new ItemStack(item(c.id()), c.count());
    }

    public static ItemStack[] stacks(Object[] specs) {
        ItemStack[] out = new ItemStack[specs.length];
        for (int i = 0; i < specs.length; i++) out[i] = stack(specs[i]);
        return out;
    }

    public static TagKey<Item> tag(Object raw) {
        String id = String.valueOf(raw instanceof Spec wrapped ? wrapped.raw : raw);
        return TagKey.create(Registries.ITEM, Ids.parse(id.startsWith("#") ? id.substring(1) : id));
    }

    public static net.minecraft.world.level.block.Block block(Object raw) {
        String id = String.valueOf(raw instanceof Spec wrapped ? wrapped.raw : raw);
        return BuiltInRegistries.BLOCK.getOptional(Ids.parse(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown block " + id));
    }

    public static TagKey<net.minecraft.world.level.block.Block> blockTag(Object raw) {
        String id = String.valueOf(raw instanceof Spec wrapped ? wrapped.raw : raw);
        return TagKey.create(Registries.BLOCK, Ids.parse(id.startsWith("#") ? id.substring(1) : id));
    }

    public static AStack ingredient(Object spec) {
        if (spec instanceof Spec wrapped) spec = wrapped.raw;
        if (spec instanceof AStack stack) return stack;
        if (spec instanceof ItemStack stack) return new ComparableStack(stack);
        Counted c = counted(String.valueOf(spec));
        if (c.id().startsWith("#")) return new TagStack(tag(c.id()), c.count());
        return new ComparableStack(item(c.id()), c.count());
    }

    public static AStack[] ingredients(Object[] specs) {
        AStack[] out = new AStack[specs.length];
        for (int i = 0; i < specs.length; i++) out[i] = ingredient(specs[i]);
        return out;
    }

    public static FluidType fluidType(Object spec) {
        if (spec instanceof Spec wrapped) spec = wrapped.raw;
        if (spec instanceof FluidType type) return type;
        String name = String.valueOf(spec).trim().toUpperCase(Locale.US);
        FluidType type = Fluids.fromName(name);
        if (type == Fluids.NONE && !name.equals("NONE")) throw new IllegalArgumentException("Unknown fluid " + spec);
        return type;
    }

    public static FluidStack fluid(Object spec) {
        if (spec instanceof Spec wrapped) spec = wrapped.raw;
        if (spec instanceof FluidStack stack) return stack.copy();
        Counted c = counted(String.valueOf(spec));
        String id = c.id();
        int pressure = 0;
        int at = id.indexOf('@');
        if (at >= 0) {
            pressure = Integer.parseInt(id.substring(at + 1).trim());
            id = id.substring(0, at);
        }
        return new FluidStack(fluidType(id), c.count(), pressure);
    }

    public static FluidStack[] fluids(Object[] specs) {
        FluidStack[] out = new FluidStack[specs.length];
        for (int i = 0; i < specs.length; i++) out[i] = fluid(specs[i]);
        return out;
    }

    public static NTMMaterial material(String name) {
        NTMMaterial material = Mats.matByName.get(name);
        if (material == null) throw new IllegalArgumentException("Unknown material " + name);
        return material;
    }

    public static Mats.MaterialStack molten(Object spec) {
        if (spec instanceof Spec wrapped) spec = wrapped.raw;
        if (spec instanceof Mats.MaterialStack stack) return stack.copy();
        Counted c = counted(String.valueOf(spec));
        return new Mats.MaterialStack(material(c.id()), c.count());
    }

    public static Mats.MaterialStack[] molten(Object[] specs) {
        Mats.MaterialStack[] out = new Mats.MaterialStack[specs.length];
        for (int i = 0; i < specs.length; i++) out[i] = molten(specs[i]);
        return out;
    }

    public static MaterialShapes shape(String name) {
        MaterialShapes shape = MaterialShapes.byName(name.toUpperCase(Locale.US));
        if (shape == null) throw new IllegalArgumentException("Unknown shape " + name);
        return shape;
    }

    public static <E extends Enum<E>> E constant(Class<E> type, String name) {
        return Enum.valueOf(type, name.toUpperCase(Locale.US));
    }
}
