package com.hbm.wiaj.cannery;

import com.hbm.client.render.MachineRenderState;
import com.hbm.inventory.fluid.Fluids;
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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public class CanneryCentrifuge extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return stack("hbm:machine_gascent");
    }

    @Override
    public String getName() {
        return "cannery.centrifuge";
    }

    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(9, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);

        scene0.add(new ActionSetZoom(2, 0));

        for(int x = world.sizeX - 1; x >= 0 ; x--) {
            for(int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS));
            }

            if(x == 7) {
                scene0.add(new ActionSetTile(7, 1, 2, new Dummies.JarDummyConnector()));
                scene0.add(new ActionSetBlock(7, 1, 2, block("hbm:barrel_tcalloy")));
            }

            if(x == 6) {
                TileEntityFluidDuctNT duct = duct(Fluids.UF6);
                scene0.add(new ActionSetTile(6, 1, 2, duct));
                scene0.add(new ActionSetBlock(6, 1, 2, block("hbm:fluid_duct_neo")));
            }

            if(x == 5) {
                scene0.add(new ActionSetTile(5, 1, 2, new Dummies.JarDummyConnector()));
                CompoundTag cent = new CompoundTag();
                cent.putDouble("x", 5);
                cent.putDouble("y", 1);
                cent.putDouble("z", 2);
                cent.putInt("rotation", 2);
                scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorGasCent(), cent)));
            }

            scene0.add(new ActionWait(2));
        }

        scene0.add(new ActionCreateActor(1, new ActorFancyPanel(font(), -15, -50, new Object[][] {{resolveKey("cannery.centrifuge.0")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));

        JarScene scene1 = new JarScene(script);

        scene1.add(new ActionCreateActor(1, new ActorFancyPanel(font(), -15, 10, new Object[][] {{resolveKey("cannery.centrifuge.1")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.CENTER)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(1));

        scene1.add(new ActionSetZoom(4, 20));

        scene1.add(new ActionCreateActor(1, new ActorFancyPanel(font(), 0, 40, new Object[][] {{resolveKey("cannery.centrifuge.2")}}, 150)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(1));
        scene1.add(new ActionSetZoom(-2, 20));
        scene1.add(new ActionWait(20));

        CompoundTag c2 = new CompoundTag(); c2.putDouble("x", 4); c2.putDouble("y", 1); c2.putDouble("z", 2); c2.putInt("rotation", 2);
        scene1.add(new ActionCreateActor(1, new ActorTileEntity(new ActorGasCent(), c2)));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionCreateActor(2, new ActorFancyPanel(font(), 0, 0, new Object[][] {{resolveKey("cannery.centrifuge.3")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.CENTER)));

        scene1.add(new ActionWait(100));
        scene1.add(new ActionRemoveActor(2));
        scene1.add(new ActionSetZoom(-2, 20));

        scene1.add(new ActionCreateActor(2, new ActorFancyPanel(font(), 0, 0, new Object[][] {{resolveKey("cannery.centrifuge.4")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.CENTER)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));

        CompoundTag c3 = new CompoundTag(); c3.putDouble("x", 3); c3.putDouble("y", 1); c3.putDouble("z", 2); c3.putInt("rotation", 2);
        scene1.add(new ActionCreateActor(2, new ActorTileEntity(new ActorGasCent(), c3)));
        scene1.add(new ActionWait(10));
        CompoundTag c4 = new CompoundTag(); c4.putDouble("x", 2); c4.putDouble("y", 1); c4.putDouble("z", 2); c4.putInt("rotation", 2);
        scene1.add(new ActionCreateActor(3, new ActorTileEntity(new ActorGasCent(), c4)));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionCreateActor(4, new ActorFancyPanel(font(), 0, 0, new Object[][] {{resolveKey("cannery.centrifuge.5")}}, 200)
                .setColors(colorCopper).setOrientation(Orientation.CENTER)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(4));

        scene1.add(new ActionCreateActor(4, new ActorFancyPanel(font(), 28, -30, new Object[][] {{stack("hbm:upgrade_gc_speed")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionCreateActor(5, new ActorFancyPanel(font(), 45, 35, new Object[][] {{" = ", stack("hbm:nugget_u238", 11), stack("hbm:nugget_u235")}}, 0)
                .setColors(colorCopper).setOrientation(Orientation.LEFT)));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    /** A duct carrying {@code fluid}, as upstream's {@code new TileEntityPipeBaseNT(); setType(fluid)}. */
    static TileEntityFluidDuctNT duct(com.hbm.inventory.fluid.FluidType fluid) {
        TileEntityFluidDuctNT duct = new com.hbm.tileentity.network.TileEntityFluidDuctVariant(BlockPos.ZERO, block("hbm:fluid_duct_neo"));
        duct.setFluidType(fluid);
        return duct;
    }

    /**
     * Upstream drew the centrifuge's two parts itself; this replays the gas centrifuge's own renderer, which
     * draws exactly those two parts. That renderer adds its legacy base yaw of 270 degrees to the state's,
     * so the actor's rotation is handed over less that base.
     */
    public static class ActorGasCent implements ITileActorRenderer {

        private static final float BASE_YAW = 270F;

        @Override
        public void renderActor(WorldInAJar world, PoseStack pose, SubmitSink sink, CameraInfo camera, int ticks, float interp, CompoundTag data) {
            GemBER<BlockEntity, CompatRenderState> renderer = JarRenderers.gem(ModBlockEntities.MACHINE_GAS_CENT.get());
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
