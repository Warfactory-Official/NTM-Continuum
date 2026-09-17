package com.hbm.client.render.weapon;

import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderMaresleg; the constructor selects the broken skin. */
public final class MareslegWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/maresleg.obj");
    private final Surface body;
    private WavefrontObj model;
    public MareslegWeaponRenderer(String texture){body=Surface.cutout("hbm:textures/models/weapons/"+texture+".png");}
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.25F*o,-1F*o,2F*o,0,-3.875/8D,1);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).slide(Gem.Axes.X,s->(float)s.bus("RECOIL")[0]*2).slide(Gem.Axes.Y,s->(float)s.bus("RECOIL")[1]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]).angle(Gem.Axes.X,s->(float)s.bus("RECOIL")[2]*5).angle(Gem.Axes.Z,s->(float)s.bus("TURN")[2]).spin(Gem.Axes.X,0,0,-4,s->(float)s.bus("LIFT")[0]).spin(Gem.Axes.X,0,0,-4,s->(float)-s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,0,-2,s->(float)-s.bus("FLIP")[0]);g.child(o.part("Gun"),body);g.child(o.part("Stock"),body).visibleIf(s->!shortened(s));g.child(o.part("Barrel"),body).visibleIf(s->!shortened(s));g.child(o.part("Lever"),body).spin(Gem.Axes.X,0,.125F,-2.875F,s->(float)s.bus("LEVER")[0]);g.child(o.part("Shell"),body).slide(Gem.Axes.X,s->(float)s.bus("SHELL")[0]).slide(Gem.Axes.Y,s->(float)(s.bus("SHELL")[1]-.75)).slide(Gem.Axes.Z,s->(float)s.bus("SHELL")[2]);g.child(o.part("Shell"),body).slide(Gem.Axes.Y,s->-.5F).visibleIf(s->s.bus("FLAG")[0]!=0);Gem.Part<WeaponPose>rest=d.joint().visibleIf(s->!s.animated);rest.child(o.part("Gun"),body);rest.child(o.part("Lever"),body);rest.child(o.part("Stock"),body).visibleIf(s->!shortened(s));rest.child(o.part("Barrel"),body).visibleIf(s->!shortened(s));}
    private static boolean shortened(WeaponPose s){return SpecialWeaponRenderers.itemPath(s.stack).equals("gun_maresleg_broken");}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        if (shortened(stack)) {
            pose.scale(2.5F, 2.5F, 2.5F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(-1F, 0F, 0F);
        } else {
            pose.scale(1.4375F, 1.4375F, 1.4375F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(-0.5F, 0.5F, 0F);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.75F, 1.75F, 1.75F);
        pose.translate(0F, 0.25F, 3F);
    }

    private static boolean shortened(ItemStack stack) {
        return SpecialWeaponRenderers.itemPath(stack).equals("gun_maresleg_broken")
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SAWED_OFF);
    }
}
