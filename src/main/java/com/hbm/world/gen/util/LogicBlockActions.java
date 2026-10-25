package com.hbm.world.gen.util;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.blocks.generic.BlockSkeletonHolder;
import com.hbm.blocks.generic.LogicBlock.TileEntityLogicBlock;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.item.EntityFallingBlockNT;
import com.hbm.entity.missile.EntityMissileTier2;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.entity.mob.ai.EntityAIFireGun;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.special.ItemClayTablet;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.tileentity.bomb.TileEntityCharge;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.MobUtil;
import dev.golem.core.Ids;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Spawns;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.7 {@code LogicBlockActions}: what a logic block does each tick, by name. The phase and timer of the block
 * entity drive them; most act once when the phase first reaches 1 and then remove or replace the block.
 *
 * <p>Deviations, each forced by what the port has: 1.7's {@code DEAD_GUY_TOWER_BASE} was never registered and is
 * not ported; the fodder step used {@code getHeightValue}, read here as the motion-blocking height map; 1.7 gave
 * {@code MobUtil} a fresh {@code Random} per call, the port uses the level's.
 */
public class LogicBlockActions {

    public static LinkedHashMap<String, Consumer<TileEntityLogicBlock>> actions;

    /** 1.7 {@code Vec3NT.rotateAroundYDeg}: the horizontal part of a vector turned by {@code deg} about Y. */
    public static double[] rotateAroundYDeg(double x, double z, double deg) {
        double rad = Math.toRadians(deg);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        return new double[]{x * cos + z * sin, z * cos - x * sin};
    }

