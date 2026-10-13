package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.generic.DecoModelBlock;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.machine.Spotlight;
import com.hbm.blocks.machine.SpotlightModular;
import com.hbm.tileentity.deco.TileEntityDecoModel;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;

/**
 * One renderer for the OBJ-drawn decorations of {@code com.hbm.deco}: the three spotlight fixtures
 * (1.7 {@code RenderLight}), the steel pole and tape recorder ({@code RenderTapeBlock}), the antenna
 * top ({@code RenderAntennaTop}) and the satellite dish ({@code RenderPoleSatelliteReceiver}).
 *
 * <p>The old {@code ObjUtil} rotated vertices with 1.7's {@code Vec3.rotateAround*}. Those formulas are
 * the right-handed rotation JOML uses about Y, but the <i>inverse</i> one about X and Z (the same
 * asymmetry modern {@code Vec3.xRot/yRot/zRot} still carry), so pitch (Z) and roll (X) are negated here
 * and yaw (Y) is not; the order (roll about X, then pitch about Z, then yaw about Y) is kept.
 * The block-atlas icons the old code mapped the meshes onto are sampled as standalone textures, so the
 * OBJ UVs already span the whole 16x16 icon, which is exactly what {@code getInterpolatedU/V} did.
 */
public class DecoModelRenderer extends GemBER<TileEntityDecoModel, DecoModelRenderState> {

    private record Skin(WavefrontObj model, Surface surface) {}

    private final Map<Block, Skin> spotlights = new HashMap<>();
    private final Map<String, Geometry> spotlightParts = new HashMap<>();
    private Skin pole, antenna, tape, satellite, computer;
    private final Map<Block, Skin> variants = new HashMap<>();
    private Geometry toaster, crtMonitor, crtScreen;

    private static Skin skin(String obj, String texture) {
        return new Skin(WavefrontObj.load(Ids.of(Constants.MOD_ID, obj)), Surface.cutoutNoCull(Constants.MOD_ID + ":" + texture));
    }

    private void build() {
        Skin cage = skin("models/lights/cage_lamp.obj", "textures/blocks/cage_lamp.png");
        Skin cageOff = skin("models/lights/cage_lamp.obj", "textures/blocks/cage_lamp_off.png");
        Skin fluoro = skin("models/lights/fluorescent_lamp.obj", "textures/blocks/fluorescent_lamp.png");
        Skin fluoroOff = skin("models/lights/fluorescent_lamp.obj", "textures/blocks/fluorescent_lamp_off.png");
        Skin flood = skin("models/lights/flood_lamp.obj", "textures/blocks/flood_lamp.png");
        Skin floodOff = skin("models/lights/flood_lamp.obj", "textures/blocks/flood_lamp_off.png");
        spotlights.put(ModDecoContent.spotlight_incandescent.get(), cage);
        spotlights.put(ModDecoContent.spotlight_incandescent_off.get(), cageOff);
        spotlights.put(ModDecoContent.spotlight_fluoro.get(), fluoro);
        spotlights.put(ModDecoContent.spotlight_fluoro_off.get(), fluoroOff);
        spotlights.put(ModDecoContent.spotlight_halogen.get(), flood);
        spotlights.put(ModDecoContent.spotlight_halogen_off.get(), floodOff);
        spotlightParts.put("CageLamp", cage.model().part("CageLamp"));
        spotlightParts.put("FloodLamp", flood.model().part("FloodLamp"));
        for (String part : new String[]{"FluoroSingle", "FluoroCap", "FluoroMid"}) {
            spotlightParts.put(part, fluoro.model().part(part));
        }
        pole = skin("models/blocks/pole.obj", "textures/blocks/steel_beam.png");
        tape = skin("models/blocks/taperecorder.obj", "textures/blocks/deco_tape_recorder.png");
        antenna = skin("models/blocks/antenna_top.obj", "textures/blocks/deco_pole_top.png");
        satellite = skin("models/deco/pole_satellite_receiver.obj", "textures/models/deco/polesatellitereceiver.png");
        computer = skin("models/blocks/puter.obj", "textures/blocks/deco_computer.png");
        Skin toasterSkin = skin("models/blocks/toaster.obj", "textures/blocks/toaster_iron.png");
        toaster = toasterSkin.model().all();
        variants.put(ModDecoContent.deco_toaster_iron.get(), toasterSkin);
        variants.put(ModDecoContent.deco_toaster_steel.get(), skin("models/blocks/toaster.obj", "textures/blocks/toaster_steel.png"));
        variants.put(ModDecoContent.deco_toaster_wood.get(), skin("models/blocks/toaster.obj", "textures/blocks/toaster_wood.png"));
        Skin crtSkin = skin("models/blocks/crt.obj", "textures/blocks/crt_clean.png");
        crtMonitor = crtSkin.model().part("Monitor");
        crtScreen = crtSkin.model().part("Screen");
        variants.put(ModDecoContent.deco_crt_clean.get(), crtSkin);
        variants.put(ModDecoContent.deco_crt_broken.get(), skin("models/blocks/crt.obj", "textures/blocks/crt_broken.png"));
        variants.put(ModDecoContent.deco_crt_blinking.get(), skin("models/blocks/crt.obj", "textures/blocks/crt_blinking.png"));
        variants.put(ModDecoContent.deco_crt_bsod.get(), skin("models/blocks/crt.obj", "textures/blocks/crt_bsod.png"));
    }

