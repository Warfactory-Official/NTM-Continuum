package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.ItemWasteStorage;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;

/**
 * The eight 1.7 storage-drum waste families ({@code nuclear_waste_long/short[_depleted][_tiny]}), whose waste class was
 * the item metadata. One item per class as ntm-next registers them ({@code <base>_<class>}, e.g.
 * {@code nuclear_waste_long_thorium}), each drawn with its family's 1.7 sprite; the class is never stored on the stack
 * (it used to be the int {@code hbm:storage_waste_class}; History §19es-itemvariant).
 */
public final class ModStorageDrumItems {

    private ModStorageDrumItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    private static final Map<String, WasteFamily> FAMILIES = new HashMap<>();

    public static final WasteFamily nuclear_waste_long = family("nuclear_waste_long", ItemWasteStorage.Family.LONG);
    public static final WasteFamily nuclear_waste_long_tiny = family("nuclear_waste_long_tiny", ItemWasteStorage.Family.LONG);
    public static final WasteFamily nuclear_waste_short = family("nuclear_waste_short", ItemWasteStorage.Family.SHORT);
    public static final WasteFamily nuclear_waste_short_tiny = family("nuclear_waste_short_tiny", ItemWasteStorage.Family.SHORT);
    public static final WasteFamily nuclear_waste_long_depleted = family("nuclear_waste_long_depleted", ItemWasteStorage.Family.LONG);
    public static final WasteFamily nuclear_waste_long_depleted_tiny = family("nuclear_waste_long_depleted_tiny", ItemWasteStorage.Family.LONG);
    public static final WasteFamily nuclear_waste_short_depleted = family("nuclear_waste_short_depleted", ItemWasteStorage.Family.SHORT);
    public static final WasteFamily nuclear_waste_short_depleted_tiny = family("nuclear_waste_short_depleted_tiny", ItemWasteStorage.Family.SHORT);

    static {
        for (WasteFamily family : List.of(nuclear_waste_long, nuclear_waste_long_tiny, nuclear_waste_short,
                nuclear_waste_short_tiny, nuclear_waste_long_depleted, nuclear_waste_long_depleted_tiny,
                nuclear_waste_short_depleted, nuclear_waste_short_depleted_tiny)) {
            ModTabs.addStacksTo(HbmTab.CONTROL, family::stacks);
        }
    }

    /** The class names of a family in its enum (= 1.7 metadata) order, read without loading the item class. */
    public static String[] classNames(ItemWasteStorage.Family family) {
        Enum<?>[] values = family == ItemWasteStorage.Family.LONG ? ItemWasteStorage.LongWasteClass.values()
                : ItemWasteStorage.ShortWasteClass.values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) names[i] = values[i].name().toLowerCase(Locale.ROOT);
        return names;
    }

    private static WasteFamily family(String base, ItemWasteStorage.Family kind) {
        String[] names = classNames(kind);
        List<RegistryObject<ItemWasteStorage>> members = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            int variant = i;
            members.add(REG.<ItemWasteStorage>item(base + "_" + names[i],
                            (Item.Properties p) -> new ItemWasteStorage(p.stacksTo(64), kind, base, variant))
                    .transform(Presets.sprite("hbm:items/" + base)));
        }
        WasteFamily family = new WasteFamily(base, List.copyOf(members));
        FAMILIES.put(base, family);
        return family;
    }

    /** The family an item of {@code base} belongs to. */
    public static WasteFamily family(String base) {
        WasteFamily family = FAMILIES.get(base);
        if (family == null) throw new IllegalArgumentException("no storage-drum waste family " + base);
        return family;
    }

    /** The member of {@code item}'s family for class {@code variant}, {@code count} of it. */
    public static ItemStack make(Item item, int variant, int count) {
        ItemStack stack = ItemWasteStorage.make(item, variant);
        stack.setCount(count);
        return stack;
    }

    /** One storage-drum waste family: one registered item per waste class (index = 1.7 metadata). */
    public static final class WasteFamily {
        public final String base;
        private final List<RegistryObject<ItemWasteStorage>> members;

        private WasteFamily(String base, List<RegistryObject<ItemWasteStorage>> members) {
            this.base = base;
            this.members = members;
        }

        /** The family's class-0 member, standing for the family where 1.7 named the bare item. */
        public ItemWasteStorage get() {
            return members.get(0).get();
        }

        /** The member for waste class {@code variant} (wrapped as 1.7's {@code abs(meta) % length}). */
        public ItemWasteStorage get(int variant) {
            return members.get(Math.abs(variant) % members.size()).get();
        }

        public ItemStack stack(int variant) {
            return new ItemStack(get(variant));
        }

        /** Whether {@code item} is a member of this family. */
        public boolean is(Item item) {
            return item instanceof ItemWasteStorage waste && waste.base().equals(base);
        }

        public int size() {
            return members.size();
        }

        public List<ItemStack> stacks() {
            List<ItemStack> stacks = new ArrayList<>(members.size());
            for (RegistryObject<ItemWasteStorage> member : members) stacks.add(new ItemStack(member.get()));
            return stacks;
        }
    }

    public static void init() {}
}
