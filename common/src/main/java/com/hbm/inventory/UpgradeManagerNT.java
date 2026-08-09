package com.hbm.inventory;

import java.util.HashMap;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.tileentity.IUpgradeInfoProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Per-machine upgrade tallier (1.12.2 {@code UpgradeManagerNT}). Machines that implement
 * {@link IUpgradeInfoProvider} own one instance and call {@link #checkSlots} each tick with their
 * upgrade slot range (<b>inclusive</b> on both ends, like the original's
 * {@code Arrays.copyOfRange(slots, start, end + 1)}); effects then read {@link #getLevel}.
 *
 * <p>Semantics ported verbatim: every {@link ItemMachineUpgrade} in the range whose type appears in
 * the owner's {@code getValidUpgrades()} adds its {@code tier} to that type's level, clamped to the
 * machine's maximum. The scan caches the last-seen slot contents and early-outs when unchanged.
 */
public class UpgradeManagerNT {

    public final BlockEntity owner;
    private ItemStack[] cachedSlots;
    private final HashMap<UpgradeType, Integer> upgrades = new HashMap<>();

    public UpgradeManagerNT(BlockEntity owner) {
        this.owner = owner;
    }

    /** Scans inventory slots {@code start..end} (inclusive) for upgrades. */
    public void checkSlots(NTMItemHandler inventory, int start, int end) {
        if (!(owner instanceof IUpgradeInfoProvider upgradable)) return;

        ItemStack[] slots = new ItemStack[end - start + 1];
        for (int i = 0; i < slots.length; i++) slots[i] = inventory.getStackInSlot(start + i);

        if (cachedSlots != null && slotsUnchanged(slots)) return;
        cachedSlots = new ItemStack[slots.length];
        for (int i = 0; i < slots.length; i++) cachedSlots[i] = slots[i].copy();

        upgrades.clear();
        HashMap<UpgradeType, Integer> valid = upgradable.getValidUpgrades();
        if (valid == null) return;

        for (ItemStack stack : slots) {
            if (stack.getItem() instanceof ItemMachineUpgrade item && valid.containsKey(item.type)) {
                int level = upgrades.getOrDefault(item.type, 0) + item.tier;
                upgrades.put(item.type, Math.min(level, valid.get(item.type)));
            }
        }
    }

    private boolean slotsUnchanged(ItemStack[] slots) {
        if (cachedSlots.length != slots.length) return false;
        for (int i = 0; i < slots.length; i++) {
            if (!ItemStack.matches(cachedSlots[i], slots[i])) return false;
        }
        return true;
    }

    public int getLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }
}
