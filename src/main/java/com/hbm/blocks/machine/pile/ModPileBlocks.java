package com.hbm.blocks.machine.pile;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModDataComponents;
import com.hbm.registration.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
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
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** A per-face cube: pile_brick_top on both ends, the brick front/back and its side on east and west. */
    public static final RegistryObject<BlockPileBrick> pile_brick = REG.block(
            "pile_brick", BlockPileBrick::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().ignitedByLava())
            .transform(Presets.shape(Models.parented("minecraft:block/cube", Slots
                    .of("up", "hbm:blocks/pile_brick_top", "down", "hbm:blocks/pile_brick_top")
                    .and("north", "hbm:blocks/pile_brick").and("south", "hbm:blocks/pile_brick")
                    .and("west", "hbm:blocks/pile_brick_side").and("east", "hbm:blocks/pile_brick_side")
                    .and("particle", "hbm:blocks/pile_brick"))).noItem())
            .loot(Loot.self());
    /**
     * The assembled cell dispatches its META (solid/channel/edge, core, fuel in/out, air in/out, control) onto
     * five bottom-top cubes. It has no item; its item definition (pointing at the block model) is kept as shipped.
     */
    public static final RegistryObject<BlockPile> pile_block = REG.block(
            "pile_block", BlockPile::new,
            p -> p.strength(15.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .model(d -> d.models(Models.group()
                            .and("", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block", "hbm:blocks/pile_block_top"))
                            .and("_core", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block_core", "hbm:blocks/pile_block_top"))
                            .and("_input", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block_input", "hbm:blocks/pile_block_top"))
                            .and("_output", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block_output", "hbm:blocks/pile_block_top"))
                            .and("_control", Models.cubeBottomTop("hbm:blocks/pile_block_control_top", "hbm:blocks/pile_block", "hbm:blocks/pile_block_control_top")))
                    .states(Variants.select("meta", "0", "", "1", "_core", "2", "", "3", "_input", "4", "_output",
                            "5", "_input", "6", "_output", "7", "_control", "8", "")))
            .transform(Presets.shares("hbm:block/pile_block"))
            .loot(Loot.nothing());
    /** The three docking devices: one invisible block (renderer-drawn) across twelve meta values; item is OBJ. */
    public static final RegistryObject<BlockPileDevice> pile_device = REG.block(
            "pile_device", BlockPileDevice::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(Presets.statesOnly(Variants.uniformRange("meta", 0, 11)))
            .loot(Loot.self());

    public static final RegistryObject<BlockItem> pile_brick_item =
            REG.blockItem(pile_brick)
                    .transform(Presets.shares("hbm:block/pile_brick"))
                    .tab(HbmTab.MACHINES);
    public static final RegistryObject<PileDeviceItem> pile_device_item =
            REG.item("pile_device", (net.minecraft.world.item.Item.Properties p) -> new PileDeviceItem(pile_device.get(), p));
    public static final RegistryObject<ItemPileRodMK2> pile_rod =
            REG.item("pile_rod", ItemPileRodMK2::new, p -> p.stacksTo(1))
                    .icon(d -> d.models(Models.group()
                                    .and("", Models.generated("hbm:items/pile_rod_ra226be"))
                                    .and("_po210be", Models.generated("hbm:items/pile_rod_po210be"))
                                    .and("_zr", Models.generated("hbm:items/pile_rod_zr"))
                                    .and("_nu", Models.generated("hbm:items/pile_rod_nu"))
                                    // 1.7 pile_rod.pu239: the plain _pu239 name is the deprecated old-style rod's model and texture
                                    .and("_mk2_pu239", Models.generated("hbm:items/pile_rod_mk2_pu239"))
                                    .and("_rgp", Models.generated("hbm:items/pile_rod_rgp"))
                                    .and("_waste", Models.generated("hbm:items/pile_rod_waste"))
                                    .and("_thorium", Models.generated("hbm:items/pile_rod_thorium"))
                                    .and("_thorium_fuel", Models.generated("hbm:items/pile_rod_thorium_fuel")))
                            .selects(Constants.MOD_ID + ":item_variant"));

    static {
        ModTabs.addStacksTo(HbmTab.MACHINES,
                () -> IntStream.range(0, 3).mapToObj(ModPileBlocks::pileDeviceItem).toList());
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> java.util.Arrays.stream(EnumPileRod.VALUES)
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
