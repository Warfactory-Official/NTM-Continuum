package com.hbm.items.special;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemAutogen extends Item {

    public final MaterialShapes shape;
    public final NTMMaterial material;

    public ItemAutogen(Properties properties, MaterialShapes shape, NTMMaterial material) {
        super(properties);
        this.shape = shape;
        this.material = material;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(material.getDisplayName() + " " + shape.displayName);
    }
}
