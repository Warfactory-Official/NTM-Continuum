package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.handler.HazmatRegistry;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.armor.ArmorAshGlasses;
import com.hbm.items.armor.ArmorDesh;
import com.hbm.items.armor.ArmorDiesel;
import com.hbm.items.armor.ArmorEnvsuit;
import com.hbm.items.armor.ArmorHat;
import com.hbm.items.armor.ArmorModel;
import com.hbm.items.armor.ArmorNo9;
import com.hbm.items.armor.IAttackHandler;
import com.hbm.items.armor.IDamageHandler;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.IArmorDisableModel.EnumPlayerPart;
import com.hbm.items.special.ItemDrop;
import com.hbm.items.special.ItemSimpleConsumable;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.DamageResistanceHandler.ResistanceStats;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Effects;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.function.Function;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import static com.hbm.util.DamageResistanceHandler.CATEGORY_EXPLOSION;
import static com.hbm.util.DamageResistanceHandler.CATEGORY_FIRE;
import static com.hbm.util.DamageResistanceHandler.CATEGORY_PHYSICAL;

/**
 * The remaining 1.7 {@code ModItemsArmor} ids: the steam, diesel and environment suits, the
 * two ballistic jackets, the rag masks, the goggles, ash goggles, Fabulous Hat, mining helmet and "Beta Features",
 * plus the three IV bags 1.7 declared in {@code ItemSimpleConsumable.init}.
 *
 * <p>Every 1.7 {@code EnumHelper} material was its own constant and so its own set; each is its own
 * {@link GolemArmorMaterial} here, named after its worn equipment asset. The modelled pieces' assets are transparent
 * (the body is drawn by the armour render layers); the jackets and masks keep 1.7's flat armour textures.
 */
public final class ModArmorSetBItems {

    private ModArmorSetBItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    // Materials (1.7 arrays are {helmet, chest, legs, boots}; 1.7 had no toughness)
    /**
     * Held apart so only the item factories reach them: the materials are registry-backed, and this class also
     * runs at build time (Dejson) without a game.
     */
    static final class Materials {
        private Materials() {}

        /** 1.7 {@code aMatRags} ("HBM_RAGS", 150, {1, 1, 1, 1}, 0), repaired with rags; one asset per mask texture. */
        static final GolemArmorMaterial RAG_DAMP = new GolemArmorMaterial(Constants.MOD_ID, "rag_damp", 150,
                1, 1, 1, 1, 0, 0F, 0F, "repairs/rags");
        static final GolemArmorMaterial RAG_PISS = new GolemArmorMaterial(Constants.MOD_ID, "rag_piss", 150,
                1, 1, 1, 1, 0, 0F, 0F, "repairs/rags");
        /** Vanilla 1.7 {@code ArmorMaterial.IRON} (15, {2, 6, 5, 2}, 9). */
        static final GolemArmorMaterial GOGGLES = new GolemArmorMaterial(Constants.MOD_ID, "goggles", 15,
                2, 6, 5, 2, 9, 0F, 0F, "repairs/infamy");
        static final GolemArmorMaterial ASHGLASSES = new GolemArmorMaterial(Constants.MOD_ID, "ashglasses", 15,
                2, 6, 5, 2, 9, 0F, 0F, "repairs/infamy");
        /** 1.7 {@code aMatAlloy} (40, {3, 8, 6, 3}, 12), which never had a repair material. */
        static final GolemArmorMaterial HAT = new GolemArmorMaterial(Constants.MOD_ID, "nossy_hat", 40,
                3, 8, 6, 3, 12, 0F, 0F, "repairs/alloy");
        /** 1.7 {@code aMatSteel} (30, {3, 8, 6, 3}, 5), repaired with steel. */
        static final GolemArmorMaterial NO9 = new GolemArmorMaterial(Constants.MOD_ID, "no9", 30,
                3, 8, 6, 3, 5, 0F, 0F, "repairs/steel");
        static final GolemArmorMaterial JACKT = new GolemArmorMaterial(Constants.MOD_ID, "jackt", 30,
                3, 8, 6, 3, 5, 0F, 0F, "repairs/steel");
        static final GolemArmorMaterial JACKT2 = new GolemArmorMaterial(Constants.MOD_ID, "jackt2", 30,
                3, 8, 6, 3, 5, 0F, 0F, "repairs/steel");
        /** 1.7 {@code aMatDesh} ("HBM_DESH", 150, {3, 8, 6, 3}, 0), repaired with desh. */
        static final GolemArmorMaterial DESH = new GolemArmorMaterial(Constants.MOD_ID, "steamsuit", 150,
                3, 8, 6, 3, 0, 0F, 0F, "repairs/desh_suit");
        /** 1.7 {@code aMatDiesel} ("HBM_BNUUY", 150, {3, 8, 6, 3}, 0), repaired with copper plates. */
        static final GolemArmorMaterial DIESEL = new GolemArmorMaterial(Constants.MOD_ID, "dieselsuit", 150,
                3, 8, 6, 3, 0, 0F, 0F, "repairs/dieselsuit");
        /** 1.7 {@code aMatEnv} ("HBM_ENV", 150, {3, 8, 6, 3}, 10), repaired with HEV plating. */
        static final GolemArmorMaterial ENV = new GolemArmorMaterial(Constants.MOD_ID, "envsuit", 150,
                3, 8, 6, 3, 10, 0F, 0F, "repairs/hev");
    }

