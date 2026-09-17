package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/** Exact Gem port of original-hbm ItemRenderMK108, including its procedural grenade belt. */
public final class Mk108WeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/mk108.obj");
    private static final Surface BODY=Surface.cutout("hbm:textures/models/weapons/mk108.png");
    private WavefrontObj model;
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.25F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1F*o,-1.5F*o,2.5F*o,-.75F,-.75F,1.5F);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).spin(Gem.Axes.X,0,-1,-8,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,1,-4,s->(float)s.bus("LIFT")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);g.child(o.part("Gun"),BODY);g.child(o.part("Barrel"),BODY).slide(Gem.Axes.Z,s->(float)s.bus("BARREL")[2]*2);g.child(o.part("Lid"),BODY).spin(Gem.Axes.X,0,.6875F,-1F,s->(float)s.bus("LID")[0]);for(int i=1;i<=3;i++){final int n=i;Gem.Part<WeaponPose>grenade=g.joint().visibleIf(s->grenadeVisible(s,n)).slide(Gem.Axes.X,s->(float)s.bus("GRENH"+n)[0]).slide(Gem.Axes.Y,s->(float)s.bus("GRENV"+n)[1]).spin(Gem.Axes.X,0,0,-2.3125F,s->-90F).spin(Gem.Axes.Y,0,0,-2.3125F,s->-(float)s.bus("GRENS"+n)[0]);grenade.child(o.part("Grenade"),BODY);}Gem.Part<WeaponPose>drum=g.joint().slide(Gem.Axes.X,s->(float)s.bus("DRUM")[0]).slide(Gem.Axes.Y,s->(float)s.bus("DRUM")[1]).slide(Gem.Axes.Z,s->(float)s.bus("DRUM")[2]);drum.child(o.part("Drum"),BODY);for(int i=0;i<8;i++){final int n=i;drum.child(o.part("Belt"),BODY).pose(s->beltPose(s,n));}d.part(o.parts("Gun","Barrel","Lid","Drum"),BODY).visibleIf(s->!s.animated);for(int i=0;i<8;i++){final int n=i;d.part(o.part("Belt"),BODY).visibleIf(s->!s.animated).pose(s->staticBeltPose(n));}}
    private static Matrix4f beltPose(WeaponPose s,int index){double[]loaded={0,0,-5,0,-5,60,45,-10,0},unloaded={0,-30,-60,-45,-45,0,0,0,0};double progress=s.animated?s.bus("BELT")[0]:1;double x=.0625*22,y=.0625*-46,angle=0;float vx=0,vy=.53125F;double[]xs=new double[9],ys=new double[9],as=new double[9];for(int n=0;n<9;n++){xs[n]=x;ys[n]=y;as[n]=angle-90;double delta=unloaded[n]+(loaded[n]-unloaded[n])*progress;angle+=delta;double r=Math.toRadians(-delta),nx=vx*Math.cos(r)-vy*Math.sin(r),ny=vx*Math.sin(r)+vy*Math.cos(r);vx=(float)nx;vy=(float)ny;x+=vx;y+=vy;}double cycle=s.animated?s.bus("CYCLE")[0]:1;double xx=xs[index]+(xs[index+1]-xs[index])*cycle,yy=ys[index]+(ys[index+1]-ys[index])*cycle,aa=as[index]+(as[index+1]-as[index])*cycle;return new Matrix4f().translate((float)xx,(float)yy,0).rotateZ((float)Math.toRadians(aa));}
    private static Matrix4f staticBeltPose(int i){return beltPose(new WeaponPose(),i);}
    private static boolean grenadeVisible(WeaponPose s,int i){if(!s.animated)return false;return s.bus("GRENH"+i)[0]!=0&&s.bus("GRENH"+i)[0]>-4;}

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.375F, 1.375F, 1.375F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, 0.5F, 0.25F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(2F, 2F, 2F);
        pose.translate(1F, -2.5F, 4F);
    }
}
