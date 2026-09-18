package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact two-rail Gem port of original-hbm ItemRenderMareslegAkimbo. */
public final class MareslegAkimboWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/maresleg.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/maresleg.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);}
    @Override protected void setup(Gem.Model<WeaponPose>d){for(int side=-1;side<=1;side+=2)addAkimboGun(d,side,side<0?0:1);d.part(model().parts("Gun","Lever"),BODY).visibleIf(s->!s.animated&&s.context!=net.minecraft.world.item.ItemDisplayContext.GUI);Gem.Part<WeaponPose>left=d.joint().visibleIf(s->!s.animated&&s.context==net.minecraft.world.item.ItemDisplayContext.GUI).rotate(Gem.Axes.Z,225).rotate(Gem.Axes.Y,90).rotate(Gem.Axes.X,25).rotate(Gem.Axes.Y,45).translate(-1,0,0);left.child(model().parts("Gun","Lever"),BODY);Gem.Part<WeaponPose>right=d.joint().visibleIf(s->!s.animated&&s.context==net.minecraft.world.item.ItemDisplayContext.GUI).translate(0,0,5).rotate(Gem.Axes.Z,225).rotate(Gem.Axes.Y,-90).rotate(Gem.Axes.X,-90).rotate(Gem.Axes.X,25).rotate(Gem.Axes.Y,-45).translate(1,0,0);right.child(model().parts("Gun","Lever"),BODY);}

    private void addAkimboGun(Gem.Model<WeaponPose>d,int side,int rail){Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).slide(Gem.Axes.X,s->aimLerp(-1.5F*.8F*side,0)).slide(Gem.Axes.Y,s->aimLerp(-.8F,-3.875/8D)).slide(Gem.Axes.Z,s->aimLerp(2F*.8F,1)).stretch(s->.375F).slide(Gem.Axes.X,s->(float)s.bus("RECOIL",rail)[0]*2).slide(Gem.Axes.Y,s->(float)s.bus("RECOIL",rail)[1]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL",rail)[2]).angle(Gem.Axes.X,s->(float)s.bus("RECOIL",rail)[2]*5).angle(Gem.Axes.Z,s->(float)s.bus("TURN",rail)[2]).spin(Gem.Axes.X,0,0,-4,s->(float)s.bus("LIFT",rail)[0]).spin(Gem.Axes.X,0,0,-4,s->(float)-s.bus("EQUIP",rail)[0]).spin(Gem.Axes.X,0,0,-2,s->(float)-s.bus("FLIP",rail)[0]);g.child(model().part("Gun"),BODY);g.child(model().part("Lever"),BODY).spin(Gem.Axes.X,0,.125F,-2.875F,s->(float)s.bus("LEVER",rail)[0]);g.child(model().part("Shell"),BODY).slide(Gem.Axes.X,s->(float)s.bus("SHELL",rail)[0]).slide(Gem.Axes.Y,s->(float)(s.bus("SHELL",rail)[1]-.75)).slide(Gem.Axes.Z,s->(float)s.bus("SHELL",rail)[2]);g.child(model().part("Shell"),BODY).slide(Gem.Axes.Y,s->-.5F).visibleIf(s->s.bus("FLAG",rail)[0]!=0);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        pose.scale(1F, 1F, -1F);
        pose.translate(8F, 8F, 0F);
        pose.scale(2.5F, 2.5F, 2.5F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.75F, 1.75F, 1.75F);
        pose.translate(0F, 0.25F, 3F);
    }
}
