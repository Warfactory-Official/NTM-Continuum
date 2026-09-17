package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact original-hbm G3 renderer with the Zebra's permanent scope and silencer skin. */
public final class G3ZebraWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/g3.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/g3_zebra.png");
    private static final Surface ATTACHMENTS=Surface.cutout("hbm:textures/models/weapons/g3_attachments.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.25F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.66F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.25F*o,-1F*o,2.75F*o,0,-5.53125/8D,1.46875);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).spin(Gem.Axes.X,0,-2,-6,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,0,-4,s->(float)s.bus("LIFT")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.part("Rifle"),BODY);g.child(o.part("Stock"),BODY);g.child(o.part("Trigger"),BODY);Gem.Part<WeaponPose>m=g.joint().slide(Gem.Axes.X,s->(float)s.bus("MAG")[0]).slide(Gem.Axes.Y,s->(float)s.bus("MAG")[1]).slide(Gem.Axes.Z,s->(float)s.bus("MAG")[2]).angle(Gem.Axes.Z,s->(float)s.bus("SPEEN")[2]).angle(Gem.Axes.Y,s->(float)s.bus("SPEEN")[1]);m.child(o.part("Magazine"),BODY);m.child(o.part("Bullet"),BODY).visibleIf(s->s.bus("BULLET")[0]==0);g.child(o.part("Guide_And_Bolt"),BODY).slide(Gem.Axes.Z,s->(float)s.bus("BOLT")[2]);Gem.Part<WeaponPose>plug=g.joint().slide(Gem.Axes.Z,s->(float)s.bus("PLUG")[2]).translate(0,.625F,0).angle(Gem.Axes.Z,s->(float)s.bus("HANDLE")[2]).translate(0,-.625F,0);plug.child(o.part("Plug"),BODY);plug.joint().translate(0,.625F,5.25F).rotate(Gem.Axes.Z,22.5F).angle(Gem.Axes.Y,s->(float)s.bus("HANDLE")[1]).rotate(Gem.Axes.Z,-22.5F).translate(0,-.625F,-5.25F).child(o.part("Handle"),BODY);g.child(o.part("Selector"),BODY).spin(Gem.Axes.X,0,-.875F,-3.5F,s->-30F*(1-ItemGunBaseNT.getMode(s.stack,0)));g.child(o.part("Scope"),ATTACHMENTS);g.child(o.part("Silencer"),ATTACHMENTS);d.part(o.parts("Rifle","Stock","Magazine","Guide_And_Bolt","Plug","Handle","Trigger","Selector"),BODY).visibleIf(s->!s.animated);d.part(o.parts("Scope","Silencer"),ATTACHMENTS).visibleIf(s->!s.animated);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        boolean silencer = G3WeaponRenderer.hasSilencer(stack);
        if (G3WeaponRenderer.hasStock(stack)) {
            pose.scale(0.875F, 0.875F, 0.875F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(silencer ? 50F : 45F));
            pose.translate(silencer ? 0.75F : -0.5F, 0.5F, 0F);
        } else {
            pose.scale(1.125F, 1.125F, 1.125F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(silencer ? 55F : 45F));
            pose.translate(2.5F, 0.5F, 0F);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1F, 1F, 1F);
        pose.translate(0F, 2F, 4F);
    }
}
