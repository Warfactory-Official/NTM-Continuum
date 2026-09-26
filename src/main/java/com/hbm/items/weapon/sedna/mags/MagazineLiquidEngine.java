package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.api.fluid.FluidType;
import java.util.function.Supplier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MagazineLiquidEngine implements IMagazine<FluidType> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";
    private final Supplier<FluidType[]> acceptedSupplier;
    public int index;
    public int capacity;
    private FluidType[] acceptedTypes;

    public MagazineLiquidEngine(int index, int capacity, Supplier<FluidType[]> acceptedTypes) {
        this.index = index;
        this.capacity = capacity;
        this.acceptedSupplier = acceptedTypes;
    }

    public static int getMagCount(ItemStack stack, int index) {
        return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index);
    }

    public static void setMagCount(ItemStack stack, int index, int value) {
        ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value);
    }

    public FluidType[] accepted() {
        if (acceptedTypes == null) acceptedTypes = acceptedSupplier.get();
        return acceptedTypes;
    }

    @Override public FluidType getType(ItemStack stack, Container inventory) { return accepted()[0]; }
    @Override public void setType(ItemStack stack, FluidType type) {}
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
        return ItemFluidIcon.make(getType(stack, player.getInventory()));
    }
    @Override public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return getAmount(stack, player.getInventory()) + "/" + capacity + "mB";
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
