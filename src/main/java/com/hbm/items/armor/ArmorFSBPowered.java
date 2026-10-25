package com.hbm.items.armor;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.machine.ItemSelfcharger;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Electric full-set armour: an {@link ArmorFSB} that runs on HE and stops working when it runs out.
 *
 * <p>Ported 1:1 from 1.12.2 {@code com.hbm.items.armor.ArmorFSBPowered}. Three behaviours carry over
 * that are worth knowing before touching this class, because none of them is obvious:
 *
 * <ul>
 * <li><b>A fresh suit is full.</b> Reading the charge of a stack that has never stored one yields
 *     {@link #getMaxCharge}. Upstream implemented this by <i>writing</i> the tag inside the getter; here
 *     it is a read-side default, because a component write marks the stack dirty and resyncs it, and
 *     doing that from a getter would resync every frame the tooltip is drawn.
 * <li><b>Damage drains power, not durability.</b> {@code setDamage} is redirected into
 *     {@link #dischargeBattery} at {@code consumption} HE per point, and the item has no real
 *     durability. This is why a power suit never breaks: it goes flat instead.
 * <li><b>The drain only runs while the set is complete.</b> A lone helmet costs nothing, and neither
 *     does anything in creative.
 * </ul>
 *
 * <p>The deprecated held {@code ItemSelfcharger} subtraction is retained: its fixed discharge rate is
 * divided by four before the complete-set drain, including the old negative-drain edge case.
 */
public class ArmorFSBPowered extends ArmorFSB implements IBatteryItem {

    public final long maxPower;
    public final long chargeRate;
    public final long consumption;
    public final long drain;

    public ArmorFSBPowered(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
            long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
        this.drain = drain;
    }

    /** A powered suit's bonuses are live only while it has charge. */
    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    @Override
    public long getCharge(ItemStack stack) {
        // The read-side "spawns full" default; see the class comment for why this does not write.
        return ModDataComponents.ARMOR_CHARGE.getOr(stack, getMaxCharge(stack));
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        long clamped = Math.max(0L, Math.min(i, getMaxCharge(stack)));
        // Write-only-on-change: setting a component dirties the stack and resyncs it, so an
        // unconditional per-tick write is a permanent packet loop (see ItemGunBaseNT.write).
        if (ModDataComponents.ARMOR_CHARGE.getOr(stack, getMaxCharge(stack)) != clamped) {
            ModDataComponents.ARMOR_CHARGE.set(stack, clamped);
        }
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        setCharge(stack, getCharge(stack) + i);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        setCharge(stack, getCharge(stack) - i);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        ItemStack battery = ArmorModHandler.pryMod(stack, ArmorModHandler.battery);
        if (battery.getItem() instanceof ItemModBattery mod) return (long) (maxPower * mod.mod);
        return maxPower;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    /** Suits never feed power back out; they are a sink only. */
    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }

    /**
     * The idle drain, one tick's worth, charged to every piece of a complete set.
     *
     * <p>1.7.10 {@code ArmorFSBPowered.onArmorTick} calls {@code super.onArmorTick} first, so a powered suit with
     * {@code setHasGeigerSound} (AJR, AJR-O, HEV, RPA, ...) clicks like any other FSB suit. 1.12.2 skips the
     * super call and leaves the built-in counter silent.
     */
    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        super.armorTick(level, player, stack);
        if (player.isCreative() || this.drain <= 0) return;
        if (!ArmorFSB.hasFSBArmor(player)) return;
        long netDrain = drain;
        ItemSelfcharger selfcharger = heldSelfcharger(player);
        if (selfcharger != null) netDrain -= selfcharger.getDischargeRate(stack) / 4L;
        dischargeBattery(stack, netDrain);
    }

    private static ItemSelfcharger heldSelfcharger(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ItemSelfcharger battery) return battery;
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof ItemSelfcharger battery ? battery : null;
    }

    // --- the charge shown on the item's durability bar ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long max = getMaxCharge(stack);
        return max <= 0 ? 0 : Math.round(13.0F * getCharge(stack) / max);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x33CC33; // green, as the battery items use
    }

    // Power armour cannot be enchanted. Upstream said so four ways (getItemEnchantability, isEnchantable,
    // canApplyAtEnchantingTable, isBookEnchantable); from 26.1 enchantability is a data component and
    // there is nothing to override. An enchantment value of 0 on the material is the whole statement,
    // which is what HbmArmorMaterials.T51 declares.

    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.literal("Charge: " + Library.getShortNumber(getCharge(stack)) + " / "
                + Library.getShortNumber(getMaxCharge(stack))));
        super.addInformation(stack, tooltip);
    }
}
