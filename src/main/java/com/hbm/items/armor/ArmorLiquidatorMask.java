package com.hbm.items.armor;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.api.item.IGasMask;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ArmorLiquidatorMask}: the Liquidator helmet, a filterable full hood. Its tooltip is only the
 * filter listing - upstream's override never called super, so the set-bonus lines the other three
 * pieces show are absent here.
 */
public class ArmorLiquidatorMask extends ArmorLiquidator implements IGasMask {

    public ArmorLiquidatorMask(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties);
    }

    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        ArmorUtil.addGasMaskTooltip(stack, tooltip);
    }

    @Override
    public List<HazardClass> getBlacklist(ItemStack stack) {
        return ArmorDataMaps.gasMaskBlacklist(this);
    }

    @Override
    public ItemStack getFilter(ItemStack stack) {
        return GasMaskFilters.get(stack);
    }

    @Override
    public boolean isFilterApplicable(ItemStack stack, ItemStack filter) {
        return GasMaskFilters.applicable(filter);
    }

    @Override
    public void installFilter(ItemStack stack, ItemStack filter) {
        GasMaskFilters.install(stack, filter);
    }

    @Override
    public void damageFilter(ItemStack stack, int damage) {
        GasMaskFilters.damage(stack, damage);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        InteractionResult removed = GasMaskFilters.sneakRemove(level, player, hand);
        return removed != null ? removed : super.compatUse(level, player, hand);
    }
}
