package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderShredder for both autoshotgun skins. */
public final class ShredderWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/shredder.obj");
    private final Surface body;
    private WavefrontObj model;
    public ShredderWeaponRenderer(String texture){body=Surface.cutout("hbm:textures/models/weapons/"+texture+".png");}
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.5F*o,-1.25F*o,1.5F*o,0,-6.25/8D,.5);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.25F).spin(Gem.Axes.X,0,-2,-6,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,0,-4,s->(float)s.bus("LIFT")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.part("Gun"),body);Gem.Part<WeaponPose>m=g.joint().slide(Gem.Axes.X,s->(float)s.bus("MAG")[0]).slide(Gem.Axes.Y,s->(float)s.bus("MAG")[1]).slide(Gem.Axes.Z,s->(float)s.bus("MAG")[2]).spin(Gem.Axes.X,0,-1,-.5F,s->(float)s.bus("SPEEN")[0]);m.child(o.part("Magazine"),body);m.child(o.part("Shells"),body).spin(Gem.Axes.Z,0,-1,-.5F,s->(float)s.bus("CYCLE")[2]);d.part(o.parts("Gun","Magazine","Shells"),body).visibleIf(s->!s.animated);}
}
