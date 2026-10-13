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

/** Exact Gem port of 1.7 ItemRenderAmat; the skin is selected by the registration helper. */
public final class AmatWeaponRenderer extends WeaponRenderer {
    private static final Identifier MODEL_ID=Ids.of("hbm","models/weapons/amat.obj");
    private static final Surface SILENCER=Surface.cutout("hbm:textures/models/weapons/g3_attachments.png");
    private final Surface body;
    private WavefrontObj model,g3Model;
    public AmatWeaponRenderer(String texture){body=Surface.cutout("hbm:textures/models/weapons/"+texture+".png");}
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    private WavefrontObj g3(){if(g3Model==null)g3Model=WavefrontObj.load(Ids.of("hbm","models/weapons/g3.obj"));return g3Model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1F*o,-1F*o,3.25F*o,0,-4.875/8D,1.875);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]).spin(Gem.Axes.X,0,-3,-8,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,-3,-8,s->(float)s.bus("LIFT")[0]);g.child(o.part("Gun"),body);g.child(o.part("Bolt"),body).spin(Gem.Axes.Z,0,.625F,0,s->(float)s.bus("BOLT_TURN")[2]).slide(Gem.Axes.Z,s->(float)s.bus("BOLT_PULL")[2]);g.child(o.part("Magazine"),body).slide(Gem.Axes.X,s->(float)s.bus("MAG")[0]).slide(Gem.Axes.Y,s->(float)s.bus("MAG")[1]).slide(Gem.Axes.Z,s->(float)s.bus("MAG")[2]);Gem.Part<WeaponPose>scope=g.child(o.part("Scope"),body).slide(Gem.Axes.X,s->(float)s.bus("SCOPE_THROW")[0]).slide(Gem.Axes.Y,s->(float)s.bus("SCOPE_THROW")[1]).slide(Gem.Axes.Z,s->(float)s.bus("SCOPE_THROW")[2]).spin(Gem.Axes.X,0,1.5F,-4.5F,s->(float)s.bus("SCOPE_SPIN")[0]);scope.visibleIf(AmatWeaponRenderer::scoped);Gem.Part<WeaponPose>left=g.joint().spin(Gem.Axes.Z,.3125F,-.625F,-1F,s->(float)s.bus("BIPOD")[1]);left.child(o.part("BipodHingeLeft"),body);left.joint().spin(Gem.Axes.X,.3125F,-.625F,-1F,s->(float)s.bus("BIPOD")[0]).child(o.part("BipodLeft"),body);Gem.Part<WeaponPose>right=g.joint().spin(Gem.Axes.Z,-.3125F,-.625F,-1F,s->(float)-s.bus("BIPOD")[1]);right.child(o.part("BipodHingeRight"),body);right.joint().spin(Gem.Axes.X,-.3125F,-.625F,-1F,s->(float)s.bus("BIPOD")[0]).child(o.part("BipodRight"),body);d.part(o.parts("Gun","Bolt","Magazine","BipodHingeLeft","BipodLeft","BipodHingeRight","BipodRight"),body).visibleIf(s->!s.animated);d.part(o.part("Scope"),body).visibleIf(s->!s.animated&&scoped(s));d.part(g3().part("Silencer"),SILENCER).visibleIf(s->!s.animated&&silenced(s)).translate(0,.625F,-4.3125F).scale(1.25F);d.part(o.part("MuzzleBrake"),body).visibleIf(s->!s.animated&&!silenced(s));g.child(o.part("MuzzleBrake"),body).visibleIf(s->!silenced(s));g.child(g3().part("Silencer"),SILENCER).visibleIf(AmatWeaponRenderer::silenced).translate(0,.625F,-4.3125F).scale(1.25F);}
    @Override protected boolean scopedIn(ItemStack stack){return fullyAimed();}
    private static boolean scoped(WeaponPose s){return true;}
    private static boolean silenced(WeaponPose s){return SpecialWeaponRenderers.itemPath(s.stack).equals("gun_amat_penance");}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        if (silenced(stack)) {
            pose.scale(0.8175F, 0.8175F, 0.8175F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(-0.5F, 0.5F, -1F);
        } else {
            pose.scale(0.9375F, 0.9375F, 0.9375F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(45F));
            pose.translate(-0.5F, 0.5F, 0F);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.translate(0F, 0.5F, 6.75F);
    }

    private static boolean silenced(ItemStack stack) {
        return SpecialWeaponRenderers.itemPath(stack).equals("gun_amat_penance")
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
    }
}
