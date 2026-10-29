package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.FuelType;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart.Rarity;
import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.hbm.items.weapon.MissilePartTable;
import dev.golem.core.Ids;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.hook.Hook;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The custom missile system's items: all 122 {@code mp_*} parts of 1.7 {@code ModItems} and the assembled
 * {@code missile_custom}, declared in upstream's order with upstream's builder calls.
 *
 * <p>Registry ids are the 1.7 unlocalized names, so the stability fin field {@code mp_stability_20_flat} is id
 * {@code mp_s_20} and the chips {@code mp_chip_N} are {@code mp_c_N}, exactly as upstream registered them.
 *
 * <p>1.7's {@code copy()} variants carried over the base part's type, sizes, attributes and health, and nothing
 * else, not even its author, so each copy here restates only the base's builder call and health before its own
 * modifiers. Every part stacks to one and sits on the missile tab, except the two upstream hid
 * ({@code mp_fuselage_10_long_kerosene_dash}, {@code mp_fuselage_15_kerosene_pip}).
 */
public final class ModCustomMissileItems {

    private ModCustomMissileItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** The assembled missile's five parts; 1.7 kept them as numeric item ids on the stack NBT. */
    public static final GolemComponent<ItemCustomMissile.Parts> MISSILE_PARTS =
            GolemComponent.of(Constants.MOD_ID, "missile_custom_parts", () -> ItemCustomMissile.Parts.CODEC); // lazy: the codec names the item registry

