package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderLiberator. */
public final class LiberatorWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID = Ids.of("hbm", "models/weapons/liberator.obj");
    private static final Surface BODY = Surface.cutout("hbm:textures/models/weapons/liberator.png");
    private WavefrontObj model;

    private WavefrontObj model() { if (model == null) model = WavefrontObj.load(MODEL_ID); return model; }
    @Override public org.joml.Vector3fc[] extents() {
        float[] a = model().boundsMin(), b = model().boundsMax(); org.joml.Vector3fc[] out = new org.joml.Vector3fc[8]; int i = 0;
        for (int x=0;x<2;x++) for (int y=0;y<2;y++) for (int z=0;z<2;z++) out[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]); return out;
    }
    @Override protected float getTurnMagnitude(ItemStack s) { return ItemGunBaseNT.getIsAiming(s) ? 2.5F : -0.25F; }
    @Override public float viewFov(ItemStack s,float f,float p) { return aimingFov(s,f,p,0.33F); }
    @Override public void setupFirstPerson(ItemStack s, PoseStack p) { p.translate(0,0,0.875); float o=.8F; standardAimingTransform(s,p,-1.5F*o,-1.25F*o,1.25F*o,0,-4.625/8D,.25); }

    @Override protected void setup(Gem.Model<WeaponPose> d) {
        WavefrontObj o=model();
        Gem.Part<WeaponPose> gun=d.joint().visibleIf(s->s.animated).stretch(s->.375F)
                .spin(Gem.Axes.X,0F,-1F,-3F,s->(float)s.bus("EQUIP")[0])
                .spin(Gem.Axes.X,0F,-3F,-3F,s->(float)s.bus("LIFT")[0])
                .slide(Gem.Axes.X,s->(float)s.bus("RECOIL")[0]*2).slide(Gem.Axes.Y,s->(float)s.bus("RECOIL")[1]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2])
                .angle(Gem.Axes.X,s->(float)s.bus("RECOIL")[2]*10);
        gun.child(o.part("Gun"),BODY);
        Gem.Part<WeaponPose> barrel=gun.joint().spin(Gem.Axes.X,0F,-.5F,.75F,s->(float)s.bus("BREAK")[0]);
        barrel.child(o.part("Barrel"),BODY);
        for(String part:new String[]{"Shell1","Shell2","Shell3","Shell4"}) barrel.child(o.part(part),BODY).slide(Gem.Axes.X,s->(float)s.bus(part)[0]).slide(Gem.Axes.Y,s->(float)s.bus(part)[1]).slide(Gem.Axes.Z,s->(float)s.bus(part)[2]);
        barrel.child(o.part("Latch"),BODY).spin(Gem.Axes.X,0F,1.15625F,.75F,s->(float)s.bus("LATCH")[0]);
        d.part(o.parts("Gun","Barrel","Shell1","Shell2","Shell3","Shell4","Latch"),BODY).visibleIf(s->!s.animated);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.5F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.translate(0F, 1F, 3F);
    }
}
