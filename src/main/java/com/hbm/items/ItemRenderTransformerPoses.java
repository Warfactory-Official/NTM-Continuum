package com.hbm.items;

import dev.golem.core.render.display.DisplaySlot;
import dev.golem.core.render.display.Pose17;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.7 {@code ItemRenderTransformer} (and {@code ItemRendererMeteorSword}, which is the same chain with its own
 * numbers) as the hand poses of the item's own {@code item/handheld} sprite.
 *
 * <p>Those renderers drew the ordinary sprite ({@code renderItemIn2D}) and only moved it: in third person
 * {@code rotate(rtp) translate(ttp) rotate(45, Z) rotate(180, Y) rotate(-45, Z) scale(stp)} inside the engine's
 * full-3D hand pose (every item on them is an {@code ItemSword}/{@code ItemTool}), in first person a quarter-turn
 * dance ending in {@code scale(2 * sfp) translate(-0.5, -0.5, 0.25)}. {@code ClientProxy} registered them on the
 * ten high-tier swords and the eight bismuth/volcanic/chlorophyte/mese tools, with {@code stp = 1.7, 1.7, 0.85}:
 * those swords were held at almost twice the size of a plain sword. Nothing else is drawn, so the pose is all
 * there is to port; the sprite, the inventory icon and the dropped item stay the plain {@code item/handheld} ones
 * ({@code handleRenderType} declined {@code ENTITY}, and {@code INVENTORY} was the identity for these numbers).
 *
 * <p>The body of each pose is {@code renderItemIn2D}'s unit square: {@code x = 1 - u}, {@code y = 1 - v},
 * {@code z} from {@code -1/16} to {@code 0}. That is {@code item/generated}'s slab turned half round Y
 * ({@link #SPRITE_TO_2D}). The full-3D hand pose mirrors (see {@link Pose17.Klass#ITEM_RENDER_WEAPON_FULL3D});
 * a sprite slab is its own mirror image through its middle plane, so the third-person poses fold that mirror into
 * {@link #SLAB_FLIP} and ship unmirrored, which keeps vanilla's culled item faces the right way out. The left hands
 * are the right hands' mirror images, as vanilla's own sprite models are.
 *
 * <p>Plain Java and JOML only: the declarations call this from {@code dejson.gradle.kts}'s plain JVM.
 */
public final class ItemRenderTransformerPoses {

    private ItemRenderTransformerPoses() {}

    /** {@code ClientProxy} {@code ttp_high}: every Transformer sword but desh, and the eight tools. */
    public static final float[] TTP_HIGH = {0.125F, 0.625F, 0F};
    /** {@code ClientProxy} {@code ttp_low}: the desh sword. */
    public static final float[] TTP_LOW = {0F, 0.75F, 0F};
    /** {@code ItemRendererMeteorSword}'s own third-person translate (the meteorite sword tiers). */
    public static final float[] TTP_METEOR = {0.2F, 0.55F, 0F};
    /** {@code ClientProxy} {@code sfp}: the swords' first-person scale (also {@code ItemRendererMeteorSword}'s). */
    public static final float[] SFP_SWORD = {1.36F, 1.36F, 0.68F};
    /** {@code ClientProxy} {@code sfp_default}: the bismuth/volcanic/chlorophyte/mese tools. */
    public static final float[] SFP_DEFAULT = {1F, 1F, 1F};

    /** {@code rtp} and {@code rfp}, which every registration shared: rotate 180 about Y, then -90 about Z. */
    private static final float[] ROTATION = {0F, 180F, -90F};
    /** {@code stp}, which every registration shared. */
    private static final float[] STP = {1.7F, 1.7F, 0.85F};

    /** {@code item/generated} block space to {@code renderItemIn2D} space: the same slab, turned about Y. */
    static final Matrix4f SPRITE_TO_2D = new Matrix4f()
            .translate(0.5F, 0.5F, -1F / 32F).rotateY((float) Math.PI).translate(-0.5F, -0.5F, -0.5F);

    /** The sprite slab mirrored through its own middle plane, {@code z = 8/16}: the same slab, faces swapped. */
    static final Matrix4f SLAB_FLIP = new Matrix4f().translate(0F, 0F, 0.5F).scale(1F, 1F, -1F).translate(0F, 0F, -0.5F);

    /** Mirror of block space about {@code x = 1/2}: a right-hand pose to its left hand. */
    private static final Matrix4f MIRROR_X = new Matrix4f().translate(0.5F, 0F, 0F).scale(-1F, 1F, 1F).translate(-0.5F, 0F, 0F);

    /** The four hand poses of a Transformer item with third-person translate {@code ttp}, first-person scale {@code sfp}. */
    public static Map<DisplaySlot, Matrix4f> poses(float[] ttp, float[] sfp) {
        Matrix4f third = Pose17.frame(Pose17.Klass.ITEM_RENDER_WEAPON_FULL3D, DisplaySlot.THIRD_PERSON_RIGHT_HAND)
                .mul(equipped(ttp)).mul(SPRITE_TO_2D).mul(SLAB_FLIP);
        Matrix4f first = Pose17.frame(Pose17.Klass.ITEM_RENDER_WEAPON_FULL3D, DisplaySlot.FIRST_PERSON_RIGHT_HAND)
                .mul(firstPerson(sfp)).mul(SPRITE_TO_2D);
        Map<DisplaySlot, Matrix4f> poses = new EnumMap<>(DisplaySlot.class);
        poses.put(DisplaySlot.THIRD_PERSON_RIGHT_HAND, third);
        poses.put(DisplaySlot.THIRD_PERSON_LEFT_HAND, mirrored(third));
        poses.put(DisplaySlot.FIRST_PERSON_RIGHT_HAND, first);
        poses.put(DisplaySlot.FIRST_PERSON_LEFT_HAND, mirrored(first));
        return poses;
    }

    /**
     * A plain sprite of a full-3D item (no {@code IItemRenderer}): {@code renderItemIn2D} straight inside the engine's
     * full-3D third-person pose and its first-person pose. This is {@code item/handheld}'s 1.7 meaning
     * ({@code setFull3D()}), declared once as {@code hbm:item/handheld_full3d} (see {@code ModItemModels}).
     */
    public static Map<DisplaySlot, Matrix4f> full3D() {
        Matrix4f third = Pose17.frame(Pose17.Klass.ITEM_RENDER_WEAPON_FULL3D, DisplaySlot.THIRD_PERSON_RIGHT_HAND)
                .mul(SPRITE_TO_2D).mul(SLAB_FLIP);
        Matrix4f first = Pose17.frame(Pose17.Klass.ITEM_RENDER_WEAPON_FULL3D, DisplaySlot.FIRST_PERSON_RIGHT_HAND)
                .mul(SPRITE_TO_2D);
        Map<DisplaySlot, Matrix4f> poses = new EnumMap<>(DisplaySlot.class);
        poses.put(DisplaySlot.THIRD_PERSON_RIGHT_HAND, third);
        poses.put(DisplaySlot.THIRD_PERSON_LEFT_HAND, mirrored(third));
        poses.put(DisplaySlot.FIRST_PERSON_RIGHT_HAND, first);
        poses.put(DisplaySlot.FIRST_PERSON_LEFT_HAND, mirrored(first));
        return poses;
    }

    /** {@code case EQUIPPED}, op for op. */
    static Matrix4f equipped(float[] ttp) {
        return new Matrix4f()
                .rotateY(rad(ROTATION[1])).rotateZ(rad(ROTATION[2])).rotateX(rad(ROTATION[0]))
                .translate(ttp[0], ttp[1], ttp[2])
                .rotateZ(rad(45F)).rotateY(rad(180F)).rotateZ(rad(-45F))
                .scale(STP[0], STP[1], STP[2]);
    }

    /** {@code case EQUIPPED_FIRST_PERSON}, op for op ({@code tfp} was declared but never read). */
    static Matrix4f firstPerson(float[] sfp) {
        return new Matrix4f()
                .rotateY(rad(ROTATION[1])).rotateZ(rad(ROTATION[2])).rotateX(rad(ROTATION[0]))
                .translate(0.5F, 0.5F, 0F).translate(-0.5F, -0.5F, 0F)
                .rotateY(rad(180F)).rotateZ(rad(-90F))
                .translate(0.5F, 0.5F, 0F)
                .scale(sfp[0] * 2F, sfp[1] * 2F, sfp[2] * 2F)
                .translate(-0.5F, -0.5F, 0.25F);
    }

    private static Matrix4f mirrored(Matrix4f right) {
        return new Matrix4f(MIRROR_X).mul(right).mul(MIRROR_X);
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
