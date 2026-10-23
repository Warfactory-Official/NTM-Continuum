package com.hbm.client;

import com.hbm.blocks.ModEnergyBedrockBlocks;
import com.hbm.blocks.ModEnergyNetBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.network.FluidDuctBox;
import com.hbm.blocks.network.FluidDuctPaintable;
import com.hbm.blocks.network.PowerCableBox;
import com.hbm.blocks.network.BlockCablePaintable;
import com.hbm.blocks.network.PneumoTubePaintableBlock;
import com.hbm.client.render.BoxDuctModel;
import com.hbm.client.render.CableModel;
import com.hbm.client.render.NeoDuctModel;
import com.hbm.client.render.PneumoTubeModel;
import com.hbm.client.render.PowerCableBoxModel;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.network.DuctPaint;
import com.hbm.tileentity.network.TileEntityCablePaintable;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.tileentity.network.TileEntityPneumoTubePaintable;
import dev.golem.core.client.BlockTint;
import dev.golem.core.client.Tints;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.render.Disguises;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * 1.7's world-rendered network blocks drawn into the chunk mesh as 1.7 drew them: the paintable cable and tube wear
 * their paint block with their overlay pass over it, the coated ducts' overlay and the pipes take the carried fluid's
 * colour, and the red cable, ducts, box ducts, box cables and pneumatic tubes are their 1.7 shapes.
 */
public final class ChunkModelRegistrations {

    private ChunkModelRegistrations() {}

    private static final String CABLE_OVERLAY = "hbm:blocks/red_cable_overlay";
    private static final String TUBE_OVERLAY = "hbm:blocks/pneumatic_tube_paintable_overlay";

