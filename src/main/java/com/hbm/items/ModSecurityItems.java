package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.hbm.items.tool.ItemCounterfeitKeys;
import com.hbm.items.tool.ItemLock;

/**
 * The padlocks and the key imitation kit (1.7.10 {@code ModItems} lines 3838–3844). {@code key},
 * {@code key_fake}, {@code key_red}, {@code key_red_cracked} and {@code pin} are generated in
 * {@code ModItems}; the unbreakable padlock has no creative tab, as in the original.
 */
public final class ModSecurityItems {
    private ModSecurityItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemLock> padlock_rusty =
            REG.item("padlock_rusty", p -> new ItemLock(p, 1), p -> p.stacksTo(1)).transform(Presets.sprite());
    public static final RegistryObject<ItemLock> padlock =
            REG.item("padlock", p -> new ItemLock(p, 0.1), p -> p.stacksTo(1)).transform(Presets.sprite());
    public static final RegistryObject<ItemLock> padlock_reinforced =
            REG.item("padlock_reinforced", p -> new ItemLock(p, 0.02), p -> p.stacksTo(1)).transform(Presets.sprite());
    public static final RegistryObject<ItemLock> padlock_unbreakable =
            REG.item("padlock_unbreakable", p -> new ItemLock(p, 0), p -> p.stacksTo(1)).transform(Presets.sprite());
    /** setTextureName("key_pair"). */
    public static final RegistryObject<ItemCounterfeitKeys> key_kit =
            REG.item("key_kit", ItemCounterfeitKeys::new, p -> p.stacksTo(1)).transform(Presets.sprite("hbm:items/key_pair"));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, key_kit);
        ModTabs.addTo(HbmTab.CONSUMABLES, padlock_rusty);
        ModTabs.addTo(HbmTab.CONSUMABLES, padlock);
        ModTabs.addTo(HbmTab.CONSUMABLES, padlock_reinforced);
    }

    public static void init() {}
}
