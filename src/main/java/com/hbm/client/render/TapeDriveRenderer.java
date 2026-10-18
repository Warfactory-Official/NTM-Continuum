package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineTapeDrive;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderTapeDrive}: the frame, a drive per filled slot and its full-bright status light. */
public class TapeDriveRenderer extends GemBER<TileEntityMachineTapeDrive, TapeDriveRenderState> {

    private static final int RED = 0xFFFF0000;
    private static final int AMBER = 0xFFFFBF00;
    private static final int GREEN = 0xFF00FF00;

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/tape_drive.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/tape_drive.png");
    private final Surface light = Surface.emissive(Constants.MOD_ID + ":textures/models/misc/white.png");

    @Override
    protected void setup(Gem.Model<TapeDriveRenderState> declaration) {
        Gem.Part<TapeDriveRenderState> machine = declaration.joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Frame"), surface);
        for (int i = 0; i < 12; i++) {
            int slot = i;
            Gem.Part<TapeDriveRenderState> bay = machine.joint()
                    .translate(0F, 0.25F - 0.5F * (i / 6), 0.3125F - (i % 6) * 0.125F);
            bay.child(model.part("Drive"), surface)
                    .visibleIf(state -> state.tapes[slot] != TileEntityMachineTapeDrive.SLOT_EMPTY);
            bay.child(model.part("Light"), light)
                    .visibleIf(state -> state.tapes[slot] != TileEntityMachineTapeDrive.SLOT_EMPTY)
                    .tint(state -> switch (state.tapes[slot]) {
                        case TileEntityMachineTapeDrive.SLOT_EMPTY_TAPE -> AMBER;
                        case TileEntityMachineTapeDrive.SLOT_FILLED_TAPE -> GREEN;
                        default -> RED;
                    })
                    .light(state -> 0xF000F0);
        }
    }

    @Override
    public TapeDriveRenderState createRenderState() {
        return new TapeDriveRenderState();
    }

    @Override
    public void extract(TileEntityMachineTapeDrive be, TapeDriveRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = switch (be.getBlockState().getValue(MachineTapeDrive.FACING)) {
            case SOUTH -> 270F;
            case NORTH -> 90F;
            case WEST -> 180F;
            default -> 0F;
        };
        System.arraycopy(be.tapes, 0, state.tapes, 0, state.tapes.length);
    }
}
