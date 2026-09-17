package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderCarbine. */
public final class CarbineWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/carbine.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/huntsman.png");
    private static final Surface SCOPE=Surface.cutout("hbm:textures/models/weapons/carbine_scope.png");
    private static final Surface BAYONET=Surface.cutout("hbm:textures/models/weapons/carbine_bayonet.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.5F*o,-1.5F*o,.875F*o,0,-6.25/8D,.25);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.5F).spin(Gem.Axes.X,0,-1,-2,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,0,-2,s->(float)s.bus("LIFT")[0]).slide(Gem.Axes.X,s->(float)s.bus("STAB")[0]).slide(Gem.Axes.Y,s->(float)s.bus("STAB")[1]).slide(Gem.Axes.Z,s->(float)s.bus("STAB")[2]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.part("Gun"),BODY);g.child(o.part("Slide"),BODY).slide(Gem.Axes.Z,s->(float)s.bus("SLIDE")[2]);Gem.Part<WeaponPose>m=g.joint().slide(Gem.Axes.X,s->(float)s.bus("MAG")[0]).slide(Gem.Axes.Y,s->(float)s.bus("MAG")[1]).slide(Gem.Axes.Z,s->(float)s.bus("MAG")[2]);m.child(o.part("Magazine"),BODY);m.child(o.part("Bullet"),BODY).visibleIf(s->s.bus("BULLET")[0]!=1);d.part(o.parts("Gun","Slide","Magazine","Bullet"),BODY).visibleIf(s->!s.animated);d.part(o.part("IronSight"),BODY).visibleIf(s->!s.animated);d.part(o.part("Scope"),SCOPE).visibleIf(s->!s.animated&&scoped(s));d.part(o.part("Bayonet"),BAYONET).visibleIf(s->!s.animated&&bayonet(s));g.child(o.part("IronSight"),BODY).visibleIf(s->!scoped(s));g.child(o.part("Scope"),SCOPE).visibleIf(CarbineWeaponRenderer::scoped);g.child(o.part("Bayonet"),BAYONET).visibleIf(CarbineWeaponRenderer::bayonet);}
    private static boolean scoped(WeaponPose s){return SpecialWeaponRenderers.itemPath(s.stack).equals("gun_carbine_scoped");}
    private static boolean bayonet(WeaponPose s){return SpecialWeaponRenderers.itemPath(s.stack).equals("gun_carbine_bayonet");}
}
