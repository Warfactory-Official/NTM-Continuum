package com.hbm.handler;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.special.ItemKitCustom;
import com.hbm.items.special.ItemKitNBT;
import dev.golem.core.Ids;
import dev.golem.core.world.Advancements;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.7 {@code BobmazonOfferFactory}: the Bobmazon catalogue's standard offers and the hidden catalogue's special
 * ones, each a stack, an achievement requirement and a price in bottle caps. The 1.7 {@code GUIScreenBobmazon.Offer}
 * and {@code Requirement} live here so the server never loads the screen class.
 *
 * <p>Offers are item ids in 1.7's order; one whose item the port does not register yet is left out. The two lists
 * are built the same way on client and server from the same registry, so the offer index the purchase packet
 * carries means the same offer on both sides. 1.7 achievements are the port's {@code hbm:} advancements of the
 * old-hbm 1.12 names; a requirement whose advancement is not loaded cannot be met outside creative.
 */
public final class BobmazonOfferFactory {

    private BobmazonOfferFactory() {}

    private static List<Offer> standard;
    private static List<Offer> special;

    public static List<Offer> standard() {
        init();
        return standard;
    }

    public static List<Offer> special() {
        init();
        return special;
    }

    private static ItemStack hbm(String id, int count) {
        return of("hbm", id, count);
    }

    private static ItemStack of(String namespace, String id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of(namespace, id));
        if (item == null || item == Items.AIR) {
            UNRESOLVED.add(namespace + ":" + id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    /** Every id an offer (or a requirement icon) named that the registry did not resolve. */
    private static final Set<String> UNRESOLVED = ConcurrentHashMap.newKeySet();

    /**
     * The offer and icon ids that resolved to nothing. Expected: only the HIDDEN icon
     * {@code hbm:achievement_icon} (now {@code achievement_icon_questionmark}, which resolves); anything else is a
     * misspelt id silently dropping an offer. Checked by {@code BobmazonKitIdGameTests}.
     */
    public static Set<String> unresolvedIds() {
        init();
        for (Requirement requirement : Requirement.values()) requirement.icon();
        return new TreeSet<>(UNRESOLVED);
    }

    private static ItemStack named(ItemStack stack, String name) {
        if (!stack.isEmpty()) ItemStacks.setCustomName(stack, Component.literal(name));
        return stack;
    }

    private static ItemStack barrel(FluidType type, int count) {
        ItemStack stack = ModFluidItems.fluid_barrel.get().make(type, ModFluidItems.fluid_barrel.get().capacity);
        stack.setCount(count);
        return stack;
    }

    private static void add(List<Offer> list, ItemStack stack, Requirement requirement, int cost) {
        add(list, stack, requirement, cost, 0);
    }

    private static void add(List<Offer> list, ItemStack stack, Requirement requirement, int cost, int rating) {
        if (!stack.isEmpty()) list.add(new Offer(stack, requirement, cost, rating));
    }

    private static ItemStack[] present(ItemStack... stacks) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) list.add(stack);
        return list.toArray(new ItemStack[0]);
    }

