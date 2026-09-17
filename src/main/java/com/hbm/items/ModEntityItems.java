package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.items.machine.ItemBatteryCreative;
import com.hbm.items.special.ItemChopper;
import com.hbm.items.special.ItemTrain;
import com.hbm.items.tool.ItemBoatRubber;
import com.hbm.items.tool.ItemCouplingTool;
import com.hbm.items.tool.ItemDrone;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.tool.ItemPlasticBag;
import com.hbm.items.weapon.ItemDisperser;
import com.hbm.items.weapon.ItemGrenadeDynamite;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.lib.Library;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.Locale;

/**
 * The items the ntm-next entity port spawns from or drops: spawn items, the rubber boat, the coupling
 * tool, carts, drones, trains, the grenade family, the disperser canisters and the drifting bag's
 * loot. Ids, stack sizes and constructors follow ntm-next's {@code ModItems} declarations, themselves
 * the 1.7 {@code ModItems} entries.
 *
 * <p>Not here: the 122 custom-missile parts and the custom missile item (ntm-next {@code MP_*} /
 * {@code MISSILE_CUSTOM}); {@code EntityMissileCustom} compiles against the part classes and reads
 * empty parts until that family is registered.
 */
public final class ModEntityItems {

    private ModEntityItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static <I extends Item> RegistryObject<I> tabbed(RegistryObject<I> item, HbmTab tab) {
        ModTabs.addTo(tab, item);
        return item;
    }

    // --- spawn items (1.7 ItemChopper family) ---
    public static final RegistryObject<ItemChopper> spawn_chopper = tabbed(ITEMS.registerItem("chopper",
            p -> new ItemChopper(p, () -> ModNtmEntities.HUNTER_CHOPPER.get()), p -> p.stacksTo(1)), HbmTab.WEAPONS);
    public static final RegistryObject<ItemChopper> spawn_worm = tabbed(ITEMS.registerItem("spawn_worm",
            p -> new ItemChopper(p, () -> ModNtmEntities.BALLS_O_TRON.get(), 0, mob -> { },
                    "Without a player in survival mode",
                    "to target, he struggles around a lot.",
                    "",
                    "He's doing his best so please show him",
                    "some consideration."), p -> p.stacksTo(1)), HbmTab.WEAPONS);
    public static final RegistryObject<ItemChopper> spawn_ufo = tabbed(ITEMS.registerItem("spawn_ufo",
            p -> new ItemChopper(p, () -> ModNtmEntities.UFO.get(), 35, mob -> ((EntityUFO) mob).scanCooldown = 100),
            p -> p.stacksTo(1)), HbmTab.WEAPONS);
    public static final RegistryObject<ItemChopper> spawn_duck = tabbed(ITEMS.registerItem("spawn_duck",
            p -> new ItemChopper(p, () -> ModNtmEntities.DUCK.get()), p -> p.stacksTo(16)), HbmTab.WEAPONS);

    // --- vehicles ---
    public static final RegistryObject<ItemBoatRubber> boat_rubber = tabbed(ITEMS.registerItem("boat_rubber",
            ItemBoatRubber::new, p -> p.stacksTo(1)), HbmTab.MACHINES);
    public static final RegistryObject<ItemCouplingTool> coupling_tool = tabbed(ITEMS.registerItem("coupling_tool",
            ItemCouplingTool::new, p -> p.stacksTo(1)), HbmTab.MACHINES);
    /** 1.7 ModItems.cart: one item per cart type, the chassis carried on the stack. */
    public static final ItemFamily<ItemModMinecart.EnumMinecart, ItemModMinecart> cart = ItemFamily.of(ITEMS,
            ItemModMinecart.EnumMinecart.class, c -> "cart_" + c.id, ItemModMinecart::new, p -> p.stacksTo(4));
    /** 1.7 ModItems.train - setCreativeTab(null) */
    public static final ItemFamily<ItemTrain.EnumTrainType, ItemTrain> train = ItemFamily.of(ITEMS,
            ItemTrain.EnumTrainType.class, t -> "train_" + t.name().toLowerCase(Locale.ROOT), ItemTrain::new,
            p -> p.stacksTo(1));
    public static final ItemFamily<ItemDrone.EnumDroneType, ItemDrone> drone = ItemFamily.of(ITEMS,
            ItemDrone.EnumDroneType.class, d -> "drone_" + d.name().toLowerCase(Locale.ROOT), ItemDrone::new,
            p -> p);

