package com.hbm.client.render;

import java.util.List;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.machine.ItemBatteryCreative;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.NoiseDisplacedRenderType;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Random;

import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The battery socket: the {@code Socket} cradle, and the {@code Battery} or {@code Capacitor} body of
 * whatever pack is plugged into it, wearing that pack's own texture.
 */
public class BatterySocketRenderer extends GemBER<TileEntityBatterySocket, BatterySocketRenderState> {

    private static final Surface EMPTY = Surface.cutout("hbm:textures/models/machines/battery_socket.png");

    private final WavefrontObj model = WavefrontObj.load(Ids.of("hbm", "models/battery_socket.obj"));
    private final List<String> bodyParts = List.copyOf(model.partNames());
    /**
     * 1.7's {@code battery.obj} "Supports" part, split into its own file so the pack icons need not exclude it.
     */
    private final WavefrontObj supports = WavefrontObj.load(Ids.of("hbm", "models/battery_socket_supports.obj"));
    private final WavefrontObj chuddie = WavefrontObj.load(Ids.of("hbm", "models/mobs/chuddie.obj"));
    private static final Identifier CHUDDIE = Ids.of("hbm", "textures/models/chuddie.png");
    private static final String ENERGY_BALL = "hbm:energy_ball";
    private static final Identifier NOISE = Ids.of("hbm", "textures/misc/noise_2.png");
    /** The ball's centre between chuddie's hands, in model units, and its radius in blocks (below 1, see the shader). */
    private static final float BALL_X = 0.64F, BALL_Y = 1.13F, BALL_RADIUS = 0.24F;
    private final Map<Identifier, Surface> skins = new HashMap<>();

    /**
     * Match the 1.12.2 RenderBatterySocket placement: centre, facing-rotate, then its model offset.
     */
    @Override
    protected void setup(Gem.Model<BatterySocketRenderState> declaration) {
        Gem.Part<BatterySocketRenderState> socket = declaration.offScreen().viewDistance(256)
                                                               .joint()
                                                               .translate(0.5F, 0F, 0.5F)
                                                               .angle(Gem.Axes.Y, state -> state.yaw)
                                                               .translate(-0.5F, 0F, 0.5F);

        socket.child(model.part("Socket"), EMPTY);
        // 1.7: the supports are drawn while something sits two blocks above the socket (checked once a second)
        socket.child(supports.part("Supports"), EMPTY).visibleIf(state -> state.frame);
        pack(socket, "Battery");
        pack(socket, "Capacitor");
    }

    private void pack(Gem.Part<BatterySocketRenderState> socket, String part) {
        int selected = bodyParts.indexOf(part);
        socket.child(model.part(part), EMPTY)
              .visibleIf(state -> selected == state.bodyPart)
              .skin(this::skinOf);
    }

    private Surface skinOf(BatterySocketRenderState state) {
        Identifier texture = state.batteryTexture;
        return texture == null ? EMPTY : skins.computeIfAbsent(texture, id -> Surface.cutout(id.toString()));
    }

    @Override
    public BatterySocketRenderState createRenderState() {
        return new BatterySocketRenderState();
    }

