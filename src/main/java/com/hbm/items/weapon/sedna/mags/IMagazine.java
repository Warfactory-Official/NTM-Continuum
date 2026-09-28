package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.tool.ItemCasingBag;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Where a {@link com.hbm.items.weapon.sedna.Receiver} gets its ammo from. The receiver does not care
 * whether that is a detachable box, a shotgun tube, a belt or an energy cell — the magazine owns
 * capacity, type restrictions and the whole reload sequence.
 *
     * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.mags.IMagazine}. The casing bag is an
     * inventory-side sink, so casing collection remains independent of the projectile renderer.
 * {@code Container} stands in for the old {@code IInventory}; a null container means "no inventory to
 * draw from", exactly as upstream, which is how turret and creative magazines work.
 */
public interface IMagazine<T> {

    /** Source-shaped view of a carried ammo bag used by magazine implementations. */
    static ItemAmmoBag.InventoryAmmoBag ammoBag(ItemStack slot) {
        return slot.getItem() instanceof ItemAmmoBag ? new ItemAmmoBag.InventoryAmmoBag(slot) : null;
    }

    static boolean isInfiniteBag(ItemStack slot) {
        return slot.getItem() instanceof ItemAmmoBag bag && bag.isInfinite();
    }

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

    /** Casing definition used by the optional spent-casing path. */
    default ItemStack getCasing(ItemStack stack, Container inventory) {
        T type = getType(stack, inventory);
        return type instanceof BulletConfig bullet && bullet.casingStack() != null
                ? bullet.casingStack().copy() : ItemStack.EMPTY;
    }

    /** Adds spent casing progress to the first casing bag in the player's inventory. */
    static void handleAmmoBag(Container inventory, BulletConfig config, int shotsFired) {
        if (!(inventory instanceof net.minecraft.world.entity.player.Inventory)
                || config == null || config.casingStack() == null
                || config.casingAmount <= 0 || shotsFired <= 0) return;
        for (int i = 0; i < Math.min(36, inventory.getContainerSize()); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof ItemCasingBag
                    && ItemCasingBag.pushCasing(stack, config.casingStack(),
                    (float) shotsFired / config.casingAmount * 0.5F)) return;
        }
    }

    /**
     * 1.7 {@code shouldUseUpTrenchie}: a Trenchmaster or an Ace of Spades wearer spends a round only two
     * times in three.
     */
    static boolean shouldUseUpTrenchie(Container inventory) {
        if (inventory instanceof net.minecraft.world.entity.player.Inventory playerInventory) {
            net.minecraft.world.entity.player.Player player = playerInventory.player;
            boolean trenchie = com.hbm.items.armor.ArmorTrenchmaster.isTrenchMaster(player);
            boolean aos = com.hbm.items.armor.ArmorTrenchmaster.hasAoS(player);
            if (trenchie || aos) return player.getRandom().nextInt(3) < 2;
        }
        return true;
    }
}
