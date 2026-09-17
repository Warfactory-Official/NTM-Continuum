package com.hbm.entity.cart;

import com.hbm.entity.ModNtmEntities;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import com.hbm.items.tool.ItemModMinecart.EnumMinecart;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code entity/cart/EntityMinecartSemtex}: carries semtex; 1.7 drew the top and sides from separate skins.
 */
public class EntityMinecartSemtex extends EntityMinecartNTM {

    public EntityMinecartSemtex(EntityType<? extends EntityMinecartSemtex> type, Level level) {
        super(type, level);
    }

    public EntityMinecartSemtex(Level level, double x, double y, double z, EnumCartBase base) {
        super(ModNtmEntities.CART_SEMTEX.get(), level, x, y, z, base);
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(getBase(), EnumMinecart.SEMTEX);
    }
}