    /** A stack of an item by registry id, or empty if the port does not register it. */
    public static ItemStack stack(String id, int count) {
        Identifier key = Ids.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.getValue(key), count);
    }

    static ItemStack clayTablet(int variant) {
        return ItemClayTablet.of(variant);
    }

    private static int heightValue(Level world, int x, int z) {
        return world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    private static void chat(Player player, String tag, String message) {
        if (player == null) return;
        player.sendSystemMessage(Component.literal(tag).withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(message).withStyle(ChatFormatting.RESET)));
    }

    public static Consumer<TileEntityLogicBlock> PHASE_ABERRATOR = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (tile.phase == 1 || tile.phase == 2) {
            tile.player = world.getNearestPlayer(x, y, z, 25, false);
            if (tile.timer == 0) {
                double vx = 20, vz = 0;
                for (int i = 0; i < 10; i++) {
                    if (vx > 8) vx += world.getRandom().nextInt(10) - 5;

                    EntityUndeadSoldier mob = Entities.createTriggered(ModNtmEntities.UNDEAD_SOLDIER.get(), world);
                    for (int j = 0; j < 7 && mob != null; j++) {
                        mob.snapTo(x + 0.5 + vx, heightValue(world, (int) (x + 0.5 + vx), (int) (z + 0.5 + vz)),
                                z + 0.5 + vz, i * 36F, 0);
                        if (mob.canSpawnAtCurrentPosition()) {
                            Spawns.finalizeTriggered(mob, world);
                            if (tile.player != null) mob.setTarget(tile.player);
                            world.addFreshEntity(mob);
                            break;
                        }
                    }
                    double[] turned = rotateAroundYDeg(vx, vz, 36D);
                    vx = turned[0];
                    vz = turned[1];
                }
            }
        }
        if (tile.phase > 2) {
            BlockEntity te = world.getBlockEntity(pos.above(18));
            if (te instanceof BlockSkeletonHolder.TileEntitySkeletonHolder skeleton) {
                skeleton.setItem(world.getRandom().nextInt(5) == 0 ? stack("hbm:item_secret_aberrator", 1) : clayTablet(1));
            }
            world.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
        }
    };

    private static void collapseRoof(TileEntityLogicBlock tile, int r) {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (tile.phase == 0) return;

        // from explosionChaos because i cannot be assed
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + pos.getX();
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + pos.getY();
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + pos.getZ();
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        BlockPos at = new BlockPos(X, Y, Z);
                        BlockState state = world.getBlockState(at);
                        if (state.getBlock().getExplosionResistance() <= 70) {
                            // 1.7 spawned the falling copy and left the block itself where it was
                            EntityFallingBlockNT.fall(world, at, state);
                        }
                    }
                }
            }
        }
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    public static Consumer<TileEntityLogicBlock> COLLAPSE_ROOF_RAD_5 = (tile) -> collapseRoof(tile, 4);
    public static Consumer<TileEntityLogicBlock> COLLAPSE_ROOF_RAD_10 = (tile) -> collapseRoof(tile, 8);

    public static Consumer<TileEntityLogicBlock> FODDER_WAVE = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        int x = pos.getX(), z = pos.getZ();
        if (tile.phase == 1) {
            double vx = 5, vz = 0;
            for (int i = 0; i < 10; i++) {
                Zombie mob = Entities.createTriggered(EntityType.ZOMBIE, world);
                if (mob != null) {
                    mob.snapTo(x + 0.5 + vx, heightValue(world, x, z), z + 0.5 + vz, i * 36F, 0);
                    MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolAdv, world.getRandom());
                    world.addFreshEntity(mob);
                }
                double[] turned = rotateAroundYDeg(vx, vz, 36D);
                vx = turned[0];
                vz = turned[1];
            }
            world.setBlockAndUpdate(pos, ModBlocks.block_steel.get().defaultBlockState());
        }
    };

    private static void mobs(TileEntityLogicBlock tile, EntityType<? extends Mob> type, Consumer<Mob> dress) {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (tile.phase == 1) {
            for (int i = 0; i < 3; i++) {
                Mob mob = Entities.createTriggered(type, world);
                if (mob != null) {
                    mob.snapTo(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
                    dress.accept(mob);
                    world.addFreshEntity(mob);
                }
                world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static EntityAIFireGun gunTask(Mob mob, double maxRange, float inaccuracy) {
        EntityAIFireGun gunTask = new EntityAIFireGun(mob);
        gunTask.minWait = 4;
        gunTask.maxWait = 5;
        gunTask.maxRange = maxRange;
        gunTask.burstTime = 6;
        gunTask.inaccuracy = inaccuracy;
        gunTask.randomBurst = false;
        return gunTask;
    }

    public static Consumer<TileEntityLogicBlock> SKELETONS_GUN_TIER_1 = (tile) -> mobs(tile, EntityType.SKELETON, mob -> {
        RandomSource rand = mob.getRandom();
        MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolGunsTier1, rand);
        MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolMasks, rand);
        MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolRanged, rand);
    });

    public static Consumer<TileEntityLogicBlock> SKELETONS_GUN_TIER_2 = (tile) -> mobs(tile, EntityType.SKELETON, mob -> {
        MobUtil.addFireTask(mob, gunTask(mob, 50, 5F));
        MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolGunsTier2, mob.getRandom());
        MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolRanged, mob.getRandom());
    });

    public static Consumer<TileEntityLogicBlock> SKELETONS_GUN_TIER_3 = (tile) -> mobs(tile, EntityType.SKELETON, mob -> {
        MobUtil.addFireTask(mob, gunTask(mob, 100, 1F));
        MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolGunsTier3, mob.getRandom());
        MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolAdvRanged, mob.getRandom());
    });

    public static Consumer<TileEntityLogicBlock> ZOMBIES_TIER_1 = (tile) -> mobs(tile, EntityType.ZOMBIE,
            mob -> MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolCommon, mob.getRandom()));

    public static Consumer<TileEntityLogicBlock> ZOMBIES_TIER_2 = (tile) -> mobs(tile, EntityType.ZOMBIE,
            mob -> MobUtil.assignItemsToEntity(mob, MobUtil.slotPoolAdv, mob.getRandom()));

    public static Consumer<TileEntityLogicBlock> PUZZLE_TEST = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();

        if (tile.phase == 2) {
            world.setBlockAndUpdate(pos, ModStorageBlocks.crate_steel.get().defaultBlockState());

            LightningBolt blitz = Entities.createTriggered(EntityType.LIGHTNING_BOLT, world);
            if (blitz != null) {
                blitz.snapTo(pos.getX(), heightValue(world, pos.getX(), pos.getZ()) + 2, pos.getZ(), 0, 0);
                world.addFreshEntity(blitz);
            }

            if (world.getBlockEntity(pos) instanceof Container crate) crate.setItem(15, new ItemStack(ModGunItems.gun_bolter.get()));
        }
    };

    public static Consumer<TileEntityLogicBlock> MISSILE_STRIKE = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (tile.phase != 1) return;

        chat(world.getNearestPlayer(x, y, z, 25, false), "[COMMAND UNIT]", " Missile Fired");

        ForgeDirection dir = tile.direction;
        ForgeDirection parallel = dir.getRotation(ForgeDirection.UP);

        EntityMissileTier2.EntityMissileStrong missile = new EntityMissileTier2.EntityMissileStrong(world,
                x + dir.offsetX * 300, 200, z + dir.offsetZ * 300,
                x + parallel.offsetX * 30 + dir.offsetX * 30,
                z + parallel.offsetZ * 30 + dir.offsetZ * 30);
        world.addFreshEntity(missile); // 1.7: WorldUtil.loadAndSpawnEntityInWorld

        world.setBlockAndUpdate(pos, ModFallingBlocks.block_electrical_scrap.get().defaultBlockState());
    };

    public static Consumer<TileEntityLogicBlock> RAD_CONTAINMENT_SYSTEM = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        ForgeDirection direction = tile.direction.getOpposite();
        ForgeDirection rot = direction.getRotation(ForgeDirection.UP);

        // 1.7 (x - rot, y - 1, z - rot, x + rot + dir * 15, y + 1, z + rot + dir * 15).expand(2, 2, 2), corners as given
        double minX = x - rot.offsetX, maxX = x + rot.offsetX + direction.offsetX * 15;
        double minZ = z - rot.offsetZ, maxZ = z + rot.offsetZ + direction.offsetZ * 15;
        AABB bb = new AABB(Math.min(minX, maxX) - 2, y - 3, Math.min(minZ, maxZ) - 2,
                Math.max(minX, maxX) + 2, y + 3, Math.max(minZ, maxZ) + 2);

        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, bb)) {
            Vec3 vec = new Vec3(e.getX() - (x + 0.5), (e.getY() + e.getEyeHeight()) - (y + 0.5), e.getZ() - (z + 0.5));
            double len = vec.length();
            vec = vec.normalize();
            len = Math.max(len, 1D);

            float res = 0;
            for (int i = 1; i < len; i++) {
                int ix = (int) Math.floor(x + 0.5 + vec.x * i);
                int iy = (int) Math.floor(y + 0.5 + vec.y * i);
                int iz = (int) Math.floor(z + 0.5 + vec.z * i);
                res += world.getBlockState(new BlockPos(ix, iy, iz)).getBlock().getExplosionResistance();
            }
            if (res < 1) res = 1;

            float eRads = 100F;
            eRads /= res;
            eRads /= (float) (len * len);

            ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.HAZMAT2, eRads);
        }

        if (tile.phase == 2 && tile.timer > 40) {
            chat(world.getNearestPlayer(x, y, z, 25, false), "[RAD CONTAINMENT SYSTEM]",
                    " Diagnostics found containment failure, commencing lockdown");

            for (int i = 1; i < 20; i++) {
                BlockPos check = new BlockPos(x + direction.offsetX * i, y + 1, z + direction.offsetZ * i);
                BlockEntity te = MultiblockSurface.coreEntity(world, check);
                if (te instanceof TileEntityDoorGeneric door) {
                    door.setPins(456);
                    door.close();
                    door.lock();
                    break;
                }
            }

            tile.phase = 3;
        }
    };

    public static Consumer<TileEntityLogicBlock> POWER_LOCK = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();

        if (tile.phase == 0 && LogicBlockConditions.playerIn(world, LogicBlockConditions.cube(pos, 3))) {
            chat(world.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 300, false), "[POWER LOCK]",
                    " Low Power Warning! Locking Safe");
            tile.phase++;

            for (Direction dir : Direction.values()) {
                if (world.getBlockEntity(pos.relative(dir)) instanceof TileEntityLockableBase safe) {
                    safe.setPins(world.getRandom().nextInt(999));
                    break;
                }
            }
        }
    };

    public static Consumer<TileEntityLogicBlock> BOMB_TRAP = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        ForgeDirection direction = tile.direction.getOpposite();

        if (tile.phase == 1) {
            // 1.7 placed it only along z, and faced it north (meta 2), whatever the direction
            BlockPos at = pos.offset(0, 0, direction.offsetZ);
            world.setBlock(at, ModExplosivesBlocks.charge_c4.get().defaultBlockState()
                    .setValue(BlockChargeBase.FACING, Direction.NORTH), 3);
            if (world.getBlockEntity(at) instanceof TileEntityCharge bomb) {
                bomb.timer = 2400;
                bomb.started = true;
            }
            world.setBlockAndUpdate(pos, tile.disguise != null ? tile.disguise : Blocks.AIR.defaultBlockState());
        }
    };

    public static Consumer<TileEntityLogicBlock> BOMB_CRANE = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();

        if (tile.phase == 0) {
            world.setBlock(pos.above(), ModExplosivesBlocks.charge_c4.get().defaultBlockState()
                    .setValue(BlockChargeBase.FACING, Direction.UP), 3);
            if (world.getBlockEntity(pos.above()) instanceof TileEntityCharge bomb) bomb.timer = 1200;
        }

        if (tile.phase >= 1) {
            if (world.getBlockEntity(pos.above()) instanceof TileEntityCharge bomb) bomb.started = true;
            world.setBlockAndUpdate(pos, ModBlocks.block_steel.get().defaultBlockState());
        }
    };

    public static Consumer<TileEntityLogicBlock> DEAD_GUY_CRANE = (tile) -> {
        ServerLevel world = (ServerLevel) tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (tile.phase == 1) {
            world.setBlockAndUpdate(pos, ModDungeonBlocks.skeleton_holder.get().defaultBlockState());
            List<Player> players = world.getEntitiesOfClass(Player.class, LogicBlockConditions.cube(pos, 25));
            Player player = players.isEmpty() ? null : players.get(0);

            if (world.getBlockEntity(pos) instanceof BlockSkeletonHolder.TileEntitySkeletonHolder skeleton) {
                if (player != null && hasItem(player, ModGunItems.gun_hangman.get())) {
                    skeleton.setItem(clayTablet(0));
                } else {
                    skeleton.setItem(new ItemStack(ModGunItems.gun_hangman.get()));
                }
            }
        }
    };

    /** 1.7 {@code InventoryPlayer.hasItem}: any slot holding the item, whatever its data. */
    private static boolean hasItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) return true;
        }
        return false;
    }

    public static List<String> getActionNames() {
        return new ArrayList<>(actions.keySet());
    }

    // register new actions here
    static {
        initialize();
    }

    public static void initialize() {
        actions = new LinkedHashMap<>();
        // logic actions
        actions.put("FODDER_WAVE", FODDER_WAVE);
        actions.put("POWER_LOCK", POWER_LOCK);
        actions.put("COLLAPSE_ROOF_RAD_5", COLLAPSE_ROOF_RAD_5);
        actions.put("COLLAPSE_ROOF_RAD_10", COLLAPSE_ROOF_RAD_10);
        actions.put("BOMB_TRAP", BOMB_TRAP);
        actions.put("BOMB_CRANE", BOMB_CRANE);
        actions.put("DEAD_GUY_CRANE", DEAD_GUY_CRANE);

        // Mob Block Actions
        actions.put("SKELETON_GUN_TIER_1", SKELETONS_GUN_TIER_1);
        actions.put("SKELETON_GUN_TIER_2", SKELETONS_GUN_TIER_2);
        actions.put("SKELETON_GUN_TIER_3", SKELETONS_GUN_TIER_3);

        actions.put("ZOMBIE_TIER_1", ZOMBIES_TIER_1);
        actions.put("ZOMBIE_TIER_2", ZOMBIES_TIER_2);

        // example actions
        actions.put("ABERRATOR", PHASE_ABERRATOR);
        actions.put("PUZZLE_TEST", PUZZLE_TEST);
        actions.put("MISSILE_STRIKE", MISSILE_STRIKE);
        actions.put("IRRADIATE_ENTITIES_AOE", RAD_CONTAINMENT_SYSTEM);
    }
}
