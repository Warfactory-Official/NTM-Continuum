package com.hbm.items.weapon.sedna.impl;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.GunAnimation;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;

/** Fluid-backed chemthrower state; EntityChemical firing waits for the projectile owner. */
public class ItemGunChemthrower extends ItemGunBaseNT implements IFillableItem {
    public static final int CONSUMPTION = 3;
    public static final int TRANSFER_SPEED = 50;
    private static final String KEY_TYPE = "chemtype";
    private static final String KEY_COUNT = "chemcount";

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
                offset.z, offset.y, offset.x).setFluid(Fluids.fromID(type(stack)));
        context.entity.level().addFreshEntity(chemical);
        count(stack, count(stack) - CONSUMPTION);
    };

    private static int type(ItemStack stack) { return getValueInt(stack, KEY_TYPE); }
    private static int count(ItemStack stack) { return getValueInt(stack, KEY_COUNT); }
    private static void type(ItemStack stack, int value) { setValueInt(stack, KEY_TYPE, value); }
    private static void count(ItemStack stack, int value) { setValueInt(stack, KEY_COUNT, value); }

    @Override public boolean acceptsFluid(FluidType fluid, ItemStack stack) { return count(stack) == 0 || Fluids.fromID(type(stack)) == fluid; }
    @Override public int tryFill(FluidType fluid, int amount, ItemStack stack) {
        if (!acceptsFluid(fluid, stack)) return amount;
        if (count(stack) == 0) type(stack, fluid.getID());
        int moved = Math.min(Math.min(amount, TRANSFER_SPEED), 3000 - count(stack));
        count(stack, count(stack) + moved);
        return amount - moved;
    }
    @Override public boolean providesFluid(FluidType fluid, ItemStack stack) { return Fluids.fromID(type(stack)) == fluid; }
    @Override public int tryEmpty(FluidType fluid, int amount, ItemStack stack) {
        int moved = Math.min(Math.min(amount, TRANSFER_SPEED), count(stack));
        count(stack, count(stack) - moved);
        return moved;
    }
    @Override public FluidType getFirstFluidType(ItemStack stack) { return Fluids.fromID(type(stack)); }
    @Override public int getFill(ItemStack stack) { return count(stack); }
}
