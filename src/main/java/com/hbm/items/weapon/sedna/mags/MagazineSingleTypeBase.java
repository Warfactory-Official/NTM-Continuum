package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.tool.ItemAmmoBag;
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
     * <p>Ammo bags are searched after the player's ordinary inventory, matching the old slot-order rule.
     * An infinite bag supplies rounds without mutating its contents.
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

    /**
     * The round currently chambered, or the first this magazine accepts when nothing is.
     *
     * <p>Upstream stores an index into the global config list and a stack that has never been loaded
     * carries zero, which is a real config — one this magazine almost never accepts, so the answer is
     * its first accepted round. Here the stored type is a registry id and an unloaded stack carries no
     * id at all, which is the same state and has to give the same answer: without the fallback a gun
     * that has not been reloaded yet reports no ammo type, and the HUD next to its round counter is
     * empty until the first magazine goes in.
     */
    @Override
    public BulletConfig getType(ItemStack stack, Container inventory) {
        BulletConfig cfg = BulletConfigs.get(getMagType(stack, index));
        if (cfg != null && acceptedBullets.contains(cfg)) return cfg;
        return acceptedBullets.isEmpty() ? null : acceptedBullets.get(0);
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
        if (!IMagazine.shouldUseUpTrenchie(inventory) && getCapacity(stack) != 1) return;
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
        IMagazine.handleAmmoBag(inventory, this.getType(stack, inventory), amount);
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
            if (!slot.isEmpty()) {
                int used = loadFromStack(stack, slot, loadLimit);
                if (used > 0) inventory.removeItem(i, used);
                loadLimit -= used;
            }
            if (loadLimit <= 0) return;
            if (slot.getItem() instanceof com.hbm.items.tool.ItemAmmoBag bag) {
                ItemAmmoBag.InventoryAmmoBag contents = new ItemAmmoBag.InventoryAmmoBag(slot);
                for (int j = 0; j < contents.getSlots() && loadLimit > 0; j++) {
                    ItemStack nested = contents.getStackInSlot(j);
                    if (nested.isEmpty()) continue;
                    int used = loadFromStack(stack, nested, loadLimit, bag.isInfinite());
                    if (used > 0) {
                        if (!bag.isInfinite()) {
                            nested.shrink(used);
                            contents.setStackInSlot(j, nested);
                        }
                        loadLimit -= used;
                    }
                }
            }
        }
    }

    private int loadFromStack(ItemStack stack, ItemStack source, int loadLimit) {
        return loadFromStack(stack, source, loadLimit, false);
    }

    private int loadFromStack(ItemStack stack, ItemStack source, int loadLimit, boolean infinite) {
        BulletConfig config = getAmount(stack, null) == 0 ? acceptedConfig(source) : getType(stack, null);
        if (config == null || config.ammoItem() != source.getItem()) return 0;
        if (getAmount(stack, null) == 0) setType(stack, config);
        int wantsToLoad = (int) Math.ceil((double) (getCapacity(stack) - getAmount(stack, null))
                / config.ammoReloadCount);
        int available = infinite ? 9_999 : source.getCount();
        int toLoad = Math.min(Math.min(wantsToLoad, available), loadLimit);
        if (toLoad > 0) setAmount(stack, Math.min(getAmount(stack, null) + toLoad * config.ammoReloadCount, capacity));
        return toLoad;
    }

    private BulletConfig acceptedConfig(ItemStack source) {
        for (BulletConfig config : acceptedBullets) if (config.ammoItem() == source.getItem()) return config;
        return null;
    }

    /** The first round that could be loaded: the chambered type, or the first accepted one if empty. */
    public BulletConfig getFirstConfig(ItemStack stack, Container inventory) {
        if (inventory == null) return null;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) continue;
            BulletConfig direct = getAmount(stack, null) == 0 ? acceptedConfig(slot) : getType(stack, null);
            if (direct != null && direct.ammoItem() == slot.getItem()) return direct;
            if (slot.getItem() instanceof com.hbm.items.tool.ItemAmmoBag) {
                ItemAmmoBag.InventoryAmmoBag contents = new ItemAmmoBag.InventoryAmmoBag(slot);
                for (int j = 0; j < contents.getSlots(); j++) {
                    ItemStack nested = contents.getStackInSlot(j);
                    if (nested.isEmpty()) continue;
                    BulletConfig config = getAmount(stack, null) == 0 ? acceptedConfig(nested) : getType(stack, null);
                    if (config != null && config.ammoItem() == nested.getItem()) return config;
                }
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
    @Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, Math.max(amount, 0)); }

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

    @Override public ItemStack getCasing(ItemStack stack, Container inventory) {
        BulletConfig type = getType(stack, inventory);
        return type == null || type.casingStack() == null ? ItemStack.EMPTY : type.casingStack().copy();
    }
}
