package com.hbm.blocks;

import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockBeaconable;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The ten plain {@code BlockBeaconable} storage blocks from 1.12.2. */
public final class ModBeaconBlocks {

    private ModBeaconBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    public static final RegistryObject<BlockBeaconable> block_cadmium = beacon("block_cadmium", 5.0F, 30.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_bismuth = beacon("block_bismuth", 5.0F, 30.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_coltan = beacon("block_coltan", 5.0F, 30.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_tantalium = beacon("block_tantalium", 5.0F, 30.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_niobium = beacon("block_niobium", 5.0F, 30.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_lanthanium = beacon("block_lanthanium", 5.0F, 30.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    // block_actinium is a BlockHazard (1.7 new BlockHazard().makeBeaconable()): ModHazardBlocks
    public static final RegistryObject<BlockBeaconable> block_polymer = beacon("block_polymer", 3.0F, 10.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_bakelite = beacon("block_bakelite", 3.0F, 10.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_rubber = beacon("block_rubber", 3.0F, 10.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockBeaconable> block_zirconium = beacon("block_zirconium", 5.0F, 10.0F, () -> SoundType.METAL)
            .transform(Presets.cubeAll().viaItemModel())
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    private static Reg.Handle<BlockBeaconable> beacon(String id, float hardness, float resistance, Supplier<SoundType> sound) {
        Reg.Handle<BlockBeaconable> block = REG.block(id, BlockBeaconable::new,
                p -> p.strength(hardness, resistance).sound(sound.get()).requiresCorrectToolForDrops());
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.BLOCKS, item);
        return block;
    }

    public static void init() {}
}
