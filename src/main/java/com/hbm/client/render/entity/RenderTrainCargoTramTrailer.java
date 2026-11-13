package com.hbm.client.render.entity;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.entity.train.TrainCargoTramTrailer;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;

/**
 * The tram's trailer: the flatbed mesh with 1.7's ladder of crate piles growing with its filled slots, each a framed
 * {@code ModBlocks.crate} item at twice its size as 1.7's dummy {@code EntityItem} under {@code renderInFrame}.
 */
public class RenderTrainCargoTramTrailer extends RenderTrainCargoTram<TrainCargoTramTrailer> {

    /** 1.7's ladder of hand-placed piles, {x, y, z} per crate, indexed by ceil(slots / 5) - 1. */
    private static final double[][][] PILES = {
            {{0.0, 0.375, 0.0}},
            {{0.1, 0.375, 0.25}, {-0.1, 0.375, -0.25}},
            {{0.1, 0.375, 0.0}, {-0.1, 0.375, 0.375}, {-0.1, 0.375, -0.375}},
            {{0.2, 0.375, 0.3}, {0.2, 0.375, -0.2}, {-0.2, 0.375, 0.2}, {-0.2, 0.375, -0.3}},
            {{0.2, 0.375, 0.6}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.5}, {-0.2, 0.375, 0.2}, {-0.2, 0.375, -0.3}},
            {{0.2, 0.375, 0.6}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.5},
             {-0.2, 0.375, 0.5}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.6}},
            {{0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4},
             {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5}, {0.0, 0.6875, -0.25}},
            {{0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4},
             {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5},
             {0.0, 0.6875, -0.25}, {0.0, 0.6875, 0.15}},
            {{0.2, 0.375, 0.4}, {0.2, 0.375, 0.0}, {0.2, 0.375, -0.4},
             {-0.2, 0.375, 0.3}, {-0.2, 0.375, -0.1}, {-0.2, 0.375, -0.5},
             {0.0, 0.6875, -0.25}, {0.0, 0.6875, 0.15}, {-0.1, 0.375, 0.8}}
    };

    public RenderTrainCargoTramTrailer(EntityRendererProvider.Context context) {
        super(context, NtmEntityModels.TRAIN_CARGO_TRAM_TRAILER, ResourceManager.tram_trailer_tex);
    }

    @Override
    protected void extractCargo(TrainCargoTramTrailer entity, State state, float partialTicks) {
        if (state.occupiedSlots <= 0) {
            state.crate.clear();
            return;
        }
        FramedItem.resolve(state.crate, new ItemStack(ModDecoCrateBlocks.crate.get()), entity.level());
    }

    @Override
    protected void submitCargo(State state, PoseStack poseStack, SubmitSink sink) {
        if (state.occupiedSlots <= 0 || state.crate.isEmpty()) return;

        double[][] pile = PILES[Math.min((state.occupiedSlots - 1) / 5, PILES.length - 1)];

        poseStack.pushPose();
        poseStack.scale(2F, 2F, 2F);
        for (double[] at : pile) {
            poseStack.pushPose();
            poseStack.translate(at[0], at[1], at[2]);
            FramedItem.framedBlockPrefix(poseStack);
            FramedItem.draw(poseStack, state.crate, sink, state.lightCoords);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
