package com.hbm.items.special;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.sound.ModSounds;
import com.mojang.logging.LogUtils;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.7 {@code ItemStarterKit} ("i do not care how much 'optimization' you want to throw at this dumpster fire"):
 * right-clicking a kit stuffs its fixed give-list into the inventory (whatever does not fit is lost, hence the
 * "empty inventory" tooltip), optionally swaps the wearer's armour for a hazmat suit, plays the unpack sound and
 * uses up one kit. The {@code this == ModItems.x} chain is the {@link Kind}.
 *
 * <p>Entries are item ids resolved when the kit is opened, in 1.7's order; 1.7 metadata stacks are named by
 * their port ids ({@code circuit:VACUUM_TUBE} is {@code circuit_vacuum_tube}, a full canister or barrel is the
 * container filled with that fluid; a 1.7 field whose registry name differed - {@code machine_reactor_breeding},
 * {@code reactor_research}, {@code machine_rtg}, the explosive lenses - is named by that registry id, and the
 * metadata cells are the one {@code cell} item with its fluid component). An entry whose item the port does not register yet is skipped with a
 * warning rather than invented, and fills in once that item exists.
 */
public class ItemStarterKit extends CompatItem {

    private static final Logger LOGGER = LogUtils.getLogger();

    public enum Kind {
        NUKE_STARTER, NUKE_ADVANCED, NUKE_COMMERCIALLY, NUKE_ELECTRIC, GADGET, BOY, MAN, MIKE, TSAR, MULTI, CUSTOM,
        FLEIJA, SOLINIUM, PROTOTYPE, MISSILE, STEALTH_BOY, EUPHEMIUM, HAZMAT, HAZMAT_RED, HAZMAT_GREY
    }

    private final Kind kind;

