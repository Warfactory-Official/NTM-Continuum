package com.hbm.items.armor;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ArmorDiesel} ("bnuuy"): the diesel suit.
 *
 * <p>Each piece carries a {@code +0.25} knockback-resistance modifier with operation 1 (multiply base). The
 * attribute's base is 0, so the modifier multiplies zero and does nothing; ported as written. The legs puff the
 * {@code bnuuy} exhaust every third world tick while the full working set is worn, to everyone within 100 blocks.
 * It takes plain or cracked diesel.
 */
public class ArmorDiesel extends ArmorFSBFueled {

    public ArmorDiesel(GolemArmorMaterial material, ArmorSlot slot, Properties properties, Supplier<FluidType> fuel,
                       int maxFuel, int fillRate, int consumption, int drain) {
        super(material, slot, properties, fuel, maxFuel, fillRate, consumption, drain);
    }

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET};

    /** Per worn piece, as {@link ArmorDesh#updateModifiers}. */
    public static void updateModifiers(Player player) {
        var knockback = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        for (EquipmentSlot slot : SLOTS) {
            String id = "hbm:armor_diesel_" + slot.getName();
            if (player.getItemBySlot(slot).getItem() instanceof ArmorDiesel) {
                Attributes.setTransient(knockback, id, 0.25D, Attributes.Op.MULTIPLY_BASE);
            } else if (Attributes.has(knockback, id)) {
                Attributes.removeTransient(knockback, id);
            }
        }
    }

    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        super.armorTick(level, player, stack);

        if (!level.isClientSide() && this == ModArmorSetBItems.dieselsuit_legs.get() && ArmorFSB.hasFSBArmor(player)
                && level.getGameTime() % 3 == 0) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "bnuuy");
            data.putInt("player", player.getId());
            HbmEffectNT.Bnuuy.send(level, player.getX(), player.getY(), player.getZ(), data, 100);
        }
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == Fluids.DIESEL || type == Fluids.DIESEL_CRACK;
    }
}
