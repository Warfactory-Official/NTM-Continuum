package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.handler.ability.IWeaponAbility;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemHot;
import com.hbm.items.tool.HoeSchrabidium;
import com.hbm.items.tool.ItemChainsaw;
import com.hbm.items.tool.ItemSwordAbility;
import com.hbm.items.tool.ItemSwordAbilityPower;
import com.hbm.items.tool.ItemSwordMeteorite;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.items.tool.ItemToolAbility.EnumToolType;
import com.hbm.items.tool.ItemToolAbilityFueled;
import com.hbm.items.tool.ItemToolAbilityPower;
import com.hbm.items.tool.ModHoe;
import com.hbm.items.tool.RedstoneSword;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.packet.ToolAbilityControlPayload;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ToolTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The ability tool and weapon family: every 1.7 {@code ItemToolAbility} / {@code ItemSwordAbility} tool set,
 * the powered and fueled variants, the hoes, the special tools, the meteorite sword chain and its
 * materials. The {@code tMat*} tool materials of 1.7 {@code MainRegistry} and {@code ModItems} are the
 * {@link ToolTier} constants below, with their original numbers.
 */
public final class ModAbilityToolItems {

    private ModAbilityToolItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);

    public static final RegistryObject<SoundEvent> CHAINSAW_SOUND = sound("weapon.chainsaw");
    public static final RegistryObject<SoundEvent> CRUCIBLE_DEPLOY = sound("weapon.cdeploy");
    public static final RegistryObject<SoundEvent> CRUCIBLE_SWING = sound("weapon.cswing");

    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Ids.of(Constants.MOD_ID, id)));
    }

    private static TagKey<Item> repairs(String name) {
        return TagKey.create(Registries.ITEM, Ids.of(Constants.MOD_ID, "repairs/" + name));
    }

    // --- 1.7 tool materials: EnumHelper.addToolMaterial(name, harvestLevel, maxUses, efficiency, damage, enchantability) ---
    public static final ToolTier tMatSchrab = new ToolTier(3, 10000, 50.0F, 100.0F, 200, repairs("schrabidium"));
    public static final ToolTier tMatChainsaw = new ToolTier(3, 1500, 50.0F, 22.0F, 0, repairs("steel"));
    public static final ToolTier tMatSteel = new ToolTier(3, 750, 8.0F, 2.0F, 10, repairs("steel"));
    public static final ToolTier tMatTitan = new ToolTier(3, 1000, 9.0F, 2.5F, 15, repairs("titanium"));
    /** Deprecated in 1.7, and never given a repair item. */
    public static final ToolTier tMatAlloy = new ToolTier(3, 2000, 15.0F, 5.0F, 5, null);
    public static final ToolTier tMatCMB = new ToolTier(3, 8500, 40.0F, 55F, 100, repairs("cmb"));
    public static final ToolTier tMatElec = new ToolTier(3, 0, 30.0F, 12.0F, 2, null);
    public static final ToolTier tMatDesh = new ToolTier(2, 0, 7.5F, 2.0F, 10, repairs("desh"));
    /** 1.7 never called setRepairItem on tMatCobalt, so the plain cobalt tools cannot be anvil-repaired. */
    public static final ToolTier tMatCobalt = new ToolTier(3, 750, 9.0F, 2.5F, 60, null);
    public static final ToolTier matDecCobalt = new ToolTier(3, 2500, 15.0F, 2.5F, 75, repairs("cobalt_ingot"));
    public static final ToolTier matStarmetal = new ToolTier(3, 3000, 20.0F, 2.5F, 100, repairs("starmetal_ingot"));
    public static final ToolTier matBismuth = new ToolTier(4, 0, 50F, 0.0F, 200, repairs("bismuth"));
    public static final ToolTier matVolcano = new ToolTier(4, 0, 50F, 0.0F, 200, repairs("bismuth"));
    public static final ToolTier matChlorophyte = new ToolTier(4, 0, 75F, 0.0F, 200, repairs("chlorophyte"));
    public static final ToolTier matMese = new ToolTier(4, 0, 100F, 0.0F, 200, repairs("paa"));
    /** HBM_DWARVEN, with the pickaxe's own setMaxDamage(250) folded in (it is the material's only user). */
    public static final ToolTier matDwarf = new ToolTier(2, 250, 4F, 0.0F, 10, repairs("copper"));
    public static final ToolTier matMeteorite = new ToolTier(4, 0, 50F, 0.0F, 200, repairs("paa"));
    public static final ToolTier matMeseGavel = new ToolTier(4, 0, 50F, 0.0F, 200, repairs("paa"));
    /** centri_stick: tMatElec with setMaxDamage(50). */
    public static final ToolTier tMatElecStick = new ToolTier(3, 50, 30.0F, 12.0F, 2, null);
    /** smashing_hammer: tMatSteel with setMaxDamage(2500). */
    public static final ToolTier tMatSteelHammer = new ToolTier(3, 2500, 8.0F, 2.0F, 10, repairs("steel"));
    /**
     * CRUCIBLE (level 10, 3 uses, 50, 100, 0). Its three uses are its charge, which the item counts itself
     * (see {@link ItemCrucible}); vanilla wear would break it on the third hit, where 1.7 kept it empty.
     */
    public static final ToolTier matCrucible = new ToolTier(10, 0, 50.0F, 100.0F, 0, null);

    /** Every id this family registers, in registration order, for the harness and the declarations. */
    public static final List<String> IDS = new ArrayList<>();

    private static <I extends Item> RegistryObject<I> reg(String id, HbmTab tab, Function<Item.Properties, I> factory) {
        return reg(id, tab, factory, Rarity.COMMON);
    }

    private static <I extends Item> RegistryObject<I> reg(String id, HbmTab tab, Function<Item.Properties, I> factory,
                                                         Rarity rarity) {
        RegistryObject<I> ro = ITEMS.registerItem(id, factory, p -> rarity == Rarity.COMMON ? p : p.rarity(rarity));
        IDS.add(id);
        if (tab != null) ModTabs.addTo(tab, ro);
        return ro;
    }

    private static final HbmTab T = HbmTab.CONSUMABLES;

    // --- schrabidium ---
    public static final RegistryObject<ItemSwordAbility> schrabidium_sword = reg("schrabidium_sword", T,
            p -> new ItemSwordAbility(p, 75, 0, tMatSchrab)
                    .addAbility(IWeaponAbility.RADIATION, 1)
                    .addAbility(IWeaponAbility.VAMPIRE, 0), Rarity.RARE);
    public static final RegistryObject<ItemToolAbility> schrabidium_pickaxe = reg("schrabidium_pickaxe", T,
            p -> new ItemToolAbility(p, 20, 0, tMatSchrab, EnumToolType.PICKAXE)
                    .addAbility(IWeaponAbility.RADIATION, 0)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0), Rarity.RARE);
    public static final RegistryObject<ItemToolAbility> schrabidium_axe = reg("schrabidium_axe", T,
            p -> new ItemToolAbility(p, 25, 0, tMatSchrab, EnumToolType.AXE)
                    .addAbility(IWeaponAbility.RADIATION, 0)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0), Rarity.RARE);
    public static final RegistryObject<ItemToolAbility> schrabidium_shovel = reg("schrabidium_shovel", T,
            p -> new ItemToolAbility(p, 15, 0, tMatSchrab, EnumToolType.SHOVEL)
                    .addAbility(IWeaponAbility.RADIATION, 0)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0), Rarity.RARE);
    public static final RegistryObject<HoeSchrabidium> schrabidium_hoe = reg("schrabidium_hoe", T,
            p -> new HoeSchrabidium(p, tMatSchrab), Rarity.RARE);

    // --- titanium ---
    public static final RegistryObject<ItemSwordAbility> titanium_sword = reg("titanium_sword", T,
            p -> new ItemSwordAbility(p, 6.5F, 0, tMatTitan));
    public static final RegistryObject<ItemToolAbility> titanium_pickaxe = reg("titanium_pickaxe", T,
            p -> new ItemToolAbility(p, 4.5F, 0, tMatTitan, EnumToolType.PICKAXE));
    public static final RegistryObject<ItemToolAbility> titanium_axe = reg("titanium_axe", T,
            p -> new ItemToolAbility(p, 5.5F, 0, tMatTitan, EnumToolType.AXE)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistryObject<ItemToolAbility> titanium_shovel = reg("titanium_shovel", T,
            p -> new ItemToolAbility(p, 3.5F, 0, tMatTitan, EnumToolType.SHOVEL));
    public static final RegistryObject<ModHoe> titanium_hoe = reg("titanium_hoe", T, p -> new ModHoe(p, tMatTitan));

    // --- steel ---
    public static final RegistryObject<ItemSwordAbility> steel_sword = reg("steel_sword", T,
            p -> new ItemSwordAbility(p, 6F, 0, tMatSteel)
                    .addAbility(IWeaponAbility.STUN, 0));
    public static final RegistryObject<ItemToolAbility> steel_pickaxe = reg("steel_pickaxe", T,
            p -> new ItemToolAbility(p, 4F, 0, tMatSteel, EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0));
    public static final RegistryObject<ItemToolAbility> steel_axe = reg("steel_axe", T,
            p -> new ItemToolAbility(p, 5F, 0, tMatSteel, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistryObject<ItemToolAbility> steel_shovel = reg("steel_shovel", T,
            p -> new ItemToolAbility(p, 3F, 0, tMatSteel, EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 0));
    public static final RegistryObject<ModHoe> steel_hoe = reg("steel_hoe", T, p -> new ModHoe(p, tMatSteel));

    // --- advanced alloy (LEGACY) ---
    public static final RegistryObject<ItemSwordAbility> alloy_sword = reg("alloy_sword", T,
            p -> new ItemSwordAbility(p, 8F, 0, tMatAlloy)
                    .addAbility(IWeaponAbility.STUN, 0));
    public static final RegistryObject<ItemToolAbility> alloy_pickaxe = reg("alloy_pickaxe", T,
            p -> new ItemToolAbility(p, 5F, 0, tMatAlloy, EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0));
    public static final RegistryObject<ItemToolAbility> alloy_axe = reg("alloy_axe", T,
            p -> new ItemToolAbility(p, 7F, 0, tMatAlloy, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistryObject<ItemToolAbility> alloy_shovel = reg("alloy_shovel", T,
            p -> new ItemToolAbility(p, 4F, 0, tMatAlloy, EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 0));
    public static final RegistryObject<ModHoe> alloy_hoe = reg("alloy_hoe", T, p -> new ModHoe(p, tMatAlloy));

    // --- CMB steel ---
    public static final RegistryObject<ItemSwordAbility> cmb_sword = reg("cmb_sword", T,
            p -> new ItemSwordAbility(p, 35F, 0, tMatCMB)
                    .addAbility(IWeaponAbility.STUN, 0)
                    .addAbility(IWeaponAbility.VAMPIRE, 0));
    public static final RegistryObject<ItemToolAbility> cmb_pickaxe = reg("cmb_pickaxe", T,
            p -> new ItemToolAbility(p, 10F, 0, tMatCMB, EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistryObject<ItemToolAbility> cmb_axe = reg("cmb_axe", T,
            p -> new ItemToolAbility(p, 30F, 0, tMatCMB, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistryObject<ItemToolAbility> cmb_shovel = reg("cmb_shovel", T,
            p -> new ItemToolAbility(p, 8F, 0, tMatCMB, EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistryObject<ModHoe> cmb_hoe = reg("cmb_hoe", T, p -> new ModHoe(p, tMatCMB));

    // --- electric (HE) ---
    public static final RegistryObject<ItemSwordAbilityPower> elec_sword = reg("elec_sword", T,
            p -> (ItemSwordAbilityPower) new ItemSwordAbilityPower(p, 12.5F, 0, tMatElec, 500000, 1000, 100)
                    .addAbility(IWeaponAbility.STUN, 2));
    public static final RegistryObject<ItemToolAbilityPower> elec_pickaxe = reg("elec_pickaxe", T,
            p -> (ItemToolAbilityPower) new ItemToolAbilityPower(p, 6F, 0, tMatElec, EnumToolType.PICKAXE, 500000, 1000, 100)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final RegistryObject<ItemToolAbilityPower> elec_axe = reg("elec_axe", T,
            p -> (ItemToolAbilityPower) new ItemToolAbilityPower(p, 10F, 0, tMatElec, EnumToolType.AXE, 500000, 1000, 100)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IWeaponAbility.CHAINSAW, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setShears());
    public static final RegistryObject<ItemToolAbilityPower> elec_shovel = reg("elec_shovel", T,
            p -> (ItemToolAbilityPower) new ItemToolAbilityPower(p, 5F, 0, tMatElec, EnumToolType.SHOVEL, 500000, 1000, 100)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));

    // --- desh ---
    public static final RegistryObject<ItemSwordAbility> desh_sword = reg("desh_sword", T,
            p -> new ItemSwordAbility(p, 12.5F, 0, tMatDesh)
                    .addAbility(IWeaponAbility.STUN, 0));
    public static final RegistryObject<ItemToolAbility> desh_pickaxe = reg("desh_pickaxe", T,
            p -> new ItemToolAbility(p, 5F, -0.05, tMatDesh, EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final RegistryObject<ItemToolAbility> desh_axe = reg("desh_axe", T,
            p -> new ItemToolAbility(p, 7.5F, -0.05, tMatDesh, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistryObject<ItemToolAbility> desh_shovel = reg("desh_shovel", T,
            p -> new ItemToolAbility(p, 4F, -0.05, tMatDesh, EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final RegistryObject<ModHoe> desh_hoe = reg("desh_hoe", T, p -> new ModHoe(p, tMatDesh));

    // --- cobalt ---
    public static final RegistryObject<ItemSwordAbility> cobalt_sword = reg("cobalt_sword", T,
            p -> new ItemSwordAbility(p, 12F, 0, tMatCobalt));
    public static final RegistryObject<ItemToolAbility> cobalt_pickaxe = reg("cobalt_pickaxe", T,
            p -> new ItemToolAbility(p, 4F, 0, tMatCobalt, EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 0));
    public static final RegistryObject<ItemToolAbility> cobalt_axe = reg("cobalt_axe", T,
            p -> new ItemToolAbility(p, 6F, 0, tMatCobalt, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistryObject<ItemToolAbility> cobalt_shovel = reg("cobalt_shovel", T,
            p -> new ItemToolAbility(p, 3.5F, 0, tMatCobalt, EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 0));
    public static final RegistryObject<ModHoe> cobalt_hoe = reg("cobalt_hoe", T, p -> new ModHoe(p, tMatCobalt));

    // --- decorated cobalt ---
    public static final RegistryObject<ItemSwordAbility> cobalt_decorated_sword = reg("cobalt_decorated_sword", T,
            p -> new ItemSwordAbility(p, 15F, 0, matDecCobalt)
                    .addAbility(IWeaponAbility.BOBBLE, 0));
    public static final RegistryObject<ItemToolAbility> cobalt_decorated_pickaxe = reg("cobalt_decorated_pickaxe", T,
            p -> new ItemToolAbility(p, 6F, 0, matDecCobalt, EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistryObject<ItemToolAbility> cobalt_decorated_axe = reg("cobalt_decorated_axe", T,
            p -> new ItemToolAbility(p, 8F, 0, matDecCobalt, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IWeaponAbility.BEHEADER, 0));
    public static final RegistryObject<ItemToolAbility> cobalt_decorated_shovel = reg("cobalt_decorated_shovel", T,
            p -> new ItemToolAbility(p, 5F, 0, matDecCobalt, EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2));
    public static final RegistryObject<ModHoe> cobalt_decorated_hoe = reg("cobalt_decorated_hoe", T,
            p -> new ModHoe(p, matDecCobalt));

    // --- starmetal ---
    public static final RegistryObject<ItemSwordAbility> starmetal_sword = reg("starmetal_sword", T,
            p -> new ItemSwordAbility(p, 25F, 0, matStarmetal)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .addAbility(IWeaponAbility.STUN, 1)
                    .addAbility(IWeaponAbility.BOBBLE, 0));
    public static final RegistryObject<ItemToolAbility> starmetal_pickaxe = reg("starmetal_pickaxe", T,
            p -> new ItemToolAbility(p, 8F, 0, matStarmetal, EnumToolType.PICKAXE)
                    .addAbility(IToolAreaAbility.RECURSION, 3)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IWeaponAbility.STUN, 1));
    public static final RegistryObject<ItemToolAbility> starmetal_axe = reg("starmetal_axe", T,
            p -> new ItemToolAbility(p, 12F, 0, matStarmetal, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.RECURSION, 3)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .addAbility(IWeaponAbility.STUN, 1));
    public static final RegistryObject<ItemToolAbility> starmetal_shovel = reg("starmetal_shovel", T,
            p -> new ItemToolAbility(p, 7F, 0, matStarmetal, EnumToolType.SHOVEL)
                    .addAbility(IToolAreaAbility.RECURSION, 3)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 4)
                    .addAbility(IWeaponAbility.STUN, 1));
    public static final RegistryObject<ModHoe> starmetal_hoe = reg("starmetal_hoe", T, p -> new ModHoe(p, matStarmetal));

    // --- special tools ---
    public static final RegistryObject<ItemToolAbility> centri_stick = reg("centri_stick", T,
            p -> new ItemToolAbility(p, 3F, 0, tMatElecStick, EnumToolType.MINER)
                    .addAbility(IToolHarvestAbility.CENTRIFUGE, 0));
    public static final RegistryObject<ItemToolAbility> smashing_hammer = reg("smashing_hammer", T,
            p -> new ItemToolAbility(p, 12F, -0.1, tMatSteelHammer, EnumToolType.MINER)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0));

    public static final RegistryObject<ItemToolAbility> bismuth_pickaxe = reg("bismuth_pickaxe", T,
            p -> new ItemToolAbility(p, 15F, 0, matBismuth, EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.STUN, 2)
                    .addAbility(IWeaponAbility.VAMPIRE, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .setDepthRockBreaker());
    public static final RegistryObject<ItemToolAbility> bismuth_axe = reg("bismuth_axe", T,
            p -> new ItemToolAbility(p, 25F, 0, matBismuth, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.VAMPIRE, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));

    public static final RegistryObject<ItemToolAbility> volcanic_pickaxe = reg("volcanic_pickaxe", T,
            p -> new ItemToolAbility(p, 15F, 0, matVolcano, EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.FIRE, 0)
                    .addAbility(IWeaponAbility.VAMPIRE, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .setDepthRockBreaker());
    public static final RegistryObject<ItemToolAbility> volcanic_axe = reg("volcanic_axe", T,
            p -> new ItemToolAbility(p, 25F, 0, matVolcano, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IWeaponAbility.FIRE, 1)
                    .addAbility(IWeaponAbility.VAMPIRE, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));

    public static final RegistryObject<ItemToolAbility> chlorophyte_pickaxe = reg("chlorophyte_pickaxe", T,
            p -> new ItemToolAbility(p, 20F, 0, matChlorophyte, EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.LUCK, 3)
                    .addAbility(IToolHarvestAbility.CENTRIFUGE, 0)
                    .addAbility(IToolHarvestAbility.MERCURY, 0)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.VAMPIRE, 2)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .setDepthRockBreaker());
    public static final RegistryObject<ItemToolAbility> chlorophyte_axe = reg("chlorophyte_axe", T,
            p -> new ItemToolAbility(p, 50F, 0, matChlorophyte, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 1)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.LUCK, 3)
                    .addAbility(IWeaponAbility.STUN, 4)
                    .addAbility(IWeaponAbility.VAMPIRE, 3)
                    .addAbility(IWeaponAbility.BEHEADER, 0));

    public static final RegistryObject<ItemToolAbility> mese_pickaxe = reg("mese_pickaxe", T,
            p -> new ItemToolAbility(p, 35F, 0, matMese, EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 2)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 2)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.CRYSTALLIZER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 5)
                    .addAbility(IToolAreaAbility.EXPLOSION, 3)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.PHOSPHORUS, 0)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .setDepthRockBreaker());
    public static final RegistryObject<ItemToolAbility> mese_axe = reg("mese_axe", T,
            p -> new ItemToolAbility(p, 75F, 0, matMese, EnumToolType.AXE)
                    .addAbility(IToolAreaAbility.HAMMER, 2)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 2)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 5)
                    .addAbility(IToolAreaAbility.EXPLOSION, 3)
                    .addAbility(IWeaponAbility.STUN, 4)
                    .addAbility(IWeaponAbility.PHOSPHORUS, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0));

    public static final RegistryObject<ItemSwordAbility> dnt_sword = reg("dnt_sword", T,
            p -> new ItemSwordAbility(p, 12F, 0, matMese));

    public static final RegistryObject<ItemToolAbility> dwarven_pickaxe = reg("dwarven_pickaxe", T,
            p -> new ItemToolAbility(p, 5F, -0.1, matDwarf, EnumToolType.MINER)
                    .addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0));

    public static final RegistryObject<ItemSwordAbility> mese_gavel = reg("mese_gavel", T,
            p -> new ItemSwordAbility(p, 250, 1.5, matMeseGavel)
                    .addAbility(IWeaponAbility.PHOSPHORUS, 0)
                    .addAbility(IWeaponAbility.RADIATION, 2)
                    .addAbility(IWeaponAbility.STUN, 3)
                    .addAbility(IWeaponAbility.VAMPIRE, 4)
                    .addAbility(IWeaponAbility.BEHEADER, 0)
                    .setGavel());

    public static final RegistryObject<ItemChainsaw> chainsaw = reg("chainsaw", T,
            p -> (ItemChainsaw) new ItemChainsaw(p, 25, -0.05, tMatChainsaw, EnumToolType.AXE, 5000, 1, 250,
                    Fluids.DIESEL, Fluids.DIESEL_CRACK, Fluids.KEROSENE, Fluids.BIOFUEL, Fluids.GASOLINE,
                    Fluids.GASOLINE_LEADED, Fluids.PETROIL, Fluids.PETROIL_LEADED, Fluids.COALGAS, Fluids.COALGAS_LEADED)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IWeaponAbility.CHAINSAW, 1)
                    .addAbility(IWeaponAbility.BEHEADER, 0).setShears());

    public static final RegistryObject<RedstoneSword> redstone_sword = reg("redstone_sword", T, RedstoneSword::new);

    public static final RegistryObject<ItemCrucible> crucible = reg("crucible", HbmTab.WEAPONS,
            p -> new ItemCrucible(p, 5000, 1F, matCrucible));

    // --- meteorite ---
    public static final RegistryObject<ItemHot> ingot_meteorite = reg("ingot_meteorite", HbmTab.MATERIALS,
            p -> new ItemHot(p, 200));
    public static final RegistryObject<ItemHot> ingot_meteorite_forged = reg("ingot_meteorite_forged", HbmTab.MATERIALS,
            p -> new ItemHot(p, 200));
    public static final RegistryObject<ItemHot> blade_meteorite = reg("blade_meteorite", HbmTab.MATERIALS,
            p -> new ItemHot(p, 200));

    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword = meteorite("meteorite_sword", 9F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_seared = meteorite("meteorite_sword_seared", 10F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_reforged = meteorite("meteorite_sword_reforged", 12.5F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_hardened = meteorite("meteorite_sword_hardened", 15F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_alloyed = meteorite("meteorite_sword_alloyed", 17.5F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_machined = meteorite("meteorite_sword_machined", 20F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_treated = meteorite("meteorite_sword_treated", 22.5F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_etched = meteorite("meteorite_sword_etched", 25F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_bred = meteorite("meteorite_sword_bred", 30F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_irradiated = meteorite("meteorite_sword_irradiated", 35F);
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_fused = meteorite("meteorite_sword_fused", 50F);
    /** 1.7 excluded the baleful sword from NEI; it stays out of the creative tab here. */
    public static final RegistryObject<ItemSwordMeteorite> meteorite_sword_baleful = reg("meteorite_sword_baleful", null,
            p -> new ItemSwordMeteorite(p, 75F, 0, matMeteorite, "meteorite_sword_baleful"));

    private static RegistryObject<ItemSwordMeteorite> meteorite(String id, float damage) {
        return reg(id, T, p -> new ItemSwordMeteorite(p, damage, 0, matMeteorite, id));
    }

    public static void init() {
        // Force the component classes before the loader flushes the data-component registers.
        ItemToolAbility.ABILITY_CONFIG.getClass();
        ItemToolAbilityFueled.FUEL.getClass();
        ItemCrucible.init();
        GolemNetwork.declarePlayToClient(PlayerInformPayload.CODEC);
        GolemNetwork.registerPlayToServer(ToolAbilityControlPayload.CODEC, ToolAbilityControlPayload::handleServer);
        com.hbm.items.AbilityToolDeclarations.declare();
    }
}
