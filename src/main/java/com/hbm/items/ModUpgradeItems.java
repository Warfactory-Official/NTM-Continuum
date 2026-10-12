package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;

/**
 * The machine upgrade modules (1.12.2 {@code ItemMachineUpgrade} entries of {@code ModItems}): the
 * SPEED / EFFECT (effectiveness) / POWER (power-saving) / OVERDRIVE chains, tiers Mk.I–III (the tier is
 * the level each item contributes; see {@link com.hbm.inventory.UpgradeManagerNT}). All stack size 1,
 * old {@code controlTab}. Kept out of the generated {@link ModItems} so codegen never clobbers them.
 * (Also hosts {@code crystal_xen}, an {@code ItemDrop} the generator doesn't port, needed for the Scream upgrade
 * recipe — kept here so it survives a regen.)
 */
public final class ModUpgradeItems {

    private ModUpgradeItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemMachineUpgrade> upgrade_speed_1 = upgrade("upgrade_speed_1", UpgradeType.SPEED, 1);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_speed_2 = upgrade("upgrade_speed_2", UpgradeType.SPEED, 2);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_speed_3 = upgrade("upgrade_speed_3", UpgradeType.SPEED, 3);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_effect_1 = upgrade("upgrade_effect_1", UpgradeType.EFFECT, 1);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_effect_2 = upgrade("upgrade_effect_2", UpgradeType.EFFECT, 2);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_effect_3 = upgrade("upgrade_effect_3", UpgradeType.EFFECT, 3);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_power_1 = upgrade("upgrade_power_1", UpgradeType.POWER, 1);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_power_2 = upgrade("upgrade_power_2", UpgradeType.POWER, 2);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_power_3 = upgrade("upgrade_power_3", UpgradeType.POWER, 3);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_overdrive_1 = upgrade("upgrade_overdrive_1", UpgradeType.OVERDRIVE, 1);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_overdrive_2 = upgrade("upgrade_overdrive_2", UpgradeType.OVERDRIVE, 2);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_overdrive_3 = upgrade("upgrade_overdrive_3", UpgradeType.OVERDRIVE, 3);
    // Afterburner chain (oil wave): drills burn tanked GAS for bonus power (old ModItems L362-364).
    public static final RegistryObject<ItemMachineUpgrade> upgrade_afterburn_1 = upgrade("upgrade_afterburn_1", UpgradeType.AFTERBURN, 1);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_afterburn_2 = upgrade("upgrade_afterburn_2", UpgradeType.AFTERBURN, 2);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_afterburn_3 = upgrade("upgrade_afterburn_3", UpgradeType.AFTERBURN, 3);

    public static final RegistryObject<ItemMachineUpgrade> upgrade_fortune_1 = upgrade("upgrade_fortune_1", UpgradeType.FORTUNE, 1);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_fortune_2 = upgrade("upgrade_fortune_2", UpgradeType.FORTUNE, 2);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_fortune_3 = upgrade("upgrade_fortune_3", UpgradeType.FORTUNE, 3);
    /** Deletes vacuumed scrap drops ({@link ItemMachineUpgrade#isScrap}) — the mining laser's trash filter. */
    public static final RegistryObject<ItemMachineUpgrade> upgrade_nullifier = upgrade("upgrade_nullifier", UpgradeType.SPECIAL, 0); // 1.7 new ItemMachineUpgrade(): SPECIAL
    // The SPECIAL (tier-0) drop-processing chain (each crafts from the previous): smelter → shredder →
    // centrifuge → crystallizer. The mining laser processes mined drops through the matching recipe set.
    public static final RegistryObject<ItemMachineUpgrade> upgrade_smelter = upgrade("upgrade_smelter", UpgradeType.SPECIAL, 0);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_shredder = upgrade("upgrade_shredder", UpgradeType.SPECIAL, 0);
    /** The mining-laser centrifuge step; matched outputs replace the raw drop, while a recipe miss passes through. */
    public static final RegistryObject<ItemMachineUpgrade> upgrade_centrifuge = upgrade("upgrade_centrifuge", UpgradeType.SPECIAL, 0);
    public static final RegistryObject<ItemMachineUpgrade> upgrade_crystallizer = upgrade("upgrade_crystallizer", UpgradeType.SPECIAL, 0);
    /** Gas centrifuge overclock: permits the terminal HEUF6 separation and shortens its processing time. */
    public static final RegistryObject<ItemMachineUpgrade> upgrade_gc_speed = upgrade("upgrade_gc_speed", UpgradeType.SPECIAL, 0);
    /** The Screaming Scientist upgrade — in the mining laser: ×4 cycles, ×4 speed, ×20 consumption, and
     * screams at the mined block each tick (1.12.2 {@code upgrade_screm}). */
    public static final RegistryObject<ItemMachineUpgrade> upgrade_screm = upgrade("upgrade_screm", UpgradeType.SPECIAL, 0); // 1.7 new ItemMachineUpgrade(): SPECIAL

    /** Artificial Xen Crystal: 1.7 {@code ItemDrop} (a dangerous drop that lifts the ground it lands on); the Scream upgrade's ingredient. */
    public static final RegistryObject<com.hbm.items.special.ItemDrop> crystal_xen =
            REG.<com.hbm.items.special.ItemDrop>item("crystal_xen",
                            p -> new com.hbm.items.special.ItemDrop(p, com.hbm.items.special.ItemDrop.Kind.XEN_CRYSTAL), p -> p.stacksTo(1))
                    .transform(Presets.sprite())
                    .tab(HbmTab.CONTROL);

    private static RegistryObject<ItemMachineUpgrade> upgrade(String name, UpgradeType type, int tier) {
        return REG.<ItemMachineUpgrade>item(name, p -> new ItemMachineUpgrade(type, tier, p), p -> p.stacksTo(1))
                .transform(Presets.sprite())
                .tab(HbmTab.CONTROL);
    }

    public static void init() {}
}
