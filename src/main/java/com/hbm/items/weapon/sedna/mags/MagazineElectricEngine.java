package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.Library;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.7: only reachable via the ENGINE weapon mod's magazine swap; ships with the drill. */
public class MagazineElectricEngine implements IMagazine<Object> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";
    public int index;
    public int capacity;

    public MagazineElectricEngine(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    public static int getMagCount(ItemStack stack, int index) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index);
    }
    public static void setMagCount(ItemStack stack, int index, int value) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value);
    }

    @Override public Object getType(ItemStack stack, Container inventory) { return null; }
    @Override public void setType(ItemStack stack, Object type) {}
    @Override public int getCapacity(ItemStack stack) { return capacity; }
    @Override public void useUpAmmo(ItemStack stack, Container inventory, int amount) {
        setAmount(stack, Math.max(getAmount(stack, inventory) - amount, 0));
    }
    @Override public int getAmount(ItemStack stack, Container inventory) { return getMagCount(stack, index); }
    @Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }
    @Override public boolean canReload(ItemStack stack, Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, Container inventory) {}
    @Override public void reloadAction(ItemStack stack, Container inventory) {}
    @Override public ItemStack getIconForHUD(ItemStack stack, Player player) {
        // ntm-next uses ModItems.BATTERY_CREATIVE; that registry entry is not in NTM-Continuum.
        return ItemStack.EMPTY;
    }
    @Override public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return Library.getShortNumber(getAmount(stack, player.getInventory())) + "/"
                + Library.getShortNumber(capacity) + "HE";
    }
    @Override public void setAmountBeforeReload(ItemStack stack, int amount) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount);
    }
    @Override public int getAmountBeforeReload(ItemStack stack) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index);
    }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount);
    }
    @Override public int getAmountAfterReload(ItemStack stack) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index);
    }
}
