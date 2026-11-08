package com.hbm.util;

import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.IHazardType;
import com.hbm.blocks.machine.MachineFieldDisturber;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.items.weapon.GunB92;
import com.hbm.items.weapon.GunB92Cell;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.ItemStacks;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import org.jspecify.annotations.Nullable;

/**
 * The rules behind HBM's category tags ({@code hbm:guns}, {@code hbm:hazardous/hot}, {@code hbm:blast_resistant/nuclear},
 * ...): what the build's tag generator writes and what the drift GameTest holds the shipped tags against.
 */
public final class TagCategories {

    private TagCategories() {}

    /** 1.7's {@code HazardTypeBase}s by tag name ({@code hbm:hazardous/<name>}), in 1.7's order. */
    public static final Map<String, IHazardType> HAZARDS = new LinkedHashMap<>();

    static {
        HAZARDS.put("radioactive", HazardRegistry.RADIATION);
        HAZARDS.put("digamma", HazardRegistry.DIGAMMA);
        HAZARDS.put("hot", HazardRegistry.HOT);
        HAZARDS.put("blinding", HazardRegistry.BLINDING);
        HAZARDS.put("asbestos", HazardRegistry.ASBESTOS);
        HAZARDS.put("coal_dust", HazardRegistry.COAL);
        HAZARDS.put("hydroactive", HazardRegistry.HYDROACTIVE);
        HAZARDS.put("explosive", HazardRegistry.EXPLOSIVE);
    }

    /** {@code hbm:blast_resistant/<tier>} and the explosion resistance it needs (1.7 setResistance x 0.6). */
    public static final Map<String, Float> BLAST_TIERS = new LinkedHashMap<>();

    static {
        BLAST_TIERS.put("large_explosion", 70F); // 1.7 ExplosionLarge cannot break it
        BLAST_TIERS.put("nuclear", 200F);        // 1.7 ExplosionNukeGeneric's threshold
        BLAST_TIERS.put("obsidian", 1200F);
    }

    public static boolean isFrom(String namespace, Item item) {
        return namespace.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    public static boolean isFrom(String namespace, Block block) {
        return namespace.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    public static boolean isGun(Item item) {
        return item instanceof ItemGunBaseNT || item instanceof GunB92;
    }

    /** Every item of {@code namespace} a gun loads: each bullet config's round, and the B92's energy cell. */
    public static Set<Item> ammo(String namespace) {
        Set<Item> ammo = new LinkedHashSet<>();
        for (BulletConfig config : BulletConfigs.all()) {
            Item item = config.ammo == null ? null : config.ammo.get();
            if (item != null && isFrom(namespace, item)) ammo.add(item);
        }
        for (Item item : BuiltInRegistries.ITEM) if (item instanceof GunB92Cell && isFrom(namespace, item)) ammo.add(item);
        return ammo;
    }

    public static boolean isFood(Item item) {
        return ItemStacks.foodProperties(new ItemStack(item)) != null;
    }

    public static @Nullable ArmorSlot armorSlot(Item item) {
        return item instanceof CompatArmorItem armor ? armor.armorSlot() : null;
    }

    /** The items of {@code namespace} in one of 1.7's creative tabs: bombs, missiles, machines. */
    public static Set<Item> tab(HbmTab tab, String namespace) {
        Set<Item> items = new LinkedHashSet<>();
        for (ItemStack stack : ModTabs.displayStacks(ModTabs.get(tab).getKey())) {
            if (isFrom(namespace, stack.getItem())) items.add(stack.getItem());
        }
        return items;
    }

    /** The hazard types of {@code types} an item carries by default, as tag names. */
    public static Set<String> hazards(Item item, Map<String, IHazardType> types) {
        Set<String> names = new LinkedHashSet<>();
        ItemStack stack = new ItemStack(item);
        for (Map.Entry<String, IHazardType> hazard : types.entrySet()) {
            if (HazardSystem.getHazardLevelFromStack(stack, hazard.getValue()) > 0) names.add(hazard.getKey());
        }
        return names;
    }

    /** A block 1.7 gives a hazard: its item carries one. */
    public static Set<String> hazards(Block block, Map<String, IHazardType> types) {
        Item item = block.asItem();
        return item instanceof BlockItem ? hazards(item, types) : Set.of();
    }

    /**
     * Blocks kept out of the blast tiers, whose resistance protects nothing: replaceable blocks (fluids at 300 because
     * fluids are, fire, light beams), falling blocks (digamma ash), bombs (at 120 so a blast does not set off its
     * neighbours) and 1.7's unobtainable field jammer, at 120 like them.
     */
    public static boolean blastExcluded(Block block) {
        return block.defaultBlockState().canBeReplaced() || block instanceof FallingBlock || block instanceof IBomb
                || block instanceof MachineFieldDisturber;
    }

    /** The blast tiers a block meets, highest last; empty below the lowest or when excluded. */
    public static Set<String> blastTiers(Block block) {
        Set<String> tiers = new LinkedHashSet<>();
        if (blastExcluded(block)) return tiers;
        float resistance = block.getExplosionResistance();
        for (Map.Entry<String, Float> tier : BLAST_TIERS.entrySet()) {
            if (resistance >= tier.getValue()) tiers.add(tier.getKey());
        }
        return tiers;
    }

    public static boolean shieldsRadiation(Block block) {
        return block instanceof IRadResistantBlock;
    }
}