    // Source: ModItemsArmor.java:25-35
    public static final RegistryObject<ArmorModel> goggles = control("goggles",
            p -> new ArmorModel(Materials.GOGGLES, ArmorSlot.HELMET, p, "goggles"));
    public static final RegistryObject<ArmorAshGlasses> ashglasses = control("ashglasses",
            p -> new ArmorAshGlasses(Materials.ASHGLASSES, ArmorSlot.HELMET, p));
    public static final RegistryObject<CompatArmorItem> mask_rag = control("mask_rag",
            p -> new CompatArmorItem(Materials.RAG_DAMP, ArmorSlot.HELMET, p));
    public static final RegistryObject<CompatArmorItem> mask_piss = control("mask_piss",
            p -> new CompatArmorItem(Materials.RAG_PISS, ArmorSlot.HELMET, p));
    /** 1.7 field {@code hat}, registered as {@code nossy_hat}. */
    public static final RegistryObject<ArmorHat> nossy_hat = control("nossy_hat", "hat",
            p -> new ArmorHat(Materials.HAT, ArmorSlot.HELMET, p));
    /** setMaxDamage(0). */
    public static final RegistryObject<ArmorNo9> no9 = control("no9",
            p -> new ArmorNo9(Materials.NO9, ArmorSlot.HELMET, CompatArmorItem.unbreakable(p)));
    /** An {@code ItemDrop} with no creative tab. */
    public static final RegistryObject<ItemDrop> beta = REG.item("beta", ItemDrop::new, p -> p.stacksTo(1))
            .transform(Presets.sprite());

