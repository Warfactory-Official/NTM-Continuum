package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/**
 * Exact two-rail Gem port of original-hbm ItemRenderMinigunDual. The weapon table draws 1.7's
 * renderModTable: configuration 0 the "GunDual" body, any other the "Gun" body, each with its barrels.
 */
public final class MinigunDualWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/minigun.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/minigun_dual.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.25F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);}
    @Override protected void setup(Gem.Model<WeaponPose>d){for(int side=-1;side<=1;side+=2)addAkimboGun(d,side,side<0?0:1);d.part(model().parts("Gun","Barrels"),BODY).visibleIf(s->!s.animated&&s.context!=net.minecraft.world.item.ItemDisplayContext.GUI&&!s.modTable);d.part(model().part("GunDual"),BODY).visibleIf(s->s.modTable&&s.tableIndex==0);d.part(model().part("Gun"),BODY).visibleIf(s->s.modTable&&s.tableIndex!=0);d.part(model().part("Barrels"),BODY).visibleIf(s->s.modTable);Gem.Part<WeaponPose>left=d.joint().visibleIf(s->!s.animated&&s.context==net.minecraft.world.item.ItemDisplayContext.GUI&&!s.modTable).rotate(Gem.Axes.Z,225).rotate(Gem.Axes.Y,90).rotate(Gem.Axes.Y,45);left.child(model().parts("GunDual","Barrels"),BODY);Gem.Part<WeaponPose>right=d.joint().visibleIf(s->!s.animated&&s.context==net.minecraft.world.item.ItemDisplayContext.GUI&&!s.modTable).translate(0,0,8).rotate(Gem.Axes.Z,225).rotate(Gem.Axes.Y,-90).rotate(Gem.Axes.X,-90).rotate(Gem.Axes.Y,-45);right.child(model().parts("Gun","Barrels"),BODY);}

    private void addAkimboGun(Gem.Model<WeaponPose>d,int side,int rail){Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).slide(Gem.Axes.X,s->aimLerp(-2.75F*.8F*side,0)).slide(Gem.Axes.Y,s->aimLerp(-1.75F*.8F,0)).slide(Gem.Axes.Z,s->aimLerp(2.5F*.8F,0)).stretch(s->.375F).spin(Gem.Axes.X,0,3,-6,s->(float)s.bus("EQUIP",rail)[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL",rail)[2]);g.child(model().part(side<0?"GunDual":"Gun"),BODY);g.child(model().part("Barrels"),BODY).angle(Gem.Axes.Z,s->(float)(s.bus("ROTATE",rail)[2]*side));}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        pose.scale(1F, 1F, -1F);
        pose.translate(8F, 8F, 0F);
        pose.scale(0.875F, 0.875F, 0.875F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.75F, 1.75F, 1.75F);
        pose.translate(-1F, -3.5F, 8F);
    }
}
