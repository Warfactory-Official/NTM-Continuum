package com.hbm.wiaj.cannery;

import com.hbm.blocks.machine.PowerDetector;
import com.hbm.client.render.FireboxRenderState;
import com.hbm.tileentity.ModBlockEntities;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CanneryFirebox extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:heater_firebox");
    }

    @Override
    public String getName() {
        return "cannery.firebox";
    }

    public JarScript createScript() {

        WorldInAJar world = new WorldInAJar(5, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);

        scene0.add(new ActionSetZoom(3, 0));

        for(int x = world.sizeX - 1; x >= 0 ; x--) {
            for(int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
            }
            scene0.add(new ActionWait(2));
        }

        scene0.add(new ActionWait(8));

        CompoundTag firebox = new CompoundTag(); firebox.putDouble("x", 2); firebox.putDouble("y", 1); firebox.putDouble("z", 2); firebox.putInt("rotation", 5);
        scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorFirebox(), firebox)));

        scene0.add(new ActionWait(10));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, -10, new Object[][] {{resolveKey("cannery.firebox.0")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, -10, new Object[][] {{resolveKey("cannery.firebox.1")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(5));
        scene0.add(new ActionUpdateActor(0, "open", true));
        scene0.add(new ActionWait(30));

        scene0.add(new ActionUpdateActor(0, "isOn", true));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), -50, 40, new Object[][] {{new ItemStack(Items.COAL)}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), -50, 40, new Object[][] {{stack("hbm:coke_coal")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), -50, 40, new Object[][] {{stack("hbm:solid_fuel")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), -50, 40, new Object[][] {{stack("hbm:rocket_fuel")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), -50, 40, new Object[][] {{stack("hbm:solid_fuel_bf")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionUpdateActor(0, "open", false));
        scene0.add(new ActionWait(30));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, -10, new Object[][] {{resolveKey("cannery.firebox.2")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, -10, new Object[][] {{resolveKey("cannery.firebox.3")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(10));

        JarScene scene1 = new JarScene(script);

        CompoundTag stirling = new CompoundTag();
        stirling.putDouble("x", 2);
        stirling.putDouble("y", 2);
        stirling.putDouble("z", 2);
        stirling.putInt("rotation", 2);
        stirling.putBoolean("hasCog", true);
        scene1.add(new ActionCreateActor(1, new ActorTileEntity(new JarRenderers.Stirling(), stirling)));
        scene1.add(new ActionUpdateActor(1, "speed", 0F));

        scene1.add(new ActionWait(10));
        scene1.add(new ActionCreateActor(2, new ActorFancyPanel(font(), 0, -45, new Object[][] {{resolveKey("cannery.firebox.4")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));
        scene1.add(new ActionWait(10));

        for(int i = 0; i < 60; i++) {
            scene1.add(new ActionUpdateActor(1, "speed", i / 5F));
            scene1.add(new ActionWait(1));
        }

        scene1.add(new ActionSetTile(1, 2, 2, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetTile(0, 2, 2, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetTile(0, 1, 2, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetTile(0, 1, 3, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetBlock(0, 2, 2, block("hbm:red_cable")));
        scene1.add(new ActionSetBlock(0, 1, 2, block("hbm:red_cable")));
        scene1.add(new ActionSetBlock(0, 1, 3, detector(false)));
        scene1.add(new ActionWait(10));
        scene1.add(new ActionSetBlock(0, 1, 3, detector(true)));
        scene1.add(new ActionWait(100));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    /** {@code machine_detector} with metadata 0 (off) or 1 (on). */
    static BlockState detector(boolean on) {
        BlockState state = block("hbm:machine_detector");
        return state.hasProperty(PowerDetector.ON) ? state.setValue(PowerDetector.ON, on) : state;
    }

    /**
     * Upstream drew the firebox's parts itself; this replays the firebox's own renderer. That renderer's
     * door hinge, the lit interior and the empty one are the same parts upstream picked by hand.
     */
    public static class ActorFirebox implements ITileActorRenderer {

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            GemBER<BlockEntity, CompatRenderState> renderer = JarRenderers.gem(ModBlockEntities.HEATER_FIREBOX.get());
            if (renderer == null) return;
            FireboxRenderState state = (FireboxRenderState) renderer.createRenderState();
            state.yaw = JarRenderers.rotationYaw(Tags.getIntOr(data, "rotation", 0));
            state.burning = Tags.getBooleanOr(data, "isOn", false);
            float doorAngle = Tags.getFloatOr(data, "angle", 0F);
            float prevDoorAngle = Tags.getFloatOr(data, "lastAngle", 0F);
            state.doorAngle = prevDoorAngle + (doorAngle - prevDoorAngle) * interp;
            pose.pushPose();
            pose.translate(Tags.getDoubleOr(data, "x", 0D), Tags.getDoubleOr(data, "y", 0D), Tags.getDoubleOr(data, "z", 0D));
            JarRenderers.submit(renderer, state, pose, sink, camera);
            pose.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {

            boolean open = Tags.getBooleanOr(data, "open", false);
            float doorAngle = Tags.getFloatOr(data, "angle", 0F);
            data.putFloat("lastAngle", doorAngle);

            float swingSpeed = (doorAngle / 10F) + 3;

            if(open) {
                doorAngle += swingSpeed;
            } else {
                doorAngle -= swingSpeed;
            }

            doorAngle = Math.max(0F, Math.min(135F, doorAngle));
            data.putFloat("angle", doorAngle);
        }
    }
}
