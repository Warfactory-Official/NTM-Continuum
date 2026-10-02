package com.hbm.blocks;

import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockNTMLadder;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/** The twelve HBM ladder blocks, backed by vanilla {@link net.minecraft.world.level.block.LadderBlock}. */
public final class ModLadderBlocks {

    private ModLadderBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    public static final RegistryObject<BlockNTMLadder> ladder_sturdy = ladder("ladder_sturdy", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_sturdy")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_iron = ladder("ladder_iron", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_iron")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_gold = ladder("ladder_gold", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_gold")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_aluminium = ladder("ladder_aluminium", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_aluminium")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_copper = ladder("ladder_copper", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_copper")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_titanium = ladder("ladder_titanium", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_titanium")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_lead = ladder("ladder_lead", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_lead")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_cobalt = ladder("ladder_cobalt", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_cobalt")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_steel = ladder("ladder_steel", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_steel")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_tungsten = ladder("ladder_tungsten", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_tungsten")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_red = ladder("ladder_red", false)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_red")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);
    public static final RegistryObject<BlockNTMLadder> ladder_red_top = ladder("ladder_red_top", true)
            .model(d -> d.statesOnly().states(Variants.facing()))
            .icon(d -> d.models(Models.blockItem("hbm:block/ladder_red_top")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .tag(BlockTag.CLIMBABLE);

    private static Reg.Handle<BlockNTMLadder> ladder(String id, boolean top) {
        Reg.Handle<BlockNTMLadder> block = REG.block(id,
                p -> new BlockNTMLadder(p, top),
                p -> p.strength(0.25F, 2.0F).sound(SoundType.LADDER).noOcclusion().noCollision());
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.BLOCKS, item);
        return block;
    }

    public static void init() {}
}
