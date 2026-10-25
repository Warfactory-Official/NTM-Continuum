package com.hbm.commands;

import com.hbm.Constants;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.entity.train.EntityRailCarCargo;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModItems;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * Development-only server half of the entity render smoke: spawns one of every ported entity that can stand
 * still in a grid in front of the player, frozen (no AI, invulnerable, no gravity) so the client can look at
 * the roster and screenshot it. Entities that detonate, fly off or eat the world are left out and listed
 * on the omission list below.
 *
 * <p>Started from the client by {@code EntityRenderSmokeClient} through {@code /hbm entity_smoke}.
 */
public final class EntityRenderSmoke {

    private EntityRenderSmoke() {}

    /** Ported entity ids the smoke spawns, in grid order. */
    public static final List<String> ROSTER = List.of(
            // mobs
            "entity_undead_soldier", "entity_fbi", "entity_ghost", "entity_test_dummy",
            "entity_mob_gold_creeper", "entity_mob_volatile_creeper", "entity_mob_phosgene_creeper",
            "entity_mob_tainted_creeper", "entity_mob_nuclear_creeper",
            "entity_cyber_crab", "entity_tesla_crab", "entity_taint_crab", "entity_taintcrawler",
            "entity_fucc_a_ducc", "entity_plastic_bag", "entity_pigeon",
            "entity_parasite_maggot", "entity_radiation_blaze",
            "entity_glyphid", "entity_glyphid_brawler", "entity_glyphid_behemoth", "entity_glyphid_brenda",
            "entity_glyphid_bombardier", "entity_glyphid_blaster", "entity_glyphid_scout", "entity_glyphid_digger",
            "entity_glyphid_nuclear",
            "entity_fbi_drone", "entity_mob_mask_man", "entity_ufo", "entity_balls_o_tron", "entity_balls_o_tron_seg",
            "entity_mob_hunter_chopper", "entity_siege_craft", "entity_siege_tunneler",
            // vehicles and props
            "entity_cart_ore", "entity_cart_powder", "entity_cart_semtex", "entity_cart_crate", "entity_cart_destroyer",
            "entity_minecart_test", "entity_rubber_boat", "entity_cargo_tram", "entity_cargo_tram_trailer",
            "boxcar", "torpedo", "duchessgambit", "falling_building",
            "entity_bobmazon_delivery", "entity_miner_lander", "entity_soyuz", "entity_soyuz_capsule",
            "parachute_crate", "entity_delivery_drone", "entity_request_drone", "entity_c_item", "entity_c_package",
            "entity_falling_block",
            // projectiles and effects that hold still long enough to see
            "entity_meteor", "entity_stray_cog", "entity_firework_ball", "entity_bullet", "entity_chopper_mine",
            "entity_acid_bomb", "entity_zeta", "disperser_canister", "grenade_bouncy_generic",
            "entity_agent_orange", "entity_chlorine_fx", "entity_cloud_fx", "entity_pink_cloud_fx",
            "cloud_fleija", "cloud_solinium", "cloud_rainbow", "entity_emp_blast",
            "missile_test", "missile_cluster", "missile_rain", "missile_volcano", "entity_missile_shuttle",
            "entity_custom_missile", "c130", "entity_bomber", "entity_falling_bomb", "entity_burning_foeq",
            "entity_siege_laser", "waste_item", "item_buoyant", "tnt",
            // the 25x duck goes last so it sits behind everything else
            "entity_elder_one");

    /**
     * Not spawned: nuke_mk3, entity_balefire, entity_tom_bust, entity_laser_blast,
     * entity_moonstone_blast, entity_tom_the_moonstone, entity_digamma_quasar (destroy the fixture);
     * grenade_universal (needs a grenade stack); entity_falling_multiblock (needs a structure);
     * missile_schrabidium/bhole/emp/cluster_strong/drill (identical bodies to listed ones); entity_waste_pearl,
     * entity_bounding_dummy, entity_seat_dummy, entity_waypoint (invisible by design).
     */
    public static final int GRID_COLUMNS = 8;
    public static final double SPACING_X = 5D;
    public static final double SPACING_Z = 6D;

    /**
     * The roster this run spawns: {@code HBM_ENTITY_SMOKE_ROSTER} (comma-separated ids) narrows it to a
     * hand-picked subset for a close look; otherwise the whole {@link #ROSTER}.
     */
    public static List<String> roster() {
        String subset = System.getenv("HBM_ENTITY_SMOKE_ROSTER");
        if (subset == null || subset.isBlank()) return ROSTER;
        List<String> ids = new ArrayList<>();
        for (String id : subset.split(",")) if (!id.isBlank()) ids.add(id.trim());
        return ids;
    }

    public static boolean hostile() {
        return System.getenv("HBM_ENTITY_SMOKE_AI") != null;
    }

    /** A subset roster is a close-up: fewer columns, tighter spacing. */
    public static boolean closeUp() {
        return roster() != ROSTER;
    }

    public static int columns() {
        return closeUp() ? 4 : GRID_COLUMNS;
    }

    public static double spacingX() {
        return closeUp() ? 3D : SPACING_X;
    }

    public static double spacingZ() {
        return closeUp() ? 3D : SPACING_Z;
    }

