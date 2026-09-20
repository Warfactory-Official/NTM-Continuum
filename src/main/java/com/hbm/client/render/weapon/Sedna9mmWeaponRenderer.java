package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** OBJ renderers for the Gen-3 Greasegun, LAG, Uzi, and Uzi akimbo guns. */
public final class Sedna9mmWeaponRenderer extends WeaponRenderer {

    public enum Variant {
        GREASEGUN("greasegun", "greasegun"),
        LAG("mike_hawk", "lag"),
        UZI("uzi", "uzi"),
        UZI_AKIMBO("uzi", "uzi");

        private final String model;
        private final String texture;

        Variant(String model, String texture) {
            this.model = model;
            this.texture = texture;
        }
    }

    private static final Surface SATURNITE = Surface.cutout("hbm:textures/models/weapons/uzi_saturnite.png");

    private final Variant variant;
    private final Identifier modelId;
    private final Surface body;
    private WavefrontObj model;

    public Sedna9mmWeaponRenderer(Variant variant) {
        this.variant = variant;
        this.modelId = Ids.of("hbm", "models/weapons/" + variant.model + ".obj");
        this.body = Surface.cutout("hbm:textures/models/weapons/" + variant.texture + ".png");
    }

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(modelId);
        return model;
    }

    private static boolean silenced(WeaponPose state, int rail) {
        return XWeaponModManager.hasUpgrade(state.stack, rail, XWeaponModManager.ID_SILENCER);
    }

    /** Upstream binds one texture for the whole uzi, so the skin moves per part rather than per mesh. */
    private Gem.SurfaceOf<WeaponPose> skin(int rail) {
        return state -> XWeaponModManager.hasUpgrade(state.stack, rail, XWeaponModManager.ID_UZI_SATURN)
                ? SATURNITE : body;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : variant == Variant.GREASEGUN ? -0.5F : -0.25F;
    }

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        switch (variant) {
            case GREASEGUN -> standardAimingTransform(stack, pose,
                    -1.5F * offset, -1F * offset, 1.75F * offset,
                    0, -2.625 / 8D, 1.125);
            case LAG -> standardAimingTransform(stack, pose,
                    -1.5F * offset, -1F * offset, 1.5F * offset,
                    0, -3.375 / 8D, 0.5);
            case UZI -> standardAimingTransform(stack, pose,
                    -1.75F * offset, -1.5F * offset, 2.5F * offset,
                    0, -4.375 / 8D, 1);
            case UZI_AKIMBO -> { }
        }
    }

    @Override
    public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    /**
     * Genuinely two trees, not one with its buses at rest: the greasegun's stock sits four units back
     * in the hand and at the origin in a slot, and the uzi's is folded 180° in the hand. Upstream wrote
     * a separate rest-pose part list for that reason, and the gate on {@code animated} is that list.
     *
     * <p>The greasegun's ejection flap is translated up by 0.53125 and back down by 0.5125, a
     * thousandth short. That is upstream's, and the part was authored against it.
     */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        switch (variant) {
            case GREASEGUN -> {
                greasegun(declaration);
                rest(declaration, "Gun", "Stock", "Magazine", "Handle", "Flap", "Bullet");
            }
            case LAG -> {
                lag(declaration);
                rest(declaration, "Grip", "Slide", "Hammer");
            }
            case UZI -> {
                uzi(held(declaration), 0, 1, "Gun");
                Gem.Part<WeaponPose> still = declaration.joint().visibleIf(state -> !state.animated);
                still.child(model().parts("Gun", "StockBack", "StockFront", "Slide", "Magazine"), body)
                        .skin(skin(0));
                still.child(model().part("Silencer"), body).skin(skin(0))
                        .visibleIf(state -> silenced(state, 0));
            }
            case UZI_AKIMBO -> {
                float offset = 0.8F;
                for (int side = -1; side <= 1; side += 2) {
                    int rail = side == -1 ? 0 : 1;
                    int mirror = side;
                    uzi(held(declaration)
                            .slide(Gem.Axes.X, state -> aimLerp(-2.25F * offset * mirror, 0))
                            .slide(Gem.Axes.Y, state -> aimLerp(-1.5F * offset, -4.375 / 8D))
                            .slide(Gem.Axes.Z, state -> aimLerp(2.5F * offset, 1)),
                            rail, side, side < 0 ? "GunMirror" : "Gun");
                }
                akimboRest(declaration);
            }
        }
    }

    private static Gem.Part<WeaponPose> held(Gem.Model<WeaponPose> declaration) {
        return declaration.joint().visibleIf(state -> state.animated);
    }

    private void rest(Gem.Model<WeaponPose> declaration, String... parts) {
        declaration.part(model().parts(parts), body).visibleIf(state -> !state.animated);
    }

    private void greasegun(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> gun = held(declaration)
                .scale(0.375F)
                .spin(Gem.Axes.X, 0F, -3F, -3F, state -> (float) state.bus("EQUIP")[0])
                .spin(Gem.Axes.X, 0F, -3F, -3F, state -> (float) state.bus("LIFT")[0])
                .angle(Gem.Axes.Z, state -> ItemGunBaseNT.aimingProgress < 1F
                        ? (float) state.bus("TURN")[2] : 0F)
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2]);

        gun.child(obj.part("Gun"), body);
        gun.child(obj.part("Stock"), body)
                .translate(0F, 0F, -4F)
                .slide(Gem.Axes.Z, state -> (float) -state.bus("STOCK")[2]);

        Gem.Part<WeaponPose> magazine = gun.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG")[2]);
        magazine.child(obj.part("Magazine"), body);
        magazine.child(obj.part("Bullet"), body).visibleIf(state -> state.bus("BULLET")[0] != 1);

        gun.child(obj.part("Handle"), body)
                .spin(Gem.Axes.X, 0F, -1.4375F, -0.125F, state -> (float) state.bus("HANDLE")[0]);
        gun.child(obj.part("Flap"), body)
                .translate(0F, 0.53125F, 0F)
                .angle(Gem.Axes.Z, state -> (float) state.bus("FLAP")[2])
                .translate(0F, -0.5125F, 0F);
    }

    private void lag(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> gun = held(declaration)
                .scale(0.25F)
                .rotate(Gem.Axes.Y, 90F)
                .spin(Gem.Axes.Z, 4F, -4F, 0F, state -> (float) -state.bus("EQUIP")[0])
                .slide(Gem.Axes.X, state -> (float) state.bus("ADD_TRANS")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("ADD_TRANS")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("ADD_TRANS")[2])
                .angle(Gem.Axes.Z, state -> (float) state.bus("ADD_ROT")[2])
                .angle(Gem.Axes.Y, state -> (float) state.bus("ADD_ROT")[1]);

        Gem.Part<WeaponPose> grip = gun.child(obj.part("Grip"), body).bus(state -> state.bus("Grip"));

        grip.child(obj.part("Slide"), body).bus(state -> state.bus("Slide"));

        grip.child(obj.part("Hammer"), body)
                .translate(3.125F, 0.125F, 0F)
                .rotate(Gem.Axes.Z, -25F)
                .translate(-3.125F, -0.125F, 0F)
                .bus(state -> state.bus("Hammer"));

        grip.child(obj.part("Bullet"), body)
                .visibleIf(Sedna9mmWeaponRenderer::chambered)
                .bus(state -> state.bus("Bullet"));

        grip.child(obj.part("Magazine"), body).bus(state -> state.bus("Magazine"));
    }

    private static boolean chambered(WeaponPose state) {
        return state.stack.getItem() instanceof ItemGunBaseNT gun
                && gun.getPrimaryMagazine(state.stack, 0).getAmount(state.stack, null) > 0;
    }

    private void uzi(Gem.Part<WeaponPose> parent, int rail, int side, String gunPart) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> gun = parent
                .scale(0.25F)
                .slide(Gem.Axes.X, state -> (float) state.bus("YEET", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("YEET", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("YEET", rail)[2])
                .angle(Gem.Axes.Z, state -> (float) (state.bus("SPEEN", rail)[0] * side))
                .spin(Gem.Axes.X, 0F, -2F, -4F, state -> (float) state.bus("EQUIP", rail)[0])
                .spin(Gem.Axes.X, 0F, 0F, -6F, state -> (float) state.bus("LIFT", rail)[0])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL", rail)[2]);

        gun.child(obj.part(gunPart), body).skin(skin(rail));
        gun.child(obj.part("Silencer"), body).skin(skin(rail))
                .visibleIf(state -> silenced(state, rail));

        Gem.Part<WeaponPose> stock = gun.joint()
                .translate(0F, 0.3125F, -5.75F)
                .angle(Gem.Axes.X, state -> (float) (180 - state.bus("STOCKFRONT", rail)[0]))
                .translate(0F, -0.3125F, 5.75F);
        stock.child(obj.part("StockFront"), body).skin(skin(rail));

        stock.joint()
                .translate(0F, -0.3125F, -3F)
                .angle(Gem.Axes.X, state -> (float) (-200 - state.bus("STOCKBACK", rail)[0]))
                .translate(0F, 0.3125F, 3F)
                .child(obj.part("StockBack"), body).skin(skin(rail));

        gun.child(obj.part("Slide"), body).skin(skin(rail))
                .slide(Gem.Axes.Z, state -> (float) state.bus("SLIDE", rail)[2]);

        Gem.Part<WeaponPose> magazine = gun.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG", rail)[2]);
        magazine.child(obj.part("Magazine"), body).skin(skin(rail));
        magazine.child(obj.part("Bullet"), body).visibleIf(state -> state.bus("BULLET", rail)[0] == 1);
    }

    /** The akimbo rest pose: the pair crossed, as the old inventory draw laid them out. */
    private void akimboRest(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        String[] parts = {"StockBack", "StockFront", "Slide", "Magazine"};

        Gem.Part<WeaponPose> left = declaration.joint()
                .visibleIf(state -> !state.animated)
                .rotate(Gem.Axes.Z, 225F)
                .rotate(Gem.Axes.Y, 90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, 45F)
                .translate(0F, 1F, 0F);
        left.child(obj.part("Gun"), body).skin(skin(1));
        left.child(obj.parts(parts), body).skin(skin(1));
        left.child(obj.part("Silencer"), body).skin(skin(1))
                .visibleIf(state -> silenced(state, 1));

        Gem.Part<WeaponPose> right = declaration.joint()
                .visibleIf(state -> !state.animated)
                .translate(0F, 0F, 5F)
                .rotate(Gem.Axes.Z, 225F)
                .rotate(Gem.Axes.Y, -90F)
                .rotate(Gem.Axes.X, -90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, -45F)
                .translate(0F, 1F, 0F);
        right.child(obj.part("GunMirror"), body).skin(skin(0));
        right.child(obj.parts(parts), body).skin(skin(0));
        right.child(obj.part("Silencer"), body).skin(skin(0))
                .visibleIf(state -> silenced(state, 0));
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        if (variant == Variant.UZI_AKIMBO) {
            pose.scale(1F, 1F, -1F);
            pose.translate(8F, 8F, 0F);
            pose.scale(1.5F, 1.5F, 1.5F);
            return;
        }
        super.setupInv(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        switch (variant) {
            case GREASEGUN -> pose.translate(-0.5F, 2F, 0F);
            case LAG -> pose.translate(2.5F, 1F, 0F);
            default -> pose.translate(0F, 1F, 0F);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.translate(0F, 1F, variant == Variant.GREASEGUN ? 3F : 1F);
    }
}
