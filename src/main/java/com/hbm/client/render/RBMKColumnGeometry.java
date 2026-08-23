package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Small box emitter for the column pieces which cannot be baked because their Y is runtime-configured. */
final class RBMKColumnGeometry {
    private RBMKColumnGeometry() {}
    static void box(PoseStack.Pose pose, VertexConsumer out, float x0, float y0, float z0, float x1, float y1, float z1,
                    int light, boolean caps) {
        box(pose, out, x0, y0, z0, x1, y1, z1, light, light, caps);
    }
    static void box(PoseStack.Pose pose, VertexConsumer out, float x0, float y0, float z0, float x1, float y1, float z1,
                    int sideLight, int capLight, boolean caps) {
        if (caps) {
            face(out, pose, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1, capLight, 0,1,0);
            face(out, pose, x1,y0,z1, x1,y0,z0, x0,y0,z0, x0,y0,z1, capLight, 0,-1,0);
        } else {
            face(out, pose, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, sideLight, 0,0,-1);
            face(out, pose, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, sideLight, 0,0,1);
            face(out, pose, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, sideLight, -1,0,0);
            face(out, pose, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, sideLight, 1,0,0);
        }
    }
    private static void face(VertexConsumer out, PoseStack.Pose pose, float ax,float ay,float az,float bx,float by,float bz,
                             float cx,float cy,float cz,float dx,float dy,float dz,int light,float nx,float ny,float nz) {
        Vertexes.vertex(out,pose,ax,ay,az,-1,0,0,OverlayTexture.NO_OVERLAY,light,nx,ny,nz); Vertexes.vertex(out,pose,bx,by,bz,-1,1,0,OverlayTexture.NO_OVERLAY,light,nx,ny,nz);
        Vertexes.vertex(out,pose,cx,cy,cz,-1,1,1,OverlayTexture.NO_OVERLAY,light,nx,ny,nz); Vertexes.vertex(out,pose,dx,dy,dz,-1,0,1,OverlayTexture.NO_OVERLAY,light,nx,ny,nz);
    }
}
