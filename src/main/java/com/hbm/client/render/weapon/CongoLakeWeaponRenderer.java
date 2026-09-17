package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderCongoLake. */
public final class CongoLakeWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/congolake.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/congolake.png");
    private static final Surface CASING=Surface.cutout("hbm:textures/particle/casings.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.25F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.5F*o,-2F*o,1.25F*o,0,-10/8D,.25);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.5F);g.child(o.part("Gun"),BODY).bus(s->s.bus("Gun"));g.child(o.part("Pump"),BODY).bus(s->s.bus("Pump"));g.child(o.part("Loop"),BODY).bus(s->s.bus("Loop"));g.child(o.part("Sight"),BODY).bus(s->s.bus("Sight")).spin(Gem.Axes.X,0,2.125F,3F,s->aimLerp(0,-90));Gem.Part<WeaponPose>guard=g.child(o.part("GuardOuter"),BODY).bus(s->s.bus("GuardOuter"));guard.child(o.part("GuardInner"),BODY).bus(s->s.bus("GuardInner"));Gem.Part<WeaponPose>shell=g.joint().bus(s->s.bus("Shell"));shell.child(o.part("Shell"),CASING);shell.child(o.part("ShellFore"),CASING);d.part(o.parts("Gun","Pump","Sight","Loop","GuardOuter","GuardInner","Shell","ShellFore"),BODY).visibleIf(s->!s.animated);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(2.5F, 2.5F, 2.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, -1.25F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.translate(0F, -2.5F, 4F);
        pose.scale(2.5F, 2.5F, 2.5F);
    }
}
