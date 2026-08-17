package com.hbm.items.weapon.sedna.mags;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Where a {@link com.hbm.items.weapon.sedna.Receiver} gets its ammo from. The receiver does not care
 * whether that is a detachable box, a shotgun tube, a belt or an energy cell — the magazine owns
 * capacity, type restrictions and the whole reload sequence.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.mags.IMagazine}. Two members are dropped with
 * their subsystems: {@code getCasing} (the spent-casing particle is not ported) and the static
 * {@code handleAmmoBag}/{@code shouldUseUpTrenchie} helpers (no ammo bag, no Trenchmaster armour yet).
 * {@code Container} stands in for the old {@code IInventory}; a null container means "no inventory to
 * draw from", exactly as upstream, which is how turret and creative magazines work.
 */
public interface IMagazine<T> {

    /** What ammo is loaded right now. */
    T getType(ItemStack stack, Container inventory);

    /** Sets the loaded ammo type. */
    void setType(ItemStack stack, T type);

    /** How much ammo this magazine can hold. */
    int getCapacity(ItemStack stack);

    /** How much ammo is loaded right now. */
    int getAmount(ItemStack stack, Container inventory);

    /** Sets the loaded amount. */
    void setAmount(ItemStack stack, int amount);

    /** Removes {@code amount} rounds. */
    void useUpAmmo(ItemStack stack, Container inventory, int amount);

    /** Whether a reload can even start — i.e. there is room and the player has matching rounds. */
    boolean canReload(ItemStack stack, Container inventory);

    /** Lets the magazine adopt a new ammo type before the reload animation begins. */
    void initNewType(ItemStack stack, Container inventory);

    /** One reload cycle: load a shell, or swap the whole magazine. */
    void reloadAction(ItemStack stack, Container inventory);

    /** The stack shown by the ammo HUD. */
    ItemStack getIconForHUD(ItemStack stack, Player player);

    /** The "12 / 30" line shown by the ammo HUD. */
    String reportAmmoStateForHUD(ItemStack stack, Player player);

    /** Remembers the load level from before a reload started. */
    void setAmountBeforeReload(ItemStack stack, int amount);

    /**
     * The load level before the current reload began. Note that the item's synced state usually arrives
     * after the animation packet, so animations keyed on "was it empty" must use this, not the live count.
     */
    int getAmountBeforeReload(ItemStack stack);

    /** Records the load level after each reload cycle. */
    void setAmountAfterReload(ItemStack stack, int amount);

    /** The load level after the most recent reload cycle. */
    int getAmountAfterReload(ItemStack stack);
}
