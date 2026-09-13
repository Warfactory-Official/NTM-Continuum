package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Animated legacy combustion-engine renderer with the exact model offset, hatch pivot and canister tint. */
public final class CombustionEngineRenderer
        extends GemBER<TileEntityMachineCombustionEngine, CombustionEngineRenderState> {

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID,
            "models/machines/combustion_engine.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/combustion_engine.png");

    /**
     * The canister is tinted by the fuel it holds, and white where the fuel has no legacy tint — which
     * is the same {@code 0xFFFFFFFF} the untinted path means, so it is one part either way.
     */
    @Override
    protected void setup(Gem.Model<CombustionEngineRenderState> declaration) {
        Gem.Part<CombustionEngineRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(-0.5F, 0F, 3F);

        machine.child(model.part("Engine"), surface);
        machine.child(model.part("Canister"), surface)
                .tint(state -> state.canisterColor);
        machine.child(model.part("Hatch"), surface)
                .spin(Gem.Axes.Y, 1F, 0F, -2.6875F, state -> -state.doorAngle);
    }

    @Override
    public CombustionEngineRenderState createRenderState() {
        return new CombustionEngineRenderState();
    }

    @Override
    public void extract(TileEntityMachineCombustionEngine be, CombustionEngineRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = 90F + switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.doorAngle = be.prevDoorAngle + (be.doorAngle - be.prevDoorAngle) * partialTicks;

        Integer canister = canisterColor(be.tank.getTankType().getName());
        state.canisterColor = canister == null ? 0xFFFFFFFF : legacyCanisterTint(canister);
    }

    /** The legacy renderer tints with CD_Canister.color, not FluidType.color. */
    private static Integer canisterColor(String fluid) {
        return switch (fluid) {
            case "OIL", "CRACKOIL", "COALOIL", "OIL_DS", "CRACKOIL_DS" -> 0x424242;
            case "HEAVYOIL", "HEAVYOIL_VACUUM" -> 0x513F39;
            case "BITUMEN" -> 0x5A5877;
            case "SMEAR" -> 0x624F3B;
            case "HEATINGOIL", "HEATINGOIL_VACUUM" -> 0x694235;
            case "RECLAIMED" -> 0xF65723;
            case "PETROIL" -> 0x2369F6;
            case "LUBRICANT" -> 0xF1CC05;
            case "NAPHTHA", "NAPHTHA_CRACK", "NAPHTHA_DS" -> 0x5F6D44;
            case "DIESEL", "DIESEL_CRACK" -> 0xFF2C2C;
            case "LIGHTOIL", "LIGHTOIL_CRACK", "LIGHTOIL_DS", "LIGHTOIL_VACUUM" -> 0xB46B52;
            case "KEROSENE", "KEROSENE_REFORM" -> 0xFF377D;
            case "BIOFUEL" -> 0x9EB623;
            case "NITAN" -> 0x6B238C;
            case "GASOLINE" -> 0x2F7747;
            case "COALGAS" -> 0x2E155F;
            case "FRACKSOL" -> 0x4F887F;
            case "ETHANOL" -> 0xEAFFF3;
            case "PETROIL_LEADED" -> 0x2331F6;
            case "GASOLINE_LEADED" -> 0x2F775A;
            case "COALGAS_LEADED" -> 0x1E155F;
            case "WOODOIL" -> 0xBF7E4F;
            case "COALCREOSOTE" -> 0x285A3F;
            case "SEEDSLURRY" -> 0x7CC35E;
            case "SOLVENT" -> 0xE4E3EF;
            case "HYDRAZINE" -> 0x31517D;
            case "REFORMATE" -> 0xD180D6;
            case "XYLENE" -> 0xA380D6;
            case "DIESEL_REFORM", "DIESEL_CRACK_REFORM" -> 0xFFC500;
            default -> null;
        };
    }

    /** Convert the old {@code channel / 256F} tint to the closest 8-bit vertex colour. */
    private static int legacyCanisterTint(int rgb) {
        int r = Math.round(((rgb >> 16) & 0xFF) / 256F * 255F);
        int g = Math.round(((rgb >> 8) & 0xFF) / 256F * 255F);
        int b = Math.round((rgb & 0xFF) / 256F * 255F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
