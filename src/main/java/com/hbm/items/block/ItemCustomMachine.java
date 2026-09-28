package com.hbm.items.block;

import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemCustomMachine}: one controller item per configured machine. 1.7 kept the machine's index in the
 * damage value ({@code index + 100}); the port keeps the same number in {@link ModDataComponents#ITEM_VARIANT}.
 */
public class ItemCustomMachine extends CompatBlockItem {

    public ItemCustomMachine(Block block, Properties properties) {
        super(block, properties);
    }

    /** 1.7 {@code getSubItems}: one stack per configured machine, in config order. */
    public List<ItemStack> subItems() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < CustomMachineConfigJSON.niceList.size(); i++) list.add(of(i));
        return list;
    }

    public ItemStack of(int index) {
        ItemStack stack = new ItemStack(this);
        ModDataComponents.ITEM_VARIANT.set(stack, index + 100);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        MachineConfiguration conf = CustomMachineConfigJSON.byVariant(ModDataComponents.ITEM_VARIANT.getOr(stack, 0));
        if (conf != null) return Component.literal(conf.displayName());
        return Component.literal("INVALID MACHINE CONTROLLER");
    }
}
