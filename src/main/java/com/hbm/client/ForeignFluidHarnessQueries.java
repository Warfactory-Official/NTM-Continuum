package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.fluid.FluidCompat;
import dev.golem.core.fluid.FluidView;
import dev.golem.core.fluid.TokenFluids;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The {@code hbm_fluidcaps} harness query, read by {@code ForeignFluidCaps}. Every move
 * goes through the loader's standard fluid capability ({@link FluidCompat}), as another mod's pipe would. Ops:
 * {@code registry} (HBM fluids in the vanilla fluid registry), {@code find} (the tanks at {@code x,y,z} on {@code side}),
 * {@code insert}/{@code extract} ({@code fluid} = an HBM fluid name, {@code mb}), {@code move} (extract at
 * {@code x,y,z,side}, insert at {@code tx,ty,tz,tside}), {@code bucket} (a vanilla water bucket's own handler emptied
 * into {@code x,y,z,side}), {@code jei} (the HBM fluids JEI lists as fluid ingredients, client side). Inert unless the
 * harness runs.
 */
public final class ForeignFluidHarnessQueries {

    private ForeignFluidHarnessQueries() {}

    /** Set by the JEI plugin once its runtime exists: the names of the HBM fluid ingredients JEI lists. */
    public static volatile Supplier<List<String>> jeiFluids = null;

    public static void register() {
        HarnessQueryRegistry.register("hbm_fluidcaps", ForeignFluidHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        String op = params.has("op") ? params.get("op").getAsString() : "registry";
        if (op.equals("jei")) return jei();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return new JsonObject();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                result.complete(player == null ? new JsonObject() : onServer((ServerLevel) player.level(), op, params));
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            return result.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("error", e.toString());
            return error;
        }
    }

    private static JsonObject jei() {
        JsonObject json = new JsonObject();
        Supplier<List<String>> source = jeiFluids;
        json.addProperty("ready", source != null);
        if (source == null) return json;
        List<String> names = source.get();
        json.addProperty("count", names.size());
        JsonArray array = new JsonArray();
        names.forEach(array::add);
        json.add("names", array);
        return json;
    }

    private static int i(JsonObject p, String k, int d) {
        return p.has(k) ? p.get(k).getAsInt() : d;
    }

    private static Direction side(JsonObject p, String key) {
        if (!p.has(key)) return null;
        String name = p.get(key).getAsString();
        for (Direction direction : Direction.values()) if (direction.getSerializedName().equals(name)) return direction;
        return null;
    }

    private static Fluid fluid(JsonObject p) {
        return Fluids.toVanilla(Fluids.byRegistryName(p.has("fluid") ? p.get("fluid").getAsString() : "none"));
    }

    private static JsonObject onServer(ServerLevel level, String op, JsonObject p) {
        JsonObject json = new JsonObject();
        BlockPos pos = new BlockPos(i(p, "x", 0), i(p, "y", 0), i(p, "z", 0));
        Direction side = side(p, "side");
        long mb = p.has("mb") ? p.get("mb").getAsLong() : 1000L;
        switch (op) {
            case "registry" -> {
                int hbm = 0;
                for (Fluid fluid : BuiltInRegistries.FLUID) if (TokenFluids.isToken(fluid)) hbm++;
                json.addProperty("tokens", hbm);
                json.addProperty("fluidTypes", Fluids.getAll().length);
                json.addProperty("diesel", BuiltInRegistries.FLUID.getKey(Fluids.DIESEL.getFluid()).toString());
                json.addProperty("water", BuiltInRegistries.FLUID.getKey(Fluids.WATER.getFluid()).toString());
            }
            case "find" -> describe(json, FluidCompat.find(level, pos, side));
            case "insert" -> {
                FluidView view = FluidCompat.find(level, pos, side);
                json.addProperty("moved", view == null ? -1 : view.insert(fluid(p), mb, false));
                describe(json, view);
            }
            case "extract" -> {
                FluidView view = FluidCompat.find(level, pos, side);
                json.addProperty("moved", view == null ? -1 : view.extract(fluid(p), mb, false));
                describe(json, view);
            }
            case "move" -> {
                FluidView from = FluidCompat.find(level, pos, side);
                FluidView to = FluidCompat.find(level, new BlockPos(i(p, "tx", 0), i(p, "ty", 0), i(p, "tz", 0)), side(p, "tside"));
                if (from == null || to == null) {
                    json.addProperty("moved", -1);
                } else {
                    Fluid fluid = fluid(p);
                    long room = to.insert(fluid, from.extract(fluid, mb, true), true);
                    long taken = from.extract(fluid, room, false);
                    json.addProperty("moved", to.insert(fluid, taken, false));
                    json.addProperty("taken", taken);
                }
            }
            case "bucket" -> {
                FluidView bucket = FluidCompat.findItem(new ItemStack(Items.WATER_BUCKET));
                FluidView target = FluidCompat.find(level, pos, side);
                if (bucket == null || target == null) {
                    json.addProperty("moved", -1);
                } else {
                    Fluid water = net.minecraft.world.level.material.Fluids.WATER;
                    long drained = bucket.extract(water, target.insert(water, 1000, true), false);
                    json.addProperty("moved", target.insert(water, drained, false));
                    json.addProperty("bucketAfter", BuiltInRegistries.ITEM.getKey(bucket.container().getItem()).toString());
                }
                describe(json, target);
            }
            default -> json.addProperty("error", "unknown op " + op);
        }
        return json;
    }

    private static void describe(JsonObject json, FluidView view) {
        json.addProperty("present", view != null);
        if (view == null) return;
        JsonArray tanks = new JsonArray();
        for (int t = 0; t < view.tanks(); t++) {
            JsonObject tank = new JsonObject();
            FluidType type = Fluids.fromVanilla(view.fluid(t));
            tank.addProperty("fluid", BuiltInRegistries.FLUID.getKey(view.fluid(t)).toString());
            tank.addProperty("hbm", type.getRegistryName());
            tank.addProperty("amount", view.amount(t));
            tank.addProperty("capacity", view.capacity(t));
            tanks.add(tank);
        }
        json.add("tanks", tanks);
    }
}
