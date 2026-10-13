package com.hbm.client.render.weapon;

import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of 1.7 ItemRenderDoubleBarrel, including Sacred Dragon skin support. */
public final class DoubleBarrelWeaponRenderer extends WeaponRenderer {
    private static final Identifier MODEL_ID=Ids.of("hbm","models/weapons/sacred_dragon.obj");
    private final Surface body;
    private WavefrontObj model;
    public DoubleBarrelWeaponRenderer(String texture){body=Surface.cutout("hbm:textures/models/weapons/"+texture+".png");}
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.25F*o,-1F*o,2F*o,0,-2/8D,1);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).slide(Gem.Axes.X,s->(float)s.bus("RECOIL")[0]*3).slide(Gem.Axes.Y,s->(float)s.bus("RECOIL")[1]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]).angle(Gem.Axes.X,s->(float)s.bus("RECOIL")[2]*10).spin(Gem.Axes.X,0,0,-4,s->(float)-s.bus("EQUIP")[0]).spin(Gem.Axes.Y,0,0,-4,s->(float)s.bus("TURN")[1]).spin(Gem.Axes.X,0,0,-4,s->(float)-s.bus("LIFT")[0]);g.child(o.part("Stock"),body);Gem.Part<WeaponPose>b=g.joint().spin(Gem.Axes.X,0,-.4375F,-.875F,s->(float)s.bus("BARREL")[0]);b.child(o.part("BarrelShort"),body);b.child(o.part("Barrel"),body).visibleIf(s->!SpecialWeaponRenderers.itemPath(s.stack).equals("gun_double_barrel_sacred_dragon"));b.child(o.part("Buckle"),body).spin(Gem.Axes.Y,.75F,0,-.6875F,s->(float)s.bus("BUCKLE")[1]);b.child(o.part("Lever"),body).spin(Gem.Axes.Z,.3125F,-.3125F,0,s->(float)s.bus("LEVER")[2]);b.child(o.part("Shells"),body).visibleIf(s->s.bus("NO_AMMO")[0]==0).slide(Gem.Axes.X,s->(float)s.bus("SHELLS")[0]).slide(Gem.Axes.Y,s->(float)s.bus("SHELLS")[1]).slide(Gem.Axes.Z,s->(float)s.bus("SHELLS")[2]).spin(Gem.Axes.X,0,0,1,s->(float)s.bus("SHELL_FLIP")[0]);d.part(o.parts("Stock","BarrelShort","Buckle","Lever","Shells"),body).visibleIf(s->!s.animated);d.part(o.part("Barrel"),body).visibleIf(s->!s.animated&&!SpecialWeaponRenderers.itemPath(s.stack).equals("gun_double_barrel_sacred_dragon"));}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        if (isSawedOff(stack)) {
            pose.scale(2F, 2F, 2F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(-2F, 0.5F, 0F);
        } else {
            pose.scale(1.375F, 1.375F, 1.375F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(0F, 0.5F, 0F);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.75F, 1.75F, 1.75F);
        pose.translate(0F, 1F, 3F);
    }

    private static boolean isSawedOff(ItemStack stack) {
        return SpecialWeaponRenderers.itemPath(stack).equals("gun_double_barrel_sacred_dragon")
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF);
    }
}
