package com.hbm.wiaj.cannery;

import com.hbm.client.render.FELRenderState;
import com.hbm.client.render.MachineRenderState;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetTile;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionUpdateActor;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.ITileActorRenderer;
import com.hbm.wiaj.actors.JarRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.data.Tags;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CannerySILEX extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:machine_silex");
    }

    @Override
    public String getName() {
        return "cannery.silex";
    }

    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(17, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);

        scene0.add(new ActionSetZoom(2, 0));

        for(int x = world.sizeX - 1; x >= 0 ; x--) {
            for(int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
            }

            scene0.add(new ActionWait(1));
        }

        scene0.add(new ActionWait(9));

        CompoundTag fel = new CompoundTag();
        fel.putDouble("x", 11D);
        fel.putDouble("y", 1D);
        fel.putDouble("z", 2D);
        fel.putInt("rotation", 5);
        fel.putInt("length", 11);
        scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorFEL(), fel)));

        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), 15, -5, new Object[][] {{resolveKey("cannery.silex.0")}}, 100)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(3));
        scene0.add(new ActionWait(10));

        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{stack("hbm:laser_crystal_co2")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 1));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{stack("hbm:laser_crystal_bismuth")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 2));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{stack("hbm:laser_crystal_cmb")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 3));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{stack("hbm:laser_crystal_dnt")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 4));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{stack("hbm:laser_crystal_digamma")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 5));
        scene0.add(new ActionWait(20));

        scene0.add(new ActionRemoveActor(3));
        scene0.add(new ActionUpdateActor(0, "mode", 0));
        scene0.add(new ActionWait(20));

        for(int y = 1; y < 4; y++) {
            for(int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(5, y, z, Blocks.STONE));
            }
            scene0.add(new ActionWait(5));
        }

        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{resolveKey("cannery.silex.1")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 3));
        scene0.add(new ActionUpdateActor(0, "length", 4));

        scene0.add(new ActionWait(40));
        scene0.add(new ActionUpdateActor(0, "length", 11));
        scene0.add(new ActionSetBlock(5, 2, 2, Blocks.FIRE));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(5, 2, 2, Blocks.AIR));
        scene0.add(new ActionWait(40));

        for(int y = 1; y < 4; y++) {
            for(int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(5, y, z, block("hbm:brick_concrete")));
            }

            if(y == 2) {
                scene0.add(new ActionUpdateActor(0, "length", 4));
            }

            scene0.add(new ActionWait(5));
        }

        scene0.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{resolveKey("cannery.silex.2")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(40));
        scene0.add(new ActionRemoveActor(3));

        for(int y = 3; y > 0; y--) {
            for(int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(5, y, z, Blocks.AIR));
            }

            if(y == 2) {
                scene0.add(new ActionUpdateActor(0, "length", 11));
            }

            scene0.add(new ActionWait(5));
        }

        scene0.add(new ActionUpdateActor(0, "mode", 0));

        JarScene scene1 = new JarScene(script);

        CompoundTag silex0 = new CompoundTag();
        silex0.putDouble("x", 5D);
        silex0.putDouble("y", 1D);
        silex0.putDouble("z", 2D);
        silex0.putInt("rotation", 2);
        scene1.add(new ActionCreateActor(1, new ActorTileEntity(new ActorSILEX(), silex0)));

        scene1.add(new ActionWait(20));

        scene1.add(new ActionCreateActor(3, new ActorFancyPanel(font(), 42, -18, new Object[][] {{resolveKey("cannery.silex.3")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(80));

        scene1.add(new ActionCreateActor(3, new ActorFancyPanel(font(), 60, 32, new Object[][] {{resolveKey("cannery.silex.4")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));

        scene1.add(new ActionWait(60));

        scene1.add(new ActionCreateActor(3, new ActorFancyPanel(font(), 12, 32, new Object[][] {{resolveKey("cannery.silex.5")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(3));
        scene1.add(new ActionWait(10));

        TileEntityFluidDuctNT duct = CanneryCentrifuge.duct(Fluids.PEROXIDE);
        scene1.add(new ActionSetTile(5, 2, 0, duct));
        scene1.add(new ActionSetTile(5, 1, 0, duct));
        scene1.add(new ActionSetTile(6, 1, 0, duct));
        scene1.add(new ActionSetTile(7, 1, 0, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetTile(5, 2, 1, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetBlock(5, 2, 0, block("hbm:fluid_duct_neo")));
        scene1.add(new ActionSetBlock(5, 1, 0, block("hbm:fluid_duct_neo")));
        scene1.add(new ActionSetBlock(6, 1, 0, block("hbm:fluid_duct_neo")));
        scene1.add(new ActionSetBlock(7, 1, 0, block("hbm:barrel_tcalloy")));

        scene1.add(new ActionWait(20));
        scene1.add(new ActionUpdateActor(0, "mode", 3));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionCreateActor(3, new ActorFancyPanel(font(), 42, -18, new Object[][] {{resolveKey("cannery.silex.6")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(80));

        scene1.add(new ActionCreateActor(3, new ActorFancyPanel(font(), 42, -18, new Object[][] {{resolveKey("cannery.silex.7")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(60));

        scene1.add(new ActionCreateActor(3, new ActorFancyPanel(font(), -7, -15, new Object[][] {{resolveKey("cannery.silex.8")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(3));
        scene1.add(new ActionWait(10));

        CompoundTag silex1 = new CompoundTag();
        silex1.putDouble("x", 1D);
        silex1.putDouble("y", 1D);
        silex1.putDouble("z", 2D);
        silex1.putInt("rotation", 2);
        scene1.add(new ActionCreateActor(2, new ActorTileEntity(new ActorSILEX(), silex1)));
        scene1.add(new ActionWait(10));

        for(int i = 1; i < 5; i++) {
            scene1.add(new ActionSetTile(i, 1, 0, duct));
            scene1.add(new ActionSetBlock(i, 1, 0, block("hbm:fluid_duct_neo")));
        }
        scene1.add(new ActionSetTile(1, 2, 0, duct));
        scene1.add(new ActionSetBlock(1, 2, 0, block("hbm:fluid_duct_neo")));
        scene1.add(new ActionSetTile(1, 2, 1, new Dummies.JarDummyConnector()));

        scene1.add(new ActionWait(20));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    /**
     * The FEL and its beam through the FEL's own renderer. Upstream picked the beam colour from the
     * crystal's GUI colour (cycling a dim hue for the visible crystal) and so does this; the beam itself is
     * the port renderer's, a straight beam from the aperture, where upstream's actor layered a spiral and a
     * jittering beam.
     */
    public static class ActorFEL implements ITileActorRenderer {

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            GemBER<BlockEntity, CompatRenderState> renderer = JarRenderers.gem(ModBlockEntities.MACHINE_FEL.get());
            if (renderer == null) return;
            FELRenderState state = (FELRenderState) renderer.createRenderState();
            state.yaw = JarRenderers.rotationYaw(Tags.getIntOr(data, "rotation", 0));
            int mode = Tags.getIntOr(data, "mode", 0);
            state.length = Tags.getIntOr(data, "length", 0);
            state.beam = mode > 0 && mode < 6;
            if (state.beam) {
                EnumWavelengths wave = EnumWavelengths.values()[mode];
                int color = wave.guiColor;
                if (color == 0) {
                    long time = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
                    color = java.awt.Color.HSBtoRGB(time / 50.0F, 0.5F, 0.1F) & 16777215;
                }
                state.color = color;
            }
            pose.pushPose();
            pose.translate(Tags.getDoubleOr(data, "x", 0D), Tags.getDoubleOr(data, "y", 0D), Tags.getDoubleOr(data, "z", 0D));
            JarRenderers.submit(renderer, state, pose, sink, camera);
            pose.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) { }
    }

    /** The SILEX through its own renderer, which adds a base yaw of 90 degrees to the state's. */
    public static class ActorSILEX implements ITileActorRenderer {

        private static final float BASE_YAW = 90F;

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            GemBER<BlockEntity, CompatRenderState> renderer = JarRenderers.gem(ModBlockEntities.MACHINE_SILEX.get());
            if (renderer == null) return;
            MachineRenderState state = (MachineRenderState) renderer.createRenderState();
            state.yaw = JarRenderers.rotationYaw(Tags.getIntOr(data, "rotation", 0)) - BASE_YAW;
            pose.pushPose();
            pose.translate(Tags.getDoubleOr(data, "x", 0D), Tags.getDoubleOr(data, "y", 0D), Tags.getDoubleOr(data, "z", 0D));
            JarRenderers.submit(renderer, state, pose, sink, camera);
            pose.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) { }
    }
}