    /** 1.7 {@code FluidDuctPaintable.colorMultiplier}: the carried type's colour, NONE's grey included; white painted. */
    private static final BlockTint DUCT_FLUID = new BlockTint() {
        @Override
        public int color(BlockState state) {
            return 0xFF000000 | Fluids.NONE.getColor();
        }

        @Override
        public int colorInWorld(BlockState state, BlockGetter level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT duct)) return color(state);
            if (duct instanceof DuctPaint.Holder painted && painted.paint().get() != null) return 0xFFFFFFFF;
            return 0xFF000000 | duct.getFluidType().getColor();
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of();
        }
    };

    /** 1.7 {@code RenderBoxDuct}: the white box duct takes {@code ColorUtil.lightenColor(type colour, 0.25)}. */
    private static final BlockTint WHITE_BOX = new BlockTint() {
        @Override
        public int color(BlockState state) {
            return lighten(Fluids.NONE.getColor());
        }

        @Override
        public int colorInWorld(BlockState state, BlockGetter level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT duct)) return color(state);
            return lighten(duct.getFluidType().getColor());
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return Set.of();
        }
    };

    private static int lighten(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r = (int) (r + (255 - r) * 0.25D);
        g = (int) (g + (255 - g) * 0.25D);
        b = (int) (b + (255 - b) * 0.25D);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static void register() {
        registerPipes();
        Disguises.register(ModEnergyNetBlocks.red_cable_paintable, new Disguises.Look() {
            @Override
            public @Nullable BlockState disguise(BlockGetter level, BlockPos pos, BlockState state) {
                if (!state.getValue(BlockCablePaintable.PAINTED)) return null;
                Block paint = level.getBlockEntity(pos) instanceof TileEntityCablePaintable cable ? cable.getPaint() : null;
                return paint == null ? null : paint.defaultBlockState();
            }

            @Override
            public void overlays(BlockGetter level, BlockPos pos, BlockState state, Disguises.Overlays out) {
                if (!state.getValue(BlockCablePaintable.OVERLAY) || disguise(level, pos, state) == null) return;
                for (Direction face : Direction.values()) out.face(face, CABLE_OVERLAY, -1);
            }
        });
        Disguises.register(ModPneumaticBlocks.PNEUMATIC_TUBE_PAINTABLE, new Disguises.Look() {
            @Override
            public @Nullable BlockState disguise(BlockGetter level, BlockPos pos, BlockState state) {
                return level.getBlockEntity(pos) instanceof TileEntityPneumoTubePaintable tube ? tube.getCamo() : null;
            }

            /** 1.7's own labelling: {@code _in} on the ejection face, {@code _out} on the insertion face. */
            @Override
            public void overlays(BlockGetter level, BlockPos pos, BlockState state, Disguises.Overlays out) {
                if (!state.getValue(PneumoTubePaintableBlock.OVERLAY)) return;
                TileEntityPneumoTubePaintable tube =
                        level.getBlockEntity(pos) instanceof TileEntityPneumoTubePaintable t ? t : null;
                for (Direction face : Direction.values()) {
                    String sprite = TUBE_OVERLAY;
                    if (tube != null && face == tube.ejectionDir) sprite = TUBE_OVERLAY + "_in";
                    else if (tube != null && face == tube.insertionDir) sprite = TUBE_OVERLAY + "_out";
                    out.face(face, sprite, -1);
                }
            }
        });
    }

    public static void registerBlockColors() {
        Tints.registerBlockTint(DUCT_FLUID, ModFluidNetBlocks.fluid_duct_paintable, ModFluidNetBlocks.fluid_duct_neo,
                ModFluidNetBlocks.fluid_duct_neo_silver, ModFluidNetBlocks.fluid_duct_neo_colored);
        Tints.registerBlockTint(WHITE_BOX, ModFluidNetBlocks.fluid_duct_box[2]);
    }

    /** 1.7 {@code FluidDuctPaintable} and its exhaust twin: the paint, with the port overlay over it (metadata 0). */
    private static final Disguises.Look PAINTED_DUCT = new Disguises.Look() {
        @Override
        public @Nullable BlockState disguise(BlockGetter level, BlockPos pos, BlockState state) {
            return level.getBlockEntity(pos) instanceof DuctPaint.Holder duct ? duct.paint().get() : null;
        }

        @Override
        public void overlays(BlockGetter level, BlockPos pos, BlockState state, Disguises.Overlays out) {
            if (!state.getValue(FluidDuctPaintable.OVERLAY) || disguise(level, pos, state) == null) return;
            for (Direction face : Direction.values()) out.face(face, "hbm:blocks/fluid_duct_paintable_overlay", -1);
        }
    };

    private static void registerPipes() {
        Disguises.register(ModFluidNetBlocks.fluid_duct_paintable, PAINTED_DUCT);
        Disguises.register(ModFluidNetBlocks.fluid_duct_paintable_block_exhaust, PAINTED_DUCT);
        Disguises.register(ModMachineBlocks.red_cable, new CableModel());
        NeoDuctModel neo = new NeoDuctModel();
        Disguises.register(ModFluidNetBlocks.fluid_duct_neo, neo);
        Disguises.register(ModFluidNetBlocks.fluid_duct_neo_silver, neo);
        Disguises.register(ModFluidNetBlocks.fluid_duct_neo_colored, neo);
        BoxDuctModel box = new BoxDuctModel();
        for (RegistryObject<FluidDuctBox>[] material : ModFluidNetBlocks.fluid_duct_box) {
            for (RegistryObject<FluidDuctBox> duct : material) Disguises.register(duct, box);
        }
        for (RegistryObject<? extends FluidDuctBox> duct : ModFluidNetBlocks.fluid_duct_exhaust) Disguises.register(duct, box);
        PowerCableBoxModel cableBox = new PowerCableBoxModel();
        for (RegistryObject<PowerCableBox> cable : ModEnergyBedrockBlocks.red_cable_box) Disguises.register(cable, cableBox);
        Disguises.register(ModPneumaticBlocks.PNEUMATIC_TUBE, new PneumoTubeModel());
    }
}