    /** 1.7 {@code missile_custom}: stack size 1, no creative tab of its own (the missile tab adds prebuilt ones). */
    public static final RegistryObject<ItemCustomMissile> missile_custom =
            REG.item("missile_custom", ItemCustomMissile::new, p -> p.stacksTo(1));

    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_10_kerosene = part("mp_thruster_10_kerosene",
            p -> p.makeThruster(FuelType.KEROSENE, 1F, 1.5F, PartSize.SIZE_10).setHealth(10F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_10_solid = part("mp_thruster_10_solid",
            p -> p.makeThruster(FuelType.SOLID, 1F, 1.5F, PartSize.SIZE_10).setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_10_xenon = part("mp_thruster_10_xenon",
            p -> p.makeThruster(FuelType.XENON, 1F, 1.5F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_kerosene = part("mp_thruster_15_kerosene",
            p -> p.makeThruster(FuelType.KEROSENE, 1F, 7.5F, PartSize.SIZE_15).setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_kerosene_dual = part("mp_thruster_15_kerosene_dual",
            p -> p.makeThruster(FuelType.KEROSENE, 1F, 2.5F, PartSize.SIZE_15).setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_kerosene_triple = part("mp_thruster_15_kerosene_triple",
            p -> p.makeThruster(FuelType.KEROSENE, 1F, 5F, PartSize.SIZE_15).setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_solid = part("mp_thruster_15_solid",
            p -> p.makeThruster(FuelType.SOLID, 1F, 5F, PartSize.SIZE_15).setHealth(20F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_solid_hexdecuple = part("mp_thruster_15_solid_hexdecuple",
            p -> p.makeThruster(FuelType.SOLID, 1F, 5F, PartSize.SIZE_15).setHealth(25F).setRarity(Rarity.UNCOMMON));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_hydrogen = part("mp_thruster_15_hydrogen",
            p -> p.makeThruster(FuelType.HYDROGEN, 1F, 7.5F, PartSize.SIZE_15).setHealth(20F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_hydrogen_dual = part("mp_thruster_15_hydrogen_dual",
            p -> p.makeThruster(FuelType.HYDROGEN, 1F, 2.5F, PartSize.SIZE_15).setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_balefire_short = part("mp_thruster_15_balefire_short",
            p -> p.makeThruster(FuelType.BALEFIRE, 1F, 5F, PartSize.SIZE_15).setHealth(25F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_balefire = part("mp_thruster_15_balefire",
            p -> p.makeThruster(FuelType.BALEFIRE, 1F, 5F, PartSize.SIZE_15).setHealth(25F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_balefire_large = part("mp_thruster_15_balefire_large",
            p -> p.makeThruster(FuelType.BALEFIRE, 1F, 7.5F, PartSize.SIZE_15).setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_15_balefire_large_rad = part("mp_thruster_15_balefire_large_rad",
            p -> p.makeThruster(FuelType.BALEFIRE, 1F, 7.5F, PartSize.SIZE_15).setAuthor("The Master").setHealth(35F).setRarity(Rarity.UNCOMMON));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_20_kerosene = part("mp_thruster_20_kerosene",
            p -> p.makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_20_kerosene_dual = part("mp_thruster_20_kerosene_dual",
            p -> p.makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_20_kerosene_triple = part("mp_thruster_20_kerosene_triple",
            p -> p.makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_20_solid = part("mp_thruster_20_solid",
            p -> p.makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20).setHealth(35F).setWittyText("It's basically just a big hole at the end of the fuel tank."));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_20_solid_multi = part("mp_thruster_20_solid_multi",
            p -> p.makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20).setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> mp_thruster_20_solid_multier = part("mp_thruster_20_solid_multier",
            p -> p.makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20).setHealth(35F).setWittyText("Did I miscount? Hope not."));
    public static final RegistryObject<ItemCustomMissilePart> mp_stability_10_flat = part("mp_stability_10_flat",
            p -> p.makeStability(0.5F, PartSize.SIZE_10).setHealth(10F));
    public static final RegistryObject<ItemCustomMissilePart> mp_stability_10_cruise = part("mp_stability_10_cruise",
            p -> p.makeStability(0.25F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryObject<ItemCustomMissilePart> mp_stability_10_space = part("mp_stability_10_space",
            p -> p.makeStability(0.35F, PartSize.SIZE_10).setHealth(5F).setRarity(Rarity.COMMON).setWittyText("Standing there alone, the ship is waiting / All systems are go, are you sure?"));
    public static final RegistryObject<ItemCustomMissilePart> mp_stability_15_flat = part("mp_stability_15_flat",
            p -> p.makeStability(0.5F, PartSize.SIZE_15).setHealth(10F));
    public static final RegistryObject<ItemCustomMissilePart> mp_stability_15_thin = part("mp_stability_15_thin",
            p -> p.makeStability(0.35F, PartSize.SIZE_15).setHealth(5F));
    public static final RegistryObject<ItemCustomMissilePart> mp_stability_15_soyuz = part("mp_stability_15_soyuz",
            p -> p.makeStability(0.25F, PartSize.SIZE_15).setHealth(15F).setRarity(Rarity.COMMON).setWittyText("Союз!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_stability_20_flat = part("mp_s_20",
            p -> p.makeStability(0.5F, PartSize.SIZE_20));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene = part("mp_fuselage_10_kerosene",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setAuthor("Hoboy").setHealth(20F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_camo = part("mp_fuselage_10_kerosene_camo",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.COMMON).setTitle("Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_desert = part("mp_fuselage_10_kerosene_desert",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.COMMON).setTitle("Desert Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_sky = part("mp_fuselage_10_kerosene_sky",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.COMMON).setTitle("Sky Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_flames = part("mp_fuselage_10_kerosene_flames",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.UNCOMMON).setTitle("Sick Flames"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_insulation = part("mp_fuselage_10_kerosene_insulation",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.COMMON).setTitle("Orange Insulation").setHealth(25F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_sleek = part("mp_fuselage_10_kerosene_sleek",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.RARE).setTitle("IF-R&D").setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_metal = part("mp_fuselage_10_kerosene_metal",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.UNCOMMON).setTitle("Bolted Metal").setHealth(30F).setAuthor("Hoboy"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_kerosene_taint = part("mp_fuselage_10_kerosene_taint",
            p -> p.makeFuselage(FuelType.KEROSENE, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.UNCOMMON).setAuthor("Sam").setTitle("Tainted"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid = part("mp_fuselage_10_solid",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_flames = part("mp_fuselage_10_solid_flames",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.UNCOMMON).setTitle("Sick Flames"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_insulation = part("mp_fuselage_10_solid_insulation",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.COMMON).setTitle("Orange Insulation").setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_sleek = part("mp_fuselage_10_solid_sleek",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.RARE).setTitle("IF-R&D").setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_soviet_glory = part("mp_fuselage_10_solid_soviet_glory",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.EPIC).setAuthor("Hoboy").setHealth(35F).setTitle("Soviet Glory"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_cathedral = part("mp_fuselage_10_solid_cathedral",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.RARE).setAuthor("Satan").setTitle("Unholy Cathedral").setWittyText("Quakeesque!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_moonlit = part("mp_fuselage_10_solid_moonlit",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.UNCOMMON).setAuthor("The Master & Hoboy").setTitle("Moonlit"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_battery = part("mp_fuselage_10_solid_battery",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.UNCOMMON).setAuthor("wolfmonster222").setHealth(30F).setTitle("Ecstatic").setWittyText("I got caught eating batteries again :("));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_solid_duracell = part("mp_fuselage_10_solid_duracell",
            p -> p.makeFuselage(FuelType.SOLID, 2500F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F).setRarity(Rarity.RARE).setAuthor("Hoboy").setTitle("Duracell").setHealth(30F).setWittyText("The crunchiest battery on the market!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_xenon = part("mp_fuselage_10_xenon",
            p -> p.makeFuselage(FuelType.XENON, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_xenon_bhole = part("mp_fuselage_10_xenon_bhole",
            p -> p.makeFuselage(FuelType.XENON, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.RARE).setAuthor("Sten89").setTitle("Morceus-1457"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene = part("mp_fuselage_10_long_kerosene",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setAuthor("Hoboy").setHealth(30F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_camo = part("mp_fuselage_10_long_kerosene_camo",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.COMMON).setTitle("Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_desert = part("mp_fuselage_10_long_kerosene_desert",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.COMMON).setTitle("Desert Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_sky = part("mp_fuselage_10_long_kerosene_sky",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.COMMON).setTitle("Sky Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_flames = part("mp_fuselage_10_long_kerosene_flames",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.UNCOMMON).setTitle("Sick Flames"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_insulation = part("mp_fuselage_10_long_kerosene_insulation",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.COMMON).setTitle("Orange Insulation").setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_sleek = part("mp_fuselage_10_long_kerosene_sleek",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.RARE).setTitle("IF-R&D").setHealth(40F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_metal = part("mp_fuselage_10_long_kerosene_metal",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.UNCOMMON).setAuthor("Hoboy").setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_dash = part("mp_fuselage_10_long_kerosene_dash",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.EPIC).setAuthor("Sam").setTitle("Dash").setWittyText("I wash my hands of it."));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_taint = part("mp_fuselage_10_long_kerosene_taint",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.UNCOMMON).setAuthor("Sam").setTitle("Tainted"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_kerosene_vap = part("mp_fuselage_10_long_kerosene_vap",
            p -> p.makeFuselage(FuelType.KEROSENE, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(30F).setRarity(Rarity.EPIC).setAuthor("VT-6/24").setTitle("Minty Contrail").setWittyText("Upper rivet!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_solid = part("mp_fuselage_10_long_solid",
            p -> p.makeFuselage(FuelType.SOLID, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_solid_flames = part("mp_fuselage_10_long_solid_flames",
            p -> p.makeFuselage(FuelType.SOLID, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F).setRarity(Rarity.UNCOMMON).setTitle("Sick Flames"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_solid_insulation = part("mp_fuselage_10_long_solid_insulation",
            p -> p.makeFuselage(FuelType.SOLID, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F).setRarity(Rarity.COMMON).setTitle("Orange Insulation").setHealth(40F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_solid_sleek = part("mp_fuselage_10_long_solid_sleek",
            p -> p.makeFuselage(FuelType.SOLID, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F).setRarity(Rarity.RARE).setTitle("IF-R&D").setHealth(45F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_solid_soviet_glory = part("mp_fuselage_10_long_solid_soviet_glory",
            p -> p.makeFuselage(FuelType.SOLID, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F).setRarity(Rarity.EPIC).setAuthor("Hoboy").setHealth(45F).setTitle("Soviet Glory").setWittyText("Fully Automated Luxury Gay Space Communism!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_solid_bullet = part("mp_fuselage_10_long_solid_bullet",
            p -> p.makeFuselage(FuelType.SOLID, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F).setRarity(Rarity.COMMON).setAuthor("Sam").setTitle("Bullet Bill"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_long_solid_silvermoonlight = part("mp_fuselage_10_long_solid_silvermoonlight",
            p -> p.makeFuselage(FuelType.SOLID, 5000F, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F).setRarity(Rarity.UNCOMMON).setAuthor("The Master").setTitle("Silver Moonlight"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_15_kerosene = part("mp_fuselage_10_15_kerosene",
            p -> p.makeFuselage(FuelType.KEROSENE, 10000F, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_15_solid = part("mp_fuselage_10_15_solid",
            p -> p.makeFuselage(FuelType.SOLID, 10000F, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_15_hydrogen = part("mp_fuselage_10_15_hydrogen",
            p -> p.makeFuselage(FuelType.HYDROGEN, 10000F, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_10_15_balefire = part("mp_fuselage_10_15_balefire",
            p -> p.makeFuselage(FuelType.BALEFIRE, 10000F, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene = part("mp_fuselage_15_kerosene",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setAuthor("Hoboy").setHealth(50F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_camo = part("mp_fuselage_15_kerosene_camo",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.COMMON).setTitle("Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_desert = part("mp_fuselage_15_kerosene_desert",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.COMMON).setTitle("Desert Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_sky = part("mp_fuselage_15_kerosene_sky",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.COMMON).setTitle("Sky Camo"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_insulation = part("mp_fuselage_15_kerosene_insulation",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.COMMON).setTitle("Orange Insulation").setHealth(55F).setWittyText("Rest in spaghetti Columbia :("));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_metal = part("mp_fuselage_15_kerosene_metal",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.UNCOMMON).setAuthor("Hoboy").setTitle("Bolted Metal").setHealth(60F).setWittyText("Metal frame with metal plating reinforced with bolted metal sheets and metal."));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_decorated = part("mp_fuselage_15_kerosene_decorated",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.UNCOMMON).setAuthor("Hoboy").setTitle("Decorated").setHealth(60F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_steampunk = part("mp_fuselage_15_kerosene_steampunk",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.RARE).setAuthor("Hoboy").setTitle("Steampunk").setHealth(60F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_polite = part("mp_fuselage_15_kerosene_polite",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.LEGENDARY).setAuthor("Hoboy").setTitle("Polite").setHealth(60F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_blackjack = part("mp_fuselage_15_kerosene_blackjack",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.LEGENDARY).setTitle("Queen Whiskey").setHealth(100F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_lambda = part("mp_fuselage_15_kerosene_lambda",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.RARE).setAuthor("VT-6/24").setTitle("Lambda Complex").setHealth(75F).setWittyText("MAGNIFICENT MICROWAVE CASSEROLE"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_minuteman = part("mp_fuselage_15_kerosene_minuteman",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.UNCOMMON).setAuthor("Spexta").setTitle("MX 1702"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_pip = part("mp_fuselage_15_kerosene_pip",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.EPIC).setAuthor("The Doctor").setTitle("LittlePip").setWittyText("31!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_taint = part("mp_fuselage_15_kerosene_taint",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.UNCOMMON).setAuthor("Sam").setTitle("Tainted").setWittyText("DUN-DUN!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_kerosene_yuck = part("mp_fuselage_15_kerosene_yuck",
            p -> p.makeFuselage(FuelType.KEROSENE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.EPIC).setAuthor("Hoboy").setTitle("Flesh").setWittyText("Note: Never clean DNA vials with your own spit.").setHealth(60F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid = part("mp_fuselage_15_solid",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_insulation = part("mp_fuselage_15_solid_insulation",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.COMMON).setTitle("Orange Insulation").setHealth(65F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_desh = part("mp_fuselage_15_solid_desh",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.RARE).setAuthor("Hoboy").setTitle("Desh Plating").setHealth(80F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_soviet_glory = part("mp_fuselage_15_solid_soviet_glory",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.RARE).setAuthor("Hoboy").setTitle("Soviet Glory").setHealth(70F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_soviet_stank = part("mp_fuselage_15_solid_soviet_stank",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.EPIC).setAuthor("Hoboy").setTitle("Soviet Stank").setHealth(15F).setWittyText("Aged like a fine wine! Well, almost."));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_faust = part("mp_fuselage_15_solid_faust",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.LEGENDARY).setAuthor("Dr.Nostalgia").setTitle("Mighty Lauren").setHealth(250F).setWittyText("Welcome to Subway, may I take your order?"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_silvermoonlight = part("mp_fuselage_15_solid_silvermoonlight",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.UNCOMMON).setAuthor("The Master").setTitle("Silver Moonlight"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_snowy = part("mp_fuselage_15_solid_snowy",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.UNCOMMON).setAuthor("Dr.Nostalgia").setTitle("Chilly Day"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_panorama = part("mp_fuselage_15_solid_panorama",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.RARE).setAuthor("Hoboy").setTitle("Panorama"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_roses = part("mp_fuselage_15_solid_roses",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.UNCOMMON).setAuthor("Hoboy").setTitle("Bed of roses"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_solid_mimi = part("mp_fuselage_15_solid_mimi",
            p -> p.makeFuselage(FuelType.SOLID, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F).setRarity(Rarity.RARE).setTitle("Mimi-chan"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_hydrogen = part("mp_fuselage_15_hydrogen",
            p -> p.makeFuselage(FuelType.HYDROGEN, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_hydrogen_cathedral = part("mp_fuselage_15_hydrogen_cathedral",
            p -> p.makeFuselage(FuelType.HYDROGEN, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F).setRarity(Rarity.UNCOMMON).setAuthor("Satan").setTitle("Unholy Cathedral"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_balefire = part("mp_fuselage_15_balefire",
            p -> p.makeFuselage(FuelType.BALEFIRE, 15000F, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(75F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_20_kerosene = part("mp_fuselage_15_20_kerosene",
            p -> p.makeFuselage(FuelType.KEROSENE, 20000F, PartSize.SIZE_15, PartSize.SIZE_20).setAuthor("Hoboy").setHealth(70F));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_20_kerosene_magnusson = part("mp_fuselage_15_20_kerosene_magnusson",
            p -> p.makeFuselage(FuelType.KEROSENE, 20000F, PartSize.SIZE_15, PartSize.SIZE_20).setHealth(70F).setRarity(Rarity.RARE).setAuthor("VT-6/24").setTitle("White Forest Rocket").setWittyText("And get your cranio-conjugal parasite away from my nose cone!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_fuselage_15_20_solid = part("mp_fuselage_15_20_solid",
            p -> p.makeFuselage(FuelType.SOLID, 20000F, PartSize.SIZE_15, PartSize.SIZE_20).setHealth(70F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_10_he = part("mp_warhead_10_he",
            p -> p.makeWarhead(WarheadType.HE, 15F, 1.5F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_10_incendiary = part("mp_warhead_10_incendiary",
            p -> p.makeWarhead(WarheadType.INC, 15F, 1.5F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_10_buster = part("mp_warhead_10_buster",
            p -> p.makeWarhead(WarheadType.BUSTER, 5F, 1.5F, PartSize.SIZE_10).setHealth(5F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_10_nuclear = part("mp_warhead_10_nuclear",
            p -> p.makeWarhead(WarheadType.NUCLEAR, 35F, 1.5F, PartSize.SIZE_10).setTitle("Tater Tot").setHealth(10F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_10_nuclear_large = part("mp_warhead_10_nuclear_large",
            p -> p.makeWarhead(WarheadType.NUCLEAR, 75F, 2.5F, PartSize.SIZE_10).setTitle("Chernobyl Boris").setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_10_taint = part("mp_warhead_10_taint",
            p -> p.makeWarhead(WarheadType.TAINT, 15F, 1.5F, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.UNCOMMON).setWittyText("Eat my taint! Bureaucracy is dead and we killed it!"));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_10_cloud = part("mp_warhead_10_cloud",
            p -> p.makeWarhead(WarheadType.CLOUD, 15F, 1.5F, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.RARE));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_he = part("mp_warhead_15_he",
            p -> p.makeWarhead(WarheadType.HE, 50F, 2.5F, PartSize.SIZE_15).setHealth(10F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_incendiary = part("mp_warhead_15_incendiary",
            p -> p.makeWarhead(WarheadType.INC, 35F, 2.5F, PartSize.SIZE_15).setHealth(10F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_nuclear = part("mp_warhead_15_nuclear",
            p -> p.makeWarhead(WarheadType.NUCLEAR, 125F, 5F, PartSize.SIZE_15).setTitle("Auntie Bertha").setHealth(15F));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_nuclear_shark = part("mp_warhead_15_nuclear_shark",
            p -> p.makeWarhead(WarheadType.NUCLEAR, 125F, 5F, PartSize.SIZE_15).setHealth(15F).setRarity(Rarity.UNCOMMON).setTitle("Discount Bullet Bill").setWittyText("Nose art on a cannon bullet? Who does that?"));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_nuclear_mimi = part("mp_warhead_15_nuclear_mimi",
            p -> p.makeWarhead(WarheadType.NUCLEAR, 125F, 5F, PartSize.SIZE_15).setHealth(15F).setRarity(Rarity.RARE).setTitle("FASHIONABLE MISSILE"));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_boxcar = part("mp_warhead_15_boxcar",
            p -> p.makeWarhead(WarheadType.TX, 250F, 7.5F, PartSize.SIZE_15).setWittyText("?!?!").setHealth(35F).setRarity(Rarity.LEGENDARY));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_n2 = part("mp_warhead_15_n2",
            p -> p.makeWarhead(WarheadType.N2, 100F, 5F, PartSize.SIZE_15).setWittyText("[screams geometrically]").setHealth(20F).setRarity(Rarity.RARE));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_balefire = part("mp_warhead_15_balefire",
            p -> p.makeWarhead(WarheadType.BALEFIRE, 100F, 7.5F, PartSize.SIZE_15).setRarity(Rarity.LEGENDARY).setAuthor("VT-6/24").setHealth(15F).setWittyText("Hightower, never forgetti."));
    public static final RegistryObject<ItemCustomMissilePart> mp_warhead_15_turbine = part("mp_warhead_15_turbine",
            p -> p.makeWarhead(WarheadType.TURBINE, 200F, 5F, PartSize.SIZE_15).setRarity(Rarity.STRANGE).setHealth(250F));
    public static final RegistryObject<ItemCustomMissilePart> mp_chip_1 = part("mp_c_1",
            p -> p.makeChip(0.1F)).transform(Presets.sprite());
    public static final RegistryObject<ItemCustomMissilePart> mp_chip_2 = part("mp_c_2",
            p -> p.makeChip(0.05F)).transform(Presets.sprite());
    public static final RegistryObject<ItemCustomMissilePart> mp_chip_3 = part("mp_c_3",
            p -> p.makeChip(0.01F)).transform(Presets.sprite());
    public static final RegistryObject<ItemCustomMissilePart> mp_chip_4 = part("mp_c_4",
            p -> p.makeChip(0.005F)).transform(Presets.sprite());
    public static final RegistryObject<ItemCustomMissilePart> mp_chip_5 = part("mp_c_5",
            p -> p.makeChip(0.0F)).transform(Presets.sprite());

    /**
     * The item properties of a custom missile part, by part id, applied when the part is registered after 1.7's
     * {@code stacksTo(1)}. Plain HBM has no listener (every part stacks to one, as 1.7); NTM: Space stacks fuselages and
     * thrusters to 64, as hbm-space did, for its rocket stage clusters. A listener runs from an addon's start-up, before
     * registration (ADDON-API-INVENTORY V-167).
     */
    @FunctionalInterface
    public interface PartProperties {
        Item.Properties apply(String id, Item.Properties properties);
    }

    public static final Hook<PartProperties> PART_PROPERTIES = Hook.of(listeners -> (id, properties) -> {
        for (PartProperties listener : listeners) properties = listener.apply(id, properties);
        return properties;
    });

    private static Reg.Handle<ItemCustomMissilePart> part(String id, UnaryOperator<ItemCustomMissilePart> builder) {
        return REG.item(id, props -> {
            ItemCustomMissilePart part = builder.apply(new ItemCustomMissilePart(props));
            MissilePartTable.Row row = MissilePartTable.get(id);
            if (row != null) {
                part.setAssembly(row.height(), row.guiHeight(), Ids.of(Constants.MOD_ID, row.mesh()),
                        Ids.of(Constants.MOD_ID, row.skin()));
            }
            return part;
        }, p -> PART_PROPERTIES.invoker().apply(id, p.stacksTo(1)));
    }

    @SuppressWarnings("unchecked")
    private static final RegistryObject<? extends Item>[] TAB = new RegistryObject[]{
                mp_thruster_10_kerosene,                mp_thruster_10_solid,                mp_thruster_10_xenon,                mp_thruster_15_kerosene,                mp_thruster_15_kerosene_dual,                mp_thruster_15_kerosene_triple,                mp_thruster_15_solid,                mp_thruster_15_solid_hexdecuple,                mp_thruster_15_hydrogen,                mp_thruster_15_hydrogen_dual,                mp_thruster_15_balefire_short,                mp_thruster_15_balefire,                mp_thruster_15_balefire_large,                mp_thruster_15_balefire_large_rad,                mp_thruster_20_kerosene,                mp_thruster_20_kerosene_dual,                mp_thruster_20_kerosene_triple,                mp_thruster_20_solid,                mp_thruster_20_solid_multi,                mp_thruster_20_solid_multier,                mp_stability_10_flat,                mp_stability_10_cruise,                mp_stability_10_space,                mp_stability_15_flat,                mp_stability_15_thin,                mp_stability_15_soyuz,                mp_stability_20_flat,                mp_fuselage_10_kerosene,                mp_fuselage_10_kerosene_camo,                mp_fuselage_10_kerosene_desert,                mp_fuselage_10_kerosene_sky,                mp_fuselage_10_kerosene_flames,                mp_fuselage_10_kerosene_insulation,                mp_fuselage_10_kerosene_sleek,                mp_fuselage_10_kerosene_metal,                mp_fuselage_10_kerosene_taint,                mp_fuselage_10_solid,                mp_fuselage_10_solid_flames,                mp_fuselage_10_solid_insulation,                mp_fuselage_10_solid_sleek,                mp_fuselage_10_solid_soviet_glory,                mp_fuselage_10_solid_cathedral,                mp_fuselage_10_solid_moonlit,                mp_fuselage_10_solid_battery,                mp_fuselage_10_solid_duracell,                mp_fuselage_10_xenon,                mp_fuselage_10_xenon_bhole,                mp_fuselage_10_long_kerosene,                mp_fuselage_10_long_kerosene_camo,                mp_fuselage_10_long_kerosene_desert,                mp_fuselage_10_long_kerosene_sky,                mp_fuselage_10_long_kerosene_flames,                mp_fuselage_10_long_kerosene_insulation,                mp_fuselage_10_long_kerosene_sleek,                mp_fuselage_10_long_kerosene_metal,                mp_fuselage_10_long_kerosene_taint,                mp_fuselage_10_long_kerosene_vap,                mp_fuselage_10_long_solid,                mp_fuselage_10_long_solid_flames,                mp_fuselage_10_long_solid_insulation,                mp_fuselage_10_long_solid_sleek,                mp_fuselage_10_long_solid_soviet_glory,                mp_fuselage_10_long_solid_bullet,                mp_fuselage_10_long_solid_silvermoonlight,                mp_fuselage_10_15_kerosene,                mp_fuselage_10_15_solid,                mp_fuselage_10_15_hydrogen,                mp_fuselage_10_15_balefire,                mp_fuselage_15_kerosene,                mp_fuselage_15_kerosene_camo,                mp_fuselage_15_kerosene_desert,                mp_fuselage_15_kerosene_sky,                mp_fuselage_15_kerosene_insulation,                mp_fuselage_15_kerosene_metal,                mp_fuselage_15_kerosene_decorated,                mp_fuselage_15_kerosene_steampunk,                mp_fuselage_15_kerosene_polite,                mp_fuselage_15_kerosene_blackjack,                mp_fuselage_15_kerosene_lambda,                mp_fuselage_15_kerosene_minuteman,                mp_fuselage_15_kerosene_taint,                mp_fuselage_15_kerosene_yuck,                mp_fuselage_15_solid,                mp_fuselage_15_solid_insulation,                mp_fuselage_15_solid_desh,                mp_fuselage_15_solid_soviet_glory,                mp_fuselage_15_solid_soviet_stank,                mp_fuselage_15_solid_faust,                mp_fuselage_15_solid_silvermoonlight,                mp_fuselage_15_solid_snowy,                mp_fuselage_15_solid_panorama,                mp_fuselage_15_solid_roses,                mp_fuselage_15_solid_mimi,                mp_fuselage_15_hydrogen,                mp_fuselage_15_hydrogen_cathedral,                mp_fuselage_15_balefire,                mp_fuselage_15_20_kerosene,                mp_fuselage_15_20_kerosene_magnusson,                mp_fuselage_15_20_solid,                mp_warhead_10_he,                mp_warhead_10_incendiary,                mp_warhead_10_buster,                mp_warhead_10_nuclear,                mp_warhead_10_nuclear_large,                mp_warhead_10_taint,                mp_warhead_10_cloud,                mp_warhead_15_he,                mp_warhead_15_incendiary,                mp_warhead_15_nuclear,                mp_warhead_15_nuclear_shark,                mp_warhead_15_nuclear_mimi,                mp_warhead_15_boxcar,                mp_warhead_15_n2,                mp_warhead_15_balefire,                mp_warhead_15_turbine,                mp_chip_1,                mp_chip_2,                mp_chip_3,                mp_chip_4,                mp_chip_5};
    /** Registration catalogue, including the two authored variants excluded from creative tabs. */
    public static List<RegistryObject<? extends Item>> allParts() {
        var parts = new ArrayList<RegistryObject<? extends Item>>(List.of(TAB));
        parts.add(parts.indexOf(mp_fuselage_10_long_kerosene_metal) + 1, mp_fuselage_10_long_kerosene_dash);
        parts.add(parts.indexOf(mp_fuselage_15_kerosene_minuteman) + 1, mp_fuselage_15_kerosene_pip);
        return List.copyOf(parts);
    }

    static {
        for (RegistryObject<? extends Item> item : TAB) ModTabs.addTo(HbmTab.MISSILES, item);
        ModTabs.addStacksTo(HbmTab.MISSILES, ModCustomMissileItems::prebuiltMissiles);
    }

    /** 1.7 {@code MissileTab.displayAllReleventItems}: nine named, ready-assembled missiles after the parts. */
    public static List<ItemStack> prebuiltMissiles() {
        List<ItemStack> list = new ArrayList<>();
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_3.get(), mp_warhead_10_he.get(), mp_fuselage_10_kerosene.get(),
                mp_stability_10_flat.get(), mp_thruster_10_kerosene.get()), "Lil Bub", ChatFormatting.DARK_PURPLE));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_3.get(), mp_warhead_10_incendiary.get(), mp_fuselage_10_long_solid.get(),
                mp_stability_10_space.get(), mp_thruster_10_solid.get()), "Long Boy", ChatFormatting.DARK_PURPLE));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_3.get(), mp_warhead_10_nuclear.get(), mp_fuselage_10_15_kerosene.get(),
                mp_stability_15_flat.get(), mp_thruster_15_kerosene.get()), "Uncle Kim", ChatFormatting.DARK_PURPLE));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_3.get(), mp_warhead_10_nuclear_large.get(), mp_fuselage_10_15_balefire.get(),
                mp_stability_15_flat.get(), mp_thruster_15_balefire_large.get()), "Trotty's Toy Rocket", ChatFormatting.GREEN));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_3.get(), mp_warhead_15_nuclear_shark.get(), mp_fuselage_15_kerosene_camo.get(),
                mp_stability_15_thin.get(), mp_thruster_15_kerosene_triple.get()), "Stealthy Shark", ChatFormatting.DARK_PURPLE));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_3.get(), mp_warhead_15_he.get(), mp_fuselage_15_kerosene_polite.get(),
                mp_stability_15_thin.get(), mp_thruster_15_kerosene_dual.get()), "Polite Lad", ChatFormatting.DARK_PURPLE));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_3.get(), mp_warhead_15_n2.get(), mp_fuselage_15_solid_desh.get(),
                mp_stability_15_thin.get(), mp_thruster_15_solid_hexdecuple.get()), "NERV's Leftover Missile", ChatFormatting.DARK_PURPLE));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_5.get(), mp_warhead_15_boxcar.get(), mp_fuselage_15_kerosene_blackjack.get(),
                mp_stability_15_thin.get(), mp_thruster_15_kerosene.get()), "Auntie Blackjack", ChatFormatting.RED));
        list.add(named(ItemCustomMissile.buildMissile(mp_chip_4.get(), mp_warhead_15_balefire.get(), mp_fuselage_15_20_kerosene_magnusson.get(),
                null, mp_thruster_20_kerosene.get()), "Hightower Missile", ChatFormatting.GREEN));
        return list;
    }

    private static ItemStack named(ItemStack stack, String name, ChatFormatting color) {
        ItemStacks.setCustomName(stack, Component.literal(name).withStyle(color));
        return stack;
    }

    public static void init() {}
}
