package com.hbm.items.machine;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A casting mold — inserted into a foundry caster ({@code foundry_mold}, size 0) or basin
 * ({@code foundry_basin}, size 1) to pick what the poured metal solidifies into. The 1.12.2
 * {@code ItemMold} was one metadata item; the port registers <b>one item per mold</b>
 * ({@link ModMoldItems}), each carrying its {@link Mold} definition. {@code MoldShape} output
 * resolution moves from the ore dictionary to the {@code c:<plural>/<material>} item tags (HBM items
 * preferred), with autogen shapes resolved directly via {@link ModAutogenItems}.
 */
public class ItemMold extends Item {

    public final Mold mold;

    public ItemMold(Properties properties, Mold mold) {
        super(properties);
        this.mold = mold;
    }

    /** The mold definition of a stack, or null if it isn't a mold item. */
    public static @Nullable Mold getMold(ItemStack stack) {
        return stack.getItem() instanceof ItemMold item ? item.mold : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                java.util.function.Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.literal(mold.getTitle()).withStyle(ChatFormatting.YELLOW));
        adder.accept(Component.literal(Mats.formatAmount(mold.getCost(), false)).withStyle(ChatFormatting.GRAY));
        if (mold.size == 0) adder.accept(Component.translatable("block.hbm.foundry_mold").withStyle(ChatFormatting.GOLD));
        if (mold.size == 1) adder.accept(Component.translatable("block.hbm.foundry_basin").withStyle(ChatFormatting.RED));
    }

    // ------------------------------------------------------------------------------------------------

    /** One mold definition (1.12.2 inner class, made static). {@code size}: 0 = mold caster, 1 = basin. */
    public abstract static class Mold {
        public final int id;
        public final int size;
        public final String name;

        public Mold(int id, int size, String name) {
            this.id = id;
            this.size = size;
            this.name = name;
        }

        /** The cast item for a material — never null, {@link ItemStack#EMPTY} when there is no match. */
        public abstract ItemStack getOutput(NTMMaterial mat);

        /** The quanta needed to fill this mold (= the caster's capacity). */
        public abstract int getCost();

        public abstract String getTitle();
    }

    /** Casts a {@link MaterialShapes shape} of whatever material is poured (tag-resolved output). */
    public static class MoldShape extends Mold {

        public final MaterialShapes shape;
        public final int amount;

        public MoldShape(int id, int size, String name, MaterialShapes shape) {
            this(id, size, name, shape, 1);
        }

        public MoldShape(int id, int size, String name, MaterialShapes shape, int amount) {
            super(id, size, name);
            this.shape = shape;
            this.amount = amount;
        }

        @Override
        public ItemStack getOutput(NTMMaterial mat) {
            // Autogen shapes (cast plates, shells, gun parts, ...) resolve to their dedicated item.
            var autogen = ModAutogenItems.get(shape, mat);
            if (autogen != null) return new ItemStack(autogen, amount);

            // Tag scan (the old ore-dict lookup): prefer hbm: items, else the first tag entry.
            ItemStack first = ItemStack.EMPTY;
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(mat.tag(shape))) {
                if (first.isEmpty()) first = new ItemStack(holder.value(), amount);
                if (holder.unwrapKey().map(k -> k.identifier().getNamespace().equals("hbm")).orElse(false)) {
                    return new ItemStack(holder.value(), amount);
                }
            }
            return first;
        }

        @Override
        public int getCost() {
            return shape.quantity * amount;
        }

        @Override
        public String getTitle() {
            return shape.displayName + " x" + amount;
        }
    }

    /** Shape mold with fixed per-material overrides (stone/obsidian blocks → vanilla blocks). */
    public static class MoldBlock extends MoldShape {

        private final Map<NTMMaterial, Supplier<ItemStack>> blockOverrides = new LinkedHashMap<>();

        public MoldBlock(int id, int size, String name, MaterialShapes shape) {
            super(id, size, name, shape);
        }

        public MoldBlock override(NTMMaterial mat, Supplier<ItemStack> stack) {
            blockOverrides.put(mat, stack);
            return this;
        }

        @Override
        public ItemStack getOutput(NTMMaterial mat) {
            Supplier<ItemStack> override = blockOverrides.get(mat);
            if (override != null) return override.get().copy();
            return super.getOutput(mat);
        }
    }

    /** Casts a fixed item from exactly one material. */
    public static class MoldSingle extends Mold {

        private final Supplier<ItemStack> outSupplier;
        public final NTMMaterial mat;
        public final int amount;

        public MoldSingle(int id, int size, String name, Supplier<ItemStack> outSupplier, NTMMaterial mat, int amount) {
            super(id, size, name);
            this.outSupplier = outSupplier;
            this.mat = mat;
            this.amount = amount;
        }

        @Override
        public ItemStack getOutput(NTMMaterial mat) {
            if (this.mat != mat) return ItemStack.EMPTY;
            ItemStack s = outSupplier.get();
            return s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }

        @Override
        public int getCost() {
            return amount;
        }

        @Override
        public String getTitle() {
            ItemStack s = outSupplier.get();
            if (s.isEmpty()) return name + " x0";
            return s.getHoverName().getString() + " x" + s.getCount();
        }
    }

    /** Casts per-material fixed items (blades, stamps, casings). */
    public static class MoldMulti extends Mold {

        private final Map<NTMMaterial, Supplier<ItemStack>> map = new LinkedHashMap<>();
        public final int amount;

        public MoldMulti(int id, int size, String name, int amount) {
            super(id, size, name);
            this.amount = amount;
        }

        public MoldMulti add(NTMMaterial mat, Supplier<ItemStack> out) {
            map.put(mat, out);
            return this;
        }

        @Override
        public ItemStack getOutput(NTMMaterial mat) {
            Supplier<ItemStack> sup = this.map.get(mat);
            if (sup == null) return ItemStack.EMPTY;
            ItemStack out = sup.get();
            return out.isEmpty() ? ItemStack.EMPTY : out.copy();
        }

        @Override
        public int getCost() {
            return amount;
        }

        @Override
        public String getTitle() {
            return prettify(name);
        }
    }

    private static String prettify(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.toLowerCase(java.util.Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