    public static synchronized void init() {
        if (standard != null) return;
        List<Offer> std = new ArrayList<>();
        List<Offer> spc = new ArrayList<>();

        //gear
        add(std, of("minecraft", "torch", 64), Requirement.NONE, 2);
        add(std, hbm("definitelyfood", 16), Requirement.NONE, 4);
        add(std, hbm("nitra", 4), Requirement.CHEMICS, 16);
        add(std, hbm("gun_kit_1", 1), Requirement.ASSEMBLY, 16);
        add(std, hbm("geiger_counter", 1), Requirement.NONE, 16);
        add(std, hbm("matchstick", 16), Requirement.STEEL, 2);
        //blueprints
        // 1.7 metadata stacks, items of their own here (History §19es-itemvariant)
        add(std, hbm("blueprint_folder", 1), Requirement.ASSEMBLY, 64);
        add(std, hbm("blueprint_folder_discover", 1), Requirement.OIL, 256);
        //vending machines
        add(std, hbm("vending_machine", 1), Requirement.CHEMICS, 64);
        add(std, hbm("vending_machine_snacks", 1), Requirement.CHEMICS, 64);
        //plants (1.7 Blocks.sapling:3 is the jungle sapling)
        add(std, of("minecraft", "jungle_sapling", 1), Requirement.STEEL, 12, 9);
        add(std, hbm("plant_flower_foxglove", 1), Requirement.STEEL, 16, 5);
        add(std, hbm("plant_flower_tobacco", 1), Requirement.STEEL, 16, 9);
        add(std, hbm("plant_flower_nightshade", 1), Requirement.STEEL, 16, 3);
        add(std, hbm("plant_flower_weed", 1), Requirement.STEEL, 4, 10);
        add(std, hbm("plant_flower_cd0", 1), Requirement.NUCLEAR, 64, 8);
        //deco: EnumConcreteType is MACHINE, MACHINE_STRIPE, INDIGO, PURPLE, PINK, HAZARD, SAND, BRONZE
        for (String conc : new String[] {"machine", "machine_stripe", "indigo", "purple", "pink", "hazard", "sand", "bronze"}) {
            add(std, hbm("concrete_ext_" + conc, 16), Requirement.CHEMICS, 4);
        }
        // 1.7: every SnowglobeType (NONE included) and every PlushieType but NONE, types 1 and 2 rated 10
        for (String globe : com.hbm.blocks.DecoCrateDeclarations.trinketIds("snowglobe",
                com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType.VALUES)) {
            add(std, hbm(globe, 1), Requirement.CHEMICS, 128);
        }
        String[] plushies = com.hbm.blocks.DecoCrateDeclarations.trinketIds("plushie",
                com.hbm.blocks.generic.BlockPlushie.PlushieType.VALUES);
        for (int i = 1; i < 5; i++) add(std, hbm(plushies[i], 1), Requirement.OIL, 16, i < 3 ? 10 : 0);

        add(spc, of("minecraft", "iron_ingot", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_steel", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_copper", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_red_copper", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_titanium", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_tungsten", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_cobalt", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_desh", 64), Requirement.STEEL, 1);
        add(spc, hbm("ingot_tantalium", 64), Requirement.STEEL, 5);
        add(spc, hbm("ingot_bismuth", 16), Requirement.STEEL, 5);
        add(spc, hbm("ingot_schrabidium", 16), Requirement.STEEL, 5);
        add(spc, hbm("ingot_euphemium", 8), Requirement.STEEL, 16);
        add(spc, hbm("ingot_dineutronium", 1), Requirement.STEEL, 16);
        add(spc, hbm("ingot_starmetal", 16), Requirement.STEEL, 8);
        add(spc, hbm("ingot_semtex", 16), Requirement.STEEL, 1);
        add(spc, hbm("ingot_u235", 16), Requirement.STEEL, 1);
        add(spc, hbm("ingot_pu239", 16), Requirement.STEEL, 1);
        add(spc, hbm("ammo_container", 16), Requirement.STEEL, 5);
        add(spc, hbm("nuke_starter_kit", 1), Requirement.STEEL, 5);
        add(spc, hbm("nuke_advanced_kit", 1), Requirement.STEEL, 5);
        add(spc, hbm("nuke_commercially_kit", 1), Requirement.STEEL, 5);
        add(spc, hbm("boy_kit", 1), Requirement.STEEL, 5);
        add(spc, hbm("prototype_kit", 1), Requirement.STEEL, 10);
        add(spc, hbm("missile_kit", 1), Requirement.STEEL, 5);
        add(spc, hbm("jetpack_vector", 1), Requirement.STEEL, 2);
        add(spc, hbm("jetpack_tank", 1), Requirement.STEEL, 2);
        add(spc, hbm("gun_kit_1", 1), Requirement.STEEL, 1);
        add(spc, hbm("gun_kit_2", 1), Requirement.STEEL, 3);
        add(spc, hbm("struct_launcher_core", 1), Requirement.STEEL, 3);
        add(spc, hbm("struct_launcher_core_large", 1), Requirement.STEEL, 3);
        add(spc, hbm("struct_launcher", 40), Requirement.STEEL, 7);
        add(spc, hbm("struct_scaffold", 11), Requirement.STEEL, 7);
        add(spc, hbm("loot_10", 1), Requirement.STEEL, 2);
        add(spc, hbm("loot_15", 1), Requirement.STEEL, 2);
        add(spc, hbm("loot_misc", 1), Requirement.STEEL, 2);
        add(spc, hbm("crate_can", 1), Requirement.STEEL, 1);
        add(spc, hbm("crate_ammo", 1), Requirement.STEEL, 2);
        // old-hbm BobmazonOfferFactory: new ItemStack(crucible, 1, 3), item damage 3 and no charges (an empty crucible)
        ItemStack crucible = hbm("crucible", 1);
        if (!crucible.isEmpty()) crucible.setDamageValue(3);
        add(spc, crucible, Requirement.STEEL, 10);
        add(spc, hbm("chopper", 1), Requirement.STEEL, 10); // 1.7 ModItems.spawn_chopper registers as hbm:chopper
        add(spc, hbm("spawn_worm", 1), Requirement.STEEL, 10);
        add(spc, hbm("spawn_ufo", 1), Requirement.STEEL, 10);
        add(spc, hbm("sat_laser", 1), Requirement.HIDDEN, 8);
        add(spc, hbm("sat_gerald", 1), Requirement.HIDDEN, 32);
        add(spc, hbm("billet_yharonite", 4), Requirement.HIDDEN, 16);
        add(spc, hbm("ingot_chainsteel", 1), Requirement.HIDDEN, 16);
        add(spc, hbm("ingot_electronium", 1), Requirement.HIDDEN, 16);
        add(spc, hbm("book_of_", 1), Requirement.HIDDEN, 16);
        add(spc, hbm("mese_pickaxe", 1), Requirement.HIDDEN, 16);
        add(spc, hbm("mysteryshovel", 1), Requirement.HIDDEN, 16);
        add(spc, hbm("ntm_dirt", 1), Requirement.HIDDEN, 16);
        add(spc, hbm("euphemium_kit", 1), Requirement.HIDDEN, 64);

        add(spc, ItemKitCustom.create("Fusion Man", "For the nuclear physicist on the go", 0xff00ff, 0x800080, present(
                hbm("fusion_klystron", 1),
                hbm("fusion_torus", 1),
                hbm("fusion_mhdt", 1),
                hbm("machine_intake", 3),
                hbm("battery_spark", 1),
                hbm("machine_chemical_factory", 4),
                hbm("machine_fluidtank", 8),
                hbm("red_wire_coated", 64),
                hbm("red_cable", 64),
                barrel(Fluids.DEUTERIUM, 64),
                barrel(Fluids.TRITIUM, 64),
                barrel(Fluids.PERFLUOROMETHYL, 64),
                hbm("red_pylon_large", 8),
                hbm("substation", 4),
                hbm("red_connector", 64),
                hbm("wiring_red_copper", 1),
                hbm("machine_chungus", 3),
                hbm("template_folder", 1),
                of("minecraft", "paper", 64),
                of("minecraft", "ink_sac", 64))), Requirement.HIDDEN, 64);

        add(spc, ItemKitCustom.create("Maid's Cleaning Utensils", "For the hard to reach spots", 0x00ff00, 0x008000, present(
                hbm("gun_m2", 1),
                hbm("ammo_standard_bmg50_du", 64),
                hbm("ammo_standard_bmg50_du", 64),
                hbm("ammo_standard_bmg50_du", 64),
                hbm("ammo_standard_bmg50_du", 64),
                hbm("ammo_standard_bmg50_du", 64),
                hbm("gun_autoshotgun", 1),
                hbm("ammo_standard_g12_magnum", 64),
                hbm("ammo_standard_g12_magnum", 64),
                hbm("ammo_standard_g12_magnum", 64),
                hbm("ammo_standard_g12_explosive", 64),
                hbm("ammo_standard_g12_explosive", 64))), Requirement.HIDDEN, 64);

        add(spc, named(ItemKitNBT.create(present(
                named(hbm("rod_of_discord", 1), "Cock Joke"),
                named(hbm("canned_conserve_slime", 64), "Class A Horse Semen"),
                named(hbm("weapon_pipe_lead", 1), "Get Nutted, Dumbass"),
                hbm("gem_alexandrite", 1))), "The Nut Bucket"), Requirement.HIDDEN, 64);

        add(spc, named(ItemKitNBT.create(present(
                hbm("rpa_helmet", 1),
                hbm("rpa_plate", 1),
                hbm("rpa_legs", 1),
                hbm("rpa_boots", 1),
                hbm("gun_minigun_lacunae", 1),
                hbm("ammo_standard_capacitor_overcharge", 64),
                hbm("ammo_standard_capacitor_overcharge", 64),
                hbm("ammo_standard_capacitor_overcharge", 64))), "Frenchie's Reward"), Requirement.HIDDEN, 32);

        standard = std;
        special = spc;
    }

    public static List<Offer> getOffers(ItemStack stack) {
        if (!stack.isEmpty()) {
            if (stack.getItem() == ModMeleeKitItems.bobmazon.get()) return standard();
            if (stack.getItem() == ModMeleeKitItems.bobmazon_hidden.get()) return special();
        }
        return null;
    }

    /** 1.7 {@code GUIScreenBobmazon.Offer}. */
    public static final class Offer {

        public final ItemStack offer;
        public final Requirement requirement;
        public final int cost;
        public final int rating;
        public final String comment;
        public final String author;

        public Offer(ItemStack offer, Requirement requirement, int cost, int rating, String comment, String author) {
            this.offer = offer;
            this.requirement = requirement;
            this.cost = cost;
            this.rating = rating * 4 - 1;
            this.comment = comment;
            this.author = author;
        }

        public Offer(ItemStack offer, Requirement requirement, int cost, int rating) {
            this(offer, requirement, cost, rating, "No Ratings", "");
        }
    }

    /**
     * 1.7 {@code GUIScreenBobmazon.Requirement}: the achievement each offer needs and its icon. {@code NONE} was
     * "open your inventory", which every player has; it needs nothing here.
     */
    public enum Requirement {

        NONE(null, "minecraft:book"),
        STEEL("hbm:blast_furnace", "hbm:machine_blast_furnace"),
        ASSEMBLY("hbm:assembly", "hbm:machine_assembly_machine"),
        CHEMICS("hbm:chemplant", "hbm:machine_chemical_plant"),
        OIL("hbm:desh", "hbm:ingot_desh"),
        NUCLEAR("hbm:technetium", "hbm:ingot_tcalloy"),
        HIDDEN("hbm:hidden", "hbm:achievement_icon_questionmark");

        public final String advancement;
        private final String icon;

        Requirement(String advancement, String icon) {
            this.advancement = advancement;
            this.icon = icon;
        }

        public boolean fullfills(ServerPlayer player) {
            if (advancement == null) return true;
            return Boolean.TRUE.equals(Advancements.isDone(player, advancement));
        }

        public ItemStack icon() {
            int colon = icon.indexOf(':');
            return of(icon.substring(0, colon), icon.substring(colon + 1), 1);
        }
    }
}
