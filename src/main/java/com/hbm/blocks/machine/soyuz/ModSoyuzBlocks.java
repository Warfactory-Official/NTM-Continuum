package com.hbm.blocks.machine.soyuz;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemEnums.SoyuzSkinType;
import com.hbm.items.special.ItemSoyuz;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzStruct;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/** The satellite launch vehicle, its launch complex, and the seed block that raises one. */
public final class ModSoyuzBlocks {

    private ModSoyuzBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<SoyuzLauncher> soyuz_launcher = BLOCKS.registerBlock(
            "soyuz_launcher", SoyuzLauncher::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<SoyuzStruct> struct_soyuz_core = BLOCKS.registerBlock(
            "struct_soyuz_core", SoyuzStruct::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<BlockItem> struct_soyuz_core_item =
            ITEMS.registerBlockItem("struct_soyuz_core", struct_soyuz_core);

    public static final RegistryObject<ItemSoyuz> missile_soyuz =
            ITEMS.registerItem("missile_soyuz", ItemSoyuz::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.MACHINES, struct_soyuz_core_item);
        ModTabs.addStacksTo(HbmTab.MISSILES, () -> Arrays.stream(SoyuzSkinType.VALUES)
                .map(type -> missile_soyuz.get().of(type)).toList());
    }

    /** The seed block: an ordinary block whose entity watches for the complex to be finished around it. */
    public static final class SoyuzStruct extends dev.golem.core.world.CompatBlock implements dev.golem.core.registry.TickingBlock {

        public SoyuzStruct(Properties properties) {
            super(properties);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TileEntitySoyuzStruct(pos, state);
        }
    }

    /** Class-load hook so the registers are populated during bootstrap. */
    public static void init() {}
}