    // --- grenades (1.7 ModItems:3590 and the universal grenade parts) ---
    public static final RegistryObject<ItemGrenadeDynamite> stick_dynamite = tabbed(ITEMS.registerItem("stick_dynamite",
            p -> new ItemGrenadeDynamite(p, 3), p -> p.stacksTo(16)), HbmTab.WEAPONS);
    public static final ItemFamily<ItemGrenadeShell.EnumGrenadeShell, ItemGrenadeShell> grenade_shell = ItemFamily.of(
            ITEMS, "grenade_shell", ItemGrenadeShell.EnumGrenadeShell.class, ItemGrenadeShell::new, p -> p);
    public static final ItemFamily<ItemGrenadeFilling.EnumGrenadeFilling, ItemGrenadeFilling> grenade_filling =
            ItemFamily.of(ITEMS, "grenade_filling", ItemGrenadeFilling.EnumGrenadeFilling.class,
                    ItemGrenadeFilling::new, p -> p);
    public static final ItemFamily<ItemGrenadeFuze.EnumGrenadeFuze, ItemGrenadeFuze> grenade_fuze = ItemFamily.of(
            ITEMS, "grenade_fuze", ItemGrenadeFuze.EnumGrenadeFuze.class, ItemGrenadeFuze::new, p -> p);
    public static final ItemFamily<ItemGrenadeExtra.EnumGrenadeExtra, ItemGrenadeExtra> grenade_extra = ItemFamily.of(
            ITEMS, "grenade_extra", ItemGrenadeExtra.EnumGrenadeExtra.class, ItemGrenadeExtra::new, p -> p);
    public static final RegistryObject<ItemGrenadeUniversal> grenade_universal = tabbed(ITEMS.registerItem(
            "grenade_universal", ItemGrenadeUniversal::new, p -> p.stacksTo(4)), HbmTab.WEAPONS);

    // --- dispersers (1.7 ModItems:3881; the empty canister/gland items already exist in ModItems) ---
    public static final RegistryObject<ItemDisperser> disperser_canister = ITEMS.registerItem("disperser_canister",
            p -> new ItemDisperser(p, 2000, "disperser_canister", ItemDisperser.Kind.CANISTER,
                    Library.id("disperser_canister_empty"), Library.id("disperser_canister_overlay")),
            p -> p);
    public static final RegistryObject<ItemDisperser> glyphid_gland = ITEMS.registerItem("glyphid_gland",
            p -> new ItemDisperser(p, 4000, "glyphid_gland", ItemDisperser.Kind.GLAND,
                    Library.id("glyphid_gland_empty"), Library.id("fluid_identifier_overlay")),
            p -> p);

    // --- drops ---
    public static final RegistryObject<ItemPlasticBag> plastic_bag = tabbed(ITEMS.registerItem("plastic_bag",
            ItemPlasticBag::new, p -> p), HbmTab.CONSUMABLES);
    /**
     * 1.7 ModItems.bottled_cloud, the mask man's drop. In 1.7/ntm-next it is an armour module
     * ({@code ItemModCloud}); this port's armour-module wave owns that behaviour, so the drop is a plain
     * item until it is wired there.
     */
    public static final RegistryObject<Item> bottled_cloud = tabbed(ITEMS.registerSimpleItem("bottled_cloud"),
            HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemBatteryCreative> battery_creative = tabbed(ITEMS.registerItem(
            "battery_creative", ItemBatteryCreative::new, p -> p.stacksTo(1)), HbmTab.CONTROL);

    public static void init() {}
}
