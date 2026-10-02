package com.hbm.wiaj.cannery;

import com.hbm.client.render.MachineRenderState;
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
import com.hbm.wiaj.cannery.CanneryFirebox.ActorFirebox;
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

public class CanneryStirling extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:machine_stirling");
    }

    @Override
    public String getName() {
        return "cannery.stirling";
    }

    public CanneryBase[] seeAlso() {
        return new CanneryBase[] {
                new CanneryFirebox()
        };
    }

    @Override
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

        CompoundTag stirling = new CompoundTag();
        stirling.putDouble("x", 2);
        stirling.putDouble("z", 2);
        stirling.putInt("rotation", 2);
        scene0.add(new ActionCreateActor(1, new ActorTileEntity(new JarRenderers.Stirling(), stirling)));
        /*
         * When rewinding, the NBT tag persists. We have to manually set all variable values with UpdateActor.
         */
        scene0.add(new ActionUpdateActor(1, "speed", 0F));
        scene0.add(new ActionUpdateActor(1, "y", 2D));
        scene0.add(new ActionUpdateActor(1, "type", 0));
        scene0.add(new ActionUpdateActor(1, "hasCog", true));
        scene0.add(new ActionUpdateActor(1, "spin", 0F));
        scene0.add(new ActionUpdateActor(1, "lastSpin", 0F));

        scene0.add(new ActionWait(10));

        scene0.add(new ActionCreateActor(2, new ActorFancyPanel(font(), 0, -45, new Object[][] {{resolveKey("cannery.stirling.0")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));

        scene0.add(new ActionCreateActor(2, new ActorFancyPanel(font(), 0, 40, new Object[][] {{resolveKey("cannery.stirling.1")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(2));
        scene0.add(new ActionWait(5));
        scene0.add(new ActionUpdateActor(0, "open", true));
        scene0.add(new ActionWait(30));
        scene0.add(new ActionUpdateActor(0, "isOn", true));
        scene0.add(new ActionCreateActor(2, new ActorFancyPanel(font(), -50, 40, new Object[][] {{new ItemStack(Items.COAL)}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionRemoveActor(2));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionUpdateActor(0, "open", false));
        scene0.add(new ActionWait(30));

        for(int i = 0; i < 60; i++) {
            scene0.add(new ActionUpdateActor(1, "speed", i / 5F));
            scene0.add(new ActionWait(1));
        }

        scene0.add(new ActionWait(20));

        scene0.add(new ActionSetTile(1, 2, 2, new Dummies.JarDummyConnector()));
        scene0.add(new ActionSetTile(0, 2, 2, new Dummies.JarDummyConnector()));
        scene0.add(new ActionSetTile(0, 1, 2, new Dummies.JarDummyConnector()));
        scene0.add(new ActionSetTile(0, 1, 3, new Dummies.JarDummyConnector()));
        scene0.add(new ActionSetBlock(0, 2, 2, block("hbm:red_cable")));
        scene0.add(new ActionSetBlock(0, 1, 2, block("hbm:red_cable")));
        scene0.add(new ActionSetBlock(0, 1, 3, CanneryFirebox.detector(false)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(0, 1, 3, CanneryFirebox.detector(true)));
        scene0.add(new ActionWait(40));

        JarScene scene1 = new JarScene(script);

        scene1.add(new ActionCreateActor(2, new ActorFancyPanel(font(), 0, -45, new Object[][] {{resolveKey("cannery.stirling.2")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));

        CompoundTag burner = new CompoundTag(); burner.putDouble("x", 2); burner.putDouble("y", 1); burner.putDouble("z", 2); burner.putInt("rotation", 5);
        scene1.add(new ActionCreateActor(0, new ActorTileEntity(new ActorBurner(), burner)));
        scene1.add(new ActionUpdateActor(1, "y", 3D));
        scene1.add(new ActionSetTile(1, 2, 2, null));
        scene1.add(new ActionSetTile(1, 3, 2, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetTile(0, 3, 2, new Dummies.JarDummyConnector()));
        scene1.add(new ActionSetBlock(0, 3, 2, block("hbm:red_cable")));

        for(int i = 0; i < 100; i++) {
            scene1.add(new ActionUpdateActor(1, "speed", (i + 60) / 5F));
            scene1.add(new ActionWait(1));
        }

        scene1.add(new ActionWait(20));
        scene1.add(new ActionUpdateActor(1, "hasCog", false));
        scene1.add(new ActionSetBlock(0, 1, 3, CanneryFirebox.detector(false)));

        for(int i = 0; i < 160; i += 10) {
            scene1.add(new ActionUpdateActor(1, "speed", (160 - i) / 5F));
            scene1.add(new ActionWait(1));
        }
        scene1.add(new ActionUpdateActor(1, "speed", 0F));

        scene1.add(new ActionWait(20));

        scene1.add(new ActionCreateActor(2, new ActorFancyPanel(font(), 0, -45, new Object[][] {{resolveKey("cannery.stirling.3")}}, 250)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));
        scene1.add(new ActionWait(20));
        scene1.add(new ActionUpdateActor(1, "hasCog", true));
        scene1.add(new ActionUpdateActor(1, "type", 1));
        scene1.add(new ActionWait(20));

        for(int i = 0; i < 60; i++) {
            scene1.add(new ActionUpdateActor(1, "speed", i / 5F));
            scene1.add(new ActionWait(1));
        }

        scene1.add(new ActionSetBlock(0, 1, 3, CanneryFirebox.detector(true)));
        scene1.add(new ActionWait(100));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    /** The oil burner: upstream rendered the whole model unrotated, as does the port's own renderer. */
    public static class ActorBurner implements ITileActorRenderer {

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            GemBER<BlockEntity, CompatRenderState> renderer = JarRenderers.gem(ModBlockEntities.HEATER_OILBURNER.get());
            if (renderer == null) return;
            MachineRenderState state = (MachineRenderState) renderer.createRenderState();
            pose.pushPose();
            pose.translate(Tags.getDoubleOr(data, "x", 0D), Tags.getDoubleOr(data, "y", 0D), Tags.getDoubleOr(data, "z", 0D));
            JarRenderers.submit(renderer, state, pose, sink, camera);
            pose.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) { }
    }
}
