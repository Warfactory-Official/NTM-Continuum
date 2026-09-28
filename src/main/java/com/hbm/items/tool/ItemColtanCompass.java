package com.hbm.items.tool;

import com.hbm.items.ModUtilityToolItems;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.Random;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemColtanCompass}, the "Coltass": points towards the world's coltan deposit, a gaussian draw
 * (sigma 1500 blocks) from {@code new Random(seed + 5)}, stored on the stack as {@code colX}/{@code colZ} on its
 * first server tick. The client half - the distance readout on the info HUD and the needle, which in 1.7 was a
 * {@code TextureCompass} subclass and here is the {@code hbm:coltass} item-model property picking one of the
 * 32 {@code coltass} frames - is {@code com.hbm.client.UtilityToolClientRegistrations}.
 */
public class ItemColtanCompass extends CompatItem {

    public ItemColtanCompass(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Points towards the coltan deposit."));
        list.add(Component.literal("The deposit is a large area where coltan ore spawns like standard ore,"));
        list.add(Component.literal("it's not one large blob of ore on that exact location."));
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot,
                                       boolean held) {
        if (!ModUtilityToolItems.COLTASS.has(stack)) {
            Random colRand = new Random(world.getSeed() + 5);
            int colX = (int) (colRand.nextGaussian() * 1500);
            int colZ = (int) (colRand.nextGaussian() * 1500);
            CompoundTag tag = new CompoundTag();
            tag.putInt("colX", colX);
            tag.putInt("colZ", colZ);
            ModUtilityToolItems.COLTASS.set(stack, tag);
        }
    }

    /** The stored deposit, or null before the first server tick. */
    public static int[] target(ItemStack stack) {
        CompoundTag tag = ModUtilityToolItems.COLTASS.getOr(stack, null);
        if (tag == null) return null;
        return new int[]{Tags.getIntOr(tag, "colX", 0), Tags.getIntOr(tag, "colZ", 0)};
    }
}
