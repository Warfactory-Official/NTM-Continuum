package com.hbm.items.weapon.sedna.mags;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A fluid-backed magazine. Fluid transfer is performed by the owning weapon/tool, as upstream did. */
public class MagazineFluid implements IMagazine<FluidType> {
    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_TYPE = "magtype";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";
    public int index;
    public int capacity;
    public FluidType[] acceptedTypes;

    public MagazineFluid(int index, int capacity, FluidType... acceptedTypes) {
        this.index = index;
        this.capacity = capacity;
        this.acceptedTypes = acceptedTypes;
    }
    @Override public FluidType getType(ItemStack stack, Container inventory) { return Fluids.fromID(getMagType(stack, index)); }
    @Override public void setType(ItemStack stack, FluidType type) {
        if (type != null) setMagType(stack, index, type.getID());
    }
    @Override public int getCapacity(ItemStack stack) { return capacity; }
    @Override public int getAmount(ItemStack stack, Container inventory) { return getMagCount(stack, index); }
    @Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }
    @Override public void useUpAmmo(ItemStack stack, Container inventory, int amount) { setAmount(stack, getAmount(stack, inventory) - amount); }
    @Override public boolean canReload(ItemStack stack, Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, Container inventory) {}
    @Override public void reloadAction(ItemStack stack, Container inventory) {}
    @Override public ItemStack getIconForHUD(ItemStack stack, Player player) {
        FluidType type = getType(stack, player.getInventory());
        return type == null || type == Fluids.NONE ? ItemStack.EMPTY : ItemFluidId.make(type);
    }
    @Override public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return getAmount(stack, player.getInventory()) + "mB";
    }
    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
    @Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
    @Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }
    public static int getMagType(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE + index); }
    public static void setMagType(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE + index, value); }
    public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
    public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}
