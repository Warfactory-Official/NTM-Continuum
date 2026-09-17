package com.hbm.entity.cart;

import com.hbm.entity.ModNtmEntities;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import com.hbm.items.tool.ItemModMinecart.EnumMinecart;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code entity/cart/EntityMinecartPowder}: carries a load of powder; 1.7 drew the heap as a separate model part.
 */
public class EntityMinecartPowder extends EntityMinecartNTM {

    public EntityMinecartPowder(EntityType<? extends EntityMinecartPowder> type, Level level) {
        super(type, level);
    }

    public EntityMinecartPowder(Level level, double x, double y, double z, EnumCartBase base) {
        super(ModNtmEntities.CART_POWDER.get(), level, x, y, z, base);
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(getBase(), EnumMinecart.POWDER);
    }
}
