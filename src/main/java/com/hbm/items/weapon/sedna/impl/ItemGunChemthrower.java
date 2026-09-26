package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;

/**
 * Fluid-backed chemthrower state; EntityChemical firing waits for the projectile owner.
 *
 * <p>The tank is the receiver's own {@link MagazineFluid}, read and written through its keys rather
 * than a private pair, so the fill/empty side and everything that goes through the magazine — the HUD
 * rail, the tooltip, the fire lambdas — agree on one number.
 */
public class ItemGunChemthrower extends ItemGunBaseNT implements IFillableItem {
    public static final int CONSUMPTION = 3;
    public static final int CAPACITY = 3_000;
    public static final int TRANSFER_SPEED = 50;
    private static final int MAG_INDEX = 0;

    public ItemGunChemthrower(Properties properties, WeaponQuality quality, com.hbm.items.weapon.sedna.GunConfig... configs) {
        super(properties, quality, configs);
    }

    public static final BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_CAN_FIRE =
            (stack, context) -> count(stack) >= CONSUMPTION;

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, context) -> {
        Receiver receiver = context.config.getReceivers(stack)[0];
        ItemGunBaseNT.playAnimation(context.getPlayer(), stack, GunAnimation.CYCLE, context.configIndex);
        var offset = receiver.getProjectileOffset(stack);
        EntityChemical chemical = new EntityChemical(context.entity.level(), context.entity,
                offset.z, offset.y, offset.x).setFluid(type(stack));
        context.entity.level().addFreshEntity(chemical);
        count(stack, count(stack) - CONSUMPTION);
    };

    private static FluidType type(ItemStack stack) { return MagazineFluid.getMagFluid(stack, MAG_INDEX); }
    private static int count(ItemStack stack) { return MagazineFluid.getMagCount(stack, MAG_INDEX); }
    private static void type(ItemStack stack, FluidType value) { MagazineFluid.setMagFluid(stack, MAG_INDEX, value); }
    private static void count(ItemStack stack, int value) { MagazineFluid.setMagCount(stack, MAG_INDEX, value); }

    private int capacity(ItemStack stack) {
        return getConfig(stack, 0).getReceivers(stack)[MAG_INDEX].getMagazine(stack).getCapacity(stack);
    }

    @Override public boolean acceptsFluid(FluidType fluid, ItemStack stack) { return count(stack) == 0 || type(stack) == fluid; }
    @Override public int tryFill(FluidType fluid, int amount, ItemStack stack) {
        if (!acceptsFluid(fluid, stack)) return amount;
        if (count(stack) == 0) type(stack, fluid);
        int moved = Math.min(Math.min(amount, TRANSFER_SPEED), capacity(stack) - count(stack));
        count(stack, count(stack) + moved);
        return amount - moved;
    }
    @Override public boolean providesFluid(FluidType fluid, ItemStack stack) { return type(stack) == fluid; }
    @Override public int tryEmpty(FluidType fluid, int amount, ItemStack stack) {
        int moved = Math.min(Math.min(amount, TRANSFER_SPEED), count(stack));
        count(stack, count(stack) - moved);
        return moved;
    }
    @Override public FluidType getFirstFluidType(ItemStack stack) { return type(stack); }
    @Override public int getFill(ItemStack stack) { return count(stack); }
}
