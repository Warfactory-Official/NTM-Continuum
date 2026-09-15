package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.XFactoryDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 impl/ItemGunDrill: the fuel-fed mining tool. */
public class ItemGunDrill extends ItemGunBaseNT implements IFillableItem, IBatteryItem {

    public ItemGunDrill(Properties properties, WeaponQuality quality, GunConfig... cfg) {
        super(properties, quality, cfg);
    }

    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        int level = XFactoryDrill.getModdableHarvestLevel(stack, 2);
        return Compat.isCorrectToolForDrops(state, level);
    }

    /** Source canHarvestBlock: the drill bypasses missing/unknown block harvest tags. */
    public boolean canHarvestBlock(BlockState state) {
        return true;
    }

    private MagazineLiquidEngine engine(ItemStack stack) {
        IMagazine mag = this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        return mag instanceof MagazineLiquidEngine e ? e : null;
    }

    private MagazineElectricEngine electricEngine(ItemStack stack) {
        IMagazine mag = this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        return mag instanceof MagazineElectricEngine e ? e : null;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        if (engine == null) return false;
        for (FluidType acc : engine.accepted()) if (type == acc) return true;
        return false;
    }

    /** Compatibility marker retained from ntm-next; no current shared caller consumes it. */
    public boolean machineFillable() { return true; }

    public int capacity(ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        return engine == null ? 0 : engine.getCapacity(stack);
    }

    public FluidStack getContent(ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        if (engine == null) return null;
        int fill = engine.getAmount(stack, null);
        if (fill <= 0) return null;
        return new FluidStack(engine.getType(stack, null), fill, 0);
    }

    public void setContent(ItemStack stack, FluidStack content) {
        MagazineLiquidEngine engine = engine(stack);
        if (engine != null && content != null) engine.setAmount(stack, Math.min(content.fill, capacity(stack)));
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        // Keep the original contract: the engine decides the accepted type while filling; a
        // non-liquid replacement reports no transfer rather than returning the offered amount.
        if (engine == null) return 0;
        int fill = engine.getAmount(stack, null);
        int toFill = Math.min(amount, 50);
        toFill = Math.min(toFill, engine.getCapacity(stack) - fill);
        engine.setAmount(stack, fill + toFill);
        return amount - toFill;
    }

    /** 1.7 tryEmpty leaves the drill's fuel amount untouched and reports the requested amount. */
    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) { return amount; }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) { return false; }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {
        MagazineElectricEngine engine = electricEngine(stack);
        if (engine != null) engine.setAmount(stack, Math.min(engine.capacity,
                engine.getAmount(stack, null) + (int) amount));
    }

    @Override
    public void setCharge(ItemStack stack, long amount) {
        MagazineElectricEngine engine = electricEngine(stack);
        if (engine != null) engine.setAmount(stack, (int) amount);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
        MagazineElectricEngine engine = electricEngine(stack);
        if (engine != null) engine.setAmount(stack, Math.max(0,
                engine.getAmount(stack, null) - (int) amount));
    }

    @Override
    public long getCharge(ItemStack stack) {
        MagazineElectricEngine engine = electricEngine(stack);
        return engine == null ? 0L : engine.getAmount(stack, null);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        MagazineElectricEngine engine = electricEngine(stack);
        return engine == null ? 0L : engine.getCapacity(stack);
    }

    @Override public long getChargeRate(ItemStack stack) { return 50_000L; }
    @Override public long getDischargeRate(ItemStack stack) { return 0L; }

    @Override public FluidType getFirstFluidType(ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        return engine == null ? Fluids.NONE : engine.getType(stack, null);
    }

    @Override public int getFill(ItemStack stack) {
        MagazineLiquidEngine engine = engine(stack);
        return engine == null ? 0 : engine.getAmount(stack, null);
    }
}
