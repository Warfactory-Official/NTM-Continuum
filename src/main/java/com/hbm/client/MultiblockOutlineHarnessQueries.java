package com.hbm.client;

import com.google.gson.JsonObject;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.render.BlockOutlines;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The {@code hbm_outline} harness query: the crosshair's block and the outline drawn around it, either the
 * replacement {@link BlockOutlines} answers or the block's own shape, as bounds relative to that block and a box count.
 */
public final class MultiblockOutlineHarnessQueries {

    private MultiblockOutlineHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_outline", (mc, params) -> query(mc));
    }

    private static JsonObject query(Minecraft mc) {
        JsonObject json = new JsonObject();
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            json.addProperty("target", "none");
            return json;
        }
        BlockPos pos = hit.getBlockPos();
        VoxelShape replaced = BlockOutlines.replacement(mc.level, pos);
        VoxelShape shape = replaced != null ? replaced : mc.level.getBlockState(pos).getShape(mc.level, pos);
        json.addProperty("target", pos.getX() + "," + pos.getY() + "," + pos.getZ());
        json.addProperty("replaced", replaced != null);
        json.addProperty("boxes", shape.toAabbs().size());
        if (!shape.isEmpty()) {
            AABB b = shape.bounds();
            json.addProperty("bounds", b.minX + "," + b.minY + "," + b.minZ + " .. " + b.maxX + "," + b.maxY + "," + b.maxZ);
        }
        return json;
    }
}
