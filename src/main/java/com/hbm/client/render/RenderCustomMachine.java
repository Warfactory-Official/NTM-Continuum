package com.hbm.client.render;

import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration.ComponentDefinition;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7 {@code RenderCustomMachine}: while a configured controller's structure is incomplete, every component is
 * drawn as a small 75%-alpha cube ({@code SmallBlockPronter}) where it belongs, in the controller's frame; a
 * component that accepts several metas shows them in turn, one per second of wall clock.
 *
 * <p>1.7 read the icons off the block. The port's small-cube pass draws standalone textures, so the block's own
 * texture is found by name: {@code hbm:textures/blocks/<id>.png} (the pillar receivers' {@code _top}/{@code _side}
 * and the terminal faces by their 1.7 icon names) or {@code <ns>:textures/block/<id>.png}. A block with no such
 * file draws nothing rather than a missing-texture cube.
 */
public class RenderCustomMachine implements CompatBER<TileEntityCustomMachine, RenderCustomMachine.State> {

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float ALPHA = 0.75F;
    private static final Map<Block, Identifier[]> TEXTURES = new HashMap<>();

    public static class State extends CompatRenderState {
        public record Ghost(BlockPos offset, Identifier top, Identifier side) {}
        public final List<Ghost> ghosts = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityCustomMachine blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public void extract(TileEntityCustomMachine custom, State state, float partialTicks, Vec3 cameraPosition) {
        state.ghosts.clear();
        MachineConfiguration config = custom.config;
        if (config == null || custom.structureOK) return;

        ForgeDirection dir = custom.facing();
        BlockPos core = custom.getBlockPos();
        for (ComponentDefinition comp : config.components) {
            if (comp.metas.isEmpty()) continue;
            BlockPos offset = TileEntityCustomMachine.componentPos(core, dir, comp).subtract(core);
            int index = (int) ((System.currentTimeMillis() / 1000) % comp.metas.size());
            Identifier[] textures = textures(comp.blocks().get(index));
            if (textures == null) continue;
            state.ghosts.add(new State.Ghost(offset, textures[0], textures[1]));
        }
    }

    /** {top, side} for a block, or null when neither file exists. */
    private static Identifier[] textures(Block block) {
        return TEXTURES.computeIfAbsent(block, b -> {
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            String ns = id.getNamespace(), path = id.getPath();
            String top = path, side = path;
            if ("hbm".equals(ns)) {
                switch (path) {
                    case "cm_flux", "cm_heat" -> { top = path + "_top"; side = path + "_side"; }
                    case "custom_machine" -> { top = "cm_terminal_side"; side = "cm_terminal_side"; }
                    case "custom_machine_anchor" -> { top = "cmt_terminal_side"; side = "cmt_terminal_side"; }
                    default -> { }
                }
            }
            String dir = "hbm".equals(ns) ? "textures/blocks/" : "textures/block/";
            Identifier topTex = Ids.of(ns, dir + top + ".png");
            Identifier sideTex = Ids.of(ns, dir + side + ".png");
            var resources = Minecraft.getInstance().getResourceManager();
            if (resources.getResource(sideTex).isEmpty()) return null;
            if (resources.getResource(topTex).isEmpty()) topTex = sideTex;
            return new Identifier[]{topTex, sideTex};
        });
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (state.ghosts.isEmpty()) return;

        Map<Identifier, List<BlockPos>> sides = new LinkedHashMap<>();
        Map<Identifier, List<BlockPos>> ends = new LinkedHashMap<>();
        for (State.Ghost ghost : state.ghosts) {
            sides.computeIfAbsent(ghost.side(), k -> new ArrayList<>()).add(ghost.offset());
            ends.computeIfAbsent(ghost.top(), k -> new ArrayList<>()).add(ghost.offset());
        }

        sides.forEach((texture, offsets) -> sink.submitCustomGeometry(poseStack,
                CompatRenderTypes.entityTranslucent(texture),
                (pose, buffer) -> {
                    for (BlockPos o : offsets) {
                        SmallBlockPronter.sideFaces(pose, buffer, o.getX(), o.getY(), o.getZ(), 1F, 1F, 1F, ALPHA, FULL_BRIGHT);
                    }
                }));
        ends.forEach((texture, offsets) -> sink.submitCustomGeometry(poseStack,
                CompatRenderTypes.entityTranslucent(texture),
                (pose, buffer) -> {
                    for (BlockPos o : offsets) {
                        SmallBlockPronter.endFaces(pose, buffer, o.getX(), o.getY(), o.getZ(), 1F, 1F, 1F, ALPHA, FULL_BRIGHT);
                    }
                }));
    }
}
