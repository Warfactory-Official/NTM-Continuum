package com.hbm.blocks;

import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.world.Compat;
import com.hbm.Constants;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemBlockAnvil;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.blocks.machine.NTMAnvil.*;

/**
 * The 12 tiered NTM anvils (1.12.2 {@code ModBlocks} anvil block set). Hand-registered like
 * {@link ModDecoBlocks}; all share the {@code anvil} block-entity type (see
 * {@link com.hbm.tileentity.ModBlockEntities}) so {@link com.hbm.client.render.AnvilObjRenderer} can draw
 * their OBJ models. Loot/lang/pickaxe entries regenerate from {@code MACHINE_BLOCK_IDS} in the port-tools generator.
 */
public final class ModAnvilBlocks {

    private ModAnvilBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** All registered anvils, in registration order. The shared BE type and renderer bind to these. */
    public static final List<RegistryObject<NTMAnvil>> ANVILS = new ArrayList<>();

    public static final RegistryObject<NTMAnvil> anvil_iron           = anvil("anvil_iron", TIER_IRON)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_lead           = anvil("anvil_lead", TIER_IRON)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_steel          = anvil("anvil_steel", TIER_STEEL)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_desh           = anvil("anvil_desh", TIER_OIL)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_ferrouranium   = anvil("anvil_ferrouranium", TIER_NUCLEAR)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_saturnite      = anvil("anvil_saturnite", TIER_RBMK)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_bismuth_bronze = anvil("anvil_bismuth_bronze", TIER_RBMK)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_arsenic_bronze = anvil("anvil_arsenic_bronze", TIER_RBMK)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_schrabidate    = anvil("anvil_schrabidate", TIER_FUSION)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_dnt            = anvil("anvil_dnt", TIER_PARTICLE)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_osmiridium     = anvil("anvil_osmiridium", TIER_GERALD)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<NTMAnvil> anvil_murky          = anvil("anvil_murky", TIER_MURKY)
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    private static Reg.Handle<NTMAnvil> anvil(String id, int tier) {
        Reg.Handle<NTMAnvil> block = REG.block(id, p -> new NTMAnvil(p, tier),
                p -> p.requiresCorrectToolForDrops().strength(5.0F, 100.0F).sound(SoundType.ANVIL).noOcclusion());
        Reg.Handle<BlockItem> item = REG.item(id,
                p -> new ItemBlockAnvil(block.get(), p), p -> Compat.blockDescription(p));
        ModTabs.addTo(HbmTab.MACHINES, item);
        ANVILS.add(block);
        return block;
    }

    public static void init() {}
}
