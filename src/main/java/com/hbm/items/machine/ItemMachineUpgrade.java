package com.hbm.items.machine;

import com.hbm.api.data.MachinePartDataMaps;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.gui.UpgradeInfoHelper;
import com.hbm.items.ModUpgradeItems;
import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/**
 * A machine upgrade module (1.12.2 {@code ItemMachineUpgrade}). Carries its {@link UpgradeType} and a
 * {@code tier}: the level it contributes when plugged into a machine's upgrade slots; multiple upgrades
 * of the same type stack their tiers, clamped by the machine's {@code getValidUpgrades()} maximum
 * (see {@link com.hbm.inventory.UpgradeManagerNT}).
 *
 * <p>Hovering the item while a machine GUI is open shows that machine's exact effect numbers, exactly
 * like the 1.12.2 {@code addInformation}: the screen (an
 * {@link com.hbm.inventory.gui.IUpgradeInfoProviderSource}) exposes its {@link IUpgradeInfoProvider}
 * and {@code provideInfo} builds the lines. The client-touching screen probe lives in
 * {@link com.hbm.inventory.gui.UpgradeInfoHelper}, classloaded lazily from the tooltip path only
 * (tooltips never run on a dedicated server).
 */
public class ItemMachineUpgrade extends CompatItem {

    public final UpgradeType type;
    public final int tier;

    public ItemMachineUpgrade(UpgradeType type, int tier, Item.Properties properties) {
        super(properties);
        this.type = type;
        this.tier = tier;
    }

    /**
     * What {@code item} counts as in an upgrade slot: its {@code hbm:machine_upgrade} entry, else an upgrade item's own
     * type and tier; null when it is no upgrade.
     */
    public static MachinePartDataMaps.@org.jspecify.annotations.Nullable Upgrade upgradeOf(Item item) {
        MachinePartDataMaps.Upgrade data = MachinePartDataMaps.MACHINE_UPGRADE.get(item);
        if (data != null) return data;
        return codeUpgradeOf(item);
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public static MachinePartDataMaps.@org.jspecify.annotations.Nullable Upgrade codeUpgradeOf(Item item) {
        return item instanceof ItemMachineUpgrade upgrade && upgrade.type != null
                ? new MachinePartDataMaps.Upgrade(upgrade.type, upgrade.tier) : null;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        IUpgradeInfoProvider provider = UpgradeInfoHelper.resolveProvider();
        if (provider != null && provider.canProvideInfo(this.type, this.tier, flag.isAdvanced())) {
            List<Component> info = new ArrayList<>();
            provider.provideInfo(this.type, this.tier, info, flag.isAdvanced());
            tooltip.addAll(info);
            return;
        }

        // Static fallbacks for the special items (the 1.12.2 addInformation desc.upgradeN lines).
        if (this == ModUpgradeItems.upgrade_smelter.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade12")));
        } else if (this == ModUpgradeItems.upgrade_shredder.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade13")));
        } else if (this == ModUpgradeItems.upgrade_centrifuge.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade21")));
        } else if (this == ModUpgradeItems.upgrade_crystallizer.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade14")));
        } else if (this == ModUpgradeItems.upgrade_nullifier.get()) {
            tooltip.add(Component.translatable("desc.upgrade10").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade19")));
        } else if (this == ModUpgradeItems.upgrade_screm.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade15")));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade16")));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade17")));
        } else if (this == ModUpgradeItems.upgrade_gc_speed.get()) {
            tooltip.add(Component.literal("Gas Centrifuge Upgrade").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Allows total isotopic separation of HEUF6"));
            tooltip.add(Component.literal("Also makes the centrifuge go sicko mode").withStyle(ChatFormatting.YELLOW));
        }
    }

    /**
     * The "worthless" drops the nullifier upgrade deletes when the mining laser vacuums them up:
     * the 1.12.2 {@code scrapItems} set (kept as a predicate so no registry-order static init is needed).
     */
    public static boolean isScrap(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.GRASS_BLOCK
                || item == Items.DIRT
                || item == Items.STONE
                || item == Items.COBBLESTONE
                || item == Items.SAND
                || item == Items.SANDSTONE
                || item == Items.GRAVEL
                || item == Items.NETHERRACK
                || item == Items.END_STONE
                || item == Items.FLINT
                || item == Items.SNOWBALL
                || item == Items.WHEAT_SEEDS
                || item == Items.STICK
                || stack.is(ModBlocks.stone_gneiss.get().asItem());
    }

    public enum UpgradeType {
        SPEED,
        EFFECT,
        POWER,
        FORTUNE,
        AFTERBURN,
        OVERDRIVE,
        SPECIAL
    }
}
