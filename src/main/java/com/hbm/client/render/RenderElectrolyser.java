package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

/** The electrolyser body and the molten streams from its two outlets, 1.7's {@code "foundry"} particles. */
public final class RenderElectrolyser extends MachineObjRenderer<TileEntityMachineElectrolyser> {

    private final RenderType streamType = CompatRenderTypes.entitySolid(
            Ids.of(Constants.MOD_ID, "textures/models/machines/lava_gray.png"));

    public RenderElectrolyser() {
        super(180F, 1F, new ModelDef(Ids.of(Constants.MOD_ID, "models/machines/electrolyser.obj"),
                Ids.of(Constants.MOD_ID, "textures/models/machines/electrolyser.png")));
    }

    public static final class ElectrolyserRenderState extends MachineRenderState {
        public ForgeDirection facing = ForgeDirection.NORTH;
        public int leftColor = -1;
        public float leftLen;
        public int rightColor = -1;
        public float rightLen;
    }

    @Override
    public MachineRenderState createRenderState() {
        return new ElectrolyserRenderState();
    }

    @Override
    public void extract(TileEntityMachineElectrolyser be, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        super.extract(be, state, partialTicks, cameraPosition);
        if (state instanceof ElectrolyserRenderState pour) {
            pour.facing = MachineCore.facing(be.getBlockState());
            pour.leftColor = be.leftPourColor;
            pour.leftLen = be.leftPourLen;
            pour.rightColor = be.rightPourColor;
            pour.rightLen = be.rightPourLen;
        }
    }

    @Override
    protected void submitUndeclared(MachineRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!(state instanceof ElectrolyserRenderState pour)) return;
        stream(poseStack, sink, pour.facing.getOpposite(), pour.leftColor, pour.leftLen);
        stream(poseStack, sink, pour.facing, pour.rightColor, pour.rightLen);
    }

    /** 1.7 spawns it at the pour point, 5.875 out along the outlet and 2 up, with base and offset 0.625. */
    private void stream(PoseStack poseStack, SubmitSink sink, ForgeDirection dir, int color, float length) {
        if (color < 0 || length <= 0F) return;
        poseStack.pushPose();
        poseStack.translate(0.5F + dir.offsetX * 5.875F, 2F, 0.5F + dir.offsetZ * 5.875F);
        sink.submitCustomGeometry(poseStack, streamType,
                (pose, buffer) -> FoundryRenderHelper.pourStream(pose, buffer, dir, length, 0.625F, 0.625F, color));
        poseStack.popPose();
    }
}
