package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A firebox: the shell, its door, and the interior swapped for the lit one while it burns. */
public class FireboxRenderer<T extends TileEntityFireboxBase> extends GemBER<T, FireboxRenderState> {

    private final WavefrontObj model;
    private final Surface surface;
    private final String emptyPart;
    private final boolean translateDoor;

    public FireboxRenderer() {
        this("models/machines/firebox.obj", "textures/models/machines/firebox.png", "InnerEmpty", false);
    }

    public FireboxRenderer(String modelPath, String texturePath, String emptyPart) {
        this(modelPath, texturePath, emptyPart, false);
    }

    public FireboxRenderer(String modelPath, String texturePath, String emptyPart, boolean translateDoor) {
        this.model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, modelPath));
        this.surface = Surface.cutout(Constants.MOD_ID + ":" + texturePath);
        this.emptyPart = emptyPart;
        this.translateDoor = translateDoor;
    }

    @Override
    protected void setup(Gem.Model<FireboxRenderState> declaration) {
        Gem.Part<FireboxRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Main"), surface);

        Gem.Part<FireboxRenderState> door = machine.child(model.part("Door"), surface);
        if (translateDoor) {
            door.slide(Gem.Axes.Z, state -> state.doorAngle * 0.75F / 135F);
        } else {
            door.spin(Gem.Axes.Y, 1.375F, 0F, 0.375F, state -> -state.doorAngle);
        }

        machine.child(model.part("InnerBurning"), surface)
                .visibleIf(state -> state.burning)
                .light(state -> FoundryRenderHelper.FULLBRIGHT);
        machine.child(model.part(emptyPart), surface)
                .visibleIf(state -> !state.burning);
    }

    @Override
    public FireboxRenderState createRenderState() {
        return new FireboxRenderState();
    }

    @Override
    public void extract(T be, FireboxRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 3);
        state.yaw = 90F + switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.burning = be.wasOn;
        state.doorAngle = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * partialTicks;
    }
}