    private static boolean is(DecoModelRenderState state, DecoModelBlock.Kind kind) {
        return state.block instanceof DecoModelBlock model && model.kind == kind;
    }

    @Override
    protected void setup(Gem.Model<DecoModelRenderState> declaration) {
        build();
        declaration.viewDistance(256);

        // RenderLight: translate onto the support face, rotate yaw(Y) · pitch(Z) · roll(X), draw one group.
        Gem.Part<DecoModelRenderState> fixture = declaration.joint()
                .visibleIf(state -> spotlights.containsKey(state.block))
                .pose(state -> new Matrix4f().translate(state.offX, state.offY, state.offZ)
                        .rotateY((float) Math.toRadians(state.yaw))
                        .rotateZ((float) Math.toRadians(state.pitch))
                        .rotateX((float) Math.toRadians(state.roll)));
        fixture.child(spotlightParts.get("CageLamp"), spotlights.get(ModDecoContent.spotlight_incandescent.get()).surface())
                .mesh(state -> spotlightParts.getOrDefault(state.part, spotlightParts.get("CageLamp")))
                .skin(state -> spotlights.getOrDefault(state.block, spotlights.get(ModDecoContent.spotlight_incandescent.get())).surface());

        // RenderTapeBlock: translate(x+0.5, y, z+0.5), rotateAroundY(meta rotation [- 90° for the pole]).
        Gem.Part<DecoModelRenderState> steelPole = declaration.joint()
                .visibleIf(state -> state.block == ModDecoContent.steel_poles.get())
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        steelPole.child(pole.model().all(), pole.surface());
        Gem.Part<DecoModelRenderState> recorder = declaration.joint()
                .visibleIf(state -> state.block == ModDecoContent.tape_recorder.get())
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        recorder.child(tape.model().all(), tape.surface());

        // RenderAntennaTop: translate(x+0.5, y, z+0.5), no rotation.
        declaration.joint()
                .visibleIf(state -> state.block == ModDecoContent.pole_top.get())
                .translate(0.5F, 0F, 0.5F)
                .child(antenna.model().all(), antenna.surface());

        // RenderPoleSatelliteReceiver: TESR basis translate(0.5,1.5,0.5) · rotateZ(180) · rotateY(meta) · scale(1/16).
        declaration.joint()
                .visibleIf(state -> state.block == ModDecoContent.pole_satellite_receiver.get())
                .translate(0.5F, 1.5F, 0.5F)
                .rotate(Gem.Axes.Z, 180F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .scale(0.0625F)
                .child(satellite.model().all(), satellite.surface());

        // RenderToaster: translate(x+0.5, y, z+0.5), rotateAroundY(quadrant), the subtype icon.
        declaration.joint()
                .visibleIf(state -> state.block instanceof DecoModelBlock model && model.kind.toaster())
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .child(toaster, variants.get(ModDecoContent.deco_toaster_iron.get()).surface())
                .skin(state -> variants.getOrDefault(state.block, variants.get(ModDecoContent.deco_toaster_iron.get())).surface());

        // RenderCRT: same pose; Monitor lit normally, Screen fullbright for the blinking/BSOD subtypes (old meta >= 8).
        Gem.Part<DecoModelRenderState> crt = declaration.joint()
                .visibleIf(state -> state.block instanceof DecoModelBlock model && model.kind.crt())
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
        crt.child(crtMonitor, variants.get(ModDecoContent.deco_crt_clean.get()).surface())
                .skin(state -> variants.getOrDefault(state.block, variants.get(ModDecoContent.deco_toaster_iron.get())).surface());
        crt.child(crtScreen, variants.get(ModDecoContent.deco_crt_clean.get()).surface())
                .skin(state -> variants.getOrDefault(state.block, variants.get(ModDecoContent.deco_toaster_iron.get())).surface())
                .light(state -> is(state, DecoModelBlock.Kind.CRT_BLINKING) || is(state, DecoModelBlock.Kind.CRT_BSOD)
                        ? 0xF000F0 : state.lightCoords);

        // RenderBlockDecoModel: translate(x+0.5, y+0.5, z+0.5), rotateAroundY(rotation code).
        declaration.joint()
                .visibleIf(state -> is(state, DecoModelBlock.Kind.COMPUTER))
                .translate(0.5F, 0.5F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .child(computer.model().all(), computer.surface());
    }

    @Override
    public DecoModelRenderState createRenderState() {
        return new DecoModelRenderState();
    }

    @Override
    public void extract(TileEntityDecoModel blockEntity, DecoModelRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        state.block = bs.getBlock();
        state.yaw = state.pitch = state.roll = 0F;
        state.offX = state.offY = state.offZ = 0F;
        state.part = "";
        if (state.block instanceof Spotlight spot) {
            extractSpotlight(spot, bs, blockEntity.getLevel(), blockEntity.getBlockPos(), state);
        } else if (state.block instanceof DecoModelBlock model) {
            Direction look = bs.getValue(DecoModelBlock.FACING);
            // The quadrant table shared by RenderTapeBlock/RenderToaster/RenderCRT: looking south gives 90°, west gives 0°,
            // north gives 270°, east gives 180° (1.7 meta 2/5/3/4 for the poles, meta%4 = 0/1/2/3 for the toaster/CRT).
            float quadrant = switch (look) {
                case SOUTH -> 90F;
                case WEST -> 0F;
                case NORTH -> 270F;
                default -> 180F;
            };
            switch (model.kind) {
                case POLE -> state.yaw = quadrant - 90F;
                case TAPE_RECORDER, TOASTER_IRON, TOASTER_STEEL, TOASTER_WOOD,
                     CRT_CLEAN, CRT_BROKEN, CRT_BLINKING, CRT_BSOD -> state.yaw = quadrant;
                // GL rotateY by the stored ForgeDirection (opposite of the look): EAST(look west) 90, SOUTH(look north) 180, WEST(look east) 270
                case SATELLITE -> state.yaw = switch (look) {
                    case WEST -> 90F;
                    case NORTH -> 180F;
                    case EAST -> 270F;
                    default -> 0F;
                };
                // BlockDecoModel rotation code: 0 (look south) is 180°, 1 (look north) is 0°, 2 (look east) is 270°, 3 (look west) is 90°
                case COMPUTER -> state.yaw = switch (look) {
                    case SOUTH -> 180F;
                    case EAST -> 270F;
                    case WEST -> 90F;
                    default -> 0F;
                };
                default -> state.yaw = 0F;
            }
        }
    }

    private static void extractSpotlight(Spotlight spot, BlockState bs, Level level, BlockPos pos, DecoModelRenderState state) {
        Direction dir = bs.getValue(Spotlight.FACING);
        state.offX = 0.5F - dir.getStepX() * 0.5F;
        state.offY = 0.5F - dir.getStepY() * 0.5F;
        state.offZ = 0.5F - dir.getStepZ() * 0.5F;
        float rot = getYaw(dir);
        float pitch = getPitch(dir);
        float roll = 0;
        int connectionCount = 0;
        if (spot instanceof SpotlightModular modular && level != null) {
            Direction connectionDirection = null;
            // Searching through only adjacent blocks is simple, iterate through all directions ignoring the root and its opposite
            // Once we have found any valid connection, we'll only connect in that one axis
            for (Direction availableDir : Direction.values()) {
                if (availableDir == dir || availableDir == dir.getOpposite()) continue;
                if (modular.canConnectTo(level, pos.relative(availableDir))) {
                    connectionCount++;
                    connectionDirection = availableDir;
                    break;
                }
            }
            if (connectionDirection != null) {
                // Check if we're sandwiched between two lights
                if (modular.canConnectTo(level, pos.relative(connectionDirection.getOpposite()))) {
                    connectionCount++;
                }
                roll = getRotation(connectionDirection, dir);
            }
        }
        state.part = spot.getPartName(connectionCount);
        state.yaw = (float) Math.toDegrees(rot);
        state.pitch = -(float) Math.toDegrees(pitch);
        state.roll = -(float) Math.toDegrees(roll);
    }

    // ObjUtil.getPitch / getYaw (radians, 1.7 sign convention)
    private static float getPitch(Direction dir) {
        if (dir == Direction.UP) return (float) Math.PI * -0.5F;
        if (dir == Direction.DOWN) return (float) Math.PI * 0.5F;
        return 0;
    }

    private static float getYaw(Direction dir) {
        if (dir == Direction.NORTH) return (float) Math.PI * 0.5F;
        if (dir == Direction.SOUTH) return (float) Math.PI * -0.5F;
        if (dir == Direction.WEST) return (float) Math.PI;
        return 0;
    }

    // This is very ad-hoc, which isn't ideal (RenderLight.getRotation)
    private static float getRotation(Direction dir, Direction axis) {
        float flipX = axis == Direction.DOWN || axis == Direction.NORTH || axis == Direction.WEST ? -0.5F : 0.5F;
        float addX = axis == Direction.NORTH || axis == Direction.SOUTH ? -0.5F : 0;
        boolean flipNS = axis == Direction.WEST;
        return switch (dir) {
            case NORTH -> flipNS ? (float) Math.PI : 0;
            case SOUTH -> !flipNS ? (float) Math.PI : 0;
            case EAST -> (float) Math.PI * (flipX + addX);
            case WEST -> (float) Math.PI * (-flipX + addX);
            case UP -> (float) Math.PI * -0.5F;
            case DOWN -> (float) Math.PI * 0.5F;
        };
    }
}
