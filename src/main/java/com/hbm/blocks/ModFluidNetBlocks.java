package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.blocks.fluid.BlockAcid;
import com.hbm.blocks.fluid.BlockCorium;
import com.hbm.blocks.machine.BlockFluidBarrel;
import com.hbm.blocks.fluid.BlockRadLava;
import com.hbm.blocks.fluid.BlockStaticFluid;
import com.hbm.blocks.fluid.BlockSulfuricAcid;
import com.hbm.blocks.fluid.BlockToxic;
import com.hbm.blocks.fluid.BlockVolcanicLava;
import com.hbm.blocks.network.FluidCounterValve;
import com.hbm.blocks.network.FluidDuctBox;
import com.hbm.blocks.network.FluidDuctBoxExhaust;
import com.hbm.blocks.network.FluidDuctGauge;
import com.hbm.blocks.network.FluidDuctNeo;
import com.hbm.blocks.network.FluidDuctPaintable;
import com.hbm.blocks.network.FluidDuctPaintableBlockExhaust;
import com.hbm.blocks.network.FluidPipeAnchor;
import com.hbm.blocks.network.FluidPump;
import com.hbm.blocks.network.FluidSwitch;
import com.hbm.blocks.machine.FoundryTank;
import com.hbm.blocks.generic.RedBarrel;
import com.hbm.blocks.generic.YellowBarrel;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import com.hbm.items.block.FluidNetBlockItem;

/**
 * The fluid-network blocks not covered by {@code ModMachineBlocks}: the 1.7 duct family, the storage
 * and effect barrels, the foundry storage basin and the fluid blocks. Ids are the 1.7
 * {@code setBlockName} strings; metadata sub-blocks are flattened ({@code fluid_duct_neo_silver},
 * {@code fluid_duct_box_copper_3}, {@code fluid_duct_exhaust_0}, ...).
 */
public final class ModFluidNetBlocks {

