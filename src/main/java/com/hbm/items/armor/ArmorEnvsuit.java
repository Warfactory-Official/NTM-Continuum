package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.gear.ArmorFSBPowered;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Effects;
import dev.golem.core.world.GolemArmorMaterial;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code ArmorEnvsuit}, the M1TTY environment suit. Everything special runs from the chestplate's tick with the
 * complete working set:
 *
 * <ul>
 * <li>"SQUIRREL SPEED": {@code +0.1} flat movement speed while sprinting;</li>
 * <li>in water: air topped up to 300, 15 s of night vision, and a push of {@code 0.1 * forward input} along the
 *     look vector every tick;</li>
 * <li>out of water: night vision is taken away again unless the helmet carries a night-vision module.</li>
 * </ul>
 *
 * <p>Upstream removed the speed modifier only from this same chestplate tick, so taking the chestplate off while
 * sprinting leaves it on the player until the attribute is rebuilt; ported as written.
 */
public class ArmorEnvsuit extends ArmorFSBPowered {

    private static final String SPEED = "hbm:envsuit_squirrel_speed";

    public ArmorEnvsuit(GolemArmorMaterial material, ArmorSlot slot, Properties properties, long maxPower,
                        long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        super.armorTick(level, player, stack);

        if (this != ModArmorSetBItems.envsuit_plate.get()) return;

        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        // Upstream removed the modifier every tick and re-applied it while sprinting; asserting the end state
        // gives the same attribute without resyncing it every tick.
        boolean full = hasFSBArmor(player);
        if (full && player.isSprinting()) Attributes.setTransient(speed, SPEED, 0.1D, Attributes.Op.ADD);
        else if (Attributes.has(speed, SPEED)) Attributes.removeTransient(speed, SPEED);

        if (!full) return;

        if (player.isInWater()) {
            if (!level.isClientSide()) {
                player.setAirSupply(300);
                player.addEffect(Effects.instance(MobEffects.NIGHT_VISION, 15 * 20, 0));
            }

            double mo = 0.1D * player.zza;
            Vec3 vec = player.getLookAngle().scale(mo);
            player.setDeltaMovement(player.getDeltaMovement().add(vec));
        } else {
            boolean canRemoveNightVision = true;
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack helmetMod = ArmorModHandler.pryMod(helmet, ArmorModHandler.helmet_only);
            if (!helmetMod.isEmpty() && helmetMod.getItem() instanceof ItemModNightVision) {
                canRemoveNightVision = false;
            }

            if (!level.isClientSide() && canRemoveNightVision) {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }
}
