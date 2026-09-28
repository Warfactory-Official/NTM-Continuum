package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.generic.BlockTrinket;
import com.hbm.blocks.generic.BobbleFigure;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.items.ModWeaponModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.tileentity.deco.TileEntityBobble;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatItemRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderBobble}: a quarter-scale figure on its socket, turned {@code -(22.5 * rotation + 90)} about Y.
 * The socket, then the figure - the posed player model ({@code renderGuy}, whose head bobs on the wall clock), the
 * glowing pellet, the fumo or the drillgon - then the person's props ({@code renderPost}) and the name engraved in
 * the socket, red for VT.
 *
 * <p>1.7 drew the figure with blending on and culling off; it is submitted alpha-tested and double-sided, which
 * the skins' fully opaque or fully clear texels render identically. Fullbright parts (the pellet, Mellow's and
 * Abel's glow skins) use the maximum packed light, as {@code setLightmapTextureCoords(240, 240)} did.
 */
public final class RenderBobble extends GemBER<TileEntityBobble, RenderBobble.State> {

    public static final class State extends CompatRenderState {
        public float yaw;
        public int type;
        public long time;
        public final CompatItemRenderState item = new CompatItemRenderState();
    }

    private static final int FULLBRIGHT = 0xF000F0;

    private static Identifier tex(String path) {
        return Ids.of(Constants.MOD_ID, path);
    }

    static final Identifier SOCKET = tex("textures/models/trinkets/socket.png");
    static final Identifier GLOW = tex("textures/models/trinkets/glow.png");
    static final Identifier LAMP = tex("textures/blocks/fluorescent_lamp.png");
    static final Identifier MELLOW_GLOW = tex("textures/models/trinkets/mellowrpg8_glow.png");
    static final Identifier ABEL_GLOW = tex("textures/models/trinkets/abel_glow.png");
    static final Identifier HEV_HELMET = tex("textures/armor/hev_helmet.png");
    static final Identifier HAT = tex("textures/armor/hat.png");
    static final Identifier NI4NI = tex("textures/models/weapons/n_i_4_n_i.png");
    static final Identifier SHIMMER_AXE = tex("textures/models/shimmer_axe.png");
    static final Identifier MININUKE = tex("textures/models/weapons/fatman_mininuke.png");
    static final Identifier SACRED_DRAGON = tex("textures/models/weapons/double_barrel_sacred_dragon.png");

    private final WavefrontObj bobble = WavefrontObj.load(tex("models/trinkets/bobble.obj"));
    private final WavefrontObj hev = WavefrontObj.load(tex("models/armor/hev.obj"));
    private final WavefrontObj hat = WavefrontObj.load(tex("models/armor/hat.obj"));
    private final WavefrontObj ni4ni = WavefrontObj.load(tex("models/weapons/n_i_4_n_i.obj"));
    private final WavefrontObj shimmerAxe = WavefrontObj.load(tex("models/shimmer_axe.obj"));
    private final WavefrontObj fatman = WavefrontObj.load(tex("models/weapons/fatman.obj"));
    private final WavefrontObj doubleBarrel = WavefrontObj.load(tex("models/weapons/sacred_dragon.obj"));
    private final Font font = Minecraft.getInstance().font;

