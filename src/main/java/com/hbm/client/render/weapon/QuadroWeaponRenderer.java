package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderQuadro. */
public final class QuadroWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/quadro.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/quadro.png");
    private static final Surface ROCKET=Surface.cutout("hbm:textures/models/weapons/quadro_rocket.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.25F;}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-2.5F*o,-3.5F*o,2.5F*o,-1.5F*o,-3F*o,2.5F*o);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->1.75F).spin(Gem.Axes.X,0,-1,-1,s->(float)s.bus("EQUIP")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]).spin(Gem.Axes.X,0,-1,-1,s->(float)s.bus("RELOAD_ROTATE")[2]);g.child(o.part("Launcher"),BODY);Gem.Part<WeaponPose>r=g.joint().translate(0,-1F,0).spin(Gem.Axes.X,0,3,0,s->(float)s.bus("RELOAD_PUSH")[1]*30).slide(Gem.Axes.Z,s->(float)s.bus("RELOAD_PUSH")[0]*3);r.child(o.part("Rockets"),ROCKET);d.part(o.part("Launcher"),BODY).visibleIf(s->!s.animated);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(4.75F, 4.75F, 4.75F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, -1F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(7.5F, 7.5F, 7.5F);
        pose.translate(0F, -0.5F, -0.25F);
    }
}
