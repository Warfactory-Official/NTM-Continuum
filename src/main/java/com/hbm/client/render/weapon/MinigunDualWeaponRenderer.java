package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact two-rail Gem port of original-hbm ItemRenderMinigunDual. */
public final class MinigunDualWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/minigun.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/minigun_dual.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.25F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);}
    @Override protected void setup(Gem.Model<WeaponPose>d){for(int side=-1;side<=1;side+=2)addAkimboGun(d,side,side<0?0:1);d.part(model().parts("Gun","Barrels"),BODY).visibleIf(s->!s.animated&&s.context!=net.minecraft.world.item.ItemDisplayContext.GUI);Gem.Part<WeaponPose>left=d.joint().visibleIf(s->!s.animated&&s.context==net.minecraft.world.item.ItemDisplayContext.GUI).rotate(Gem.Axes.Z,225).rotate(Gem.Axes.Y,90).rotate(Gem.Axes.Y,45);left.child(model().parts("GunDual","Barrels"),BODY);Gem.Part<WeaponPose>right=d.joint().visibleIf(s->!s.animated&&s.context==net.minecraft.world.item.ItemDisplayContext.GUI).translate(0,0,8).rotate(Gem.Axes.Z,225).rotate(Gem.Axes.Y,-90).rotate(Gem.Axes.X,-90).rotate(Gem.Axes.Y,-45);right.child(model().parts("Gun","Barrels"),BODY);}

    private void addAkimboGun(Gem.Model<WeaponPose>d,int side,int rail){Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).slide(Gem.Axes.X,s->aimLerp(-2.75F*.8F*side,0)).slide(Gem.Axes.Y,s->aimLerp(-1.75F*.8F,0)).slide(Gem.Axes.Z,s->aimLerp(2.5F*.8F,0)).stretch(s->.375F).spin(Gem.Axes.X,0,3,-6,s->(float)s.bus("EQUIP",rail)[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL",rail)[2]);g.child(model().part(side<0?"GunDual":"Gun"),BODY);g.child(model().part("Barrels"),BODY).angle(Gem.Axes.Z,s->(float)(s.bus("ROTATE",rail)[2]*side));}
}
