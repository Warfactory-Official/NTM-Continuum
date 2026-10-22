package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import java.util.function.Function;
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
import dev.golem.core.dejson.Tints;
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
    private static final Reg REG = Reg.of(ITEMS);

    // Spawn items (1.7 ItemChopper family)
    public static final RegistryObject<ItemChopper> spawn_chopper = REG.item("chopper",
                    p -> new ItemChopper(p, () -> ModNtmEntities.HUNTER_CHOPPER.get()), p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.WEAPONS);
    public static final RegistryObject<ItemChopper> spawn_worm = REG.item("spawn_worm",
                    p -> new ItemChopper(p, () -> ModNtmEntities.BALLS_O_TRON.get(), 0, mob -> { },
                            "Without a player in survival mode",
                            "to target, he struggles around a lot.",
                            "",
                            "He's doing his best so please show him",
                            "some consideration."), p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.WEAPONS);
    public static final RegistryObject<ItemChopper> spawn_ufo = REG.item("spawn_ufo",
                    p -> new ItemChopper(p, () -> ModNtmEntities.UFO.get(), 35, mob -> ((EntityUFO) mob).scanCooldown = 100),
                    p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.WEAPONS);
    public static final RegistryObject<ItemChopper> spawn_duck = REG.item("spawn_duck",
                    p -> new ItemChopper(p, () -> ModNtmEntities.DUCK.get()), p -> p.stacksTo(16))
            .transform(Presets.sprite()).tab(HbmTab.WEAPONS);

    // Vehicles
    public static final RegistryObject<ItemBoatRubber> boat_rubber = REG.item("boat_rubber",
                    ItemBoatRubber::new, p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.MACHINES);
    public static final RegistryObject<ItemCouplingTool> coupling_tool = REG.item("coupling_tool",
                    ItemCouplingTool::new, p -> p.stacksTo(1))
            .transform(Presets.sprite()).tab(HbmTab.MACHINES);
    /**
     * 1.7 ModItems.cart: one item per cart type, the chassis carried on the stack. The icon is 1.7's two passes,
     * chassis under overlay, with the chassis pass fixed to the vanilla sprite: the {@code CART_BASE} component
     * has no select vocabulary, so a wood/steel/painted chassis draws as vanilla in hand (the entity is unaffected).
     * The other families keep 1.7's {@code <base>.<constant>} sprite names.
     */
    public static final ItemFamily<ItemModMinecart.EnumMinecart, ItemModMinecart> cart = ItemFamily.of(
            ItemModMinecart.EnumMinecart.class, c -> REG.item("cart_" + c.id, p -> new ItemModMinecart(p, c),
                            p -> p.stacksTo(4))
                    .transform(Presets.sprite("hbm:items/cart.vanilla", "hbm:items/cart_overlay." + c.id)));
    /** 1.7 ModItems.train - setCreativeTab(null) */
    public static final ItemFamily<ItemTrain.EnumTrainType, ItemTrain> train = ItemFamily.of(
            ItemTrain.EnumTrainType.class, t -> REG.item("train_" + t.name().toLowerCase(Locale.ROOT),
                            p -> new ItemTrain(p, t), p -> p.stacksTo(1))
                    .transform(Presets.sprite("hbm:items/train." + t.name().toLowerCase(Locale.ROOT))));
    public static final ItemFamily<ItemDrone.EnumDroneType, ItemDrone> drone = ItemFamily.of(
            ItemDrone.EnumDroneType.class, d -> REG.item("drone_" + d.name().toLowerCase(Locale.ROOT),
                            p -> new ItemDrone(p, d), p -> p)
                    .transform(Presets.sprite("hbm:items/drone." + d.name().toLowerCase(Locale.ROOT))));

    // Grenades (1.7 ModItems:3590 and the universal grenade parts)
    public static final RegistryObject<ItemGrenadeDynamite> stick_dynamite = REG.item("stick_dynamite",
                    p -> new ItemGrenadeDynamite(p, 3), p -> p.stacksTo(16))
            .transform(Presets.sprite()).tab(HbmTab.WEAPONS);
    public static final ItemFamily<ItemGrenadeShell.EnumGrenadeShell, ItemGrenadeShell> grenade_shell = ItemFamily.of(
            ItemGrenadeShell.EnumGrenadeShell.class, e -> grenadePart("grenade_shell", e,
                    p -> new ItemGrenadeShell(p, e)));
    public static final ItemFamily<ItemGrenadeFilling.EnumGrenadeFilling, ItemGrenadeFilling> grenade_filling =
            ItemFamily.of(ItemGrenadeFilling.EnumGrenadeFilling.class, e -> grenadePart("grenade_filling", e,
                    p -> new ItemGrenadeFilling(p, e)));
    public static final ItemFamily<ItemGrenadeFuze.EnumGrenadeFuze, ItemGrenadeFuze> grenade_fuze = ItemFamily.of(
            ItemGrenadeFuze.EnumGrenadeFuze.class, e -> grenadePart("grenade_fuze", e,
                    p -> new ItemGrenadeFuze(p, e)));
    public static final ItemFamily<ItemGrenadeExtra.EnumGrenadeExtra, ItemGrenadeExtra> grenade_extra = ItemFamily.of(
            ItemGrenadeExtra.EnumGrenadeExtra.class, e -> grenadePart("grenade_extra", e,
                    p -> new ItemGrenadeExtra(p, e)));
    public static final RegistryObject<ItemGrenadeUniversal> grenade_universal = REG.item(
                    "grenade_universal", ItemGrenadeUniversal::new, p -> p.stacksTo(4))
            // 1.7 getSubItems: every shell/filling/fuze(/extra) combination, not the bare item
            .andThen(h -> ModTabs.addStacksTo(HbmTab.WEAPONS, ItemGrenadeUniversal::subItems))
            // 1.7 ItemRenderGrenade drew every slot, the inventory included, from the stack's shell/filling/fuze
            .andThen(h -> ItemRendererDeclarations.model(h.path(), "grenade", "weapon"));

    /** A universal-grenade part: {@code <base>_<constant>}, drawn with the 1.7 {@code <base>.<constant>} sprite. */
    private static <I extends Item> RegistryObject<I> grenadePart(String base, Enum<?> constant,
                                                                  Function<Item.Properties, I> factory) {
        String name = constant.name().toLowerCase(Locale.ROOT);
        return REG.item(base + "_" + name, factory, p -> p).transform(Presets.sprite("hbm:items/" + base + "." + name));
    }

    // Dispersers (1.7 ModItems:3881; the empty canister/gland items already exist in ModItems)
    public static final RegistryObject<ItemDisperser> disperser_canister = REG.item("disperser_canister",
                    p -> new ItemDisperser(p, 2000, "disperser_canister", ItemDisperser.Kind.CANISTER), p -> p)
            .transform(Presets.sprite("hbm:items/disperser_canister", "hbm:items/disperser_canister_overlay")
                    .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));
    public static final RegistryObject<ItemDisperser> glyphid_gland = REG.item("glyphid_gland",
                    p -> new ItemDisperser(p, 4000, "glyphid_gland", ItemDisperser.Kind.GLAND), p -> p)
            .transform(Presets.sprite("hbm:items/glyphid_gland", "hbm:items/fluid_identifier_overlay")
                    .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));

    static {
        // 1.7 put both in the weapon tab, one full stack per ItemDisperser.getSubItems fluid.
        ModTabs.addStacksTo(HbmTab.WEAPONS, () -> disperser_canister.get().creativeVariants());
        ModTabs.addStacksTo(HbmTab.WEAPONS, () -> glyphid_gland.get().creativeVariants());
    }

    // Drops
    public static final RegistryObject<ItemPlasticBag> plastic_bag = REG.item("plastic_bag",
                    ItemPlasticBag::new, p -> p)
            .transform(Presets.sprite()).tab(HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemBatteryCreative> battery_creative = REG.item(
                    "battery_creative", ItemBatteryCreative::new, p -> p.stacksTo(1))
            .transform(Presets.sprite("hbm:items/battery_creative_new")).tab(HbmTab.CONTROL);

    public static void init() {}
}
