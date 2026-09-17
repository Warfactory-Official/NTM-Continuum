package com.hbm.entity.mob;

import dev.golem.core.data.CompatMob;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EntityDummy extends CompatMob {

    public EntityDummy(EntityType<? extends EntityDummy> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // 1.7: instanceof ItemArmor; the slot lookup answers a hand slot for anything that is not wearable
        EquipmentSlot slot = getEquipmentSlotForItem(held);
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) setItemSlot(slot, held.copy());

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public Component getName() {
        return Component.literal((int) (getHealth() * 10) / 10F + " / " + (int) (getMaxHealth() * 10) / 10F);
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
    }
}
