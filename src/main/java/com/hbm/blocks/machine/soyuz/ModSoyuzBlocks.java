package com.hbm.blocks.machine.soyuz;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemEnums.SoyuzSkinType;
import com.hbm.items.ItemFamily;
import com.hbm.items.special.ItemSoyuz;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzStruct;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
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
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Drops nothing: placed by hand it forms the complex at once, and breaking it takes the complex apart. */
    public static final RegistryObject<SoyuzLauncher> soyuz_launcher = REG.block(
            "soyuz_launcher", SoyuzLauncher::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(Presets.shape(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/soyuz_launcher"))).noItem())
            .loot(Loot.nothing());

    public static final RegistryObject<SoyuzStruct> struct_soyuz_core = REG.block(
            "struct_soyuz_core", SoyuzStruct::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeAll().noItem())
            .loot(Loot.self());

    public static final RegistryObject<BlockItem> struct_soyuz_core_item =
            REG.blockItem(struct_soyuz_core)
                    .transform(Presets.shares("hbm:block/struct_soyuz_core"))
                    .tab(HbmTab.MACHINES);
    /** 1.7 registers the launcher's block, and so its item, on the missile tab: placed by hand it forms at once. */
    public static final RegistryObject<BlockItem> soyuz_launcher_item =
            REG.blockItem(soyuz_launcher)
                    .transform(Presets.shares("hbm:block/soyuz_launcher"))
                    .tab(HbmTab.MISSILES);

    /**
     * 1.7 {@code missile_soyuz} metadata 0..2 (its paint), one item each as in ntm-next ({@code missile_soyuz_0/1/2}), each with its 1.7 sprite.
     */
    public static final ItemFamily<SoyuzSkinType, ItemSoyuz> missile_soyuz = ItemFamily.of(
            SoyuzSkinType.class, type -> REG.item("missile_soyuz_" + type.ordinal(), p -> new ItemSoyuz(p, type),
                            p -> p.stacksTo(1))
                    .transform(Presets.sprite("hbm:items/missile_soyuz_" + switch (type) {
                        case NORMAL -> "normal";
                        case LUNAR -> "lunar";
                        case POST_WAR -> "post_war";
                    })));

    static {
        ModTabs.addStacksTo(HbmTab.MISSILES, missile_soyuz::stacks);
    }

    /** The seed block: an ordinary block whose entity watches for the complex to be finished around it. */
    public static final class SoyuzStruct extends CompatBlock implements TickingBlock {

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
