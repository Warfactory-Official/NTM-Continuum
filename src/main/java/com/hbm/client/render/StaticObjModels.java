package com.hbm.client.render;

import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.blocks.bomb.DetCord;
import com.hbm.blocks.generic.BarbedWire;
import com.hbm.blocks.generic.BlockBarrelDeco;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.generic.BlockScaffold;
import com.hbm.blocks.generic.DecoModelBlock;
import com.hbm.blocks.machine.Spotlight;
import com.hbm.blocks.machine.SpotlightModular;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.render.Disguises;
import dev.golem.core.render.gem.Geometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * OBJ blocks whose look depends only on their state (fluorescent tubes also on their neighbours), built into the chunk
 * mesh as 1.7's block renderers built them, not drawn every frame. Poses are those of their former block-entity renderers.
 */
public final class StaticObjModels {

    private StaticObjModels() {}

    private static final Map<String, Geometry> GEOMETRY = new ConcurrentHashMap<>();

    /** A whole OBJ, or one group of it as {@code file#group}. */
    private static Geometry obj(String path) {
        return GEOMETRY.computeIfAbsent(path, p -> {
            int hash = p.indexOf('#');
            return hash < 0 ? WavefrontObj.load(Ids.of("hbm", p)).all()
                    : WavefrontObj.load(Ids.of("hbm", p.substring(0, hash))).part(p.substring(hash + 1));
        });
    }

    private record Fixed(Name path, Name sprite, Pose pose) implements Disguises.Look {
        Fixed(String path, String sprite, Pose pose) {
            this(state -> path, state -> sprite, pose);
        }

        @Override
        public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
            return state;
        }

        @Override
        public void mesh(Object key, Disguises.Meshes out) {
            BlockState state = (BlockState) key;
            out.add(obj(path.of(state)), sprite.of(state), -1, pose.of(state));
        }

