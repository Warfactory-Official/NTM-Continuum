package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
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

    private final Variant variant;
    private final Identifier modelId;
    private final Surface body;
    private WavefrontObj model;

    public Sedna22lrWeaponRenderer(Variant variant) {
        this.variant = variant;
        this.modelId = Ids.of("hbm", "models/weapons/" + variant.model + ".obj");
        this.body = Surface.cutout("hbm:textures/models/weapons/" + variant.rendererTexture + ".png");
    }

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(modelId);
        return model;
    }

    @Override
    public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] min = obj.boundsMin();
        float[] max = obj.boundsMax();
        org.joml.Vector3fc[] corners = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            corners[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0],
                    y == 0 ? min[1] : max[1], z == 0 ? min[2] : max[2]);
        }
        return corners;
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
            }
            case STAR_F -> {
                starF(held(declaration), 0, 1);
                rest(declaration, "Gun", "Slide", "Mag", "Hammer");
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
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL", 0)[2]);

        gun.child(obj.part("Gun"), body);
        gun.child(obj.part("Trigger"), body).bus(state -> state.bus("Trigger", 0));
        gun.child(obj.part("Bolt"), body)
                .slide(Gem.Axes.Z, state -> (float) state.bus("BOLT", 0)[2])
                .bus(state -> state.bus("BOLT", 0));

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
    }

    /** The akimbo rest pose: the pair crossed, as the old inventory draw laid them out. */
    private void akimboRest(Gem.Model<WeaponPose> declaration) {
        String[] parts = {"Gun", "Slide", "Mag", "Hammer"};

        declaration.part(model().parts(parts), body)
                .visibleIf(state -> !state.animated)
                .rotate(Gem.Axes.Z, 225F)
                .rotate(Gem.Axes.Y, 90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, 45F)
                .translate(0.5F, 0F, 0F);

        declaration.part(model().parts(parts), body)
                .visibleIf(state -> !state.animated)
                .translate(0F, 0F, 5F)
                .rotate(Gem.Axes.Z, 225F)
                .rotate(Gem.Axes.Y, -90F)
                .rotate(Gem.Axes.X, -90F)
                .rotate(Gem.Axes.X, 25F)
                .rotate(Gem.Axes.Y, -45F)
                .translate(-0.5F, 0F, 0F);
    }
}
