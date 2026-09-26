package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;
import com.hbm.blocks.machine.PistonInserter;
import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.BlockConveyorChute;
import com.hbm.blocks.network.BlockConveyorDouble;
import com.hbm.blocks.network.BlockConveyorExpress;
import com.hbm.blocks.network.BlockConveyorLift;
import com.hbm.blocks.network.BlockConveyorTriple;
import com.hbm.blocks.network.CraneBoxer;
import com.hbm.blocks.network.CraneExtractor;
import com.hbm.blocks.network.CraneGrabber;
import com.hbm.blocks.network.CraneInserter;
import com.hbm.blocks.network.CranePartitioner;
import com.hbm.blocks.network.CraneRouter;
import com.hbm.blocks.network.CraneSplitter;
import com.hbm.blocks.network.CraneUnboxer;
import com.hbm.items.block.ConveyorBlockItem;

/** Feature-local block and block-item registry for the conveyor belt and crane family (1.7 {@code ModBlocks} L1894-1909). */
public final class ModConveyorBlocks {

    private ModConveyorBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static final UnaryOperator<BlockBehaviour.Properties> BELT =
            p -> p.strength(2.0F, 2.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();
    private static final UnaryOperator<BlockBehaviour.Properties> CRANE =
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> CRANE_OPEN =
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion();

    // belts: no creative tab in 1.7, the wand is their item
    public static final RegistryObject<BlockConveyor> CONVEYOR = BLOCKS.registerBlock("conveyor", BlockConveyor::new, BELT);
    public static final RegistryObject<BlockConveyorExpress> CONVEYOR_EXPRESS = BLOCKS.registerBlock("conveyor_express", BlockConveyorExpress::new, BELT);
    public static final RegistryObject<BlockConveyorDouble> CONVEYOR_DOUBLE = BLOCKS.registerBlock("conveyor_double", BlockConveyorDouble::new, BELT);
    public static final RegistryObject<BlockConveyorTriple> CONVEYOR_TRIPLE = BLOCKS.registerBlock("conveyor_triple", BlockConveyorTriple::new, BELT);
    public static final RegistryObject<BlockConveyorChute> CONVEYOR_CHUTE = BLOCKS.registerBlock("conveyor_chute", BlockConveyorChute::new, BELT);
    public static final RegistryObject<BlockConveyorLift> CONVEYOR_LIFT = BLOCKS.registerBlock("conveyor_lift", BlockConveyorLift::new, BELT);

    public static final RegistryObject<CraneExtractor> CRANE_EXTRACTOR = BLOCKS.registerBlock("crane_extractor", CraneExtractor::new, CRANE);
    public static final RegistryObject<CraneInserter> CRANE_INSERTER = BLOCKS.registerBlock("crane_inserter", CraneInserter::new, CRANE);
    public static final RegistryObject<CraneGrabber> CRANE_GRABBER = BLOCKS.registerBlock("crane_grabber", CraneGrabber::new, CRANE);
    public static final RegistryObject<CraneRouter> CRANE_ROUTER = BLOCKS.registerBlock("crane_router", CraneRouter::new, CRANE);
    public static final RegistryObject<CraneBoxer> CRANE_BOXER = BLOCKS.registerBlock("crane_boxer", CraneBoxer::new, CRANE);
    public static final RegistryObject<CraneUnboxer> CRANE_UNBOXER = BLOCKS.registerBlock("crane_unboxer", CraneUnboxer::new, CRANE);
    public static final RegistryObject<CraneSplitter> CRANE_SPLITTER = BLOCKS.registerBlock("crane_splitter", CraneSplitter::new, CRANE_OPEN);
    public static final RegistryObject<CranePartitioner> CRANE_PARTITIONER = BLOCKS.registerBlock("crane_partitioner", CranePartitioner::new, CRANE_OPEN);
    public static final RegistryObject<PistonInserter> PISTON_INSERTER = BLOCKS.registerBlock("piston_inserter", PistonInserter::new, CRANE_OPEN);

    public static final RegistryObject<BlockItem> CONVEYOR_ITEM = item("conveyor", CONVEYOR);
    public static final RegistryObject<BlockItem> CONVEYOR_EXPRESS_ITEM = item("conveyor_express", CONVEYOR_EXPRESS);
    public static final RegistryObject<BlockItem> CONVEYOR_DOUBLE_ITEM = item("conveyor_double", CONVEYOR_DOUBLE);
    public static final RegistryObject<BlockItem> CONVEYOR_TRIPLE_ITEM = item("conveyor_triple", CONVEYOR_TRIPLE);
    public static final RegistryObject<BlockItem> CONVEYOR_CHUTE_ITEM = item("conveyor_chute", CONVEYOR_CHUTE);
    public static final RegistryObject<BlockItem> CONVEYOR_LIFT_ITEM = item("conveyor_lift", CONVEYOR_LIFT);
    public static final RegistryObject<BlockItem> CRANE_EXTRACTOR_ITEM = item("crane_extractor", CRANE_EXTRACTOR);
    public static final RegistryObject<BlockItem> CRANE_INSERTER_ITEM = item("crane_inserter", CRANE_INSERTER);
    public static final RegistryObject<BlockItem> CRANE_GRABBER_ITEM = item("crane_grabber", CRANE_GRABBER);
    public static final RegistryObject<BlockItem> CRANE_ROUTER_ITEM = item("crane_router", CRANE_ROUTER);
    public static final RegistryObject<BlockItem> CRANE_BOXER_ITEM = item("crane_boxer", CRANE_BOXER);
    public static final RegistryObject<BlockItem> CRANE_UNBOXER_ITEM = item("crane_unboxer", CRANE_UNBOXER);
    public static final RegistryObject<BlockItem> CRANE_SPLITTER_ITEM = item("crane_splitter", CRANE_SPLITTER);
    public static final RegistryObject<BlockItem> CRANE_PARTITIONER_ITEM = item("crane_partitioner", CRANE_PARTITIONER);
    public static final RegistryObject<BlockItem> PISTON_INSERTER_ITEM = item("piston_inserter", PISTON_INSERTER);

    private static RegistryObject<BlockItem> item(String name, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return ITEMS.registerItem(name, properties -> new ConveyorBlockItem(block.get(),
                dev.golem.core.world.Compat.blockDescription(properties)));
    }

    static {
        // 1.7 machineTab; belts and the piston inserter had no tab
        ModTabs.addTo(HbmTab.MACHINES, CRANE_EXTRACTOR_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, CRANE_INSERTER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, CRANE_GRABBER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, CRANE_ROUTER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, CRANE_BOXER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, CRANE_UNBOXER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, CRANE_SPLITTER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, CRANE_PARTITIONER_ITEM);
    }

    public static void init() {}
}
