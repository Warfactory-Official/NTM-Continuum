package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.IAnimatedItem;
import com.hbm.items.ToolAnimation;
import dev.golem.core.world.SwingReceiver;
import dev.golem.core.world.ToolTier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ItemChainsaw}: the fueled chainsaw. Every swing the server sees starts the first-person
 * {@code SWING_ROT}/{@code SWING_TRANS} animation on the holder's client ({@code onEntitySwing} ->
 * {@code IAnimatedItem.playAnimation}); the table is {@code ToolAnimations.chainsaw}, the renderer
 * {@code ItemRenderChainsaw}. 1.7's {@code IHeldSoundProvider} was an empty marker interface: the chainsaw had no held
 * or idle sound, only the chainsaw ability's sound on a kill ({@code IWeaponAbility.CHAINSAW}).
 *
 * <p>The hand: 1.7 raised it once on equip and never lowered it for an attack or for the fuel NBT changing.
 * {@link #compatIsSameHeldItem} (from {@code ItemToolAbility}) keeps the fuel component from re-equipping it, and
 * {@link #compatHoldsRaised} keeps the modern cooldowns from dipping it under the swing animation.
 */
public class ItemChainsaw extends ItemToolAbilityFueled implements SwingReceiver, IAnimatedItem {

    public ItemChainsaw(Properties properties, float damage, double movement, ToolTier material, EnumToolType type,
                        int maxFuel, int consumption, int fillRate, FluidType... acceptedFuels) {
        super(properties, damage, movement, material, type, maxFuel, consumption, fillRate, acceptedFuels);
    }

    @Override
    public boolean compatHoldsRaised(ItemStack held) {
        return true;
    }

    /**
     * 1.7 {@code onEntitySwing}. Its {@code getItemDamage() >= getMaxDamage()} guard never held for the chainsaw:
     * the fueled tool set a max damage of 1 and never damaged the stack (fuel lives in NBT), so the swing animation
     * plays with or without fuel. Returns false: the vanilla swing is not cancelled (the renderer hides it).
     */
    @Override
    public boolean compatOnEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        // 1.7 had one hand; its animations only ever run on the main hand's rail
        if (!(entity instanceof ServerPlayer player) || hand != InteractionHand.MAIN_HAND) return false;
        playAnimation(player, ToolAnimation.SWING, stack);
        return false;
    }
}