    @Override
    protected void setup(Gem.Model<State> declaration) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityBobble te, State state, float partialTicks, Vec3 cameraPosition) {
        state.yaw = -(22.5F * te.getBlockState().getValue(BlockTrinket.ROTATION) + 90F);
        state.type = te.typeOrdinal();
        state.time = System.currentTimeMillis();
        extractHeld(state, te.getLevel());
    }

    /** The prop the figure holds: VAER's cigarette, FRIZZLE's doubloons, ADAM29's can. */
    private static void extractHeld(State state, net.minecraft.world.level.@org.jspecify.annotations.Nullable Level level) {
        ItemStack held = switch (BobbleFigure.NAMES[state.type]) {
            case "VAER" -> new ItemStack(BuiltInRegistries.ITEM.getValue(tex("cigarette")));
            case "FRIZZLE" -> ModWeaponModItems.weapon_mod_special.get().of(GunFactory.EnumModSpecial.DOUBLOONS);
            case "ADAM29" -> new ItemStack(ModConsumableTailItems.can_redbomb.get());
            default -> ItemStack.EMPTY;
        };
        if (held.isEmpty()) state.item.clear();
        else state.item.extract(level, held, ItemDisplayContext.NONE);
    }

    private static @org.jspecify.annotations.Nullable RenderBobble itemRenderer;
    private final State itemState = new State();

    /**
     * 1.7 {@code ItemRenderLibrary}'s bobblehead item: {@code RenderBobble.instance.renderBobble(type)}, the whole
     * figure the placed block draws - socket, figure, props, glow skins and the engraved name - at the item's pose.
     */
    public static void renderItem(PoseStack pose, SubmitSink sink, int type, int light) {
        if (itemRenderer == null) itemRenderer = new RenderBobble();
        State state = itemRenderer.itemState;
        state.type = Math.floorMod(type, BobbleFigure.NAMES.length);
        state.time = System.currentTimeMillis();
        extractHeld(state, Minecraft.getInstance().level);
        itemRenderer.renderBobble(pose, sink, state, light);
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        float scale = 0.25F;
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        renderBobble(pose, sink, state, state.lightCoords);
        pose.popPose();
    }

    private void renderBobble(PoseStack pose, SubmitSink sink, State state, int light) {
        int type = state.type;
        part(pose, sink, bobble, CompatRenderTypes.entityCutoutCull(SOCKET), "Socket", light);

        Identifier skin = tex("textures/models/" + BobbleFigure.texture(type) + ".png");
        switch (BobbleFigure.body(type)) {
            case PELLET -> renderPellet(pose, sink, skin, state);
            case FUMO -> renderFumo(pose, sink, skin, state, light);
            case DRILLGON -> part(pose, sink, bobble, CompatRenderTypes.entityCutoutCull(skin), "Drillgon", light);
            case GUY -> renderGuy(pose, sink, skin, state, light);
        }

        pose.pushPose();
        renderPost(pose, sink, state, light);
        pose.popPose();

        renderSocket(pose, sink, state, light);
    }

    private void renderGuy(PoseStack pose, SubmitSink sink, Identifier skin, State state, int light) {
        int type = state.type;
        BobbleFigure.Pose figure = BobbleFigure.pose(type);
        String name = BobbleFigure.NAMES[type];
        String suffix = BobbleFigure.suffix(type);
        RenderType culled = CompatRenderTypes.entityCutoutCull(skin);
        RenderType open = CompatRenderTypes.entityCutoutNoCull(skin);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) figure.body()));

        if (name.equals("PEEP")) part(pose, sink, bobble, culled, "PeepTail", light);

        double[][] rot = {figure.leftLeg(), figure.rightLeg(), figure.leftArm(), figure.rightArm()};
        for (int limb = 0; limb < 4; limb++) {
            double[] p = BobbleFigure.PIVOTS[limb];
            pose.pushPose();
            pose.translate(p[0], p[1], p[2]);
            pose.mulPose(Axis.XP.rotationDegrees((float) rot[limb][0]));
            pose.mulPose(Axis.YP.rotationDegrees((float) rot[limb][1]));
            pose.mulPose(Axis.ZP.rotationDegrees((float) rot[limb][2]));
            pose.translate(-p[0], -p[1], -p[2]);
            part(pose, sink, bobble, open, BobbleFigure.LIMBS[limb] + suffix, light);
            pose.popPose();
        }

        part(pose, sink, bobble, open, "Body" + suffix, light);

        double speed = 0.005;
        double amplitude = 1;
        double[] head = figure.head();
        pose.pushPose();
        pose.translate(0D, 1.75D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(state.time * speed) * amplitude)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(state.time * speed + (Math.PI * 0.5)) * amplitude)));
        pose.mulPose(Axis.XP.rotationDegrees((float) head[0]));
        pose.mulPose(Axis.YP.rotationDegrees((float) head[1]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) head[2]));
        pose.translate(0D, -1.75D, 0D);
        part(pose, sink, bobble, open, "Head" + suffix, light);

        if (name.equals("VT")) part(pose, sink, bobble, open, "Horn", light);
        if (name.equals("PEEP")) part(pose, sink, bobble, open, "PeepHat", light);

        if (name.equals("VAER")) {
            pose.translate(0.25D, 1.9D, 0.075D);
            pose.mulPose(Axis.ZP.rotationDegrees(-60F));
            pose.scale(0.5F, 0.5F, 0.5F);
            renderItem(pose, sink, state, light);
        }

        if (name.equals("NOS")) {
            pose.translate(0D, 1.75D, 0D);
            pose.mulPose(Axis.XP.rotationDegrees(180F));
            float s = 0.095F;
            pose.scale(s, s, s);
            all(pose, sink, hat, CompatRenderTypes.entityCutoutNoCull(HAT), light);
        }
        pose.popPose();
        pose.popPose();
    }

    private void renderPellet(PoseStack pose, SubmitSink sink, Identifier skin, State state) {
        part(pose, sink, bobble, CompatRenderTypes.entityCutoutCull(skin), "Pellet", FULLBRIGHT);
        float alpha = 0.1F + (float) Math.sin(state.time * 0.001D) * 0.05F;
        int color = ((int) (alpha * 255F) & 0xFF) << 24 | 0xFFFF00;
        sink.submitCustomGeometry(pose, CompatRenderTypes.weaponSmoke(),
                (p, buffer) -> bobble.renderPartColor("PelletShine", p, buffer, color));
    }

    private void renderFumo(PoseStack pose, SubmitSink sink, Identifier skin, State state, int light) {
        part(pose, sink, bobble, CompatRenderTypes.entityCutoutCull(skin), "Fumo", light);
        double speed = 0.005;
        double amplitude = 1;
        pose.pushPose();
        pose.translate(0D, 0.75D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(state.time * speed) * amplitude)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(state.time * speed + (Math.PI * 0.5)) * amplitude)));
        pose.translate(0D, -0.75D, 0D);
        part(pose, sink, bobble, CompatRenderTypes.entityCutoutNoCull(skin), "FumoHead", light);
        pose.popPose();
    }

    private void renderPost(PoseStack pose, SubmitSink sink, State state, int light) {
        switch (BobbleFigure.NAMES[state.type]) {
            case "BLUEHAT" -> {
                float scale = 0.0625F;
                pose.translate(0D, 0.875D, -0.5D);
                pose.mulPose(Axis.YP.rotationDegrees(-90F));
                pose.mulPose(Axis.ZP.rotationDegrees(-160F));
                pose.scale(scale, scale, scale);
                part(pose, sink, hev, CompatRenderTypes.entityCutoutCull(HEV_HELMET), "Head", light);
            }
            case "FRIZZLE" -> {
                pose.pushPose();
                pose.translate(0.8D, 1.6D, 0.4D);
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                pose.mulPose(Axis.XP.rotationDegrees(10F));
                RenderType gun = CompatRenderTypes.entityCutoutCull(NI4NI);
                for (String part : new String[]{"FrameDark", "Grip", "FrameLight", "Cylinder", "Barrel"})
                    part(pose, sink, ni4ni, gun, part, light);
                pose.popPose();
                pose.translate(0.3D, 1.4D, -0.2D);
                pose.mulPose(Axis.XP.rotationDegrees(-100F));
                pose.scale(0.5F, 0.5F, 0.5F);
                renderItem(pose, sink, state, light);
            }
            case "ADAM29" -> {
                pose.translate(0.4D, 1.15D, 0.4D);
                pose.scale(0.5F, 0.5F, 0.5F);
                renderItem(pose, sink, state, light);
            }
            case "PHEO" -> {
                pose.translate(0.5D, 1.15D, 0.45D);
                pose.mulPose(Axis.XP.rotationDegrees(-60F));
                pose.scale(2F, 2F, 2F);
                all(pose, sink, shimmerAxe, CompatRenderTypes.entityCutoutCull(SHIMMER_AXE), light);
            }
            case "BOB" -> {
                pose.pushPose();
                pose.translate(0F, 0.6875F, 0.625F);
                pose.mulPose(Axis.XP.rotationDegrees(-90F));
                pose.scale(0.125F, 0.125F, 0.125F);
                pose.translate(-6F, 0F, 0F);
                for (int i = -1; i <= 1; i++) {
                    pose.translate(3F, 0F, 0F);
                    part(pose, sink, fatman, CompatRenderTypes.entityCutoutCull(MININUKE), "MiniNuke", light);
                }
                pose.popPose();
                pose.pushPose();
                pose.translate(0.25F, 0.3125F, -0.5F);
                pose.mulPose(Axis.XP.rotationDegrees(-90F));
                pose.mulPose(Axis.YP.rotationDegrees(90F));
                pose.scale(0.1F, 0.1F, 0.1F);
                RenderType dragon = CompatRenderTypes.entityCutoutCull(SACRED_DRAGON);
                for (String part : new String[]{"Stock", "BarrelShort", "Buckle", "Lever"})
                    part(pose, sink, doubleBarrel, dragon, part, light);
                pose.popPose();
            }
            case "MELLOW" -> {
                renderGuy(pose, sink, MELLOW_GLOW, state, FULLBRIGHT);
                part(pose, sink, bobble, CompatRenderTypes.plasmaGlow(LAMP), "Fluoro", FULLBRIGHT);
                part(pose, sink, bobble, CompatRenderTypes.plasmaGlow(GLOW), "Glow", FULLBRIGHT);
            }
            case "ABEL" -> renderGuy(pose, sink, ABEL_GLOW, state, FULLBRIGHT);
            default -> { }
        }
    }

    /** 1.7 {@code renderItemIn2D}: the icon over (0,0)..(1,1), 1/16 deep behind z = 0. */
    private void renderItem(PoseStack pose, SubmitSink sink, State state, int light) {
        if (state.item.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5D, 0.5D, -0.03125D);
        sink.submitItem(pose, state.item, light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    private void renderSocket(PoseStack pose, SubmitSink sink, State state, int light) {
        BobbleType type = BobbleType.of(state.type);
        float f3 = 0.01F;
        pose.translate(0.63D, 0.175F, 0.0D);
        pose.scale(f3, -f3, f3);
        pose.translate(0D, 0D, font.width(type.label) * 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        pose.translate(0F, 1F, 0F);
        sink.submitText(pose, font, 0F, 0F, Component.literal(type.label).getVisualOrderText(), false,
                Font.DisplayMode.POLYGON_OFFSET, light, type == BobbleType.VT ? 0xFFFF0000 : 0xFFFFFFFF, 0);
    }

    private static void part(PoseStack pose, SubmitSink sink, WavefrontObj model, RenderType type, String name, int light) {
        sink.submitCustomGeometry(pose, type,
                (p, buffer) -> model.renderPart(name, p, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    private static void all(PoseStack pose, SubmitSink sink, WavefrontObj model, RenderType type, int light) {
        sink.submitCustomGeometry(pose, type,
                (p, buffer) -> model.renderAll(p, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
