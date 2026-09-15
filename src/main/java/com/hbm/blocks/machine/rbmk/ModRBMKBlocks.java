package com.hbm.blocks.machine.rbmk;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.blocks.generic.BlockLayering;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** RBMK's self-contained block and block-item registrations. */
public final class ModRBMKBlocks {
    private ModRBMKBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<RBMKRod> rbmk_fuel_rod = column("rbmk_fuel_rod", p -> new RBMKRod(p, false), true);
    public static final RegistryObject<RBMKControl> rbmk_control = column("rbmk_control", p -> new RBMKControl(p, false), false);
    public static final RegistryObject<RBMKControl> rbmk_control_mod = column("rbmk_control_mod", p -> new RBMKControl(p, true), false);
    public static final RegistryObject<RBMKControlAuto> rbmk_control_auto = column("rbmk_control_auto", RBMKControlAuto::new, false);
    public static final RegistryObject<RBMKControl> rbmk_control_reasim = column("rbmk_control_reasim", p -> new RBMKControl(p, false), false);
    public static final RegistryObject<RBMKControlAuto> rbmk_control_reasim_auto = column("rbmk_control_reasim_auto", RBMKControlAuto::new, false);
    public static final RegistryObject<RBMKBoiler> rbmk_boiler = column("rbmk_boiler", RBMKBoiler::new, false);
    public static final RegistryObject<RBMKModerator> rbmk_moderator = column("rbmk_moderator", RBMKModerator::new, false);
    public static final RegistryObject<RBMKReflector> rbmk_reflector = column("rbmk_reflector", RBMKReflector::new, false);
    public static final RegistryObject<RBMKAbsorber> rbmk_absorber = column("rbmk_absorber", RBMKAbsorber::new, false);
    public static final RegistryObject<RBMKBlank> rbmk_blank = column("rbmk_blank", RBMKBlank::new, false);
    public static final RegistryObject<RBMKRod> rbmk_fuel_rod_moderated = column("rbmk_fuel_rod_moderated", p -> new RBMKRod(p, true), true);
    public static final RegistryObject<RBMKRodReaSim> rbmk_fuel_rod_reasim = column("rbmk_fuel_rod_reasim", p -> new RBMKRodReaSim(p, false), true);
    public static final RegistryObject<RBMKRodReaSim> rbmk_fuel_rod_reasim_moderated = column("rbmk_fuel_rod_reasim_moderated", p -> new RBMKRodReaSim(p, true), true);
    public static final RegistryObject<RBMKCooler> rbmk_cooler = column("rbmk_cooler", RBMKCooler::new, false);
    public static final RegistryObject<RBMKStorage> rbmk_storage = column("rbmk_storage", RBMKStorage::new, false);
    public static final RegistryObject<RBMKHeater> rbmk_heater = column("rbmk_heater", RBMKHeater::new, false);
    public static final RegistryObject<RBMKOutgasser> rbmk_outgasser = column("rbmk_outgasser", RBMKOutgasser::new, false);

    public static final RegistryObject<RBMKInlet> rbmk_steam_inlet = peripheral("rbmk_steam_inlet", RBMKInlet::new);
    public static final RegistryObject<RBMKOutlet> rbmk_steam_outlet = peripheral("rbmk_steam_outlet", RBMKOutlet::new);
    public static final RegistryObject<RBMKConsole> rbmk_console = nonColumn("rbmk_console", RBMKConsole::new);
    public static final RegistryObject<RBMKCraneConsole> rbmk_crane_console = nonColumn("rbmk_crane_console", RBMKCraneConsole::new);
    public static final RegistryObject<RBMKGauge> rbmk_gauge = nonColumn("rbmk_gauge", RBMKGauge::new);

    public static final RegistryObject<RBMKDebrisBlock> rbmk_debris = debris("rbmk_debris", RBMKDebrisBlock::new);
    public static final RegistryObject<RBMKDebrisBurning> rbmk_debris_burning = debris("rbmk_debris_burning", RBMKDebrisBurning::new);
    public static final RegistryObject<RBMKDebrisRadiating> rbmk_debris_radiating = debris("rbmk_debris_radiating", RBMKDebrisRadiating::new);
    public static final RegistryObject<RBMKDebrisDigamma> rbmk_debris_digamma = debris("rbmk_debris_digamma", RBMKDebrisDigamma::new);
    /** Current graphite storage block, required by the moderator and modified-control recipes. */
    public static final RegistryObject<Block> block_graphite = BLOCKS.registerSimpleBlock("block_graphite",
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(5F, 10F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    /** Hand-written because foam is a machine-safety block, not generated decoration content. */
    public static final RegistryObject<BlockLayering> foam_layer = BLOCKS.registerBlock("foam_layer",
            p -> new BlockLayering(p, true), p -> p.strength(0.1F).sound(net.minecraft.world.level.block.SoundType.SNOW).noOcclusion().randomTicks());
    public static final RegistryObject<BlockLayering> sand_boron_layer = BLOCKS.registerBlock("sand_boron_layer",
            BlockLayering::new, p -> p.strength(0.1F).sound(net.minecraft.world.level.block.SoundType.SAND).noOcclusion().randomTicks());

    private static <B extends Block> RegistryObject<B> column(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory, boolean fuel) { return BLOCKS.registerBlock(id, factory, p -> p.strength(3F, 30F).requiresCorrectToolForDrops()); }
    private static <B extends Block> RegistryObject<B> peripheral(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory) { return BLOCKS.registerBlock(id, factory, p -> p.strength(50F, 60F).sound(net.minecraft.world.level.block.SoundType.METAL).requiresCorrectToolForDrops()); }
    private static <B extends Block> RegistryObject<B> nonColumn(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory) { return BLOCKS.registerBlock(id, factory, p -> p.strength(3F, 30F).requiresCorrectToolForDrops().noOcclusion()); }
    private static <B extends Block> RegistryObject<B> debris(String id, java.util.function.Function<BlockBehaviour.Properties, B> factory) { return BLOCKS.registerBlock(id, factory, p -> p.strength(50F, 600F).requiresCorrectToolForDrops().noOcclusion()); }
    static {
        for (RegistryObject<? extends Block> block : java.util.List.of(rbmk_fuel_rod, rbmk_control, rbmk_control_mod, rbmk_control_auto, rbmk_control_reasim, rbmk_control_reasim_auto, rbmk_boiler, rbmk_moderator, rbmk_reflector, rbmk_absorber, rbmk_blank, rbmk_fuel_rod_moderated, rbmk_fuel_rod_reasim, rbmk_fuel_rod_reasim_moderated, rbmk_cooler, rbmk_storage, rbmk_heater, rbmk_outgasser, rbmk_steam_inlet, rbmk_steam_outlet, rbmk_console, rbmk_crane_console, rbmk_gauge, rbmk_debris, rbmk_debris_burning, rbmk_debris_radiating, rbmk_debris_digamma, foam_layer)) {
            RegistryObject<BlockItem> item = ITEMS.registerBlockItem(block.getId().getPath(), block::get);
            ModTabs.addTo(HbmTab.MACHINES, item);
        }
        RegistryObject<BlockItem> graphite = ITEMS.registerBlockItem("block_graphite", block_graphite::get);
        ModTabs.addTo(HbmTab.RESOURCES, graphite);
        RegistryObject<BlockItem> sandBoronLayer = ITEMS.registerBlockItem("sand_boron_layer", sand_boron_layer::get);
        ModTabs.addTo(HbmTab.BLOCKS, sandBoronLayer);
    }
    public static void init() {}
}
