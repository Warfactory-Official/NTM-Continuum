package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.MobConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.tileentity.bomb.TileEntityLandmine;
import com.hbm.tileentity.deco.TileEntityBobble;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import com.hbm.tileentity.network.TileEntityPipeAnchor;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.EnergyCompat;
import dev.golem.core.world.ItemStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@code hbm_gapsb} harness query of gap batch B (§19ed-gaps-b), read by {@code FinalGapsB}. Server-side values by
 * {@code op}: {@code props} (reputation, mask man timer, FBI/rad marks), {@code set} (reputation, digamma, rads, radMark, fbiMark
 * and the {@code MobConfig} timings of the boss spawns), {@code mobs} (FBI/drones/beasts/leaders/ghosts/mask men within
 * {@code r}), {@code machine} (tank/refinery damage state and fills at {@code x,y,z}), {@code bomblet} (a type-{@code t}
 * bomblet at {@code x,y,z}), {@code advancement} ({@code id} done), {@code anchor} (a pipe anchor's links and network),
 * {@code scan} (1.7 tile data of a built structure: landmines, bobbleheads, pots, skeleton books, exploded tanks, barrel
 * fluids within {@code r} of {@code x,y,z}), {@code radar} (show the map; count map cells over unloaded chunks and compare
 * them with the saved heights), {@code saved} (Core's saved-chunk height read for chunk {@code cx,cz}), {@code damage}
 * (one {@code attackEntityFromNT} on the nearest zombie with a knockback multiplier), {@code config} (the values the
 * {@code /ntmserver} keys drive), {@code energy} (the foreign energy handler at {@code x,y,z}), {@code crate} (the held
 * stack's carried inventory). Inert unless the harness runs.
 */
public final class GapsBHarnessQueries {

    private GapsBHarnessQueries() {}

    private static Entity lastGhost;

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_gapsb", GapsBHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return new JsonObject();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                result.complete(player == null ? new JsonObject() : onServer(player, params));
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

    private static int i(JsonObject p, String k, int d) {
        return p.has(k) ? p.get(k).getAsInt() : d;
    }

    private static JsonObject onServer(ServerPlayer player, JsonObject p) {
        JsonObject json = new JsonObject();
        ServerLevel level = (ServerLevel) player.level();
        String op = p.has("op") ? p.get("op").getAsString() : "props";
        BlockPos pos = new BlockPos(i(p, "x", 0), i(p, "y", 0), i(p, "z", 0));
        EntityHbmProps props = HbmPlayerProps.getData(player);
        switch (op) {
            case "props" -> {
                json.addProperty("reputation", props.reputation);
                json.addProperty("maskManTimer", props.maskManTimer);
                json.addProperty("fbiMark", props.fbiMark);
                json.addProperty("radMark", props.radMark);
                json.addProperty("time", level.getGameTime());
                json.addProperty("rads", HbmLivingProps.getRadiation(player));
                json.addProperty("digamma", HbmLivingProps.getDigamma(player));
                json.addProperty("contagion", HbmLivingProps.getContagion(player));
            }
            case "set" -> {
                if (p.has("reputation")) props.reputation = p.get("reputation").getAsInt();
                if (p.has("digamma")) HbmLivingProps.setDigamma(player, p.get("digamma").getAsDouble());
                if (p.has("rads")) HbmLivingProps.setRadiation(player, p.get("rads").getAsDouble());
                if (p.has("radMark")) props.radMark = p.get("radMark").getAsBoolean();
                if (p.has("fbiMark")) props.fbiMark = p.get("fbiMark").getAsLong();
                if (p.has("maskManTimer")) props.maskManTimer = p.get("maskManTimer").getAsInt();
                if (p.has("maskmanDelay")) MobConfig.maskmanDelay = p.get("maskmanDelay").getAsInt();
                if (p.has("enableRaids")) MobConfig.enableRaids = p.get("enableRaids").getAsBoolean();
                if (p.has("raidDelay")) MobConfig.raidDelay = p.get("raidDelay").getAsInt();
                if (p.has("raidChance")) MobConfig.raidChance = p.get("raidChance").getAsInt();
                if (p.has("raidAttackDistance")) MobConfig.raidAttackDistance = p.get("raidAttackDistance").getAsInt();
                if (p.has("elementalDelay")) MobConfig.elementalDelay = p.get("elementalDelay").getAsInt();
                if (p.has("elementalChance")) MobConfig.elementalChance = p.get("elementalChance").getAsInt();
                json.addProperty("ok", true);
            }
            case "mobs" -> {
                int r = i(p, "r", 128);
                int fbi = 0, drones = 0, beasts = 0, leaders = 0, ghosts = 0, maskmen = 0;
                for (Entity e : level.getAllEntities()) {
                    if (e.distanceTo(player) > r || !e.isAlive()) continue;
                    if (e instanceof com.hbm.entity.mob.EntityFBIDrone) drones++;
                    else if (e instanceof com.hbm.entity.mob.EntityFBI) fbi++;
                    if (e instanceof com.hbm.entity.mob.EntityRADBeast beast) { beasts++; if (beast.isLeader()) leaders++; }
                    if (e instanceof com.hbm.entity.mob.EntityGhost) ghosts++;
                    if (e instanceof com.hbm.entity.mob.EntityMaskMan) maskmen++;
                }
                json.addProperty("fbi", fbi);
                json.addProperty("drones", drones);
                json.addProperty("beasts", beasts);
                json.addProperty("leaders", leaders);
                json.addProperty("ghosts", ghosts);
                json.addProperty("maskmen", maskmen);
                if (p.has("clear") && p.get("clear").getAsBoolean()) {
                    java.util.List<Entity> kill = new java.util.ArrayList<>();
                    for (Entity e : level.getAllEntities()) {
                        if (e instanceof com.hbm.entity.mob.EntityFBI || e instanceof com.hbm.entity.mob.EntityFBIDrone
                                || e instanceof com.hbm.entity.mob.EntityRADBeast || e instanceof com.hbm.entity.mob.EntityGhost
                                || e instanceof com.hbm.entity.mob.EntityMaskMan) kill.add(e);
                    }
                    kill.forEach(Entity::discard);
                }
            }
            case "machine" -> {
                BlockPos core = com.hbm.blocks.multiblock.MultiblockSurface.coreOfAny(level, pos);
                BlockEntity be = core == null ? null : level.getBlockEntity(core);
                json.addProperty("present", be != null);
                if (be instanceof TileEntityMachineFluidTank tank) {
                    json.addProperty("kind", "tank");
                    json.addProperty("exploded", tank.hasExploded);
                    json.addProperty("onFire", tank.onFire);
                    json.addProperty("fill", tank.tank.getFill());
                    json.addProperty("fluid", tank.tank.getTankType().getName());
                    if (p.has("fillWith")) {
                        tank.tank.setTankType(com.hbm.inventory.fluid.Fluids.fromName(p.get("fillWith").getAsString()));
                        tank.tank.setFill(i(p, "amount", 100_000));
                    }
                } else if (be instanceof TileEntityMachineRefinery refinery) {
                    json.addProperty("kind", "refinery");
                    json.addProperty("exploded", refinery.hasExploded);
                    json.addProperty("onFire", refinery.onFire);
                    json.addProperty("fill", refinery.tanks[0].getFill());
                    if (p.has("amount")) refinery.tanks[0].setFill(i(p, "amount", 0));
                }
            }
            case "bomblet" -> {
                EntityBombletZeta bomblet = Entities.createTriggered(ModNtmEntities.BOMBLET_ZETA.get(), level);
                bomblet.setType(i(p, "t", 1));
                bomblet.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                json.addProperty("spawned", level.addFreshEntity(bomblet));
            }
            case "advancement" -> {
                Boolean done = dev.golem.core.world.Advancements.isDone(player, p.get("id").getAsString());
                json.addProperty("done", done != null && done);
            }
            case "anchor" -> {
                if (level.getBlockEntity(pos) instanceof TileEntityPipeAnchor anchor) {
                    JsonArray links = new JsonArray();
                    for (BlockPos l : anchor.getConnected()) links.add(l.getX() + "," + l.getY() + "," + l.getZ());
                    json.add("links", links);
                    json.addProperty("fluid", anchor.getFluidType().getName());
                    var graph = com.hbm.api.fluidmk2.FluidGraph.get(level);
                    var net = graph.networkAt(pos.asLong());
                    json.addProperty("network", net == null ? 0 : System.identityHashCode(net));
                }
            }
            case "scan" -> {
                int r = i(p, "r", 40);
                int mines = 0, primed = 0, bobbles = 0, pots = 0, books = 0, exploded = 0, barrels = 0, typedBarrels = 0;
                JsonArray bobbleTypes = new JsonArray();
                JsonArray skeletons = new JsonArray();
                long sx = 0, sy = 0, sz = 0, found = 0;
                // pots are blocks: read them within 40 of the /place position
                int pr = Math.min(r, 40);
                for (BlockPos q : BlockPos.betweenClosed(new BlockPos(pos.getX() - pr, i(p, "y0", pos.getY() - 20), pos.getZ() - pr),
                        new BlockPos(pos.getX() + pr, i(p, "y1", pos.getY() + 40), pos.getZ() + pr))) {
                    BlockState state = level.getBlockState(q);
                    if (state.getBlock() instanceof net.minecraft.world.level.block.FlowerPotBlock && state.getBlock() != net.minecraft.world.level.block.Blocks.FLOWER_POT) pots++;
                }
                // tiles: every block entity of the loaded chunks within r (the start may land away from the /place position)
                for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
                    for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
                        if (!level.hasChunk(cx, cz)) continue;
                        for (BlockEntity be : List.copyOf(level.getChunk(cx, cz).getBlockEntities().values())) {
                            BlockPos q = be.getBlockPos();
                            if (be instanceof TileEntityLandmine mine) {
                                mines++;
                                if (mine.isPrimed()) primed++;
                                sx += q.getX(); sy += q.getY(); sz += q.getZ(); found++;
                            }
                            if (be instanceof TileEntityBobble bobble) { bobbles++; bobbleTypes.add(bobble.typeOrdinal()); }
                            if (be instanceof com.hbm.blocks.generic.BlockSkeletonHolder.TileEntitySkeletonHolder holder) {
                                skeletons.add(q.getX() + "," + q.getY() + "," + q.getZ() + " "
                                        + BuiltInRegistries.ITEM.getKey(holder.getItem().getItem()));
                                if (!ItemStacks.writableBookPages(holder.getItem()).isEmpty()) books++;
                                if (found == 0) { sx = q.getX(); sy = q.getY(); sz = q.getZ(); }
                            }
                            if (be instanceof TileEntityMachineFluidTank tank && tank.hasExploded) exploded++;
                            if (be instanceof com.hbm.tileentity.machine.storage.TileEntityBarrel barrel) {
                                barrels++;
                                if (barrel.tank.getTankType() != com.hbm.inventory.fluid.Fluids.NONE) typedBarrels++;
                            }
                        }
                    }
                }
                if (found == 0 && skeletons.size() > 0) found = 1;
                if (found > 0) json.addProperty("at", (sx / found) + "," + (sy / found) + "," + (sz / found));
                json.addProperty("mines", mines);
                json.addProperty("primed", primed);
                json.addProperty("bobbles", bobbles);
                json.add("bobbleTypes", bobbleTypes);
                json.addProperty("pots", pots);
                json.addProperty("books", books);
                json.add("skeletons", skeletons);
                json.addProperty("explodedTanks", exploded);
                json.addProperty("barrels", barrels);
                json.addProperty("typedBarrels", typedBarrels);
            }
            case "radar" -> {
                if (!(level.getBlockEntity(pos) instanceof TileEntityMachineRadarNT radar)) {
                    json.addProperty("present", false);
                    break;
                }
                json.addProperty("present", true);
                radar.showMap = true;
                // 1.7 draws nothing on an unpowered radar's screen (the sweep and the saved-chunk fill do not need power)
                if (p.has("power") && p.get("power").getAsBoolean()) radar.setPower(TileEntityMachineRadarNT.maxPower);
                int unloadedCells = 0, filledUnloaded = 0, matching = 0, compared = 0;
                for (int index = 0; index < radar.map.length; index++) {
                    int x = pos.getX() - radar.getRange() + (index % 200) * radar.getRange() * 2 / 200;
                    int z = pos.getZ() - radar.getRange() + index / 200 * radar.getRange() * 2 / 200;
                    if (level.hasChunk(x >> 4, z >> 4)) continue;
                    unloadedCells++;
                    if (radar.map[index] == 0) continue;
                    filledUnloaded++;
                    if (compared < 10) {
                        int[] h;
                        try {
                            h = dev.golem.core.world.Chunks.readSavedHeights(level, x >> 4, z >> 4,
                                    Heightmap.Types.MOTION_BLOCKING).get(1, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            h = null;
                        }
                        if (h != null) {
                            compared++;
                            if ((byte) net.minecraft.util.Mth.clamp(h[(x & 15) + (z & 15) * 16], 50, 128) == radar.map[index]) matching++;
                        }
                    }
                }
                json.addProperty("unloadedCells", unloadedCells);
                json.addProperty("filledUnloaded", filledUnloaded);
                json.addProperty("compared", compared);
                json.addProperty("matching", matching);
            }
            case "saved" -> {
                int cx = i(p, "cx", 0), cz = i(p, "cz", 0);
                json.addProperty("loaded", level.hasChunk(cx, cz));
                try {
                    int[] h = dev.golem.core.world.Chunks.readSavedHeights(level, cx, cz, Heightmap.Types.MOTION_BLOCKING)
                            .get(5, TimeUnit.SECONDS);
                    json.addProperty("saved", h != null);
                    if (h != null) json.addProperty("h0", h[0]);
                } catch (Exception e) {
                    json.addProperty("error", e.toString());
                }
            }
            case "damage" -> {
                net.minecraft.world.entity.Mob zombie = null;
                for (var z : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(16))) {
                    if (z.getType() == net.minecraft.world.entity.EntityType.ZOMBIE) zombie = z;
                }
                if (zombie == null) break;
                zombie.setDeltaMovement(0, 0, 0);
                boolean hit = EntityDamageUtil.attackEntityFromNT(zombie, level.damageSources().playerAttack(player), 2F,
                        true, false, p.has("kb") ? p.get("kb").getAsDouble() : 2.0D, 0F, 0F);
                json.addProperty("hit", hit);
                json.addProperty("vx", zombie.getDeltaMovement().x);
                json.addProperty("vy", zombie.getDeltaMovement().y);
                json.addProperty("vz", zombie.getDeltaMovement().z);
            }
            case "config" -> {
                json.addProperty("hazardTickRate", RadiationConfig.itemHazardDropTickRate);
                json.addProperty("taintTrails", com.hbm.config.ServerConfig.TAINT_TRAILS.get());
                json.addProperty("keepContents", com.hbm.config.ServerConfig.CRATE_KEEP_CONTENTS.get());
                json.addProperty("openHeld", com.hbm.config.ServerConfig.CRATE_OPEN_HELD.get());
                json.addProperty("mku", com.hbm.config.ServerConfig.ENABLE_MKU.get());
                json.addProperty("damageCompat", com.hbm.config.ServerConfig.DAMAGE_COMPATIBILITY_MODE.get());
            }
            case "energy" -> {
                json.addProperty("supported", EnergyCompat.supported());
                json.addProperty("reborn", dev.golem.core.world.RebornEnergy.present());
                boolean any = false;
                for (Direction d : Direction.values()) any |= EnergyCompat.find(level, pos, d) != null;
                json.addProperty("found", any);
            }
            case "crate" -> {
                ItemStack held = player.getMainHandItem();
                json.addProperty("held", BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
                int n = 0;
                JsonArray items = new JsonArray();
                for (ItemStack s : ItemStacks.carriedItems(held, "inventory", 104)) {
                    if (s.isEmpty()) continue;
                    n++;
                    items.add(BuiltInRegistries.ITEM.getKey(s.getItem()) + " x" + s.getCount());
                }
                json.addProperty("count", n);
                json.add("items", items);
                if (player.containerMenu != null) json.addProperty("menu", player.containerMenu.getClass().getSimpleName());
            }
            case "maskman" -> {
                net.minecraft.world.item.Item acidizer = com.hbm.blocks.machine.ModMachineBlocks.machine_crystallizer.get().asItem();
                json.addProperty("crafted", player.getStats().getValue(net.minecraft.stats.Stats.ITEM_CRAFTED.get(acidizer)));
                json.addProperty("used", player.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(acidizer)));
                json.addProperty("rads", HbmLivingProps.getRadiation(player));
                json.addProperty("height", level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ()));
                json.addProperty("y", player.getY());
                json.addProperty("difficulty", level.getDifficulty().toString());
                json.addProperty("conditions", com.hbm.handler.BossSpawnHandler.maskManConditions(level, player));
                json.addProperty("timer", props.maskManTimer);
            }
            case "lantern" -> {
                BlockPos core = com.hbm.blocks.multiblock.MultiblockSurface.coreOfAny(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof com.hbm.tileentity.deco.TileEntityLanternBehemoth lantern) {
                    if (p.has("broken")) {
                        lantern.isBroken = p.get("broken").getAsBoolean();
                        lantern.setChanged();
                        lantern.networkPackNT();
                    }
                    json.addProperty("broken", lantern.isBroken);
                    json.addProperty("comTimer", lantern.comTimer);
                    json.addProperty("core", core.getX() + "," + core.getY() + "," + core.getZ());
                }
            }
            case "shuttle" -> {
                int shuttles = 0;
                boolean alexandrite = false, diamonds = false;
                for (Entity e : level.getAllEntities()) {
                    ItemStack payload = null;
                    if (e instanceof com.hbm.entity.missile.EntityBobmazon shuttle) { shuttles++; payload = shuttle.payload; }
                    if (e instanceof net.minecraft.world.entity.item.ItemEntity item) payload = item.getItem();
                    if (payload == null || payload.isEmpty()) continue;
                    if (!(payload.getItem() instanceof com.hbm.items.special.ItemKitNBT)) continue;
                    for (ItemStack content : com.hbm.items.special.ItemKitNBT.contents(payload)) {
                        if (content.is(com.hbm.items.ModItems.gem_alexandrite.get())) alexandrite = true;
                        if (content.is(net.minecraft.world.item.Items.DIAMOND)) diamonds = true;
                    }
                }
                json.addProperty("shuttles", shuttles);
                json.addProperty("alexandrite", alexandrite);
                json.addProperty("diamonds", diamonds);
            }
            case "pedestal" -> {
                // the 1.7 aberrator EOTT ritual (GOOD_KARMA): eight secrets around sixteen saturnite mechanisms
                int[][] ring = {{-2, -2}, {0, -3}, {2, -2}, {-3, 0}, {3, 0}, {-2, 2}, {0, 3}, {2, 2}};
                if (p.has("fill") && p.get("fill").getAsBoolean()) {
                    net.minecraft.world.item.Item secret = BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.of("hbm", "item_secret_aberrator"));
                    for (int[] r : ring) {
                        if (level.getBlockEntity(pos.offset(r[0], 0, r[1])) instanceof com.hbm.blocks.generic.BlockPedestal.TileEntityPedestal ped) {
                            ped.setItem(new ItemStack(secret));
                        }
                    }
                    net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag = net.minecraft.tags.TagKey.create(
                            net.minecraft.core.registries.Registries.ITEM, dev.golem.core.Ids.of("c", "mechanisms/saturnite"));
                    for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
                        if (new ItemStack(item).is(tag)) {
                            if (level.getBlockEntity(pos) instanceof com.hbm.blocks.generic.BlockPedestal.TileEntityPedestal center) {
                                center.setItem(new ItemStack(item, 16));
                            }
                            break;
                        }
                    }
                }
                if (level.getBlockEntity(pos) instanceof com.hbm.blocks.generic.BlockPedestal.TileEntityPedestal center) {
                    json.addProperty("center", BuiltInRegistries.ITEM.getKey(center.getItem().getItem()) + " x" + center.getItem().getCount());
                }
            }
            case "ghostProbe" -> {
                // why no ghost: the ring 75 blocks out (loaded? height?) and one direct roll of BossSpawnHandler.ghost
                json.addProperty("digamma", HbmLivingProps.getDigamma(player));
                json.addProperty("pos", player.getBlockX() + "," + player.getBlockY() + "," + player.getBlockZ());
                JsonArray ring = new JsonArray();
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI * 2 * k / 8;
                    int gx = (int) (player.getX() + 75 * Math.cos(a)), gz = (int) (player.getZ() + 75 * Math.sin(a));
                    boolean loaded = level.hasChunk(gx >> 4, gz >> 4);
                    ring.add(gx + "," + gz + ":" + (loaded ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz) : "unloaded"));
                }
                json.add("ring", ring);
                int before = 0, after = 0;
                for (Entity e : level.getAllEntities()) if (e instanceof com.hbm.entity.mob.EntityGhost) before++;
                com.hbm.handler.BossSpawnHandler.ghost(level, player);
                for (Entity e : level.getAllEntities()) if (e instanceof com.hbm.entity.mob.EntityGhost g) { after++; lastGhost = g; }
                if (lastGhost != null) json.addProperty("ghostAt", lastGhost.blockPosition().toShortString());
                json.addProperty("ghostsBefore", before);
                json.addProperty("ghostsAfter", after);
            }
            case "ghostCheck" -> {
                if (lastGhost != null) {
                    json.addProperty("alive", lastGhost.isAlive());
                    json.addProperty("removal", String.valueOf(lastGhost.getRemovalReason()));
                    json.addProperty("at", lastGhost.blockPosition().toShortString());
                }
            }
            case "anchorFluid" -> {
                if (level.getBlockEntity(pos) instanceof TileEntityPipeAnchor anchor) {
                    anchor.setFluidType(com.hbm.inventory.fluid.Fluids.fromName(p.get("fluid").getAsString()));
                    json.addProperty("fluid", anchor.getFluidType().getName());
                }
            }
            default -> json.addProperty("error", "unknown op " + op);
        }
        return json;
    }
}
