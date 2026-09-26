package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.blocks.network.CraneRouter;
import com.hbm.tileentity.ModConveyorBlockEntities;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.inventory.ModConveyorMenus;
import dev.golem.core.client.BlockTint;
import dev.golem.core.client.Tints;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.hbm.client.render.PartitionerRenderer;
import com.hbm.client.render.PistonInserterRenderer;
import com.hbm.client.render.SplitterRenderer;
import com.hbm.inventory.gui.GUICraneBoxer;
import com.hbm.inventory.gui.GUICraneExtractor;
import com.hbm.inventory.gui.GUICraneGrabber;
import com.hbm.inventory.gui.GUICraneInserter;
import com.hbm.inventory.gui.GUICraneRouter;
import com.hbm.inventory.gui.GUICraneUnboxer;

/** Client hooks for the conveyor belt and crane family: crane screens, the OBJ renderers, the sorter's six side tints. */
public final class ConveyorClientRegistrations {

    private ConveyorClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ModConveyorMenus.CRANE_INSERTER.get(), GUICraneInserter::new);
        sink.register(ModConveyorMenus.CRANE_EXTRACTOR.get(), GUICraneExtractor::new);
        sink.register(ModConveyorMenus.CRANE_GRABBER.get(), GUICraneGrabber::new);
        sink.register(ModConveyorMenus.CRANE_ROUTER.get(), GUICraneRouter::new);
        sink.register(ModConveyorMenus.CRANE_BOXER.get(), GUICraneBoxer::new);
        sink.register(ModConveyorMenus.CRANE_UNBOXER.get(), GUICraneUnboxer::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModConveyorBlockEntities.CRANE_SPLITTER.get(), SplitterRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModConveyorBlockEntities.CRANE_PARTITIONER.get(), PartitionerRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModConveyorBlockEntities.PISTON_INSERTER.get(), PistonInserterRenderer::new);
    }

    /** 1.7 {@code CraneRouter.colorMultiplier}: one colour per render pass, i.e. per side, i.e. per tint index. */
    public static void registerBlockColors() {
        List<BlockTint> tints = new ArrayList<>();
        for (int color : CraneRouter.SIDE_COLORS) {
            int argb = 0xFF000000 | color;
            tints.add(new BlockTint() {
                @Override
                public int color(BlockState state) {
                    return argb;
                }

                @Override
                public Set<Property<?>> relevantProperties() {
                    return Set.of();
                }
            });
        }
        Tints.registerBlockTints(tints, ModConveyorBlocks.CRANE_ROUTER);
    }
}
