package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderSTG77. Imported scope animation remains a named bus. */
public final class Stg77WeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/stg77.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/stg77.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?0.5F:-0.25F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.66F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.5F*o,-1F*o,2.5F*o,0,-5.75/8D,2);}
    @Override protected boolean scopedIn(ItemStack s){return fullyAimed();}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.5F).spin(Gem.Axes.X,0,-1,-4,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,0,-4,s->(float)s.bus("LIFT")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);Gem.Part<WeaponPose>main=g.joint().angle(Gem.Axes.Z,s->(float)s.bus("INSPECT_GUN")[2]).angle(Gem.Axes.X,s->(float)s.bus("INSPECT_GUN")[0]).bus(s->s.bus("Gun"));main.child(o.part("Gun"),BODY);main.child(o.part("Magazine"),BODY).bus(s->s.bus("Magazine"));main.child(o.part("Lever"),BODY).angle(Gem.Axes.Z,s->(float)s.bus("INSPECT_LEVER")[2]).bus(s->s.bus("Lever"));Gem.Part<WeaponPose>breech=main.joint().slide(Gem.Axes.Z,s->(float)s.bus("BOLT")[2]);breech.child(o.part("Breech"),BODY).bus(s->s.bus("Breech"));breech.child(o.part("Handle"),BODY).spin(Gem.Axes.Z,.125F,0,0,s->(float)s.bus("HANDLE")[2]).bus(s->s.bus("Handle"));main.child(o.part("Safety"),BODY).slide(Gem.Axes.X,s->(float)s.bus("SAFETY")[0]).bus(s->s.bus("Safety"));Gem.Part<WeaponPose>barrel=g.joint().slide(Gem.Axes.X,s->(float)s.bus("INSPECT_MOVE")[0]).slide(Gem.Axes.Y,s->(float)s.bus("INSPECT_MOVE")[1]).slide(Gem.Axes.Z,s->(float)s.bus("INSPECT_MOVE")[2]).angle(Gem.Axes.X,s->(float)s.bus("INSPECT_BARREL")[0]).angle(Gem.Axes.Z,s->(float)s.bus("INSPECT_BARREL")[2]).bus(s->s.bus("Gun"));barrel.child(o.part("Barrel"),BODY).bus(s->s.bus("Barrel"));d.part(o.parts("Gun","Barrel","Lever","Magazine","Safety","Breech","Handle"),BODY).visibleIf(s->!s.animated);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.375F, 1.375F, 1.375F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.5F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.translate(0F, 1F, 2F);
    }
}
