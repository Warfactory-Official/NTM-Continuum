package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.sound.ModSecuritySounds;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModKnife} (8 Inch Blade Autoinjector): every 50 ticks it stabs the wearer and takes
 * another two points of maximum health, down to a floor of 2. The penalty is one persistent modifier
 * (upstream's {@code trigamma_UUID}) re-applied one step larger each time.
 *
 * <p>Each stab also sends the wearer 1.7's {@code properJolt}: a hurt time of 10,000-19,999 ticks over a 10,000
 * duration, which leaves the hurt-camera tilt wobbling for minutes; the stab that reaches the floor sends 0/0,
 * ending it. 1.7 also broadcast a {@code bloodvomit} aux packet, but its {@code ClientProxy} had no
 * {@code bloodvomit} branch (only {@code vomit} with a blood mode, which the knife never used), so nothing was
 * ever drawn; the port sends nothing for it. The last stab grants "Some Wounds Never Heal"
 * ({@code hbm:some_wounds}).
 */
public class ItemModKnife extends ItemArmorMod {

    /** Upstream's {@code trigamma_UUID}, as a modern modifier id. */
    public static final String TRIGAMMA_ID = "hbm:trigamma";

    public ItemModKnife(Item.Properties properties) {
        super(ArmorModHandler.extra, false, true, false, false, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.RED, "Pain."));
        tooltip.add(Component.empty());
        tooltip.add(coloured(ChatFormatting.RED, "Hurts, doesn't it?"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.RED, stack, ""));
    }

    /**
     * The jolt of the last stab, sent one tick later. The stab lowers max health, the health is clamped, and the
     * client's health update resets its hurt time to 10 ({@code LocalPlayer.hurtTo}); 1.7 sent the jolt through
     * {@code PacketThreading}, which delivered it after that update, so the jolt won. Sending it on the next tick
     * keeps 1.7's order.
     */
    private static final java.util.Map<net.minecraft.server.level.ServerPlayer, net.minecraft.nbt.CompoundTag> PENDING_JOLT =
            new java.util.WeakHashMap<>();

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide()) return;
        if (entity instanceof net.minecraft.server.level.ServerPlayer waiting) {
            net.minecraft.nbt.CompoundTag jolt = PENDING_JOLT.remove(waiting);
            if (jolt != null) com.hbm.particle.helper.HbmEffectNT.ProperJolt.sendTo(waiting, 0, 0, 0, jolt);
        }
        if (entity.tickCount % 50 != 0 || entity.getMaxHealth() <= 2F) return;

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSecuritySounds.SLICER.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        AttributeInstance attribute = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attribute == null) return;
        float health = entity.getMaxHealth();
        Attributes.removeTransient(attribute, TRIGAMMA_ID);
        Attributes.setPermanent(attribute, TRIGAMMA_ID, -(entity.getMaxHealth() - health + 2), Attributes.Op.ADD);
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
        if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            if (entity.getMaxHealth() > 2F) {
                data.putInt("time", 10000 + entity.getRandom().nextInt(10000));
                data.putInt("maxTime", 10000);
            } else {
                data.putInt("time", 0);
                data.putInt("maxTime", 0);
                // 1.7 achSomeWounds: the stab that brings a player's maximum health down to the floor
                com.hbm.advancement.HbmCriteria.BLED_OUT.fire(player);
            }
            PENDING_JOLT.put(player, data);
        }
    }
}