    public ItemStarterKit(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    private static ItemStack hbm(String id, int count) {
        return of("hbm", id, count);
    }

    private static ItemStack mc(String id, int count) {
        return of("minecraft", id, count);
    }

    private static ItemStack of(String namespace, String id, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.of(namespace, id));
        if (item == null || item == Items.AIR) {
            UNRESOLVED.add(namespace + ":" + id);
            LOGGER.warn("[{}] starter kit entry {}:{} is not registered in this port; skipped", Constants.MOD_ID, namespace, id);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    /** Every kit entry id the registry did not resolve. */
    private static final Set<String> UNRESOLVED = ConcurrentHashMap.newKeySet();

    /**
     * The give-list and hazmat ids of every kit that resolved to nothing; expected empty, because a misspelt id
     * is silently skipped when the kit is opened. Checked by {@code BobmazonKitIdGameTests}.
     */
    public static Set<String> unresolvedIds() {
        for (Kind kind : Kind.values()) contents(kind);
        for (String suffix : new String[] {"", "_red", "_grey"}) {
            for (String piece : new String[] {"helmet", "plate", "legs", "boots"}) hbm("hazmat_" + piece + suffix, 1);
        }
        return new TreeSet<>(UNRESOLVED);
    }

    private static ItemStack fluid(ItemFluidTank container, FluidType type, int count) {
        ItemStack stack = container.make(type, container.capacity);
        stack.setCount(count);
        return stack;
    }

    /** The give-list of each kit, in 1.7's order (repeated single stacks stay repeated). */
    public static List<ItemStack> contents(Kind kind) {
        List<ItemStack> l = new ArrayList<>();
        switch (kind) {
            case NUKE_STARTER -> {
                l.add(hbm("ingot_uranium", 32));
                l.add(hbm("powder_yellowcake", 32));
                l.add(hbm("machine_press", 1));
                l.add(hbm("machine_blast_furnace", 1));
                l.add(hbm("machine_gascent", 1));
                l.add(hbm("machine_reactor", 1));
                l.add(hbm("machine_assembly_machine", 1));
                l.add(hbm("machine_chemical_plant", 1));
                l.add(hbm("machine_reactor_small", 1));
                l.add(hbm("machine_turbine", 2));
                l.add(hbm("radaway", 8));
                l.add(hbm("radx", 2));
                l.add(hbm("stamp_titanium_flat", 1));
                l.add(hbm("stamp_titanium_flat", 1));
                l.add(hbm("stamp_titanium_flat", 1));
                l.add(hbm("ingot_steel", 64));
                l.add(hbm("ingot_lead", 64));
                l.add(hbm("ingot_copper", 64));
                l.add(hbm("gas_mask_m65", 1));
                l.add(hbm("geiger_counter", 1));
            }
            case NUKE_ADVANCED -> {
                l.add(hbm("powder_yellowcake", 64));
                l.add(hbm("powder_plutonium", 64));
                l.add(hbm("ingot_steel", 64));
                l.add(hbm("ingot_copper", 64));
                l.add(hbm("ingot_tungsten", 64));
                l.add(hbm("ingot_lead", 64));
                l.add(hbm("ingot_polymer", 64));
                l.add(hbm("machine_blast_furnace", 3));
                l.add(hbm("machine_gascent", 3));
                l.add(hbm("machine_centrifuge", 2));
                l.add(hbm("machine_uf6_tank", 2));
                l.add(hbm("machine_puf6_tank", 2));
                l.add(hbm("machine_reactor", 2));
                l.add(hbm("machine_reactor_small", 4));
                l.add(hbm("machine_turbine", 4));
                l.add(hbm("machine_radgen", 1));
                l.add(hbm("machine_rtg_grey", 1));
                l.add(hbm("machine_assembly_machine", 3));
                l.add(hbm("machine_chemical_plant", 2));
                l.add(hbm("machine_fluidtank", 1));
                for (int i = 0; i < 3; i++) l.add(hbm("pellet_rtg", 1));
                for (int i = 0; i < 3; i++) l.add(hbm("pellet_rtg_weak", 1));
                l.add(new ItemStack(ModSingularityItems.cell.get(), 32)); // 1.7 cell_empty: the bare cell
                l.add(hbm("rod_empty", 32));
                l.add(fluid(ModFluidItems.fluid_barrel.get(), Fluids.COOLANT, 4));
                l.add(hbm("radaway_strong", 4));
                l.add(hbm("radx", 4));
                l.add(hbm("pill_iodine", 1));
                l.add(hbm("geiger_counter", 1));
                l.add(hbm("survey_scanner", 1));
                l.add(hbm("gas_mask_m65", 1));
            }
            case NUKE_COMMERCIALLY -> {
                l.add(hbm("machine_reactor_small", 8));
                l.add(hbm("machine_reactor", 8));
                l.add(hbm("machine_fluidtank", 8));
                l.add(hbm("billet_pu238be", 40));
                l.add(hbm("ingot_u233", 40));
                l.add(hbm("ingot_uranium_fuel", 32));
                l.add(hbm("ingot_plutonium_fuel", 16));
                l.add(hbm("ingot_mox_fuel", 8));
                for (int i = 0; i < 3; i++) l.add(hbm("inf_water_mk2", 1));
                l.add(hbm("rod_empty", 64));
                l.add(hbm("rod_dual_empty", 64));
                l.add(hbm("rod_quad_empty", 64));
                l.add(hbm("fluid_tank_lead_empty", 64));
                // 1.7 fluid_barrel_empty: the universal barrel with nothing in it
                l.add(new ItemStack(ModFluidItems.fluid_barrel.get(), 64));
                l.add(hbm("barrel_steel", 16));
                l.add(hbm("plate_iron", 64));
                // 1.7 Items.dye metadata 0: the ink sac
                l.add(mc("ink_sac", 64));
                l.add(hbm("radaway_flush", 8));
                l.add(hbm("iv_blood", 8));
                l.add(hbm("pill_iodine", 8));
                for (int i = 0; i < 3; i++) l.add(hbm("gas_mask_filter_combo", 1));
            }
            case NUKE_ELECTRIC -> {
                l.add(hbm("coil_copper", 16));
                l.add(hbm("coil_gold", 8));
                l.add(hbm("coil_tungsten", 8));
                l.add(hbm("motor", 4));
                l.add(hbm("circuit_vacuum_tube", 16));
                l.add(hbm("circuit_capacitor", 16));
                l.add(hbm("circuit_basic", 16));
                l.add(hbm("wiring_red_copper", 1));
                l.add(hbm("magnetron", 5));
                for (int i = 0; i < 3; i++) l.add(hbm("piston_selenium", 1));
                l.add(fluid(ModFluidNetItems.canister_full.get(), Fluids.DIESEL, 16));
                l.add(fluid(ModFluidNetItems.canister_full.get(), Fluids.BIOFUEL, 16));
                l.add(hbm("battery_potato", 1));
                l.add(hbm("screwdriver", 1));
                l.add(hbm("machine_excavator", 1));
                l.add(hbm("machine_diesel", 2));
                l.add(hbm("red_cable", 64));
                l.add(hbm("red_wire_coated", 16));
                l.add(hbm("red_pylon", 8));
                l.add(hbm("machine_battery_socket", 4));
                // 1.7 battery_pack:BATTERY_LEAD
                l.add(hbm("battery_lead", 4));
                l.add(hbm("machine_converter_he_rf", 1));
                l.add(hbm("machine_converter_rf_he", 1));
            }
            case GADGET -> {
                l.add(hbm("nuke_gadget", 1));
                for (int i = 0; i < 4; i++) l.add(hbm("gadget_explosive8", 1));
                l.add(hbm("gadget_wireing", 1));
                l.add(hbm("gadget_core", 1));
            }
            case BOY -> {
                l.add(hbm("nuke_boy", 1));
                l.add(hbm("boy_shielding", 1));
                l.add(hbm("boy_target", 1));
                l.add(hbm("boy_bullet", 1));
                l.add(hbm("boy_propellant", 1));
                l.add(hbm("boy_igniter", 1));
            }
            case MAN -> {
                l.add(hbm("nuke_man", 1));
                for (int i = 0; i < 4; i++) l.add(hbm("gadget_explosive8", 1));
                l.add(hbm("man_igniter", 1));
                l.add(hbm("man_core", 1));
            }
            case MIKE -> {
                l.add(hbm("nuke_mike", 1));
                for (int i = 0; i < 4; i++) l.add(hbm("man_explosive8", 1));
                l.add(hbm("man_core", 1));
                l.add(hbm("mike_core", 1));
                l.add(hbm("mike_deut", 1));
                l.add(hbm("mike_cooling_unit", 1));
            }
            case TSAR -> {
                l.add(hbm("nuke_tsar", 1));
                for (int i = 0; i < 4; i++) l.add(hbm("man_explosive8", 1));
                l.add(hbm("man_core", 1));
                l.add(hbm("tsar_core", 1));
            }
            case MULTI -> {
                l.add(hbm("bomb_multi", 6));
                l.add(mc("tnt", 26));
                l.add(mc("gunpowder", 2));
                l.add(hbm("pellet_cluster", 2));
                l.add(hbm("powder_fire", 2));
                l.add(hbm("powder_poison", 2));
                l.add(hbm("pellet_gas", 2));
            }
            case CUSTOM -> {
                l.add(hbm("nuke_custom", 1));
                for (int i = 0; i < 6; i++) l.add(hbm("custom_tnt", 1));
                for (int i = 0; i < 4; i++) l.add(hbm("custom_nuke", 1));
                for (int i = 0; i < 2; i++) l.add(hbm("custom_hydro", 1));
                for (int i = 0; i < 2; i++) l.add(hbm("custom_amat", 1));
                for (int i = 0; i < 3; i++) l.add(hbm("custom_dirty", 1));
                l.add(hbm("custom_schrab", 1));
                l.add(hbm("custom_fall", 1));
            }
            case FLEIJA -> {
                l.add(hbm("nuke_fleija", 1));
                for (int i = 0; i < 2; i++) l.add(hbm("fleija_igniter", 1));
                for (int i = 0; i < 3; i++) l.add(hbm("fleija_propellant", 1));
                for (int i = 0; i < 6; i++) l.add(hbm("fleija_core", 1));
            }
            case SOLINIUM -> {
                l.add(hbm("nuke_solinium", 1));
                for (int i = 0; i < 4; i++) l.add(hbm("solinium_igniter", 1));
                for (int i = 0; i < 4; i++) l.add(hbm("solinium_propellant", 1));
                l.add(hbm("solinium_core", 1));
            }
            case PROTOTYPE -> {
                l.add(hbm("nuke_prototype", 1));
                l.add(hbm("igniter", 1));
                l.add(ItemCell.getFullCell(Fluids.SAS3, 4)); // 1.7 cell_sas3: the cell full of SAS3
                // 1.7 rod_quad metadata URANIUM / LEAD / NP237
                l.add(hbm("rod_quad_uranium", 4));
                l.add(hbm("rod_quad_lead", 4));
                l.add(hbm("rod_quad_np237", 2));
            }
            case MISSILE -> {
                l.add(hbm("launch_pad", 1));
                l.add(hbm("designator", 1));
                l.add(hbm("designator_range", 1));
                l.add(hbm("designator_manual", 1));
                for (String missile : new String[]{"missile_generic", "missile_strong", "missile_burst",
                        "missile_incendiary", "missile_incendiary_strong", "missile_inferno", "missile_cluster",
                        "missile_cluster_strong", "missile_rain", "missile_buster", "missile_buster_strong",
                        "missile_drill", "missile_nuclear", "missile_nuclear_cluster", "missile_volcano",
                        "missile_doomsday", "missile_taint", "missile_micro", "missile_bhole", "missile_schrabidium",
                        "missile_emp"}) {
                    l.add(hbm(missile, 1));
                }
            }
            case EUPHEMIUM -> {
                l.add(hbm("euphemium_helmet", 1));
                l.add(hbm("euphemium_plate", 1));
                l.add(hbm("euphemium_legs", 1));
                l.add(hbm("euphemium_boots", 1));
                l.add(hbm("statue_elb_f", 1));
            }
            default -> { }
        }
        l.removeIf(ItemStack::isEmpty);
        return l;
    }

    /** The hazmat tier each kit swaps the wearer into, or -1 for none. */
    public static int hazTier(Kind kind) {
        return switch (kind) {
            case GADGET, BOY, MAN, MIKE, TSAR, HAZMAT -> 0;
            case NUKE_STARTER, SOLINIUM, HAZMAT_RED -> 1;
            case NUKE_ADVANCED, NUKE_COMMERCIALLY, FLEIJA, PROTOTYPE, HAZMAT_GREY -> 2;
            default -> -1;
        };
    }

    /** 1.7 {@code giveHaz}: drop the worn armour at eye height, then dress the player in the suit. */
    private static void giveHaz(Level world, Player p, int tier) {
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (EquipmentSlot slot : slots) {
            ItemStack worn = p.getItemBySlot(slot);
            if (!worn.isEmpty()) {
                world.addFreshEntity(new ItemEntity(world, p.getX(), p.getY() + p.getEyeHeight(), p.getZ(), worn));
            }
        }

        String suffix = tier == 1 ? "_red" : tier == 2 ? "_grey" : "";
        p.setItemSlot(EquipmentSlot.HEAD, hbm("hazmat_helmet" + suffix, 1));
        p.setItemSlot(EquipmentSlot.CHEST, hbm("hazmat_plate" + suffix, 1));
        p.setItemSlot(EquipmentSlot.LEGS, hbm("hazmat_legs" + suffix, 1));
        p.setItemSlot(EquipmentSlot.FEET, hbm("hazmat_boots" + suffix, 1));
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            for (ItemStack give : contents(kind)) player.getInventory().add(give);

            if (kind == Kind.STEALTH_BOY) {
                player.addEffect(Effects.instance(MobEffects.INVISIBILITY, 30 * 20, 1, true, true));
            }

            int haz = hazTier(kind);
            if (haz >= 0) giveHaz(world, player, haz);

            world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ITEM_UNPACK.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            player.inventoryMenu.broadcastChanges();
        }
        // 1.7: stack.stackSize-- on both sides, creative or not
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        switch (kind) {
            case NUKE_STARTER, NUKE_ADVANCED, NUKE_COMMERCIALLY, NUKE_ELECTRIC, GADGET, BOY, MAN, MIKE, TSAR,
                 PROTOTYPE, FLEIJA, SOLINIUM, MISSILE, MULTI -> list.add(Component.literal("Please empty inventory before opening!"));
            default -> { }
        }
        switch (kind) {
            case NUKE_STARTER, NUKE_ADVANCED, NUKE_COMMERCIALLY, GADGET, BOY, MAN, MIKE, TSAR, PROTOTYPE, FLEIJA,
                 SOLINIUM, HAZMAT -> list.add(Component.literal("Armor will be displaced by hazmat suit."));
            default -> { }
        }
    }
}
