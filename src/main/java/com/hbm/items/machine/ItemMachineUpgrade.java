package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A machine upgrade module (1.12.2 {@code ItemMachineUpgrade}). Carries its {@link UpgradeType} and a
 * {@code tier} — the level it contributes when plugged into a machine's upgrade slots; multiple upgrades
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

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        IUpgradeInfoProvider provider = com.hbm.inventory.gui.UpgradeInfoHelper.resolveProvider();
        if (provider != null && provider.canProvideInfo(this.type, this.tier, flag.isAdvanced())) {
            List<Component> info = new ArrayList<>();
            provider.provideInfo(this.type, this.tier, info, flag.isAdvanced());
            tooltip.addAll(info);
            return;
        }

        // Static fallbacks for the special items (the 1.12.2 addInformation desc.upgradeN lines).
        if (this == com.hbm.items.ModUpgradeItems.upgrade_smelter.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade12")));
        } else if (this == com.hbm.items.ModUpgradeItems.upgrade_shredder.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade13")));
        } else if (this == com.hbm.items.ModUpgradeItems.upgrade_centrifuge.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade21")));
        } else if (this == com.hbm.items.ModUpgradeItems.upgrade_crystallizer.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade14")));
        } else if (this == com.hbm.items.ModUpgradeItems.upgrade_nullifier.get()) {
            tooltip.add(Component.translatable("desc.upgrade10").withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade19")));
        } else if (this == com.hbm.items.ModUpgradeItems.upgrade_screm.get()) {
            tooltip.add(Component.translatable("desc.upgrade9").withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade15")));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade16")));
            tooltip.add(Component.literal(" ").append(Component.translatable("desc.upgrade17")));
        }
    }

    /**
     * The "worthless" drops the nullifier upgrade deletes when the mining laser vacuums them up —
     * the 1.12.2 {@code scrapItems} set (kept as a predicate so no registry-order static init is needed).
     */
    public static boolean isScrap(net.minecraft.world.item.ItemStack stack) {
        Item item = stack.getItem();
        return item == net.minecraft.world.item.Items.GRASS_BLOCK
                || item == net.minecraft.world.item.Items.DIRT
                || item == net.minecraft.world.item.Items.STONE
                || item == net.minecraft.world.item.Items.COBBLESTONE
                || item == net.minecraft.world.item.Items.SAND
                || item == net.minecraft.world.item.Items.SANDSTONE
                || item == net.minecraft.world.item.Items.GRAVEL
                || item == net.minecraft.world.item.Items.NETHERRACK
                || item == net.minecraft.world.item.Items.END_STONE
                || item == net.minecraft.world.item.Items.FLINT
                || item == net.minecraft.world.item.Items.SNOWBALL
                || item == net.minecraft.world.item.Items.WHEAT_SEEDS
                || item == net.minecraft.world.item.Items.STICK
                || stack.is(com.hbm.blocks.ModBlocks.stone_gneiss.get().asItem());
    }

    public enum UpgradeType {
        SPEED,
        EFFECT,
        POWER,
        FORTUNE,
        AFTERBURN,
        OVERDRIVE,
        NULLIFIER,
        SCREAM,
        SPECIAL
    }
}
