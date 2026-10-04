package com.hbm.client.render;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.effect.EntityNukeTorex.Cloudlet;
import dev.golem.core.render.Billboards;
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

    /** 1.7 FULLBRIGHT: the entity's getBrightnessForRender (15728880) lights every cloudlet and flare. */
    private static final int FULLBRIGHT = 0x00F000F0;

    /**
     * 1.7 RenderTorex.tessellateCloudlet: the interpolated, scale-mapped position; the cloudlet's own
     * scale as the half edge; colour times 0.9 (condensation) or 0.75 * colorMod, clamped as the 1.7
     * tessellator clamped; the cloudlet alpha; fullbright.
     */
    public static void write(EntityNukeTorex cloud, float partialTicks, Billboards.RowWriter writer) {
        for (Cloudlet c : cloud.cloudlets) {
            float brightness = c.type == EntityNukeTorex.TorexType.CONDENSATION ? 0.9F : 0.75F * c.colorMod;
            writer.row(c.getInterpX(partialTicks), c.getInterpY(partialTicks), c.getInterpZ(partialTicks),
                    c.getScale(),
                    channel(c.getInterpColor(0, partialTicks), brightness),
                    channel(c.getInterpColor(1, partialTicks), brightness),
                    channel(c.getInterpColor(2, partialTicks), brightness),
                    c.getAlpha(), FULLBRIGHT);
        }
    }

    /**
     * 1.7 RenderTorex.flashWrapper: for the first 100 ticks, three white additive billboards of half edge
     * {@code 25 * rollerSize} scattered around the core head by a {@code new Random(entityId)} gaussian, fading
     * linearly to nothing. Simulation-space values, not mapped by the scale, as 1.7 drew them.
     */
    public static void writeFlare(EntityNukeTorex cloud, float partialTicks, Billboards.RowWriter writer) {
        if (cloud.tickCount >= 101) return;
        double age = Math.min(cloud.tickCount + partialTicks, 100);
        float alpha = (float) ((100D - age) / 100F);

        java.util.Random rand = new java.util.Random(cloud.getId());
        for (int i = 0; i < 3; i++) {
            float x = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
            float y = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
            float z = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
            writer.row(cloud.getX() + x, cloud.getY() + y + cloud.coreHeight, cloud.getZ() + z,
                    (float) (25 * cloud.rollerSize), 1F, 1F, 1F, alpha, FULLBRIGHT);
        }
    }

    private static float channel(double color, float brightness) {
        float value = (float) color * brightness;
        return value < 0F ? 0F : value > 1F ? 1F : value;
    }
}
