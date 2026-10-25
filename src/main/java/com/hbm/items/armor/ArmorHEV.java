package com.hbm.items.armor;

import com.hbm.client.HEVOverlay;
import com.hbm.items.ModArmorSetItems;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7 {@code ArmorHEV}: the Half-Life suit. Upstream cancelled the vanilla health and armour HUD elements
 * while the full set was worn (charged or not) with the HEV chestplate, and drew its own orange readout in
 * their place. The readout is client code ({@code com.hbm.client.HEVOverlay}); this class keeps the
 * condition, which the client uses both to suppress the vanilla rows and to draw.
 */
public class ArmorHEV extends ArmorFSBPowered {

    public ArmorHEV(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                    long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }

    /** Upstream's {@code hasFSBArmorIgnoreCharge(player) && armorInventory[2] == hev_plate}. */
    public static boolean replacesHud(Player player) {
        return player != null && ArmorFSB.hasFSBArmorIgnoreCharge(player)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModArmorSetItems.hev_plate.get());
    }

    @Override
    public void handleOverlay(Object graphics, Player player) {
        if (replacesHud(player)) HEVOverlay.render(graphics, player);
    }
}
