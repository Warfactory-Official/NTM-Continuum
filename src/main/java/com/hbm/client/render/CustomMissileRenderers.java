package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.entity.MissilePronter;
import com.hbm.handler.MissileStruct;
import com.hbm.items.ModMissileItems;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemMissileStandard.MissileFormFactor;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.bomb.TileEntityCompactLauncher;
import com.hbm.tileentity.bomb.TileEntityLaunchPadLarge;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import com.hbm.tileentity.machine.TileEntityMachineMissileAssembly;
import com.hbm.blocks.machine.MachineMissileAssembly;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The five 1.7 tile renderers of the custom missile family: {@code RenderMissileAssembly},
 * {@code RenderCompactLauncher}, {@code RenderLaunchTable}, {@code RenderLaunchPadLarge} and
 * {@code RenderLaunchPadRusted}, each a straight transcription of its GL call chain onto a pose stack. The
 * custom missile itself is drawn by {@link MissilePronter}, the same code the flying missile uses.
 */
public final class CustomMissileRenderers {

    private CustomMissileRenderers() {}

    private static WavefrontObj obj(String path) {
        return WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/" + path));
    }

    private static Identifier tex(String path) {
        return Ids.of(Constants.MOD_ID, "textures/models/" + path);
    }

    private static void draw(SubmitSink sink, PoseStack pose, WavefrontObj model, RenderType type, int light) {
        sink.submitCustomGeometry(pose, type, (p, buffer) -> model.renderAll(p, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    private static void drawPart(SubmitSink sink, PoseStack pose, WavefrontObj model, String part, RenderType type, int light) {
        sink.submitCustomGeometry(pose, type, (p, buffer) -> model.renderPart(part, p, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    /** A block entity's missile struct plus the light and one float of facing. */
    public static class MissileState extends CompatRenderState {
        public @Nullable MissileStruct load;
        public float yaw;
        public PartSize padSize = PartSize.SIZE_10;
        public int tableHeight = 10;
    }

    // --- RenderMissileAssembly ---

    public static final class Assembly implements CompatBER<TileEntityMachineMissileAssembly, MissileState> {
        private final WavefrontObj rack = obj("missile_assembly.obj");
        private final WavefrontObj strut = obj("strut.obj");
        private final RenderType rackSkin = CompatRenderTypes.entityCutoutNoCull(tex("missile_assembly.png"));
        private final RenderType strutSkin = CompatRenderTypes.entityCutoutNoCull(tex("strut.png"));
        private final MissilePronter pronter = new MissilePronter();

        @Override public MissileState createRenderState() { return new MissileState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public int viewDistance() { return 256; }

        @Override
        public void extract(TileEntityMachineMissileAssembly te, MissileState state, float partialTicks, Vec3 camera) {
            state.load = te.load;
            // 1.7 meta 2 -> 180, 4 -> 270, 3 -> 0, 5 -> 90
            state.yaw = switch (te.getBlockState().getValue(MachineMissileAssembly.FACING)) {
                case NORTH -> 180F;
                case WEST -> 270F;
                case EAST -> 90F;
                default -> 0F;
            };
        }

        @Override
        public void submit(MissileState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
            pose.pushPose();
            pose.translate(0.5F, 0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
            draw(sink, pose, rack, rackSkin, state.lightCoords);

            MissileStruct missile = state.load;
            if (missile != null) {
                int range = (int) (missile.height() / 2 - 1);
                int step = range >= 2 ? 2 : 1;
                for (int i = -range; i <= range; i += step) {
                    if (i != 0) {
                        pose.translate(i, 0F, 0F);
                        draw(sink, pose, strut, strutSkin, state.lightCoords);
                        pose.translate(-i, 0F, 0F);
                    }
                }
                pose.translate(0F, 1.5F, 0F);
                pose.mulPose(Axis.ZP.rotationDegrees(180));
                pose.translate(-missile.height() / 2, 0, 0);
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.mulPose(Axis.ZP.rotationDegrees(-90));
                pronter.pront(missile, pose, sink, state.lightCoords);
            }
            pose.popPose();
        }
    }

    // --- RenderCompactLauncher ---

    public static final class Compact implements CompatBER<TileEntityCompactLauncher, MissileState> {
        private final WavefrontObj base = obj("compact_launcher.obj");
        private final RenderType skin = CompatRenderTypes.entityCutoutCull(tex("compact_launcher.png"));
        private final MissilePronter pronter = new MissilePronter();

        @Override public MissileState createRenderState() { return new MissileState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public int viewDistance() { return 256; }

        @Override
        public void extract(TileEntityCompactLauncher te, MissileState state, float partialTicks, Vec3 camera) {
            state.load = te.load;
        }

        @Override
        public void submit(MissileState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
            pose.pushPose();
            pose.translate(0.5F, 0F, 0.5F);
            draw(sink, pose, base, skin, state.lightCoords);
            pose.translate(0F, 1.0625F, 0F);
            if (state.load != null) pronter.pront(state.load, pose, sink, state.lightCoords);
            pose.popPose();
        }
    }

    // --- RenderLaunchTable ---

    public static final class Table implements CompatBER<TileEntityLaunchTable, MissileState> {
        private final WavefrontObj base = obj("launch_table/launch_table_base.obj");
        private final WavefrontObj smallPad = obj("launch_table/launch_table_small_pad.obj");
        private final WavefrontObj largePad = obj("launch_table/launch_table_large_pad.obj");
        private final WavefrontObj largeBase = obj("launch_table/launch_table_large_scaffold_base.obj");
        private final WavefrontObj largeConnector = obj("launch_table/launch_table_large_scaffold_connector.obj");
        private final WavefrontObj largeEmpty = obj("launch_table/launch_table_large_scaffold_empty.obj");
        private final WavefrontObj smallBase = obj("launch_table/launch_table_small_scaffold_base.obj");
        private final WavefrontObj smallConnector = obj("launch_table/launch_table_small_scaffold_connector.obj");
        private final WavefrontObj smallEmpty = obj("launch_table/launch_table_small_scaffold_empty.obj");
        private final RenderType baseSkin = CompatRenderTypes.entityCutoutCull(tex("missile_parts/launch_table.png"));
        private final RenderType smallPadSkin = CompatRenderTypes.entityCutoutCull(tex("missile_parts/launch_table_small_pad.png"));
        private final RenderType largePadSkin = CompatRenderTypes.entityCutoutCull(tex("missile_parts/launch_table_large_pad.png"));
        private final RenderType largeBaseSkin = CompatRenderTypes.entityCutoutCull(tex("missile_parts/launch_table_large_scaffold_base.png"));
        private final RenderType largeConnectorSkin = CompatRenderTypes.entityCutoutCull(tex("missile_parts/launch_table_large_scaffold_connector.png"));
        private final RenderType smallBaseSkin = CompatRenderTypes.entityCutoutCull(tex("missile_parts/launch_table_small_scaffold_base.png"));
        private final RenderType smallConnectorSkin = CompatRenderTypes.entityCutoutCull(tex("missile_parts/launch_table_small_scaffold_connector.png"));
        private final MissilePronter pronter = new MissilePronter();

        @Override public MissileState createRenderState() { return new MissileState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public int viewDistance() { return 256; }

        @Override
        public void extract(TileEntityLaunchTable te, MissileState state, float partialTicks, Vec3 camera) {
            state.load = te.load;
            state.padSize = te.padSize;
            // 1.7 RenderLaunchTable: the scaffold follows the loaded missile and keeps its height once it has gone
            if (te.load != null && te.load.fuselage() != null) te.height = (int) te.load.height();
            state.tableHeight = te.height;
            // 1.7 meta 2 (+Z) -> 0, 0 (+X) -> 90, 3 (-Z) -> 180, 1 (-X) -> 270
            Direction facing = te.getBlockState().getValue(BlockMultiblockCore.FACING);
            state.yaw = switch (facing) {
                case EAST -> 90F;
                case NORTH -> 180F;
                case WEST -> 270F;
                default -> 0F;
            };
        }

        @Override
        public void submit(MissileState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
            int light = state.lightCoords;
            pose.pushPose();
            pose.translate(0.5F, 0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw));

            draw(sink, pose, base, baseSkin, light);
            if (state.padSize == PartSize.SIZE_10 || state.padSize == PartSize.SIZE_15) draw(sink, pose, smallPad, smallPadSkin, light);
            if (state.padSize == PartSize.SIZE_20) draw(sink, pose, largePad, largePadSkin, light);

            pose.pushPose();
            int height = (int) (state.tableHeight * 0.75);
            RenderType baseT = largeBaseSkin, connectorT = largeConnectorSkin;
            WavefrontObj baseM = largeBase, connectorM = largeConnector, emptyM = largeEmpty;
            if (state.padSize == PartSize.SIZE_10) {
                baseT = smallBaseSkin;
                connectorT = smallConnectorSkin;
                baseM = smallBase;
                connectorM = smallConnector;
                emptyM = smallEmpty;
                pose.translate(0F, 0F, -1F);
            }
            pose.translate(0F, 1F, 3.5F);
            boolean fits = state.load != null && state.load.fuselage() != null && state.load.fuselage().top == state.padSize;
            for (int i = 0; i < state.tableHeight + 1; i++) {
                if (i < height) {
                    draw(sink, pose, baseM, baseT, light);
                } else if (i > height) {
                    draw(sink, pose, emptyM, baseT, light);
                } else if (fits) {
                    draw(sink, pose, connectorM, connectorT, light);
                } else {
                    draw(sink, pose, baseM, baseT, light);
                }
                pose.translate(0F, 1F, 0F);
            }
            pose.popPose();

            pose.translate(0F, 2.0625F, 0F);
            if (fits) pronter.pront(state.load, pose, sink, light);
            pose.popPose();
        }
    }

    // --- RenderLaunchPadLarge ---

    public static final class PadLargeState extends CompatRenderState {
        public float yaw;
        public int formFactor = -1;
        public float erector;
        public float lift;
        public boolean erected;
        public boolean readyToLoad;
        public @Nullable ItemStack toRender;
    }

    public static final class PadLarge implements CompatBER<TileEntityLaunchPadLarge, PadLargeState> {
        private final WavefrontObj erector = obj("weapons/launch_pad_erector.obj");
        private final RenderType padSkin = CompatRenderTypes.entityCutoutCull(tex("launchpad/pad.png"));
        private final Map<String, RenderType> skins = new HashMap<>();

        @Override public PadLargeState createRenderState() { return new PadLargeState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public int viewDistance() { return 256; }

        @Override
        public void extract(TileEntityLaunchPadLarge pad, PadLargeState state, float f, Vec3 camera) {
            // 1.7 meta-10: 2 -> 90, 4 -> 180, 3 -> 270, 5 -> 0
            state.yaw = switch (MachineCore.facingOrdinal(pad.getBlockState(), 5)) {
                case 2 -> 90F;
                case 4 -> 180F;
                case 3 -> 270F;
                default -> 0F;
            };
            state.formFactor = pad.formFactor;
            state.erector = pad.prevErector + (pad.erector - pad.prevErector) * f;
            state.lift = pad.prevLift + (pad.lift - pad.prevLift) * f;
            state.erected = pad.erected;
            state.readyToLoad = pad.readyToLoad;
            state.toRender = pad.toRender;
        }

        @Override
        public void submit(PadLargeState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
            int light = state.lightCoords;
            pose.pushPose();
            pose.translate(0.5F, 0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw));

            drawPart(sink, pose, erector, "Pad", padSkin, light);

            if (state.formFactor >= 0 && state.formFactor < MissileFormFactor.values().length) {
                String prefix;
                double[] offset;
                String skin;
                switch (MissileFormFactor.values()[state.formFactor]) {
                    case MICRO -> { prefix = "Micro"; offset = new double[]{1.5D, 1.25D}; skin = "erector_micro.png"; }
                    case V2 -> { prefix = "V2"; offset = new double[]{1.75D, 1.25D}; skin = "erector_v2.png"; }
                    case STRONG -> { prefix = "Strong"; offset = new double[]{3D, 1.5D}; skin = "erector_strong.png"; }
                    case HUGE -> { prefix = "Huge"; offset = new double[]{3D, 1.5D}; skin = "erector_huge.png"; }
                    case ATLAS -> { prefix = "Atlas"; offset = new double[]{4D, 1.5D}; skin = "erector_atlas.png"; }
                    default -> { prefix = "ABM"; offset = new double[]{1.5D, 1.25D}; skin = "erector_abm.png"; }
                }
                RenderType erectorSkin = skins.computeIfAbsent(skin, k -> CompatRenderTypes.entityCutoutCull(tex("launchpad/" + k)));

                pose.pushPose();
                drawPart(sink, pose, erector, prefix + "_Pad", erectorSkin, light);
                if (state.toRender != null && state.erected) drawPart(sink, pose, erector, prefix + "_Rope", erectorSkin, light);
                pose.translate(0, offset[1], -offset[0]);
                pose.mulPose(Axis.XP.rotationDegrees(-state.erector));
                pose.translate(0, -offset[1], offset[0]);
                drawPart(sink, pose, erector, prefix + "_Pivot", erectorSkin, light);
                pose.translate(0, state.lift, 0);
                drawPart(sink, pose, erector, prefix + "_Erector", erectorSkin, light);

                if (state.erected) {
                    pose.popPose();
                    pose.pushPose();
                }

                if (state.toRender != null && (state.erected || state.readyToLoad)) {
                    MissileModels.MissileModel body = MissileModels.get(state.toRender.getItem());
                    if (body != null) {
                        pose.translate(0, 2, 0);
                        pose.scale(body.scale(), body.scale(), body.scale());
                        draw(sink, pose, body.model(), body.surface().renderType(), light);
                    }
                }
                pose.popPose();
            }
            pose.popPose();
        }
    }

    // --- RenderLaunchPadRusted ---

    public static final class PadRustedState extends CompatRenderState {
        public float yaw;
        public boolean missileLoaded;
    }

    public static final class PadRusted implements CompatBER<TileEntityLaunchPadRusted, PadRustedState> {
        private final WavefrontObj silo = obj("weapons/launch_pad_silo.obj");
        private final RenderType siloSkin = CompatRenderTypes.entityCutoutCull(tex("launchpad/silo_rusted.png"));

        @Override public PadRustedState createRenderState() { return new PadRustedState(); }
        @Override public boolean shouldRenderOffScreen() { return true; }
        @Override public int viewDistance() { return 256; }

        @Override
        public void extract(TileEntityLaunchPadRusted pad, PadRustedState state, float f, Vec3 camera) {
            state.yaw = switch (MachineCore.facingOrdinal(pad.getBlockState(), 5)) {
                case 2 -> 90F;
                case 4 -> 180F;
                case 3 -> 270F;
                default -> 0F;
            };
            state.missileLoaded = pad.missileLoaded;
        }

        @Override
        public void submit(PadRustedState state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
            int light = state.lightCoords;
            pose.pushPose();
            pose.translate(0.5F, 0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
            draw(sink, pose, silo, siloSkin, light);
            if (state.missileLoaded) {
                MissileModels.MissileModel body = MissileModels.get(ModMissileItems.missile_doomsday_rusted.get());
                if (body != null) {
                    pose.translate(0, 1, 0);
                    pose.scale(body.scale(), body.scale(), body.scale());
                    draw(sink, pose, body.model(), body.surface().renderType(), light);
                }
            }
            pose.popPose();
        }
    }
}
