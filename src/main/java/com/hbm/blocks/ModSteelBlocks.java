package com.hbm.blocks;

import dev.golem.core.dejson.Variants;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockGrate;
import com.hbm.blocks.generic.BlockScaffold;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** Hand-registered stateful steel construction blocks from the old decorative block set. */
public final class ModSteelBlocks {

    private ModSteelBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);
    public static final List<RegistryObject<? extends Block>> OBJ_BLOCKS = new ArrayList<>();

    /**
     * 1.7 {@code ModSoundTypes.grate}. A holder, not a field of this class: a {@code SoundType} only exists in a
     * running game, and this class also initialises in DejsonMain's plain JVM to record its declarations.
     */
    private static final class Sounds {
        static final SoundType GRATE_SOUND = new SoundType(
                0.5F, 1.0F,
                SoundEvents.STONE_BREAK, ModSounds.METAL_BLOCK_EVENT, SoundEvents.STONE_PLACE,
                SoundEvents.STONE_HIT, SoundEvents.STONE_FALL);
    }

    /** The ten authored grate heights, each used for two {@code height} states. */
    private static final String[] GRATE_HEIGHTS = {
            "0", "0", "1", "1", "2", "2", "3", "3", "4", "4",
            "5", "5", "6", "6", "7", "7", "8", "8", "9", "9",
    };

    public static final RegistryObject<BlockGrate> steel_grate = REG.block("steel_grate", BlockGrate::new,
            p -> p.strength(2.0F, 5.0F).sound(Sounds.GRATE_SOUND).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false))
            .model(d -> d.statesOnly().states(Variants.select("height", GRATE_HEIGHTS)))
            .transform(Presets.sprite("hbm:blocks/grate_top"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<BlockScaffold> steel_scaffold = REG.block("steel_scaffold", BlockScaffold::new,
            p -> p.strength(5.0F, 15.0F).sound(SoundType.STONE).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false))
            .model(d -> d.statesOnly().states(Variants.product(Variants.uniform("orient", "horizontal_north_south", "horizontal_east_west", "vertical_north_south", "vertical_east_west"), Variants.uniform("variant", "steel", "red", "white", "yellow"))))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    static {
        Reg.Handle<BlockItem> grateItem = REG.blockItem(steel_grate, Compat::blockDescription);
        ModTabs.addTo(HbmTab.BLOCKS, grateItem);

        Reg.Handle<BlockItem> scaffoldItem = REG.blockItem(steel_scaffold,
                Compat::blockDescription);
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, BlockScaffold.Variant.values().length)
                .mapToObj(variant -> {
                    ItemStack stack = new ItemStack(scaffoldItem.get());
                    ModDataComponents.ITEM_VARIANT.set(stack, variant);
                    return stack;
                }).toList());

        OBJ_BLOCKS.add(steel_scaffold);
    }

    public static void init() {}
}
