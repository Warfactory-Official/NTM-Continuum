package com.hbm.client.render;

import com.hbm.blocks.generic.BlockPedestal.TileEntityPedestal;
import com.hbm.client.render.entity.FramedItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderPedestalTile}: the held stack floats one block up at 1.5 scale, bobbing by 1/16 on the viewing
 * player's age ({@code sin((ticksExisted + interp) * 0.1)}). A flat item is turned to face the viewer's yaw and
 * lifted 1/8; a 3D block keeps its own turn and sits 1/16 higher. The stack itself is 1.7's framed
 * {@code EntityItem} ({@code renderInFrame}), {@link FramedItem}'s arms - a gun takes Forge's {@code ENTITY} route.
 */
public class RenderPedestalTile implements CompatBER<TileEntityPedestal, RenderPedestalTile.State> {

    public static final class State extends CompatRenderState {
        public final CompatItemRenderState item = new CompatItemRenderState();
        FramedItem.Arm arm = FramedItem.Arm.SPRITE;
        float yaw;
        float age;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityPedestal pedestal, State state, float partialTicks, Vec3 cameraPosition) {
        if (pedestal.getItem().isEmpty()) {
            state.item.clear();
            return;
        }
        state.arm = FramedItem.resolveArm(state.item, pedestal.getItem(), pedestal.getLevel());
        LocalPlayer player = Minecraft.getInstance().player;
        state.yaw = player == null ? 0F : Mth.lerp(partialTicks, player.yRotO, player.getYRot());
        state.age = player == null ? 0F : player.tickCount + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        pose.scale(1.5F, 1.5F, 1.5F);
        float bob = (float) (Math.sin(state.age * 0.1) * 0.0625);

        if (!state.arm.block()) {
            pose.translate(0, 0.125, 0);
            pose.mulPose(Axis.YN.rotationDegrees(state.yaw + 180));
            pose.translate(0, bob, 0);
            FramedItem.framedFlatArm(pose, state.arm);
        } else {
            pose.translate(0, bob + 0.0625, 0);
            FramedItem.framedBlockPrefix(pose);
        }
        FramedItem.draw(pose, state.item, sink, state.lightCoords);
        pose.popPose();
    }
}
