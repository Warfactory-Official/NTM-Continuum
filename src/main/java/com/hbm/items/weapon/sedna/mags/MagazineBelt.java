package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A belt-fed receiver: rounds are consumed directly from the inventory or an ammo bag. */
public class MagazineBelt implements IMagazine<BulletConfig> {
    public static final String KEY_MAG_TYPE = "magtype";
    public final List<BulletConfig> acceptedBullets = new ArrayList<>();

    public MagazineBelt addConfigs(BulletConfig... configs) {
        Collections.addAll(acceptedBullets, configs);
        return this;
    }

    @Override public BulletConfig getType(ItemStack stack, Container inventory) {
        BulletConfig config = getFirstConfig(stack, inventory);
        if (config != null && !config.id.toString().equals(ItemGunBaseNT.getValueString(stack, KEY_MAG_TYPE))) {
            setType(stack, config);
        }
        return config;
    }
    @Override public void setType(ItemStack stack, BulletConfig type) {
        if (type != null && type.id != null) ItemGunBaseNT.setValueString(stack, KEY_MAG_TYPE, type.id.toString());
    }
    @Override public int getCapacity(ItemStack stack) { return 0; }
    @Override public void setAmount(ItemStack stack, int amount) {}

    @Override public int getAmount(ItemStack stack, Container inventory) {
        if (inventory == null) return 1;
        BulletConfig type = getFirstConfig(stack, inventory);
        if (type == null) return 0;
        int amount = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (type.matchesAmmo(slot)) amount += slot.getCount();
            if (slot.getItem() instanceof ItemAmmoBag bag) {
                ItemAmmoBag.InventoryAmmoBag contents = new ItemAmmoBag.InventoryAmmoBag(slot);
                for (int j = 0; j < contents.getSlots(); j++) {
                    if (type.matchesAmmo(contents.getStackInSlot(j))) {
                        if (bag.isInfinite()) return 9_999;
                        amount += contents.getStackInSlot(j).getCount();
                    }
                }
            }
        }
        return amount;
    }

    @Override public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
        if (inventory == null || !IMagazine.shouldUseUpTrenchie(inventory)) return;
        BulletConfig type = getFirstConfig(stack, inventory);
        if (type == null) return;
        for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
            ItemStack slot = inventory.getItem(i);
            if (type.matchesAmmo(slot)) {
                int used = Math.min(amount, slot.getCount());
                inventory.removeItem(i, used);
                IMagazine.handleAmmoBag(inventory, type, used);
                amount -= used;
            }
            if (slot.getItem() instanceof ItemAmmoBag bag) {
                ItemAmmoBag.InventoryAmmoBag contents = new ItemAmmoBag.InventoryAmmoBag(slot);
                for (int j = 0; j < contents.getSlots() && amount > 0; j++) {
                    ItemStack nested = contents.getStackInSlot(j);
                    if (!type.matchesAmmo(nested)) continue;
                    int used = Math.min(amount, bag.isInfinite() ? 9_999 : nested.getCount());
                    if (!bag.isInfinite()) contents.extractItem(j, used, false);
                    IMagazine.handleAmmoBag(inventory, type, used);
                    amount -= used;
                }
            }
        }
    }

    @Override public boolean canReload(ItemStack stack, Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, Container inventory) {}
    @Override public void reloadAction(ItemStack stack, Container inventory) {}
    @Override public ItemStack getIconForHUD(ItemStack stack, Player player) {
        BulletConfig type = getFirstConfig(stack, player.getInventory());
        return type == null ? ItemStack.EMPTY : type.toStack();
    }
    @Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return "x" + getAmount(stack, player.getInventory()); }
    @Override public void setAmountBeforeReload(ItemStack stack, int amount) {}
    @Override public int getAmountBeforeReload(ItemStack stack) { return 0; }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) {}
    @Override public int getAmountAfterReload(ItemStack stack) { return 0; }

    public BulletConfig getFirstConfig(ItemStack stack, Container inventory) {
        if (inventory == null) {
            BulletConfig saved = BulletConfigs.get(ItemGunBaseNT.getValueString(stack, KEY_MAG_TYPE));
            return acceptedBullets.contains(saved) ? saved : acceptedBullets.isEmpty() ? null : acceptedBullets.get(0);
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            for (BulletConfig config : acceptedBullets) {
                if (config.matchesAmmo(slot)) return config;
                if (slot.getItem() instanceof ItemAmmoBag) {
                    ItemAmmoBag.InventoryAmmoBag contents = new ItemAmmoBag.InventoryAmmoBag(slot);
                    for (int j = 0; j < contents.getSlots(); j++) if (config.matchesAmmo(contents.getStackInSlot(j))) return config;
                }
            }
        }
        return getFirstConfig(stack, null);
    }

    @Override public ItemStack getCasing(ItemStack stack, Container inventory) {
        BulletConfig type = getFirstConfig(stack, inventory);
        return type == null || type.casingStack() == null ? ItemStack.EMPTY : type.casingStack().copy();
    }

    public static String getMagType(ItemStack stack) { return ItemGunBaseNT.getValueString(stack, KEY_MAG_TYPE); }
    public static void setMagType(ItemStack stack, String value) { ItemGunBaseNT.setValueString(stack, KEY_MAG_TYPE, value); }
}