        @Override
        public boolean ambientOcclusion() {
            return false;
        }
    }

    @FunctionalInterface
    private interface Pose {
        Matrix4f of(BlockState state);
    }

    /** Read only while meshing: 1.20.1 has no registered blocks yet when the looks are registered. */
    @FunctionalInterface
    private interface Name {
        String of(BlockState state);
    }

    public static void register() {
        steel(ModDecoBlocks.steel_wall, "models/deco/steel_wall.obj", "hbm:models/deco/steelwall", true);
        steel(ModDecoBlocks.steel_corner, "models/deco/steel_corner.obj", "hbm:models/deco/steelcorner", true);
        // 1.7 bakes the roof with no per-facing turn, only the Z flip
        steel(ModDecoBlocks.steel_roof, "models/deco/steel_roof.obj", "hbm:models/deco/steelroof", false);
        Disguises.register(ModSteelBlocks.steel_scaffold, new Scaffold());

        Disguises.register(ModDecoContent.steel_poles, new Fixed("models/blocks/pole.obj", "hbm:blocks/steel_beam",
                state -> new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY(radians(quadrant(state) - 90F))));
        Disguises.register(ModDecoContent.tape_recorder, new Fixed("models/blocks/taperecorder.obj", "hbm:blocks/deco_tape_recorder",
                state -> new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY(radians(quadrant(state)))));
        Disguises.register(ModDecoContent.pole_top, new Fixed("models/blocks/antenna_top.obj", "hbm:blocks/deco_pole_top",
                state -> new Matrix4f().translate(0.5F, 0F, 0.5F)));
        toaster(ModDecoContent.deco_toaster_iron, "hbm:blocks/toaster_iron");
        toaster(ModDecoContent.deco_toaster_steel, "hbm:blocks/toaster_steel");
        toaster(ModDecoContent.deco_toaster_wood, "hbm:blocks/toaster_wood");
        Disguises.register(ModDecoContent.deco_computer_ibm_300pl, new Fixed("models/blocks/puter.obj", "hbm:blocks/deco_computer",
                state -> new Matrix4f().translate(0.5F, 0.5F, 0.5F).rotateY(radians(computerYaw(state)))));

        SpotlightLook lights = new SpotlightLook();
        List<Supplier<? extends Spotlight>> spots = List.of(
                ModDecoContent.spotlight_incandescent, ModDecoContent.spotlight_incandescent_off,
                ModDecoContent.spotlight_fluoro, ModDecoContent.spotlight_fluoro_off,
                ModDecoContent.spotlight_halogen, ModDecoContent.spotlight_halogen_off);
        for (Supplier<? extends Spotlight> spot : spots) {
            Disguises.register(spot, lights);
        }

        // RenderBarbedWire: -90 for an east/west placement, -180 for north/south; the spikes never turn
        for (RegistryObject<BarbedWire> wire : ModSecurityBlocks.BARBED_WIRES) {
            Disguises.register(wire, new Fixed(state -> "models/blocks/barbed_wire.obj",
                    state -> "hbm:blocks/" + ((BarbedWire) state.getBlock()).kind().texture + "_model",
                    state -> new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY(radians(
                            state.getValue(BarbedWire.FACING).getAxis() == Direction.Axis.X ? -90F : -180F))));
        }
        Disguises.register(ModSecurityBlocks.spikes, new Fixed("models/blocks/spikes.obj", "hbm:blocks/spikes",
                state -> new Matrix4f().translate(0.5F, 0F, 0.5F)));

        List<Supplier<? extends BlockBarrelDeco>> barrels = List.of(ModFluidNetBlocks.red_barrel, ModFluidNetBlocks.pink_barrel,
                ModFluidNetBlocks.yellow_barrel, ModFluidNetBlocks.vitrified_barrel, ModFluidNetBlocks.lox_barrel,
                ModFluidNetBlocks.taint_barrel);
        for (Supplier<? extends BlockBarrelDeco> barrel : barrels) {
            Disguises.register(barrel, new Fixed(state -> "models/blocks/barrel.obj#Barrel",
                    state -> "hbm:blocks/" + ((BlockBarrelDeco) state.getBlock()).barrelTexture(),
                    state -> new Matrix4f().translate(0.5F, 0F, 0.5F)));
        }
        Disguises.register(ModDecoCrateBlocks.crate_can, new Fixed("models/blocks/conservecrate.obj", "hbm:block/crate_can",
                state -> new Matrix4f().translate(0.5F, 0F, 0.5F)));
        Disguises.register(ModEnergyNetBlocks.hev_battery, new Fixed("models/blocks/battery.obj", "hbm:blocks/hev_battery",
                state -> new Matrix4f().translate(0.5F, 0F, 0.5F)));
        // the anvil OBJs are authored in 0..1 block space; the 1.12.2 blockstate "y" turns them clockwise about the centre
        for (RegistryObject<NTMAnvil> anvil : ModAnvilBlocks.ANVILS) {
            Disguises.register(anvil, new Fixed(state -> "models/anvils/" + anvil.getId().getPath() + ".obj",
                    state -> "hbm:models/anvils/" + anvil.getId().getPath(),
                    state -> new Matrix4f().translate(0.5F, 0F, 0.5F)
                            .rotateY(radians(-anvilYaw(state.getValue(NTMAnvil.FACING)))).translate(-0.5F, 0F, -0.5F)));
        }
        Disguises.register(ModExplosivesBlocks.det_cord, new DetCordLook());
    }

    private static float anvilYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90F;
            case EAST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
    }

    private static void steel(RegistryObject<Block> block, String path, String sprite, boolean turns) {
        Disguises.register(block, new Fixed(path, sprite, state -> {
            Matrix4f pose = new Matrix4f().translate(0.5F, 1.5F, 0.5F).rotateZ(radians(180F));
            if (turns) pose.rotateY(radians(legacyYaw(state.getValue(BlockStateProperties.HORIZONTAL_FACING))));
            return pose.scale(0.0625F);
        }));
    }

    private static void toaster(RegistryObject<DecoModelBlock> block, String sprite) {
        Disguises.register(block, new Fixed("models/blocks/toaster.obj", sprite,
                state -> new Matrix4f().translate(0.5F, 0F, 0.5F).rotateY(radians(quadrant(state)))));
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180F;
            case WEST -> 90F;
            case EAST -> 270F;
            default -> 0F;
        };
    }

    /** The quadrant table of RenderTapeBlock, RenderToaster and RenderCRT. */
    private static float quadrant(BlockState state) {
        return switch (state.getValue(DecoModelBlock.FACING)) {
            case SOUTH -> 90F;
            case WEST -> 0F;
            case NORTH -> 270F;
            default -> 180F;
        };
    }

    /** BlockDecoModel's rotation code: look south 180, north 0, east 270, west 90. */
    private static float computerYaw(BlockState state) {
        return switch (state.getValue(DecoModelBlock.FACING)) {
            case SOUTH -> 180F;
            case EAST -> 270F;
            case WEST -> 90F;
            default -> 0F;
        };
    }

    /** 1.7 RenderScaffoldBlock: {@code rotY(rotation) * rotZ(-pitch)} about each orientation's origin. */
    private static final class Scaffold implements Disguises.Look {
        @Override
        public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
            return state;
        }

        @Override
        public void mesh(Object key, Disguises.Meshes out) {
            BlockState state = (BlockState) key;
            Matrix4f pose = new Matrix4f().translate(0.5F, 0.5F, 0.5F);
            switch (state.getValue(BlockScaffold.ORIENT)) {
                case HORIZONTAL_NS -> pose.rotateY(radians(-90F)).translate(0F, -0.5F, 0F);
                case HORIZONTAL_EW -> pose.rotateY(radians(-180F)).translate(0F, -0.5F, 0F);
                case VERTICAL_NS -> pose.translate(0F, 0F, -0.5F).rotateY(radians(-90F)).rotateZ(radians(-90F));
                case VERTICAL_EW -> pose.translate(0.5F, 0F, 0F).rotateY(radians(-180F)).rotateZ(radians(-90F));
            }
            out.add(obj("models/blocks/scaffold.obj"), "hbm:blocks/" + state.getValue(BlockScaffold.VARIANT).texture, -1, pose);
        }

        @Override
        public boolean ambientOcclusion() {
            return false;
        }
    }

    /** 1.7 RenderDetCord's part table on {@code cable_neo.obj}; NORTH draws {@code posZ}, as upstream does. */
    private static final class DetCordLook implements Disguises.Look {
        @Override
        public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
            int mask = (state.getValue(DetCord.EAST) ? 32 : 0) + (state.getValue(DetCord.WEST) ? 16 : 0)
                    + (state.getValue(DetCord.UP) ? 8 : 0) + (state.getValue(DetCord.DOWN) ? 4 : 0)
                    + (state.getValue(DetCord.SOUTH) ? 2 : 0) + (state.getValue(DetCord.NORTH) ? 1 : 0);
            if (mask == 0b110000 || mask == 0b100000 || mask == 0b010000) return Set.of("CX");
            if (mask == 0b001100 || mask == 0b001000 || mask == 0b000100) return Set.of("CY");
            if (mask == 0b000011 || mask == 0b000010 || mask == 0b000001) return Set.of("CZ");
            Set<String> parts = new HashSet<>();
            parts.add("Core");
            if ((mask & 32) != 0) parts.add("posX");
            if ((mask & 16) != 0) parts.add("negX");
            if ((mask & 8) != 0) parts.add("posY");
            if ((mask & 4) != 0) parts.add("negY");
            if ((mask & 1) != 0) parts.add("posZ");
            if ((mask & 2) != 0) parts.add("negZ");
            return Set.copyOf(parts);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void mesh(Object key, Disguises.Meshes out) {
            for (String part : (Set<String>) key) {
                out.add(obj("models/blocks/cable_neo.obj#" + part), "hbm:blocks/det_cord", -1, CableModel.CENTRE);
            }
        }

        @Override
        public boolean ambientOcclusion() {
            return false;
        }
    }

    private record Light(Block block, Direction facing, String part, float roll) {}

    /** 1.7 RenderLight: ObjUtil's Vec3 turns about X and Z are the inverse of JOML's, so pitch and roll are negated. */
    private static final class SpotlightLook implements Disguises.Look {
        @Override
        public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
            Spotlight spot = (Spotlight) state.getBlock();
            Direction dir = state.getValue(Spotlight.FACING);
            float roll = 0F;
            int connections = 0;
            if (spot instanceof SpotlightModular modular) {
                Direction along = null;
                for (Direction candidate : Direction.values()) {
                    if (candidate == dir || candidate == dir.getOpposite()) continue;
                    if (modular.canConnectTo(level, pos.relative(candidate))) {
                        connections++;
                        along = candidate;
                        break;
                    }
                }
                if (along != null) {
                    if (modular.canConnectTo(level, pos.relative(along.getOpposite()))) connections++;
                    roll = rotation(along, dir);
                }
            }
            return new Light(spot, dir, spot.getPartName(connections), roll);
        }

        @Override
        public void mesh(Object key, Disguises.Meshes out) {
            Light light = (Light) key;
            Direction dir = light.facing();
            Matrix4f pose = new Matrix4f()
                    .translate(0.5F - dir.getStepX() * 0.5F, 0.5F - dir.getStepY() * 0.5F, 0.5F - dir.getStepZ() * 0.5F)
                    .rotateY(yaw(dir))
                    .rotateZ(-pitch(dir))
                    .rotateX(-light.roll());
            out.add(obj(lightModel(light.part()) + "#" + light.part()), sprite(light.block()), -1, pose);
        }

        @Override
        public boolean ambientOcclusion() {
            return false;
        }

        private static String lightModel(String part) {
            return switch (part) {
                case "CageLamp" -> "models/lights/cage_lamp.obj";
                case "FloodLamp" -> "models/lights/flood_lamp.obj";
                default -> "models/lights/fluorescent_lamp.obj";
            };
        }

        private static String sprite(Block block) {
            if (block == ModDecoContent.spotlight_incandescent.get()) return "hbm:blocks/cage_lamp";
            if (block == ModDecoContent.spotlight_incandescent_off.get()) return "hbm:blocks/cage_lamp_off";
            if (block == ModDecoContent.spotlight_fluoro.get()) return "hbm:blocks/fluorescent_lamp";
            if (block == ModDecoContent.spotlight_fluoro_off.get()) return "hbm:blocks/fluorescent_lamp_off";
            if (block == ModDecoContent.spotlight_halogen.get()) return "hbm:blocks/flood_lamp";
            return "hbm:blocks/flood_lamp_off";
        }

        // ObjUtil.getPitch / getYaw (radians, 1.7 sign convention)
        private static float pitch(Direction dir) {
            if (dir == Direction.UP) return (float) Math.PI * -0.5F;
            if (dir == Direction.DOWN) return (float) Math.PI * 0.5F;
            return 0;
        }

        private static float yaw(Direction dir) {
            if (dir == Direction.NORTH) return (float) Math.PI * 0.5F;
            if (dir == Direction.SOUTH) return (float) Math.PI * -0.5F;
            if (dir == Direction.WEST) return (float) Math.PI;
            return 0;
        }

        // This is very ad-hoc, which isn't ideal (RenderLight.getRotation)
        private static float rotation(Direction dir, Direction axis) {
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
}