    private ModFluidNetBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static final UnaryOperator<BlockBehaviour.Properties> PIPE =
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> STEEL =
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> EFFECT_BARREL =
            p -> p.strength(0.5F, 2.5F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops(); // 1.7 Material.iron

    // --- ducts ---

    public static final RegistryObject<FluidDuctNeo> fluid_duct_neo = BLOCKS.registerBlock("fluid_duct_neo", p -> new FluidDuctNeo(p, 0), PIPE);
    public static final RegistryObject<FluidDuctNeo> fluid_duct_neo_silver = BLOCKS.registerBlock("fluid_duct_neo_silver", p -> new FluidDuctNeo(p, 1), PIPE);
    public static final RegistryObject<FluidDuctNeo> fluid_duct_neo_colored = BLOCKS.registerBlock("fluid_duct_neo_colored", p -> new FluidDuctNeo(p, 2), PIPE);

    /** {@code fluid_duct_box_<material>_<size>}, indexed [material][size] like the 1.7 metadata. */
    public static final RegistryObject<FluidDuctBox>[][] fluid_duct_box = boxDucts();
    /** {@code fluid_duct_exhaust_<size>}. */
    public static final RegistryObject<FluidDuctBoxExhaust>[] fluid_duct_exhaust = exhaustDucts();

    public static final RegistryObject<FluidDuctPaintableBlockExhaust> fluid_duct_paintable_block_exhaust =
            BLOCKS.registerBlock("fluid_duct_paintable_block_exhaust", FluidDuctPaintableBlockExhaust::new, STEEL);
    public static final RegistryObject<FluidPipeAnchor> pipe_anchor = BLOCKS.registerBlock("pipe_anchor", FluidPipeAnchor::new, PIPE);
    public static final RegistryObject<FluidDuctPaintable> fluid_duct_paintable = BLOCKS.registerBlock("fluid_duct_paintable", FluidDuctPaintable::new, STEEL);
    public static final RegistryObject<FluidDuctGauge> fluid_duct_gauge = BLOCKS.registerBlock("fluid_duct_gauge", FluidDuctGauge::new, STEEL);
    public static final RegistryObject<FluidSwitch> fluid_switch = BLOCKS.registerBlock("fluid_switch", FluidSwitch::new, STEEL);
    public static final RegistryObject<FluidCounterValve> fluid_counter_valve = BLOCKS.registerBlock("fluid_counter_valve", FluidCounterValve::new, STEEL);
    public static final RegistryObject<FluidPump> fluid_pump = BLOCKS.registerBlock("fluid_pump", FluidPump::new, PIPE);

    // --- storage barrels ---

    public static final RegistryObject<BlockFluidBarrel> barrel_plastic = BLOCKS.registerBlock("barrel_plastic",
            p -> new BlockFluidBarrel(p, 12000, "barrel_plastic", false), p -> p.strength(2.0F, 5.0F).sound(SoundType.STONE).noOcclusion());
    public static final RegistryObject<BlockFluidBarrel> barrel_corroded = BLOCKS.registerBlock("barrel_corroded",
            p -> new BlockFluidBarrel(p, 6000, "barrel_corroded", true), p -> p.strength(2.0F, 5.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<BlockFluidBarrel> barrel_steel = BLOCKS.registerBlock("barrel_steel",
            p -> new BlockFluidBarrel(p, 16000, "barrel_steel", false), p -> p.strength(2.0F, 5.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<BlockFluidBarrel> barrel_tcalloy = BLOCKS.registerBlock("barrel_tcalloy",
            p -> new BlockFluidBarrel(p, 24000, "barrel_tcalloy", false), p -> p.strength(2.0F, 5.0F).sound(SoundType.METAL).noOcclusion());
    public static final RegistryObject<BlockFluidBarrel> barrel_antimatter = BLOCKS.registerBlock("barrel_antimatter",
            p -> new BlockFluidBarrel(p, 16000, "barrel_antimatter", false), p -> p.strength(2.0F, 5.0F).sound(SoundType.METAL).noOcclusion());

    // --- effect barrels (nukeTab); kind ids 20+ for EntityPrimedExplosive ---

    public static final RegistryObject<RedBarrel> red_barrel = BLOCKS.registerBlock("red_barrel", p -> new RedBarrel(p, "barrel_red", 20, RedBarrel.Kind.RED), EFFECT_BARREL);
    public static final RegistryObject<RedBarrel> pink_barrel = BLOCKS.registerBlock("pink_barrel", p -> new RedBarrel(p, "barrel_pink", 21, RedBarrel.Kind.PINK), EFFECT_BARREL);
    public static final RegistryObject<YellowBarrel> yellow_barrel = BLOCKS.registerBlock("yellow_barrel", p -> new YellowBarrel(p, "barrel_yellow", 22, false), EFFECT_BARREL);
    public static final RegistryObject<YellowBarrel> vitrified_barrel = BLOCKS.registerBlock("vitrified_barrel", p -> new YellowBarrel(p, "barrel_vitrified", 23, true), EFFECT_BARREL);
    public static final RegistryObject<RedBarrel> lox_barrel = BLOCKS.registerBlock("lox_barrel", p -> new RedBarrel(p, "barrel_lox", 24, RedBarrel.Kind.LOX), EFFECT_BARREL);
    public static final RegistryObject<RedBarrel> taint_barrel = BLOCKS.registerBlock("taint_barrel", p -> new RedBarrel(p, "barrel_taint", 25, RedBarrel.Kind.TAINT), EFFECT_BARREL);

    // --- foundry storage basin ---

    public static final RegistryObject<FoundryTank> foundry_tank = BLOCKS.registerBlock("foundry_tank", FoundryTank::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).noOcclusion().requiresCorrectToolForDrops());

    // --- fluid blocks (1.7 blocks/fluid, static; see BlockStaticFluid) ---

    public static final RegistryObject<BlockAcid> acid_block = BLOCKS.registerBlock("acid_block", BlockAcid::new, p -> fluid(p, 5));
    public static final RegistryObject<BlockToxic> toxic_block = BLOCKS.registerBlock("toxic_block", p -> new BlockToxic(p, true), p -> fluid(p, 15));
    public static final RegistryObject<BlockToxic> schrabidic_block = BLOCKS.registerBlock("schrabidic_block", p -> new BlockToxic(p, true), p -> fluid(p, 0));
    public static final RegistryObject<BlockSulfuricAcid> sulfuric_acid_block = BLOCKS.registerBlock("sulfuric_acid_block", BlockSulfuricAcid::new, p -> fluid(p, 0));
    public static final RegistryObject<BlockCorium> corium_block = BLOCKS.registerBlock("corium_block", BlockCorium::new, p -> fluid(p, 10, false)); // 1.7 CoriumFinite: isReplaceable() false, no random tick
    public static final RegistryObject<BlockVolcanicLava> volcanic_lava_block = BLOCKS.registerBlock("volcanic_lava_block", BlockVolcanicLava::new, p -> fluid(p, 15).randomTicks());
    public static final RegistryObject<BlockRadLava> rad_lava_block = BLOCKS.registerBlock("rad_lava_block", BlockRadLava::new, p -> fluid(p, 15).randomTicks());

    private static BlockBehaviour.Properties fluid(BlockBehaviour.Properties p, int light) {
        return fluid(p, light, true);
    }

    private static BlockBehaviour.Properties fluid(BlockBehaviour.Properties p, int light, boolean replaceable) {
        // 1.7: setResistance(500), hardness 100 (BlockFluidBase), the fluid's luminosity, no drops.
        p = p.strength(100.0F, 500.0F).noOcclusion().noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY);
        if (replaceable) p = p.replaceable();
        return light > 0 ? p.lightLevel(state -> light) : p;
    }

    // --- block items ---

    private static final List<RegistryObject<? extends BlockItem>> MACHINE_ITEMS = new ArrayList<>();
    private static final List<RegistryObject<? extends BlockItem>> BOMB_ITEMS = new ArrayList<>();

    private static RegistryObject<BlockItem> item(String id, RegistryObject<? extends net.minecraft.world.level.block.Block> block,
                                                  List<RegistryObject<? extends BlockItem>> tab) {
        RegistryObject<BlockItem> item = ITEMS.registerItem(id, p -> new FluidNetBlockItem(block.get(), p), Compat::blockDescription);
        if (tab != null) tab.add(item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<FluidDuctBox>[][] boxDucts() {
        RegistryObject<FluidDuctBox>[][] ducts = new RegistryObject[FluidDuctBox.MATERIALS.length][5];
        for (int m = 0; m < FluidDuctBox.MATERIALS.length; m++) {
            for (int s = 0; s < 5; s++) {
                int material = m, size = s;
                ducts[m][s] = BLOCKS.registerBlock(boxId(m, s), p -> new FluidDuctBox(p, material, size), PIPE);
            }
        }
        return ducts;
    }

    @SuppressWarnings("unchecked")
    private static RegistryObject<FluidDuctBoxExhaust>[] exhaustDucts() {
        RegistryObject<FluidDuctBoxExhaust>[] ducts = new RegistryObject[5];
        for (int s = 0; s < 5; s++) {
            int size = s;
            ducts[s] = BLOCKS.registerBlock(exhaustId(s), p -> new FluidDuctBoxExhaust(p, size), PIPE);
        }
        return ducts;
    }

    public static String boxId(int material, int size) {
        return "fluid_duct_box_" + FluidDuctBox.MATERIALS[material] + "_" + size;
    }

    public static String exhaustId(int size) {
        return "fluid_duct_exhaust_" + size;
    }

    static {
        item("fluid_duct_neo", fluid_duct_neo, MACHINE_ITEMS);
        item("fluid_duct_neo_silver", fluid_duct_neo_silver, MACHINE_ITEMS);
        item("fluid_duct_neo_colored", fluid_duct_neo_colored, MACHINE_ITEMS);
        for (int m = 0; m < FluidDuctBox.MATERIALS.length; m++) {
            for (int s = 0; s < 5; s++) item(boxId(m, s), fluid_duct_box[m][s], MACHINE_ITEMS);
        }
        for (int s = 0; s < 5; s++) item(exhaustId(s), fluid_duct_exhaust[s], MACHINE_ITEMS);
        item("fluid_duct_paintable_block_exhaust", fluid_duct_paintable_block_exhaust, MACHINE_ITEMS);
        item("pipe_anchor", pipe_anchor, MACHINE_ITEMS);
        item("fluid_duct_paintable", fluid_duct_paintable, MACHINE_ITEMS);
        item("fluid_duct_gauge", fluid_duct_gauge, MACHINE_ITEMS);
        item("fluid_switch", fluid_switch, MACHINE_ITEMS);
        item("fluid_counter_valve", fluid_counter_valve, MACHINE_ITEMS);
        item("fluid_pump", fluid_pump, MACHINE_ITEMS);
        item("barrel_plastic", barrel_plastic, MACHINE_ITEMS);
        item("barrel_corroded", barrel_corroded, null); // 1.7: setCreativeTab(null)
        item("barrel_steel", barrel_steel, MACHINE_ITEMS);
        item("barrel_tcalloy", barrel_tcalloy, MACHINE_ITEMS);
        item("barrel_antimatter", barrel_antimatter, MACHINE_ITEMS);
        item("foundry_tank", foundry_tank, MACHINE_ITEMS);
        item("red_barrel", red_barrel, BOMB_ITEMS);
        item("pink_barrel", pink_barrel, BOMB_ITEMS);
        item("yellow_barrel", yellow_barrel, BOMB_ITEMS);
        item("vitrified_barrel", vitrified_barrel, BOMB_ITEMS);
        item("lox_barrel", lox_barrel, BOMB_ITEMS);
        item("taint_barrel", taint_barrel, BOMB_ITEMS);
        // The fluid blocks have no item (1.7 setCreativeTab(null), placed by their buckets only).

        for (RegistryObject<? extends BlockItem> item : MACHINE_ITEMS) ModTabs.addTo(HbmTab.MACHINES, item);
        for (RegistryObject<? extends BlockItem> item : BOMB_ITEMS) ModTabs.addTo(HbmTab.BOMBS, item);
    }

    public static void init() {}

    /** Whether {@code block} is one of the static fluid blocks (for the buckets' place check). */
    public static boolean isFluidBlock(net.minecraft.world.level.block.Block block) {
        return block instanceof BlockStaticFluid;
    }
}
