package com.hbm.blocks;

import dev.golem.core.registry.BlockTag;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockGasRadon;
import com.hbm.blocks.generic.BlockGasMeltdown;
import com.hbm.blocks.generic.BlockGasAsbestos;
import com.hbm.blocks.generic.BlockGasMonoxide;
import com.hbm.blocks.generic.BlockGasRadonTomb;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Non-generated nuclear resources and gas blocks. */
public final class ModNuclearBlocks {
    private ModNuclearBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);
    /** A gas: an authored single-state model (no item). */
    private static final Preset GAS = Presets.statesOnly(Variants.simple());

    public static final RegistryObject<BlockOutgas> ore_uranium = ore("ore_uranium", 5F, 20)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<BlockOutgas> ore_uranium_scorched = ore("ore_uranium_scorched", 5F, 15)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<BlockOutgas> ore_gneiss_uranium = ore("ore_gneiss_uranium", 1.5F, 20)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<BlockOutgas> ore_gneiss_uranium_scorched = ore("ore_gneiss_uranium_scorched", 1.5F, 20)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<BlockOutgas> ore_nether_uranium = ore("ore_nether_uranium", 0.4F, 20)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<BlockOutgas> ore_nether_uranium_scorched = ore("ore_nether_uranium_scorched", 0.4F, 20)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);
    public static final RegistryObject<BlockGasRadon> gas_radon = REG.block("gas_radon",
            (BlockBehaviour.Properties p) -> new BlockGasRadon(p.noOcclusion().noCollision().replaceable().randomTicks(), 0.05F, false))
            .transform(GAS).loot(Loot.nothing());
    public static final RegistryObject<BlockGasRadon> gas_radon_dense = REG.block("gas_radon_dense",
            (BlockBehaviour.Properties p) -> new BlockGasRadon(p.noOcclusion().noCollision().replaceable().randomTicks(), 0.5F, true))
            .transform(GAS).loot(Loot.nothing());
    public static final RegistryObject<BlockGasMeltdown> gas_meltdown = REG.block("gas_meltdown",
            (BlockBehaviour.Properties p) -> new BlockGasMeltdown(p.noOcclusion().noCollision().replaceable().randomTicks()))
            .transform(GAS);
    public static final RegistryObject<BlockGasRadonTomb> gas_radon_tomb = REG.block("gas_radon_tomb",
            (BlockBehaviour.Properties p) -> new BlockGasRadonTomb(p.noOcclusion().noCollision().replaceable().randomTicks()))
            .transform(GAS);
    public static final RegistryObject<BlockGasMonoxide> gas_monoxide = REG.block("gas_monoxide",
            (BlockBehaviour.Properties p) -> new BlockGasMonoxide(p.noOcclusion().noCollision().replaceable().randomTicks()))
            .transform(GAS);
    public static final RegistryObject<BlockGasAsbestos> gas_asbestos = REG.block("gas_asbestos",
            (BlockBehaviour.Properties p) -> new BlockGasAsbestos(p.noOcclusion().noCollision().replaceable().randomTicks()))
            .transform(GAS);

    /**
     * A cube of its own icon whose item model parents the block model; drops itself. Pickaxe, iron-tool and
     * material tags stay with the ore generator ({@code port-tools/gen_ore_blocks.py}).
     */
    private static Reg.Handle<BlockOutgas> ore(String id, float hardness, int rate) {
        return REG.block(id, p -> new BlockOutgas(p, true, rate, true),
                        p -> p.strength(hardness, 6F).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks())
                .transform(Presets.cubeAll().viaItemModel())
                .loot(RawOres.drop("hbm:raw_uranium", Loot.self()));
    }

    static {
        for (RegistryObject<? extends Block> block : List.of(ore_uranium, ore_uranium_scorched,
                ore_gneiss_uranium, ore_gneiss_uranium_scorched, ore_nether_uranium, ore_nether_uranium_scorched)) {
            REG.blockItem(block).tab(HbmTab.RESOURCES);
        }
    }
    public static void init() {}
}
