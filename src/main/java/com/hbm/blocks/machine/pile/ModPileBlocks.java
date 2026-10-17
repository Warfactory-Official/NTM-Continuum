package com.hbm.blocks.machine.pile;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemFamily;
import com.hbm.items.LegacyMeta;
import com.hbm.items.ModDataComponents;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
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
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
            .loot(Loot.self()).tag(BlockTag.PICKAXE);
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
            .loot(Loot.nothing()).tag(BlockTag.PICKAXE);
    /** The three docking devices: one invisible block (renderer-drawn) across twelve meta values; item is OBJ. */
    public static final RegistryObject<BlockPileDevice> pile_device = REG.block(
            "pile_device", BlockPileDevice::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(Presets.statesOnly(Variants.uniformRange("meta", 0, 11)))
            // the drop is the device's own item (pile_loader/_vent/_control, BlockPileDevice.getDrops)
            .loot(Loot.nothing()).tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockItem> pile_brick_item =
            REG.blockItem(pile_brick)
                    .transform(Presets.shares("hbm:block/pile_brick"))
                    .tab(HbmTab.MACHINES);
    /**
     * 1.7 pile_device metadata 0..2 (loader, vent, control rod), one block item each with ntm-next's ids
     * ({@code pile_loader}, {@code pile_vent}, {@code pile_control}). The block keeps its one id and
     * {@code meta} state; the metadata-0 item is registered last so it stays the block's own item.
     */
    public static final List<RegistryObject<PileDeviceItem>> pile_device_items = pileDeviceItems();

    private static List<RegistryObject<PileDeviceItem>> pileDeviceItems() {
        String[] ids = {"pile_loader", "pile_vent", "pile_control"};
        @SuppressWarnings("unchecked")
        RegistryObject<PileDeviceItem>[] items = new RegistryObject[ids.length];
        for (int k = 1; k <= ids.length; k++) {
            int variant = k % ids.length;
            items[variant] = REG.item(ids[variant],
                    (Item.Properties p) -> new PileDeviceItem(pile_device.get(), p, variant));
        }
        return List.of(items);
    }

    /** 1.7 {@code pile_rod} metadata, one item per rod as ntm-next registers them ({@code pile_rod_mk2_<type>}). */
    public static final ItemFamily<EnumPileRod, ItemPileRodMK2> pile_rod = ItemFamily.of(
            EnumPileRod.class, type -> REG.item("pile_rod_mk2_" + type.name().toLowerCase(Locale.ROOT),
                            p -> new ItemPileRodMK2(p, type), p -> p.stacksTo(1))
                    .transform(Presets.sprite("hbm:items/" + switch (type) {
                        case RA226BE -> "pile_rod_ra226be";
                        case PO210BE -> "pile_rod_po210be";
                        case ZR -> "pile_rod_zr";
                        case NU -> "pile_rod_nu";
                        case PU239 -> "pile_rod_mk2_pu239";
                        case RGP -> "pile_rod_rgp";
                        case WASTE -> "pile_rod_waste";
                        case THORIUM -> "pile_rod_thorium";
                        case THORIUM_FUEL -> "pile_rod_thorium_fuel";
                    })));

    static {
        ModTabs.addStacksTo(HbmTab.MACHINES,
                () -> IntStream.range(0, 3).mapToObj(ModPileBlocks::pileDeviceItem).toList());
        ModTabs.addStacksTo(HbmTab.CONTROL, pile_rod::stacks);
    }

    public static ItemStack pileDeviceItem(int variant) {
        return new ItemStack(pile_device_items.get(Math.floorMod(variant, pile_device_items.size())).get());
    }

    /** Block item of one of the three pile devices (1.7 item damage = {@link #variant()}). */
    public static final class PileDeviceItem extends CompatBlockItem implements LegacyMeta {

        private final int variant;

        public PileDeviceItem(Block block, Properties properties, int variant) {
            super(block, properties);
            this.variant = variant;
        }

        public int variant() {
            return variant;
        }

        @Override
        public int legacyMeta() {
            return variant;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable("block.hbm.pile_device_" + variant);
        }
    }

    /** Class-load hook so the registers are populated during bootstrap. */
    public static void init() {}
}
