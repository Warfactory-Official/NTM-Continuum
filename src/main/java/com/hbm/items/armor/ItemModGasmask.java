package com.hbm.items.armor;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.api.item.IGasMask;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemModGasmask}: an M65 gas mask (or half mask) that attaches to any helmet's helmet-only
 * slot. It is an {@link IGasMask} itself, so a filter lives on the attachment and follows it into the
 * helmet; {@code ArmorRegistry} and {@code ArmorUtil} recurse into the module. The worn M65 is drawn by
 * {@code ArmorModRenderLayer}.
 */
public class ItemModGasmask extends ItemArmorMod implements IGasMask {

    private final boolean mono;

    public ItemModGasmask(boolean mono, Item.Properties properties) {
        super(ArmorModHandler.helmet_only, true, false, false, false, properties);
        this.mono = mono;
    }

    public boolean isMono() {
        return mono;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.GREEN, "Gas protection"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
        ArmorUtil.addGasMaskTooltip(stack, tooltip);
        List<HazardClass> haz = getBlacklist(stack);
        if (!haz.isEmpty()) {
            tooltip.add(coloured(ChatFormatting.RED, "Will never protect against:"));
            for (HazardClass clazz : haz) {
                tooltip.add(Component.literal(" -").append(Component.translatable(clazz.lang))
                        .withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.GREEN, stack, " (gas protection)"));
        ArmorUtil.addGasMaskTooltip(stack, tooltip);
    }

    @Override
    public List<HazardClass> getBlacklist(ItemStack stack) {
        return ArmorDataMaps.gasMaskBlacklist(this);
    }

    @Override
    public ItemStack getFilter(ItemStack stack) {
        return ModDataComponents.gasMaskFilter(stack);
    }

    @Override
    public boolean isFilterApplicable(ItemStack stack, ItemStack filter) {
        // Upstream accepted any filter item offered to it; the port's filters are the damageable items.
        return !filter.isEmpty() && filter.isDamageableItem();
    }

    @Override
    public void installFilter(ItemStack stack, ItemStack filter) {
        if (isFilterApplicable(stack, filter)) ModDataComponents.gasMaskFilter(stack, filter);
    }

    @Override
    public void damageFilter(ItemStack stack, int damage) {
        if (damage <= 0) return;
        ItemStack filter = getFilter(stack);
        if (filter.isEmpty() || !filter.isDamageableItem()) return;
        int next = filter.getDamageValue() + damage;
        if (next > filter.getMaxDamage()) {
            ModDataComponents.removeGasMaskFilter(stack);
        } else {
            filter.setDamageValue(next);
            installFilter(stack, filter);
        }
    }

    /**
     * Sneak-use pulls the filter back out (upstream). Installing follows the port's gas-mask convention:
     * the filter held in the other hand, because 1.7's installing {@code ItemFilter.onItemRightClick}
     * is not ported.
     */
    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack mask = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            ItemStack filter = getFilter(mask);
            if (!filter.isEmpty()) {
                ModDataComponents.removeGasMaskFilter(mask);
                if (!player.getInventory().add(filter)) player.drop(filter, true);
            }
            return InteractionResult.SUCCESS;
        }
        ItemStack offered = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!isFilterApplicable(mask, offered)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        ItemStack previous = getFilter(mask);
        installFilter(mask, offered);
        if (!player.isCreative()) offered.shrink(1);
        if (!previous.isEmpty() && !player.getInventory().add(previous)) player.drop(previous, false);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GASMASK_SCREW.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
