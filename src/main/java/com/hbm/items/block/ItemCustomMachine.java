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
 * damage value ({@code index + 100}); the port names the configuration instead ({@link ModDataComponents#CUSTOM_MACHINE},
 * its {@code unlocalizedName}), so no number is saved.
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

    /** The controller for configuration {@code index} of {@code niceList} (1.7's {@code getSubItems} order). */
    public ItemStack of(int index) {
        ItemStack stack = new ItemStack(this);
        if (index >= 0 && index < CustomMachineConfigJSON.niceList.size()) {
            ModDataComponents.CUSTOM_MACHINE.set(stack, CustomMachineConfigJSON.niceList.get(index).unlocalizedName);
        }
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        MachineConfiguration conf = CustomMachineConfigJSON.byName(ModDataComponents.CUSTOM_MACHINE.getOr(stack, null));
        if (conf != null) return Component.literal(conf.displayName());
        return Component.literal("INVALID MACHINE CONTROLLER");
    }
}
