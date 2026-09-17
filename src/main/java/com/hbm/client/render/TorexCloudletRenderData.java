package com.hbm.client.render;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.effect.EntityNukeTorex.Cloudlet;
import dev.golem.core.render.Billboards;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;

/** One shared translation of Torex cloudlets into fallback or instanced billboard rows. */
public final class TorexCloudletRenderData {

    private static final Comparator<Cloudlet> CLOUD_SORTER = (first, second) -> {
        int distance = Double.compare(second.renderSortDistanceSq, first.renderSortDistanceSq);
        return distance != 0 ? distance
                : Long.compare(first.getCreationOrdinal(), second.getCreationOrdinal());
    };

    private TorexCloudletRenderData() {}

    public static boolean instancingEnabled() {
        return !"off".equalsIgnoreCase(System.getProperty("hbm.torex.instancing", "auto"));
    }

    public static void sort(EntityNukeTorex cloud, Vec3 camera) {
        if (cloud.lastRenderSortTick == cloud.tickCount
                && Double.compare(cloud.lastRenderSortCameraX, camera.x) == 0
                && Double.compare(cloud.lastRenderSortCameraY, camera.y) == 0
                && Double.compare(cloud.lastRenderSortCameraZ, camera.z) == 0) return;
        for (Cloudlet cloudlet : cloud.cloudlets) {
            double dx = camera.x - cloudlet.posX;
            double dy = camera.y - cloudlet.posY;
            double dz = camera.z - cloudlet.posZ;
            cloudlet.renderSortDistanceSq = dx * dx + dy * dy + dz * dz;
        }
        cloud.cloudlets.sort(CLOUD_SORTER);
        cloud.lastRenderSortTick = cloud.tickCount;
        cloud.lastRenderSortCameraX = camera.x;
        cloud.lastRenderSortCameraY = camera.y;
        cloud.lastRenderSortCameraZ = camera.z;
    }

    public static void writeInstanced(EntityNukeTorex cloud, Billboards.Frame frame,
                                      Billboards.LayerWriter layers) {
        if (!cloud.isClientAnimationReady() || !instancingEnabled()) return;
        long benchmarkStart = TorexBenchmarkClient.instancedStart(cloud);
        sort(cloud, frame.cameraPosition());
        write(cloud, frame.partialTick(), layers.layer(0));
        TorexBenchmarkClient.instancedEnd(benchmarkStart, cloud.cloudlets.size(), true);
        writeFlare(cloud, frame.partialTick(), layers.layer(1));
    }

    public static void write(EntityNukeTorex cloud, float partialTicks, Billboards.RowWriter writer) {
        float cloudAlphaBase = cloud.getAlpha();
        for (Cloudlet c : cloud.cloudlets) {
            float lifeFrac = (float) c.age / (float) c.cloudletLife;
            float alpha = (1F - lifeFrac) * cloudAlphaBase;
            if (c.type == EntityNukeTorex.TorexType.CONDENSATION) alpha *= 0.25F;
            alpha = Mth.clamp(alpha, 0.0001F, 1F);

            float size = c.startingScale + lifeFrac * c.growingScale;
            float brightness = c.type == EntityNukeTorex.TorexType.CONDENSATION ? 0.9F : 0.75F * c.colorMod;
            double greying = c.type == EntityNukeTorex.TorexType.RING ? 0.05D : 0D;
            float red = clampColor(color(c, c.prevColorR, c.colorR, partialTicks, greying), brightness);
            float green = clampColor(color(c, c.prevColorG, c.colorG, partialTicks, greying), brightness);
            float blue = clampColor(color(c, c.prevColorB, c.colorB, partialTicks, greying), brightness);
            int br = lightmap(red, green, blue);

            writer.row(
                    c.prevPosX + (c.posX - c.prevPosX) * partialTicks,
                    c.prevPosY + (c.posY - c.prevPosY) * partialTicks,
                    c.prevPosZ + (c.posZ - c.prevPosZ) * partialTicks,
                    size, red, green, blue, alpha, br | (br << 16));
        }
    }

    public static void writeFlare(EntityNukeTorex cloud, float partialTicks, Billboards.RowWriter writer) {
        float flareDuration = (float) cloud.getScale() * TorexRenderer.flareBaseDuration;
        double age = Math.min(cloud.tickCount + partialTicks, flareDuration);
        float fade = (float) Math.min(1, (flareDuration - age) / flareDuration);
        if (fade <= 0F) return;

        float size = (float) (10 * cloud.rollerSize);
        int brightness = (int) (fade * 240F);
        int packedLight = brightness | (brightness << 16);
        RandomSource random = RandomSource.create(cloud.getId());
        for (int i = 0; i < 3; i++) {
            double x = cloud.getX() + random.nextGaussian() * 0.5F * cloud.rollerSize;
            double y = cloud.getY() + cloud.coreHeight + random.nextGaussian() * 0.5F * cloud.rollerSize;
            double z = cloud.getZ() + random.nextGaussian() * 0.5F * cloud.rollerSize;
            writer.row(x, y, z, size, 1F, 1F, 1F, fade, packedLight);
        }
    }

    private static double color(Cloudlet cloudlet, double previous, double current,
                                float partialTicks, double greying) {
        if (cloudlet.type == EntityNukeTorex.TorexType.CONDENSATION) return 1D;
        return previous + (current - previous) * partialTicks + greying;
    }

    private static float clampColor(double color, float brightness) {
        float channel = (float) color * brightness;
        if (channel < 0.15F) return 0.15F;
        return channel > 1F ? 1F : channel;
    }

    private static int lightmap(float red, float green, float blue) {
        float average = (red + green + blue) / 3F;
        if (average > 1F) average = 1F;
        int brightness = (int) (average * 240F);
        return brightness < 48 ? 48 : brightness;
    }
}
