package com.hbm.items.tool;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.cart.EntityMinecartCrate;
import com.hbm.entity.cart.EntityMinecartDestroyer;
import com.hbm.entity.cart.EntityMinecartNTM;
import com.hbm.entity.cart.EntityMinecartOre;
import com.hbm.entity.cart.EntityMinecartPowder;
import com.hbm.entity.cart.EntityMinecartSemtex;
import com.hbm.items.ModDataComponents;
import com.mojang.serialization.Codec;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * 1.7 {@code items/tool/ItemModMinecart}: the assembled cart item. 1.7 keyed the cart TYPE off item
 * metadata and the chassis off an NBT int; here one item per type, the chassis on {@code hbm:cart_base}.
 */
public class ItemModMinecart extends Item {

    public final EnumMinecart type;

    public ItemModMinecart(Properties props, EnumMinecart type) {
        super(props);
        this.type = type;
    }

    public static ItemStack createCartItem(EnumCartBase base, EnumMinecart cart) {
        ItemStack stack = ModEntityItems.cart.stack(cart);
        ModDataComponents.CART_BASE.set(stack, base);
        return stack;
    }

    public static EnumMinecart getCartType(ItemStack stack) {
        return ((ItemModMinecart) stack.getItem()).type;
    }

    public static EnumCartBase getBaseType(ItemStack stack) {
        return ModDataComponents.CART_BASE.getOr(stack, EnumCartBase.VANILLA);
    }

    /**
     * 1.7 onItemUse: placeable on any rail, consuming one from the stack.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!level.getBlockState(pos).is(BlockTags.RAILS)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide()) {
            ItemStack stack = context.getItemInHand();
            Entity cart = createMinecart(level, pos.getX() + 0.5D,
                    pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);

            Component customName = ItemStacks.customName(stack);
            if (customName != null) cart.setCustomName(customName);

            level.addFreshEntity(cart);
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    public static Entity createMinecart(Level level, double x, double y, double z, ItemStack stack) {
        EnumCartBase base = getBaseType(stack);
        return switch (getCartType(stack)) {
            case CRATE -> new EntityMinecartCrate(level, x, y, z, base, stack);
            case DESTROYER -> new EntityMinecartDestroyer(level, x, y, z, base);
            case POWDER -> new EntityMinecartPowder(level, x, y, z, base);
            case SEMTEX -> new EntityMinecartSemtex(level, x, y, z, base);
            case EMPTY -> new EntityMinecartOre(level, x, y, z, base);
        };
    }

    /** 1.7 ItemModMinecart.EnumCartBase - the chassis material. */
    public enum EnumCartBase implements StringRepresentable {
        VANILLA, WOOD, STEEL, PAINTED;

        public static final EnumCartBase[] VALUES = values();
        public static final Codec<EnumCartBase> CODEC = StringRepresentable.fromEnum(EnumCartBase::values);
        public final String id = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** 1.7 ItemModMinecart.EnumMinecart - the cart's payload, plus which chassis it accepts. */
    public enum EnumMinecart {
        EMPTY(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        CRATE(EnumCartBase.VANILLA),
        DESTROYER(EnumCartBase.STEEL, EnumCartBase.PAINTED),
        POWDER(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED),
        SEMTEX(EnumCartBase.WOOD, EnumCartBase.STEEL, EnumCartBase.PAINTED);

        public static final EnumMinecart[] VALUES = values();

        public final int types;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumMinecart(EnumCartBase... accepted) {
            int mask = 0;
            for (EnumCartBase type : accepted) mask |= 1 << type.ordinal();
            this.types = mask;
        }

        public boolean supportsBase(EnumCartBase type) {
            return (this.types & (1 << type.ordinal())) > 0;
        }
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return type != EnumMinecart.CRATE;
    }
}
