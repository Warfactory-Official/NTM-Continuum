package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderMAS36. */
public final class Mas36WeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/mas36.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/mas36.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.5F*o,-1.25F*o,1.75F*o,0,-4.6825/8D,.75);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).spin(Gem.Axes.X,0,-3,-3,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,-3,-3,s->(float)s.bus("LIFT")[0]).slide(Gem.Axes.Z,s->(float)s.bus("STAB")[2]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.part("Gun"),BODY);g.child(o.part("Bayonet"),BODY).visibleIf(Mas36WeaponRenderer::bayonet);g.child(o.part("Stock"),BODY).spin(Gem.Axes.X,0,.3125F,-2.125F,s->(float)s.bus("STOCK")[0]);g.child(o.part("Bolt"),BODY).spin(Gem.Axes.Z,0,1.15625F,0,s->(float)s.bus("BOLT_TURN")[2]).slide(Gem.Axes.Z,s->(float)s.bus("BOLT_PULL")[2]);g.child(o.part("Bullet"),BODY).slide(Gem.Axes.X,s->(float)s.bus("BULLET")[0]).slide(Gem.Axes.Y,s->(float)s.bus("BULLET")[1]).slide(Gem.Axes.Z,s->(float)s.bus("BULLET")[2]);g.child(o.part("Scope"),BODY).visibleIf(Mas36WeaponRenderer::scoped);Gem.Part<WeaponPose>clip=g.joint().visibleIf(s->s.bus("SHOW_CLIP")[0]!=0).slide(Gem.Axes.X,s->(float)s.bus("CLIP")[0]).slide(Gem.Axes.Y,s->(float)s.bus("CLIP")[1]).slide(Gem.Axes.Z,s->(float)s.bus("CLIP")[2]);clip.child(o.part("Clip"),BODY);clip.child(o.part("Bullets"),BODY).slide(Gem.Axes.X,s->(float)s.bus("BULLETS")[0]).slide(Gem.Axes.Y,s->(float)s.bus("BULLETS")[1]).slide(Gem.Axes.Z,s->(float)s.bus("BULLETS")[2]);d.part(o.parts("Gun","Bayonet","Stock","Bolt","Bullet","Scope","Clip","Bullets"),BODY).visibleIf(s->!s.animated);}
    private static boolean scoped(WeaponPose s){return SpecialWeaponRenderers.itemPath(s.stack).equals("gun_mas36_scoped");}
    private static boolean bayonet(WeaponPose s){return SpecialWeaponRenderers.itemPath(s.stack).equals("gun_mas36_bayonet");}
}
