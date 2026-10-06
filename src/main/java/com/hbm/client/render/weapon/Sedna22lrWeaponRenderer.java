package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** OBJ renderers for the Gen-3 AM180, Star F, and Star F akimbo guns. */
public final class Sedna22lrWeaponRenderer extends WeaponRenderer {

    public enum Variant {
        AM180("am180", "am180"),
        STAR_F("star_f", "star_f"),
        STAR_F_AKIMBO("star_f", "star_f_elite");

        private final String model;
        private final String rendererTexture;

        Variant(String model, String rendererTexture) {
            this.model = model;
            this.rendererTexture = rendererTexture;
        }
    }

    private static final Identifier SILENCER_MODEL = Ids.of("hbm", "models/weapons/uzi.obj");
    private static final Surface SILENCER = Surface.cutout("hbm:textures/models/weapons/uzi.png");

    private final Variant variant;
    private final Identifier modelId;
    private final Surface body;
    private WavefrontObj model;
    private WavefrontObj silencerModel;

    public Sedna22lrWeaponRenderer(Variant variant) {
        this.variant = variant;
        this.modelId = Ids.of("hbm", "models/weapons/" + variant.model + ".obj");
        this.body = Surface.cutout("hbm:textures/models/weapons/" + variant.rendererTexture + ".png");
    }

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(modelId);
        return model;
    }

    /** The Star F wears the uzi's silencer, mesh and skin both. */
    private WavefrontObj silencerModel() {
        if (silencerModel == null) silencerModel = WavefrontObj.load(SILENCER_MODEL);
        return silencerModel;
    }

    private static boolean silenced(WeaponPose state, int rail) {
        return XWeaponModManager.hasUpgrade(state.stack, rail, XWeaponModManager.ID_SILENCER);
    }

    private static boolean anySilenced(WeaponPose state) {
        return silenced(state, 0) || silenced(state, 1);
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : variant == Variant.AM180 ? -0.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        if (variant == Variant.AM180) {
            standardAimingTransform(stack, pose, -offset, -offset, offset, 0, -4.1875 / 8D, 0.25);
        } else {
            standardAimingTransform(stack, pose, -1.75F * offset, -1.75F * offset, 2.5F * offset,
                    0, -7.625 / 8D, 1);
        }
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    /** The AM180's drum is the one joint that reads ammunition rather than a bus. */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        switch (variant) {
            case AM180 -> {
                am180(declaration);
                rest(declaration, "Gun", "Trigger", "Bolt", "Mag", "MagPlate");
                declaration.part(model().part("Silencer"), body)
                        .visibleIf(state -> !state.animated && silenced(state, 0));
            }
            case STAR_F -> {
                starF(held(declaration), 0, 1);
                rest(declaration, "Gun", "Slide", "Mag", "Hammer");
                declaration.part(silencerModel().part("Silencer"), SILENCER)
                        .visibleIf(state -> !state.animated && silenced(state, 0))
                        .translate(0F, 2.375F, -0.25F);
            }
            case STAR_F_AKIMBO -> {
                float offset = 0.8F;
                for (int side = -1; side <= 1; side += 2) {
                    int rail = side == -1 ? 0 : 1;
                    int mirror = side;
                    starF(held(declaration)
                            .slide(Gem.Axes.X, state -> aimLerp(-2F * offset * mirror, 0))
                            .slide(Gem.Axes.Y, state -> aimLerp(-1.75F * offset, -7.625 / 8D))
                            .slide(Gem.Axes.Z, state -> aimLerp(2.5F * offset, 1)),
                            rail, side);
                }
                akimboRest(declaration);
                modTableGun(declaration);
            }
        }
    }

    private static Gem.Part<WeaponPose> held(Gem.Model<WeaponPose> declaration) {
        return declaration.joint().visibleIf(state -> state.animated);
    }

    private void rest(Gem.Model<WeaponPose> declaration, String... parts) {
        declaration.part(model().parts(parts), body).visibleIf(state -> !state.animated);
    }

    private void am180(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> gun = held(declaration)
                .scale(0.1875F)
                .spin(Gem.Axes.X, 0F, -2F, -6F, state -> (float) state.bus("EQUIP", 0)[0])
                .angle(Gem.Axes.Z, state -> (float) state.bus("TURN", 0)[2])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL", 0)[2])
                .bus(state -> state.bus("Gun", 0));

        gun.child(obj.part("Gun"), body);
        gun.child(obj.part("Silencer"), body).visibleIf(state -> silenced(state, 0));
        gun.child(obj.part("Trigger"), body).bus(state -> state.bus("Trigger", 0));
        gun.child(obj.part("Bolt"), body)
                .slide(Gem.Axes.Z, state -> (float) state.bus("BOLT", 0)[2])
                .bus(state -> state.bus("Bolt", 0));

        Gem.Part<WeaponPose> drum = gun.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG", 0)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG", 0)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG", 0)[2])
                .translate(0F, 2.0625F, 3.75F)
                .angle(Gem.Axes.X, state -> (float) state.bus("MAGTURN", 0)[0])
                .angle(Gem.Axes.Z, state -> (float) state.bus("MAGTURN", 0)[2])
                .translate(0F, -2.0625F, -3.75F)
                .translate(0F, 2.3125F, 1.5F)
                .angle(Gem.Axes.X, state -> (float) state.bus("MAGSPIN", 0)[0])
                .translate(0F, -2.3125F, -1.5F)
                .bus(state -> state.bus("Mag", 0));

        drum.child(obj.part("Mag"), body)
                .spin(Gem.Axes.Y, 0F, 0F, 1.5F, state -> (float) (-rounds(state) / 59D * 360D));
        drum.child(obj.part("MagPlate"), body);
    }

    private static int rounds(WeaponPose state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(state.stack.getItem() instanceof ItemGunBaseNT gun)) return 0;
        return gun.getPrimaryMagazine(state.stack, 0).getAmount(state.stack, minecraft.player.getInventory());
    }

    private void starF(Gem.Part<WeaponPose> parent, int rail, int side) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> gun = parent
                .scale(0.25F)
                .spin(Gem.Axes.X, 0F, -2F, -8F, state -> (float) state.bus("EQUIP", rail)[0])
                .translate(0F, 1F, -3F)
                .angle(Gem.Axes.Z, state -> (float) (state.bus("TURN", rail)[2] * side))
                .angle(Gem.Axes.X, state -> (float) state.bus("TILT", rail)[0])
                .translate(0F, -1F, 3F)
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL", rail)[2]);

        gun.child(obj.part("Gun"), body);
        gun.child(obj.part("Hammer"), body)
                .spin(Gem.Axes.X, 0F, 1.75F, -4.25F,
                        state -> (float) (60 * (state.bus("HAMMER", rail)[0] - 1)));
        gun.child(obj.part("Slide"), body)
                .slide(Gem.Axes.Z, state -> (float) (state.bus("SLIDE", rail)[2] * 2.3125));

        Gem.Part<WeaponPose> magazine = gun.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG", rail)[2]);
        magazine.child(obj.part("Mag"), body);
        magazine.child(obj.part("Bullet"), body)
                .slide(Gem.Axes.X, state -> (float) state.bus("BULLET", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("BULLET", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("BULLET", rail)[2]);

        gun.child(silencerModel().part("Silencer"), SILENCER)
                .translate(0F, 2.375F, -0.25F)
                .visibleIf(state -> silenced(state, rail));
    }

    /**
     * 1.7 {@code ItemRenderStarFAkimbo.renderModTable(stack, index)}: one elite Star F at the origin, uncrossed and
     * unscaled, with the silencer of configuration {@code index}.
     */
    private void modTableGun(Gem.Model<WeaponPose> declaration) {
        declaration.part(model().parts("Gun", "Slide", "Mag", "Hammer"), body).visibleIf(state -> state.modTable);
        declaration.part(silencerModel().part("Silencer"), SILENCER)
                .visibleIf(state -> state.modTable && silenced(state, state.tableIndex))
                .translate(0F, 2.375F, -0.25F);
    }

    /** The akimbo rest pose: the pair crossed, as the old inventory draw laid them out. */
    private void akimboRest(Gem.Model<WeaponPose> declaration) {
        String[] parts = {"Gun", "Slide", "Mag", "Hammer"};

        Gem.Part<WeaponPose> first = declaration.joint()
                .visibleIf(state -> !state.animated && !state.modTable)
                .rotate(Gem.Axes.Z, 225F)
                .rotate(Gem.Axes.Y, 90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, 45F)
                .translate(0.5F, 0F, 0F)
                .stretch(state -> anySilenced(state) ? 0.625F : 1F)
                .slide(Gem.Axes.Z, state -> anySilenced(state) ? -4F : 0F);
        first.child(model().parts(parts), body);
        first.child(silencerModel().part("Silencer"), SILENCER)
                .translate(0F, 2.375F, -0.25F)
                .visibleIf(state -> silenced(state, 1));

        Gem.Part<WeaponPose> second = declaration.joint()
                .visibleIf(state -> !state.animated && !state.modTable)
                .translate(0F, 0F, 5F)
                .rotate(Gem.Axes.Z, 225F)
                .rotate(Gem.Axes.Y, -90F)
                .rotate(Gem.Axes.X, -90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, -45F)
                .translate(-0.5F, 0F, 0F)
                .stretch(state -> anySilenced(state) ? 0.625F : 1F)
                .slide(Gem.Axes.Z, state -> anySilenced(state) ? -4F : 0F);
        second.child(model().parts(parts), body);
        second.child(silencerModel().part("Silencer"), SILENCER)
                .translate(0F, 2.375F, -0.25F)
                .visibleIf(state -> silenced(state, 0));
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        if (variant == Variant.STAR_F_AKIMBO) {
            pose.scale(1F, 1F, -1F);
            pose.translate(8F, 8F, 0F);
            pose.scale(1.5F, 1.5F, 1.5F);
            return;
        }
        super.setupInv(pose, stack);
        if (variant == Variant.AM180) {
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(1.5F, 0F, 0F);
        } else {
            pose.scale(1.5F, 1.5F, 1.5F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(-1F, -0.5F, 0F);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        if (variant == Variant.AM180) {
            pose.scale(1F, 1F, 1F);
            pose.translate(0F, -0.5F, 3F);
        } else {
            pose.translate(0F, -0.25F, 1.75F);
            pose.scale(0.75F, 0.75F, 0.75F);
        }
    }
}
