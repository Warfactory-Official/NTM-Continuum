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

/** Exact Gem port of original-hbm ItemRenderSexy; the constructor selects Sexy or Heretic skin. */
public final class SexyWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID=Ids.of("hbm","models/weapons/sexy.obj");
    private final Surface body;
    private static final Surface WHISKEY=Surface.cutout("hbm:textures/models/weapons/whiskey.png");
    private WavefrontObj model,whiskey;
    public SexyWeaponRenderer(String texture){body=Surface.cutout("hbm:textures/models/weapons/"+texture+".png");}
    private WavefrontObj model(){if(model==null)model=WavefrontObj.load(MODEL_ID);return model;}
    private WavefrontObj whiskey(){if(whiskey==null)whiskey=WavefrontObj.load(Ids.of("hbm","models/weapons/whiskey.obj"));return whiskey;}
    @Override public org.joml.Vector3fc[] extents(){float[]a=model().boundsMin(),b=model().boundsMax();org.joml.Vector3fc[]o=new org.joml.Vector3fc[8];int i=0;for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++)o[i++]=new org.joml.Vector3f(x==0?a[0]:b[0],y==0?a[1]:b[1],z==0?a[2]:b[2]);return o;}
    @Override protected float getTurnMagnitude(ItemStack s){return ItemGunBaseNT.getIsAiming(s)?2.5F:-.25F;}
    @Override public float viewFov(ItemStack s,float f,float p){return aimingFov(s,f,p,.33F);}
    @Override public void setupFirstPerson(ItemStack s,PoseStack p){p.translate(0,0,.875);float o=.8F;standardAimingTransform(s,p,-1F*o,-.75F*o,3F*o,-.5F,-.5F,2);}
    @Override protected void setup(Gem.Model<WeaponPose>d){WavefrontObj o=model();Gem.Part<WeaponPose>g=d.joint().visibleIf(s->s.animated).stretch(s->.375F).spin(Gem.Axes.X,0,-1,-8,s->(float)s.bus("EQUIP")[0]).spin(Gem.Axes.X,0,0,6,s->(float)s.bus("LOWER")[0]).slide(Gem.Axes.Z,s->(float)s.bus("RECOIL")[2]);Gem.Part<WeaponPose>drink=g.joint().visibleIf(s->s.bus("BOTTLE")[0]!=0).slide(Gem.Axes.X,s->(float)s.bus("BOTTLE")[0]).slide(Gem.Axes.Y,s->(float)s.bus("BOTTLE")[1]).slide(Gem.Axes.Z,s->(float)s.bus("BOTTLE")[2]).translate(0,2,0).angle(Gem.Axes.X,s->(float)s.bus("SIP")[0]).rotate(Gem.Axes.Y,90).rotate(Gem.Axes.X,-15).translate(0,-2,0).scale(1.5F);drink.child(whiskey().all(),WHISKEY);g.child(o.part("Gun"),body);g.child(o.part("Barrel"),body).slide(Gem.Axes.Z,s->(float)s.bus("BARREL")[2]);g.child(o.part("RecoilSpring"),body).translate(0,0,-.375F).stretch(Gem.Axes.Z,s->1F+.457247371F*(float)s.bus("BARREL")[2]).translate(0,0,.375F);g.child(o.part("Hood"),body).spin(Gem.Axes.X,0,.4375F,-2.875F,s->(float)s.bus("HOOD")[0]);g.child(o.part("Lever"),body).spin(Gem.Axes.X,0,.46875F,-6.875F,s->(float)s.bus("LEVER")[2]*60);g.child(o.part("LockSpring"),body).stretch(Gem.Axes.Z,s->1F-(float)s.bus("LEVER")[2]*.25F);Gem.Part<WeaponPose>mag=g.joint().slide(Gem.Axes.X,s->(float)s.bus("MAG")[0]).slide(Gem.Axes.Y,s->(float)s.bus("MAG")[1]).slide(Gem.Axes.Z,s->(float)s.bus("MAG")[2]).spin(Gem.Axes.Z,0,-1,0,s->(float)s.bus("MAGROT")[2]);mag.child(o.part("Magazine"),body);for(int i=0;i<6;i++){final int n=i;mag.child(o.part("Belt"),body).pose(s->beltPose(s,n));mag.child(o.part("Shell"),body).visibleIf(s->shellVisible(s,n)).pose(s->beltPose(s,n));}d.part(o.parts("Gun","Barrel","RecoilSpring","Hood","Lever","LockSpring","Magazine"),body).visibleIf(s->!s.animated);for(int i=0;i<6;i++){final int n=i;d.part(o.part("Belt"),body).visibleIf(s->!s.animated).pose(s->staticBeltPose(n));d.part(o.part("Shell"),body).visibleIf(s->!s.animated).pose(s->staticBeltPose(n));}}
    private static Matrix4f beltPose(WeaponPose s,int i){double[]loaded={0,0,20,20,50,60,70},unloaded={0,-10,-50,-60,-60,0,0};double reload=s.bus("BELT")[0],cycle=s.bus("CYCLE")[0];if(!s.animated){reload=1;cycle=1;}double x=.0625*17,y=.0625*-26,angle=0;float vx=0,vy=.4375F;double[]xs=new double[7],ys=new double[7],as=new double[7];for(int n=0;n<7;n++){xs[n]=x;ys[n]=y;as[n]=angle-90;double delta=unloaded[n]+(loaded[n]-unloaded[n])*reload;angle+=delta;double rad=Math.toRadians(-delta),nx=vx*Math.cos(rad)-vy*Math.sin(rad),ny=vx*Math.sin(rad)+vy*Math.cos(rad);vx=(float)nx;vy=(float)ny;x+=vx;y+=vy;}double xx=xs[i]+(xs[i+1]-xs[i])*cycle,yy=ys[i]+(ys[i+1]-ys[i])*cycle,aa=as[i]+(as[i+1]-as[i])*cycle;return new Matrix4f().translate((float)xx,.375F+(float)yy,0).rotateZ((float)Math.toRadians(aa)).translate(0,-.375F,0);}
    private static Matrix4f staticBeltPose(int i){return beltPose(new WeaponPose(),i);}
    private static boolean shellVisible(WeaponPose s,int i){if(!s.animated)return true;if(!(s.stack.getItem() instanceof ItemGunBaseNT g))return true;return 7-i<SpecialWeaponRenderers.shellCount(s,0)+2;}

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
        pose.scale(1.75F, 1.75F, 1.75F);
        pose.translate(1F, 1F, 6F);
    }
}
