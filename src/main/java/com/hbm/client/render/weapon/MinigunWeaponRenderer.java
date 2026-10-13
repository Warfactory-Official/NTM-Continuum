package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of 1.7 ItemRenderMinigun, including the Lacunae skin. */
public final class MinigunWeaponRenderer extends WeaponRenderer {
    private static final Identifier MODEL_ID=Ids.of("hbm","models/weapons/minigun.obj");
    private final Surface body;
    private WavefrontObj model;
    public MinigunWeaponRenderer(String texture){body=Surface.cutout("hbm:textures/models/weapons/"+texture+".png");}
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.75F*o,-1.75F*o,3.5F*o,0,-6.25/8D,1);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).spin(Gem.Axes.X,0,3,-6,s->(float)s.bus("EQUIP")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.part("Gun"),body);g.child(o.part("Grip"),body);g.child(o.part("Barrels"),body).angle(Gem.Axes.Z,s->(float)s.bus("ROTATE")[2]);d.part(o.parts("Gun","Grip","Barrels"),body).visibleIf(s->!s.animated);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(0.875F, 0.875F, 0.875F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.25F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.75F, 1.75F, 1.75F);
        pose.translate(1F, -3.5F, 8F);
    }
}
