package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared static-model and hazard-diamond pass for the two old giant storage machines. */
public abstract class GiantTankRenderer<T extends TileEntityMachineGiantTank>
        extends GemBER<T, GiantTankRenderState> {

    private static final Identifier DANGER = Ids.of(Constants.MOD_ID, "textures/models/misc/danger_diamond.png");
    protected final WavefrontObj model;
    protected final Surface body;
    protected final RenderType dangerType = CompatRenderTypes.entityTranslucentEmissive(DANGER);

    protected GiantTankRenderer(String modelName, String textureName) {
        model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/" + modelName + ".obj"));
        body = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/" + textureName + ".png");
    }

    @Override
    protected final void setup(Gem.Model<GiantTankRenderState> declaration) {
        placeMachine(declaration.offScreen().viewDistance(256).part(model.all(), body)
                .translate(0.5F, 0F, 0.5F));
    }

    /**
     * The machine's own placement, stated for the declared body. Its twin
     * {@link #applyMachineTransform} says the same thing to a pose stack, for the contents and the
     * hazard plate — those are built from the fill level rather than placed, so they keep drawing.
     */
    protected abstract void placeMachine(Gem.Part<GiantTankRenderState> part);

    @Override
    public GiantTankRenderState createRenderState() {
        return new GiantTankRenderState();
    }

    @Override
    public final void extract(T blockEntity, GiantTankRenderState state, float partialTicks, Vec3 cameraPosition) {
        state.fluidType = blockEntity.tankNew.getTankType();
        state.fill = blockEntity.tankNew.getFill();
        state.maxFill = blockEntity.tankNew.getMaxFill();
        state.gameTime = blockEntity.getLevel() == null ? 0L : blockEntity.getLevel().getGameTime();
        state.partialTick = partialTicks;
        extractMachine(blockEntity, state);
    }

    protected abstract void extractMachine(T blockEntity, GiantTankRenderState state);

    @Override
    protected final void submitUndeclared(GiantTankRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        applyMachineTransform(state, poseStack);
        if (state.fluidType != null && state.fluidType != Fluids.NONE) {
            submitHazards(state, poseStack, collector);
            submitFluid(state, poseStack, collector);
        }
        poseStack.popPose();
    }

    protected abstract void applyMachineTransform(GiantTankRenderState state, PoseStack poseStack);

    protected abstract void submitHazards(GiantTankRenderState state, PoseStack poseStack, SubmitSink collector);

    protected abstract void submitFluid(GiantTankRenderState state, PoseStack poseStack, SubmitSink collector);

    protected static float fillHeight(GiantTankRenderState state, float fullHeight) {
        return state.maxFill <= 0 ? 0F : state.fill * fullHeight / state.maxFill;
    }

    protected final void submitHazardQuad(PoseStack poseStack, SubmitSink collector, Hazard hazard) {
        collector.submitCustomGeometry(poseStack, dangerType,
                (pose, buffer) -> drawHazard(pose, buffer, hazard));
    }

    /** Port of DiamondPronter's four category tiles plus the optional central symbol. */
    private static void drawHazard(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, Hazard h) {
        float pixel = 1F / 256F;
        float symbolScale = 1F / 139F;
        textured(buffer, pose, 0F, 0.5F, -0.5F, 144 * pixel, 45 * pixel);
        textured(buffer, pose, 0F, 0.5F, 0.5F, 5 * pixel, 45 * pixel);
        textured(buffer, pose, 0F, -0.5F, 0.5F, 5 * pixel, 184 * pixel);
        textured(buffer, pose, 0F, -0.5F, -0.5F, 144 * pixel, 184 * pixel);

        category(buffer, pose, h.poison, 0F, 33F * symbolScale, pixel);
        category(buffer, pose, h.flammability, 33F * symbolScale, 0F, pixel);
        category(buffer, pose, h.reactivity, 0F, -33F * symbolScale, pixel);
        if (h.symbol.x != 0 || h.symbol.y != 0) {
            float size = 59F / 2F * symbolScale;
            vertex(buffer, pose, 0.01F, size - 33F * symbolScale, -size,
                    (h.symbol.x + 59) * pixel, h.symbol.y * pixel);
            vertex(buffer, pose, 0.01F, size - 33F * symbolScale, size,
                    h.symbol.x * pixel, h.symbol.y * pixel);
            vertex(buffer, pose, 0.01F, -size - 33F * symbolScale, size,
                    h.symbol.x * pixel, (h.symbol.y + 59) * pixel);
            vertex(buffer, pose, 0.01F, -size - 33F * symbolScale, -size,
                    (h.symbol.x + 59) * pixel, (h.symbol.y + 59) * pixel);
        }
    }

    private static void category(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                 int value, float yOffset, float zOffset, float pixel) {
        float width = 10F / 139F;
        float height = 14F / 139F;
        int x = value == 0 ? 125 : 5 + (value - 1) * 24;
        vertex(buffer, pose, 0.01F, height + yOffset, -width + zOffset,
                (x + 20) * pixel, 5 * pixel);
        vertex(buffer, pose, 0.01F, height + yOffset, width + zOffset,
                x * pixel, 5 * pixel);
        vertex(buffer, pose, 0.01F, -height + yOffset, width + zOffset,
                x * pixel, 33 * pixel);
        vertex(buffer, pose, 0.01F, -height + yOffset, -width + zOffset,
                (x + 20) * pixel, 33 * pixel);
    }

    private static void textured(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                                 float x, float y, float z, float u, float v) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v,
                OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v,
                OverlayTexture.NO_OVERLAY, 0x00F000F0, 0F, 1F, 0F);
    }

    protected static Hazard hazardFor(FluidType type) {
        if (type == null) return Hazard.ZERO;
        return switch (type.getName()) {
            case "STEAM" -> h(3, 0, 0, Symbol.NONE);
            case "HOTSTEAM", "SUPERHOTSTEAM", "ULTRAHOTSTEAM" -> h(4, 0, 0, Symbol.NONE);
            case "COOLANT" -> h(1, 0, 0, Symbol.NONE);
            case "COOLANT_HOT" -> h(1, 0, 0, Symbol.NONE);
            case "LAVA" -> h(4, 0, 0, Symbol.NOWATER);
            case "DEUTERIUM" -> h(3, 4, 0, Symbol.NONE);
            case "TRITIUM" -> h(3, 4, 0, Symbol.RADIATION);
            case "OIL" -> h(2, 1, 0, Symbol.NONE);
            case "HOTOIL" -> h(2, 3, 0, Symbol.NONE);
            case "HEAVYOIL" -> h(2, 1, 0, Symbol.NONE);
            case "BITUMEN" -> h(2, 0, 0, Symbol.NONE);
            case "SMEAR" -> h(2, 1, 0, Symbol.NONE);
            case "HEATINGOIL" -> h(2, 2, 0, Symbol.NONE);
            case "RECLAIMED" -> h(2, 2, 0, Symbol.NONE);
            case "PETROIL" -> h(1, 3, 0, Symbol.NONE);
            case "LUBRICANT" -> h(2, 1, 0, Symbol.NONE);
            case "NAPHTHA" -> h(2, 1, 0, Symbol.NONE);
            case "DIESEL", "LIGHTOIL", "KEROSENE" -> h(1, 2, 0, Symbol.NONE);
            case "GAS", "PETROLEUM", "BIOGAS" -> h(1, 4, 1, Symbol.NONE);
            case "LPG" -> h(1, 3, 1, Symbol.NONE);
            case "BIOFUEL" -> h(1, 2, 0, Symbol.NONE);
            case "NITAN" -> h(2, 4, 1, Symbol.NONE);
            case "UF6" -> h(4, 0, 2, Symbol.RADIATION);
            case "PUF6" -> h(4, 0, 4, Symbol.RADIATION);
            case "SAS3" -> h(5, 0, 4, Symbol.RADIATION);
            case "SCHRABIDIC" -> h(5, 0, 5, Symbol.ACID);
            case "AMAT", "ASCHRAB" -> h(5, 0, 5, Symbol.ANTIMATTER);
            case "PEROXIDE" -> h(3, 0, 3, Symbol.OXIDIZER);
            case "WATZ" -> h(4, 0, 3, Symbol.ACID);
            case "CRYOGEL" -> h(2, 0, 0, Symbol.CRYOGENIC);
            case "HYDROGEN" -> h(3, 4, 0, Symbol.CRYOGENIC);
            case "OXYGEN" -> h(3, 0, 0, Symbol.CRYOGENIC);
            case "XENON", "HELIUM3", "HELIUM4" -> h(0, 0, 0, Symbol.ASPHYXIANT);
            case "BALEFIRE" -> h(4, 4, 3, Symbol.RADIATION);
            case "MERCURY" -> h(2, 0, 0, Symbol.NONE);
            case "PAIN" -> h(2, 0, 1, Symbol.ACID);
            case "WASTEFLUID", "WASTEGAS" -> h(2, 0, 1, Symbol.RADIATION);
            case "GASOLINE", "COALGAS" -> h(1, 2, 0, Symbol.NONE);
            case "SPENTSTEAM" -> h(2, 0, 0, Symbol.NONE);
            case "FRACKSOL" -> h(1, 3, 3, Symbol.ACID);
            case "PLASMA_DT", "PLASMA_HD", "PLASMA_HT", "PLASMA_DH3" -> h(0, 4, 0, Symbol.RADIATION);
            case "PLASMA_XM" -> h(0, 4, 1, Symbol.RADIATION);
            case "PLASMA_BF" -> h(4, 5, 4, Symbol.ANTIMATTER);
            case "CARBONDIOXIDE" -> h(3, 0, 0, Symbol.ASPHYXIANT);
            case "DEATH" -> h(2, 0, 1, Symbol.ACID);
            case "ETHANOL" -> h(2, 3, 0, Symbol.NONE);
            case "HEAVYWATER" -> h(1, 0, 0, Symbol.NONE);
            case "CRACKOIL", "COALOIL", "NAPHTHA_CRACK" -> h(2, 1, 0, Symbol.NONE);
            case "HOTCRACKOIL" -> h(2, 3, 0, Symbol.NONE);
            case "LIGHTOIL_CRACK", "DIESEL_CRACK" -> h(1, 2, 0, Symbol.NONE);
            case "AROMATICS", "UNSATURATEDS" -> h(1, 4, 1, Symbol.NONE);
            case "PETROIL_LEADED" -> h(1, 3, 0, Symbol.NONE);
            case "GASOLINE_LEADED", "COALGAS_LEADED" -> h(1, 2, 0, Symbol.NONE);
            case "SULFURIC_ACID" -> h(3, 0, 2, Symbol.ACID);
            case "WOODOIL" -> h(2, 2, 0, Symbol.NONE);
            case "COALCREOSOTE" -> h(3, 2, 0, Symbol.NONE);
            case "NITRIC_ACID" -> h(3, 0, 2, Symbol.OXIDIZER);
            case "SOLVENT" -> h(2, 3, 0, Symbol.NONE);
            case "BLOOD_HOT" -> h(3, 0, 0, Symbol.NONE);
            case "SYNGAS" -> h(1, 4, 2, Symbol.NONE);
            case "OXYHYDROGEN" -> h(0, 4, 2, Symbol.NONE);
            case "RADIOSOLVENT" -> h(3, 3, 0, Symbol.NONE);
            case "HYDRAZINE" -> h(2, 3, 2, Symbol.NONE);
            case "CHLORINE" -> h(3, 0, 0, Symbol.OXIDIZER);
            case "AIRBLAST" -> h(0, 3, 0, Symbol.NONE);
            case "HEAVYOIL_VACUUM" -> h(2, 1, 0, Symbol.NONE);
            case "REFORMATE" -> h(2, 2, 0, Symbol.NONE);
            case "LIGHTOIL_VACUUM" -> h(1, 2, 0, Symbol.NONE);
            case "SOURGAS" -> h(4, 4, 0, Symbol.ACID);
            case "XYLENE" -> h(2, 3, 0, Symbol.NONE);
            case "HEATINGOIL_VACUUM" -> h(2, 2, 0, Symbol.NONE);
            case "DIESEL_REFORM", "DIESEL_CRACK_REFORM", "KEROSENE_REFORM" -> h(1, 2, 0, Symbol.NONE);
            case "REFORMGAS" -> h(1, 4, 1, Symbol.NONE);
            case "PHOSGENE" -> h(4, 0, 1, Symbol.NONE);
            case "MUSTARDGAS" -> h(4, 1, 1, Symbol.NONE);
            case "IONGEL" -> h(1, 0, 4, Symbol.NONE);
            case "OIL_COKER", "NAPHTHA_COKER" -> h(2, 1, 0, Symbol.NONE);
            case "GAS_COKER" -> h(1, 4, 0, Symbol.NONE);
            case "FISHOIL", "SUNFLOWEROIL" -> h(0, 1, 0, Symbol.NONE);
            case "NITROGLYCERIN" -> h(0, 4, 0, Symbol.NONE);
            case "REDMUD" -> h(3, 0, 4, Symbol.NONE);
            case "HEAVYWATER_HOT" -> h(1, 0, 0, Symbol.NONE);
            case "SODIUM", "SODIUM_HOT" -> h(1, 2, 3, Symbol.NONE);
            case "THORIUM_SALT", "THORIUM_SALT_HOT", "THORIUM_SALT_DEPLETED" -> h(2, 0, 3, Symbol.NONE);
            case "FULLERENE" -> h(3, 3, 3, Symbol.NONE);
            case "OIL_DS", "CRACKOIL_DS" -> h(2, 1, 0, Symbol.NONE);
            case "HOTOIL_DS", "HOTCRACKOIL_DS" -> h(2, 3, 0, Symbol.NONE);
            case "NAPHTHA_DS" -> h(2, 1, 0, Symbol.NONE);
            case "LIGHTOIL_DS" -> h(1, 2, 0, Symbol.NONE);
            case "STELLAR_FLUX" -> h(0, 4, 4, Symbol.ANTIMATTER);
            case "VITRIOL" -> h(2, 0, 1, Symbol.NONE);
            case "LEAD", "LEAD_HOT" -> h(4, 0, 0, Symbol.NONE);
            case "PERFLUOROMETHYL", "PERFLUOROMETHYL_COLD", "PERFLUOROMETHYL_HOT" -> h(1, 0, 1, Symbol.NONE);
            case "LYE", "SODIUM_ALUMINATE" -> h(3, 0, 1, Symbol.ACID);
            case "BAUXITE_SOLUTION" -> h(3, 0, 3, Symbol.ACID);
            case "FLUE" -> h(1, 4, 1, Symbol.NONE);
            default -> Hazard.ZERO;
        };
    }

    private static Hazard h(int poison, int flammability, int reactivity, Symbol symbol) {
        return new Hazard(poison, flammability, reactivity, symbol);
    }

    protected record Hazard(int poison, int flammability, int reactivity, Symbol symbol) {
        private static final Hazard ZERO = new Hazard(0, 0, 0, Symbol.NONE);
    }

    protected enum Symbol {
        NONE(0, 0), RADIATION(195, 2), NOWATER(195, 63), ACID(195, 124),
        ASPHYXIANT(195, 185), CRYOGENIC(134, 185), ANTIMATTER(73, 185), OXIDIZER(12, 185);

        final int x;
        final int y;

        Symbol(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