    // ModItemsArmor.java:139-159: ArmorFSBFueled suits. 1.7 setDamage never stored durability.
    public static final RegistryObject<ArmorDesh> steamsuit_helmet = desh("steamsuit_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorDesh> steamsuit_plate = desh("steamsuit_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorDesh> steamsuit_legs = desh("steamsuit_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorDesh> steamsuit_boots = desh("steamsuit_boots", ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorDiesel> dieselsuit_helmet = diesel("dieselsuit_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorDiesel> dieselsuit_plate = diesel("dieselsuit_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorDiesel> dieselsuit_legs = diesel("dieselsuit_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorDiesel> dieselsuit_boots = diesel("dieselsuit_boots", ArmorSlot.BOOTS);

    // ModItemsArmor.java:244-252: ArmorEnvsuit, 100 kHE, 1 kHE/t charge, 250 HE per point, no idle drain
    public static final RegistryObject<ArmorEnvsuit> envsuit_helmet = env("envsuit_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorEnvsuit> envsuit_plate = env("envsuit_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorEnvsuit> envsuit_legs = env("envsuit_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorEnvsuit> envsuit_boots = env("envsuit_boots", ArmorSlot.BOOTS);

    // ModItemsArmor.java:268-269: plain ModArmor chestplates, no max stack size set (armour stacks to 1)
    public static final RegistryObject<CompatArmorItem> jackt = control("jackt",
            p -> new CompatArmorItem(Materials.JACKT, ArmorSlot.CHESTPLATE, p));
    public static final RegistryObject<CompatArmorItem> jackt2 = control("jackt2",
            p -> new CompatArmorItem(Materials.JACKT2, ArmorSlot.CHESTPLATE, p));

    // ItemSimpleConsumable.init "BLOOD BAGS" (iv_empty is ModMedicineItems')
    public static final RegistryObject<ItemSimpleConsumable> iv_blood = bag("iv_blood");
    public static final RegistryObject<ItemSimpleConsumable> iv_xp_empty = bag("iv_xp_empty");
    public static final RegistryObject<ItemSimpleConsumable> iv_xp = bag("iv_xp");

    private static <T extends Item> RegistryObject<T> control(String name, Function<Item.Properties, T> factory) {
        return control(name, name, factory);
    }

    /** A wearable on the control tab, drawn with the 1.7 sprite {@code texture}. */
    private static <T extends Item> RegistryObject<T> control(String name, String texture,
                                                              Function<Item.Properties, T> factory) {
        RegistryObject<T> ro = REG.item(name, factory, p -> p.stacksTo(1)).transform(Presets.sprite("hbm:items/" + texture));
        // 1.7 ItemArmor's combat tab does not exist in the port; ModArmorItems.plain's convention.
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    private static RegistryObject<ArmorDesh> desh(String name, ArmorSlot slot) {
        RegistryObject<ArmorDesh> ro = REG.item(name, p -> new ArmorDesh(Materials.DESH, slot,
                CompatArmorItem.unbreakable(p), () -> Fluids.STEAM, 64_000, 500, 50, 1), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ArmorDiesel> diesel(String name, ArmorSlot slot) {
        RegistryObject<ArmorDiesel> ro = REG.item(name, p -> new ArmorDiesel(Materials.DIESEL, slot,
                CompatArmorItem.unbreakable(p), () -> Fluids.DIESEL, 64_000, 500, 50, 1), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ArmorEnvsuit> env(String name, ArmorSlot slot) {
        RegistryObject<ArmorEnvsuit> ro = REG.item(name, p -> new ArmorEnvsuit(Materials.ENV, slot, p,
                100_000L, 1_000L, 250L, 0L), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ItemSimpleConsumable> bag(String name) {
        RegistryObject<ItemSimpleConsumable> ro = REG.item(name, p -> new ItemSimpleConsumable(p, name), p -> p)
                .transform(Presets.sprite());
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }


    /** Set bonuses, hazard classes and radiation resistance; run once the registries are flushed. */
    public static void configure() {
        ArmorFSB steam = steamsuit_helmet.get()
                .setHasHardLanding(true)
                .addEffect(Effects.instance(Effects.haste(), 20, 4))
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel("steamsuit");
        steamsuit_plate.get().cloneStats(steam);
        steamsuit_legs.get().cloneStats(steam);
        steamsuit_boots.get().cloneStats(steam);

        ArmorFSB diesel = dieselsuit_helmet.get()
                .addEffect(Effects.instance(Effects.speed(), 20, 2))
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 2))
                .enableThermalSight(true)
                .enableVATS(true)
                .setArmorModel("dieselsuit");
        dieselsuit_plate.get().cloneStats(diesel);
        dieselsuit_legs.get().cloneStats(diesel);
        dieselsuit_boots.get().cloneStats(diesel);

        ArmorFSB env = envsuit_helmet.get()
                .addEffect(Effects.instance(Effects.speed(), 20, 1))
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 0))
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel("envsuit");
        envsuit_plate.get().cloneStats(env);
        envsuit_legs.get().cloneStats(env);
        envsuit_boots.get().cloneStats(env);

        // 1.7 ArmorUtil.register
        // 1.7 HazmatRegistry
        // 1.7 TileEntityLockableBase: either jacket multiplies the lock-pick chance by 100 (hbm:lockpick_jacket data map)
    }

    /** 1.7 {@code DamageResistanceHandler.initDefaults}, the entries for these ids. */
    public static void registerResistances() {

        // 1.7 ModEventHandler.onEntityAttacked / onEntityDamaged: every worn IAttackHandler / IDamageHandler.
        DamageEvents.onAttack(event -> {
            if (!(event.entity() instanceof Player player)) return;
            for (ItemStack stack : worn(player)) {
                if (stack.getItem() instanceof IAttackHandler handler) handler.handleAttack(event, stack);
            }
        });
        DamageEvents.onHurt(event -> {
            if (!(event.entity() instanceof Player player) || event.isCanceled()) return;
            for (ItemStack stack : worn(player)) {
                if (stack.getItem() instanceof IDamageHandler handler) handler.handleDamage(event, stack);
            }
        });
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
            EquipmentSlot.HEAD};

    /** 1.7 {@code armorInventory} order: boots first. */
    private static ItemStack[] worn(Player player) {
        ItemStack[] stacks = new ItemStack[4];
        for (int i = 0; i < 4; i++) stacks[i] = player.getItemBySlot(ARMOR[i]);
        return stacks;
    }

    /**
     * Per-player tick from {@code EntityEffectHandler.handleArmor}, both sides: the steam and diesel suits' item
     * attribute modifiers, the mining helmet's {@code onArmorTick} (a plain helmet, so the FSB tick never reaches it)
     * and 1.7 {@code ModEventHandler}'s "BETA HEALTH" (server, player tick start): carrying Beta Features pins the
     * food level at 10, turning every point above it into health.
     */
    public static void tickPlayer(Player player) {
        ArmorDesh.updateModifiers(player);
        ArmorDiesel.updateModifiers(player);

        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.getItem() instanceof ArmorNo9 no9) no9.armorTick(player.level(), player, head);

        if (!player.level().isClientSide() && player.getInventory().contains(new ItemStack(beta.get()))) {
            var food = player.getFoodData();
            if (food.getFoodLevel() > 10) player.heal(food.getFoodLevel() - 10);
            if (food.getFoodLevel() != 10) food.setFoodLevel(10);
        }
    }

    public static void init() {}
}