    /** Spawns the roster in front of {@code player} and reports what was placed. */
    public static int spawn(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        // HBM_ENTITY_SMOKE_AI=1 leaves the mobs' AI on and the player in survival, so the bosses actually attack.
        player.setGameMode(hostile() ? GameType.SURVIVAL : GameType.CREATIVE);
        // Far from wherever the save left the player, on a stone platform high above whatever terrain is
        // there, so every node's fixture is the same flat stage and nothing an earlier run left behind (or
        // detonated) is in the frame; the runner restores the save afterwards.
        int fx = player.blockPosition().getX() + 4000;
        int fz = player.blockPosition().getZ() + 4000;
        int fy = PLATFORM_Y;
        buildPlatform(level, fx, fz);
        player.teleportTo(fx + 0.5D, fy + 1D, fz + 0.5D);
        BlockPos origin = new BlockPos(fx, fy + 1, fz);
        int placed = 0;
        List<String> failed = new ArrayList<>();
        List<String> roster = roster();
        int columns = columns();
        for (int i = 0; i < roster.size(); i++) {
            String id = roster.get(i);
            int column = i % columns;
            int row = i / columns;
            double x = origin.getX() + (column - (columns - 1) / 2D) * spacingX() + 0.5D;
            double z = origin.getZ() + (closeUp() ? 4D : 8D) + row * spacingZ() + 0.5D;
            int groundY = PLATFORM_Y;
            Entity entity = place(level, id, x, groundY + 1D, z);
            if (entity == null) {
                failed.add(id);
                continue;
            }
            placed++;
            Constants.LOG.info("[HBM] Entity smoke placed {} as {} at {}, {}, {}", id,
                    entity.getClass().getSimpleName(), (int) x, groundY + 1, (int) z);
        }
        if (!failed.isEmpty()) Constants.LOG.error("[HBM] Entity smoke could not place: {}", String.join(", ", failed));
        Constants.LOG.info("[HBM] Entity smoke placed {} of {} roster entities", placed, roster.size());
        return placed;
    }

    /** The stage's top surface; high enough to clear any overworld terrain. */
    public static final int PLATFORM_Y = 230;

    /** A stone slab under the whole grid, generating the chunks it spans first. */
    private static void buildPlatform(ServerLevel level, int fx, int fz) {
        int rows = (roster().size() + columns() - 1) / columns();
        int halfWidth = (int) Math.ceil(columns() * spacingX() / 2D) + 6;
        int depth = (int) Math.ceil(rows * spacingZ()) + 14;
        for (int x = fx - halfWidth; x <= fx + halfWidth; x++) {
            for (int z = fz - 8; z <= fz + depth; z++) {
                level.getChunk(x >> 4, z >> 4);
                level.setBlock(new BlockPos(x, PLATFORM_Y, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                for (int y = PLATFORM_Y + 1; y <= PLATFORM_Y + 12; y++) {
                    BlockPos above = new BlockPos(x, y, z);
                    if (!level.getBlockState(above).isAir()) level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static @Nullable Entity place(ServerLevel level, String id, double x, double y, double z) {
        EntityType<?> type = Entities.byId(Constants.MOD_ID + ":" + id);
        if (type == null || !EntityType.getKey(type).getPath().equals(id)) {
            Constants.LOG.error("[HBM] Entity smoke: no entity type {}", id);
            return null;
        }
        try {
            Entity entity;
            if ("entity_falling_block".equals(id)) {
                // Dropped from 6 blocks up: in the air for the early capture, then lands as a block.
                BlockPos pos = BlockPos.containing(x, y + 6, z);
                return EntityFallingBlockNT.fall(level, pos, Blocks.BRICKS.defaultBlockState());
            }
            entity = Entities.createTriggered(type, level);
            if (entity == null) return null;
            Compat.setInitialPos(entity, x, y, z);
            entity.setYRot(180F);
            entity.yRotO = 180F;
            entity.setNoGravity(true);
            entity.setInvulnerable(true);
            if (entity instanceof Mob mob) {
                if (!hostile()) mob.setNoAi(true);
                mob.setPersistenceRequired();
                mob.yBodyRot = 180F;
                mob.yHeadRot = 180F;
            }
            if (entity instanceof LivingEntity living) living.setHealth(living.getMaxHealth());
            if (entity instanceof EntityMovingItem item) item.setItemStack(new ItemStack(ModItems.plate_iron.get()));
            if (entity instanceof ItemEntity item) {
                item.setItem(new ItemStack(ModItems.plate_iron.get()));
                item.setNeverPickUp();
            }
            if (entity instanceof PrimedTnt tnt) tnt.setFuse(20_000);
            if (entity instanceof EntityRailCarCargo cargo) {
                // Ten filled slots: the trailer's second crate pile.
                for (int slot = 0; slot < Math.min(10, cargo.getContainerSize()); slot++) {
                    cargo.setItem(slot, new ItemStack(ModStorageBlocks.crate_steel.get()));
                }
            }
            if (entity instanceof EntityDisperserCanister canister) {
                canister.setDisperserItem(ModEntityItems.disperser_canister.get());
                canister.setFluid(Fluids.NITAN);
            }
            if (!level.addFreshEntity(entity)) return null;
            return entity;
        } catch (RuntimeException e) {
            Constants.LOG.error("[HBM] Entity smoke: {} failed to spawn", id, e);
            return null;
        }
    }

    /** The world's ported types, so the roster can be checked against what is registered. */
    public static int registeredCount() {
        int count = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (EntityType.getKey(type).getNamespace().equals(Constants.MOD_ID)) count++;
        }
        return count;
    }

    static {
        // Touch the registrar so the roster's handles are live before any lookup.
        ModNtmEntities.class.getName();
    }
}
