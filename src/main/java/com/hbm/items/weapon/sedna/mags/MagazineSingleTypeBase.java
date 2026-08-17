package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A magazine holding one ammo type at a time: load AP and it is AP until you empty it.
 *
 * <p>Ported from 1.12.2 {@code MagazineSingleTypeBase}. The stored type is the {@link BulletConfig}'s
 * <b>registry id</b> rather than upstream's index into a mutable static list — see {@link BulletConfigs}
 * for why. The key names ({@code magcount}, {@code magtype}, {@code magprev}, {@code magafter}) are kept
 * so the shape of the saved data still matches the original.
 *
 * <p>The ammo-bag branches of {@code standardReload}/{@code getFirstConfig} are not ported: neither
 * {@code ammo_bag} nor {@code ammo_bag_infinite} exists yet. Both methods are otherwise faithful,
 * including the "fix broken data by falling back to the first accepted round" guards.
 */
public abstract class MagazineSingleTypeBase implements IMagazine<BulletConfig> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_TYPE = "magtype";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";

    /** Rounds this magazine accepts, in priority order — an empty magazine adopts the first match. */
    public List<BulletConfig> acceptedBullets = new ArrayList<>();

    /** Distinguishes several magazines on one gun. */
    public int index;
    public int capacity;

    public MagazineSingleTypeBase(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    public MagazineSingleTypeBase addConfigs(BulletConfig... cfgs) {
        acceptedBullets.addAll(Arrays.asList(cfgs));
        return this;
    }

    @Override
    public BulletConfig getType(ItemStack stack, Container inventory) {
        BulletConfig cfg = BulletConfigs.get(getMagType(stack, index));
        if (cfg == null) return null;
        // A round this magazine does not accept can only come from edited or stale data; fall back
        // rather than letting the gun fire something it was never meant to chamber.
        return acceptedBullets.contains(cfg) ? cfg : acceptedBullets.get(0);
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {
        if (type != null && type.id != null) setMagType(stack, index, type.id.toString());
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        BulletConfig config = this.getType(stack, player.getInventory());
        return config != null ? config.toStack() : ItemStack.EMPTY;
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return getAmount(stack, player.getInventory()) + " / " + getCapacity(stack);
    }

    @Override
    public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
    }

    /** True if the player has the same ammo when partially loaded, or any accepted ammo when empty. */
    @Override
    public boolean canReload(ItemStack stack, Container inventory) {
        if (this.getAmount(stack, inventory) >= this.getCapacity(stack)) return false;
        if (inventory == null) return true;
        return getFirstConfig(stack, inventory) != null;
    }

    /**
     * Moves up to {@code loadLimit} items' worth of rounds out of the inventory and into the magazine.
     *
     * <p>With no inventory at all the magazine simply fills itself — that is how a turret or a creative
     * magazine reloads.
     */
    public void standardReload(ItemStack stack, Container inventory, int loadLimit) {

        if (inventory == null) {
            BulletConfig config = this.getType(stack, null);
            if (config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); }
            this.setAmount(stack, this.capacity);
            return;
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (loadLimit <= 0) return;

            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;

            if (this.getAmount(stack, null) == 0) {
                // Empty: adopt whichever accepted round is found first.
                for (BulletConfig config : this.acceptedBullets) {
                    if (config.ammoItem() == slot.getItem()) {
                        this.setType(stack, config);
                        loadLimit -= load(stack, inventory, i, slot, config, 0, loadLimit);
                        break;
                    }
                }
            } else {
                // Partially loaded: only top up with the same round.
                BulletConfig config = this.getType(stack, null);
                if (config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); }
                if (config.ammoItem() == slot.getItem()) {
                    loadLimit -= load(stack, inventory, i, slot, config, this.getAmount(stack, null), loadLimit);
                }
            }
        }
    }

    /** Consumes items from one slot into the magazine; returns how many items were used. */
    private int load(ItemStack stack, Container inventory, int slotIndex, ItemStack slot,
            BulletConfig config, int alreadyLoaded, int loadLimit) {
        int wantsToLoad = (int) Math.ceil((double) (this.getCapacity(stack) - alreadyLoaded)
                / (double) config.ammoReloadCount);
        int toLoad = Math.min(Math.min(wantsToLoad, slot.getCount()), loadLimit);
        if (toLoad <= 0) return 0;
        this.setAmount(stack, Math.min(toLoad * config.ammoReloadCount + alreadyLoaded, this.capacity));
        inventory.removeItem(slotIndex, toLoad);
        return toLoad;
    }

    /** The first round that could be loaded: the chambered type, or the first accepted one if empty. */
    public BulletConfig getFirstConfig(ItemStack stack, Container inventory) {
        if (inventory == null) return null;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;

            if (this.getAmount(stack, null) == 0) {
                for (BulletConfig config : this.acceptedBullets) {
                    if (config.ammoItem() == slot.getItem()) return config;
                }
            } else {
                BulletConfig config = this.getType(stack, null);
                if (config == null) { config = this.acceptedBullets.get(0); this.setType(stack, config); }
                if (config.ammoItem() == slot.getItem()) return config;
            }
        }

        return null;
    }

    @Override
    public void initNewType(ItemStack stack, Container inventory) {
        if (inventory == null) return;
        BulletConfig nextConfig = getFirstConfig(stack, inventory);
        if (nextConfig != null) setType(stack, nextConfig);
    }

    @Override public int getCapacity(ItemStack stack) { return capacity; }
    @Override public int getAmount(ItemStack stack, Container inventory) { return getMagCount(stack, index); }
    @Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }

    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
    @Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
    @Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

    // MAG TYPE — a registry id, not an index, so declaration order can never reinterpret saved ammo.
    public static String getMagType(ItemStack stack, int index) { return ItemGunBaseNT.getValueString(stack, KEY_MAG_TYPE + index); }
    public static void setMagType(ItemStack stack, int index, String value) { ItemGunBaseNT.setValueString(stack, KEY_MAG_TYPE + index, value); }

    // MAG COUNT //
    public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
    public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}