    @Override
    public void extract(TileEntityBatterySocket be, BatterySocketRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 5);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };

        var level = be.getLevel();
        state.gameTime = level == null ? 0L : level.getGameTime();
        state.interp = partialTicks;
        // 1.7 refreshed `frame` once a second in updateEntity; asked every frame here (one block lookup), as extraction is
        // not tied to the tick
        state.frame = level != null && !level.getBlockState(be.getBlockPos().above(2)).isAir();

        ItemStack stack = be.syncStack;
        state.creative = false;
        state.centrepiece = null;
        state.bodyPart = -1;
        state.other.clear();
        if (stack != null && stack.getItem() instanceof ItemBatteryPack battery) {
            var pack = battery.stats();
            state.batteryPart = pack.capacitor() ? "Capacitor" : "Battery";
            state.batteryTexture = Ids.parse(pack.texture());
        } else if (stack != null && stack.getItem() instanceof ItemBatterySC) {
            // 1.7 RenderBatterySocket: every radiovoltaic variant is the "Battery" body in battery_sc.png
            state.batteryPart = "Battery";
            state.batteryTexture = ItemBatterySC.SOCKET_TEXTURE;
        } else {
            state.batteryPart = null;
            state.batteryTexture = null;
            if (stack != null && !stack.isEmpty()) {
                if (stack.getItem() instanceof ItemBatteryCreative) {
                    state.creative = true;
                } else {
                    state.centrepiece = SocketCentrepieces.of(stack);
                    if (state.centrepiece == null) state.other.extract(level, stack, ItemDisplayContext.GROUND);
                }
            }
        }
        if (state.batteryPart != null) state.bodyPart = bodyParts.indexOf(state.batteryPart);
        state.generic = !state.other.isEmpty();
    }

    /**
     * 1.7 {@code RenderBatterySocket}: a creative battery is a spinning figure, any other item floats and spins at 1.5x,
     * and both are ringed by random electric arcs. port: the figure is the chuddie model holding an energy ball
     * (the {@code energy_ball} shader, also drawn into the bloom layer), not 1.7's horned pony.
     */
    @Override
    protected void submitUndeclared(BatterySocketRenderState state, PoseStack pose, SubmitSink sink,
                                    CameraInfo camera) {
        if (!state.creative && !state.generic && state.centrepiece == null) return;
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.translate(-0.5D, 0D, 0.5D);

        if (state.creative) {
            pose.pushPose();
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.mulPose(Axis.YN.rotationDegrees((float) ((state.gameTime % 360 + state.interp) * 25D)));
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutNoCull(CHUDDIE),
                    (p, buffer) -> chuddie.renderAll(p, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY));
            float time = (state.gameTime % 24000L + state.interp) / 20F;
            SubmitSink.CustomGeometry ball = (p, buffer) -> {
                for (int corner = 0; corner < 4; corner++) {
                    NoiseDisplacedRenderType.vertex(buffer, p, BALL_X, BALL_Y, 0F, corner + BALL_RADIUS, time, 0xFF4FA8FF);
                }
            };
            sink.submitCustomGeometry(pose, NoiseDisplacedRenderType.of(ENERGY_BALL, NOISE, false), ball);
            sink.submitCustomGeometry(pose, NoiseDisplacedRenderType.of(ENERGY_BALL, NOISE, true), ball);
            pose.popPose();
        } else if (state.centrepiece != null) {
            var centrepiece = state.centrepiece;
            pose.pushPose();
            float scale = centrepiece.scale();
            pose.scale(scale, scale, scale);
            pose.mulPose(Axis.YN.rotationDegrees((float) ((state.gameTime % 360 + state.interp)
                    * centrepiece.degreesPerTick())));
            var mesh = centrepiece.model().get();
            sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(centrepiece.texture()),
                    (p, buffer) -> mesh.renderPart(centrepiece.part(), p, buffer, state.lightCoords,
                            OverlayTexture.NO_OVERLAY));
            pose.popPose();
        } else {
            pose.pushPose();
            pose.translate(0D, 0.5D, 0D);
            pose.scale(1.5F, 1.5F, 1.5F);
            pose.mulPose(Axis.YN.rotationDegrees((float) ((state.gameTime % 360 + state.interp) * 2.5D)));
            sink.submitItem(pose, state.other, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        Random rand = new Random(state.gameTime / 5);
        rand.nextBoolean();
        int animation = (int) (GameTime.now() % 1000L) / 50;
        for (int i = -1; i <= 1; i += 2)
            for (int j = -1; j <= 1; j += 2)
                if (rand.nextInt(4) == 0) {
                    pose.pushPose();
                    pose.translate(0D, 0.75D, 0D);
                    Vec3 end = new Vec3(0.4375D * i, 1.1875D, 0.4375D * j);
                    BeamPronter.prontBeam(pose, sink, end, BeamPronter.EnumWaveType.RANDOM,
                            BeamPronter.EnumBeamType.SOLID,
                            animation, 15, 0.0625F, 0x404040, 0x002040, 3, 0.025F);
                    BeamPronter.prontBeam(pose, sink, end, BeamPronter.EnumWaveType.RANDOM,
                            BeamPronter.EnumBeamType.SOLID,
                            animation, 1, 0F, 0x404040, 0x002040, 3, 0.025F);
                    pose.popPose();
                }
        pose.popPose();
    }
}
