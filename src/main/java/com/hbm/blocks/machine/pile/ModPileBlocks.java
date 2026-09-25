package com.hbm.blocks.machine.pile;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;

import java.util.stream.IntStream;

/** The Chicago Pile's blocks: loose graphite, the assembled cells, and the three docking devices. */
public final class ModPileBlocks {

    private ModPileBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockPileBrick> pile_brick = BLOCKS.registerBlock(
            "pile_brick", BlockPileBrick::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().ignitedByLava());
    public static final RegistryObject<BlockPile> pile_block = BLOCKS.registerBlock(
            "pile_block", BlockPile::new,
            p -> p.strength(15.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<BlockPileDevice> pile_device = BLOCKS.registerBlock(
            "pile_device", BlockPileDevice::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<BlockItem> pile_brick_item =
            ITEMS.registerBlockItem("pile_brick", pile_brick);
    public static final RegistryObject<PileDeviceItem> pile_device_item =
            ITEMS.registerItem("pile_device", p -> new PileDeviceItem(pile_device.get(), p));
    public static final RegistryObject<ItemPileRodMK2> pile_rod =
            ITEMS.registerItem("pile_rod", ItemPileRodMK2::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.MACHINES, pile_brick_item);
        ModTabs.addStacksTo(HbmTab.MACHINES,
                () -> IntStream.range(0, 3).mapToObj(ModPileBlocks::pileDeviceItem).toList());
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> EnumPileRod.values().stream()
                .map(type -> pile_rod.get().of(type)).toList());
    }

    public static ItemStack pileDeviceItem(int variant) {
        return pile_device_item.get().of(variant);
    }

    /** Block item for the three pile devices; upstream picks between them with item damage. */
    public static final class PileDeviceItem extends CompatBlockItem {

        public PileDeviceItem(net.minecraft.world.level.block.Block block, Properties properties) {
            super(block, properties);
        }

        public ItemStack of(int variant) {
            ItemStack stack = new ItemStack(this);
            ModDataComponents.ITEM_VARIANT.set(stack, Math.floorMod(variant, 3));
            return stack;
        }

        public int variant(ItemStack stack) {
            return Math.floorMod(ModDataComponents.ITEM_VARIANT.getOr(stack, 0), 3);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable("block.hbm.pile_device_" + variant(stack));
        }
    }

    /** Class-load hook so the registers are populated during bootstrap. */
    public static void init() {}
}
