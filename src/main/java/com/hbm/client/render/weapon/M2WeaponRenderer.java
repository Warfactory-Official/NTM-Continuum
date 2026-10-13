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

/** Exact Gem port of 1.7 ItemRenderM2. */
public final class M2WeaponRenderer extends WeaponRenderer {
    private static final Identifier MODEL_ID=Ids.of("hbm","models/weapons/m2_browning.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/m2_browning.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.5F*o,-2.5F*o,1.75F*o,0,-12.5/8D,1.75);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.75F).rotate(Gem.Axes.Y,180F).spin(Gem.Axes.X,0,1,-2.25F,s->(float)s.bus("EQUIP")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.all(),BODY);d.joint().visibleIf(s->!s.animated).rotate(Gem.Axes.Y,180F).child(o.all(),BODY);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(2.625F, 2.625F, 2.625F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0.5F, -1.25F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(5F, 5F, 5F);
        pose.translate(0.5F, -2F, 3F);
    }
}
