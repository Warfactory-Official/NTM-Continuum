package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/**
 * Exact Gem port of original-hbm ItemRenderMAS36. The weapon table draws 1.7's renderModTable: Gun, Stock,
 * Bolt, the scope and the bayonet when fitted, all at the origin (renderOther's non-EQUIPPED bayonet offset
 * is not in it).
 */
public final class Mas36WeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/mas36.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/mas36.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.5F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1.5F*o,-1.25F*o,1.75F*o,0,-4.6825/8D,.75);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).spin(Gem.Axes.X,0,-3,-3,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,-3,-3,s->(float)s.bus("LIFT")[0]).slide(Gem.Axes.Z,s->(float)s.bus("STAB")[2]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.part("Gun"),BODY);g.child(o.part("Bayonet"),BODY).visibleIf(Mas36WeaponRenderer::bayonet);g.child(o.part("Stock"),BODY).spin(Gem.Axes.X,0,.3125F,-2.125F,s->(float)s.bus("STOCK")[0]);g.child(o.part("Bolt"),BODY).spin(Gem.Axes.Z,0,1.15625F,0,s->(float)s.bus("BOLT_TURN")[2]).slide(Gem.Axes.Z,s->(float)s.bus("BOLT_PULL")[2]);g.child(o.part("Bullet"),BODY).slide(Gem.Axes.X,s->(float)s.bus("BULLET")[0]).slide(Gem.Axes.Y,s->(float)s.bus("BULLET")[1]).slide(Gem.Axes.Z,s->(float)s.bus("BULLET")[2]);g.child(o.part("Scope"),BODY).visibleIf(Mas36WeaponRenderer::scoped);Gem.Part<WeaponPose>clip=g.joint().visibleIf(s->s.bus("SHOW_CLIP")[0]!=0).slide(Gem.Axes.X,s->(float)s.bus("CLIP")[0]).slide(Gem.Axes.Y,s->(float)s.bus("CLIP")[1]).slide(Gem.Axes.Z,s->(float)s.bus("CLIP")[2]);clip.child(o.part("Clip"),BODY);clip.child(o.part("Bullets"),BODY).slide(Gem.Axes.X,s->(float)s.bus("BULLETS")[0]).slide(Gem.Axes.Y,s->(float)s.bus("BULLETS")[1]).slide(Gem.Axes.Z,s->(float)s.bus("BULLETS")[2]);d.part(o.parts("Gun","Stock","Bolt","Bullet","Clip","Bullets"),BODY).visibleIf(s->!s.animated&&!s.modTable);d.part(o.part("Scope"),BODY).visibleIf(s->!s.animated&&!s.modTable&&scoped(s));d.part(o.part("Bayonet"),BODY).visibleIf(s->!s.animated&&!s.modTable&&bayonet(s));d.part(o.parts("Gun","Stock","Bolt"),BODY).visibleIf(s->s.modTable);d.part(o.part("Scope"),BODY).visibleIf(s->s.modTable&&scoped(s));d.part(o.part("Bayonet"),BODY).visibleIf(s->s.modTable&&bayonet(s));}
    @Override protected boolean scopedIn(ItemStack s){return scoped(s)&&fullyAimed();}
    private static boolean scoped(ItemStack s){return XWeaponModManager.hasUpgrade(s,0,XWeaponModManager.ID_SCOPE);}
    private static boolean scoped(WeaponPose s){return scoped(s.stack);}
    private static boolean bayonet(WeaponPose s){return XWeaponModManager.hasUpgrade(s.stack,0,XWeaponModManager.ID_MAS_BAYONET);}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.5F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.translate(0F, 0.5F, 3F);
    }
}
