package com.hbm.handler;

import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.blocks.generic.BlockPedestal.PedestalEntry;
import com.hbm.blocks.generic.BlockPedestal.PedestalEntryType;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.MobConfig;
import com.hbm.config.WorldConfig;
import com.hbm.entity.EntityHbmProps;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.extprop.HbmPlayerProps;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Spawns;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.items.ModArmorModItems;
import dev.golem.core.platform.Lifecycle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * 1.7 {@code handler/BossSpawnHandler}: the timed world events, rolled on every world tick - meteor strikes and
 * showers, the mask man, FBI raids, radiation beasts and digamma ghosts.
 *
 * <p>Each tick, per level, one roll of {@code meteorRand.nextInt(n) == 0} with {@code n = meteorStrikeChance} (a meteor
 * every 5 hours on average) or, while a shower lasts, {@code meteorShowerChance} (every 15 minutes). A hit picks a random
 * player of that level; if they are in the overworld, their helmet mods and every charm pedestal within 100 blocks are
 * asked: a Meteor Charm cancels the strike, a Charm of Protection diverts it ({@code safe}: it falls away from the player
 * and destroys no blocks). The meteor spawns at y 384, up to 100 blocks off, and falls at 2.5 blocks a tick. A shower
 * starts with {@code 1 / (meteorStrikeChance * 100)} a tick and lasts 75..100 % of {@code meteorShowerDuration}.
 *
 * <p>Kept from 1.7 on purpose: the roll and the shower counter run once per <i>level</i> per tick, and the shower
 * counter is one static shared by all levels, so with three levels loaded a shower runs out three times as fast and
 * starts three times as often; a roll in a level whose players are not in the overworld does nothing. The Random is its
 * own ("because some dimwit keeps resetting the world rand").
 *
 * <p>The other branches of the same 1.7 method, at 1.7's {@code MobConfig} defaults (the {@code 12.M/F/E} config
 * keys, see {@link MobConfig}):
 * <ul>
 *   <li><b>Mask man</b> ({@code enableMaskman}): every 20 ticks, surface levels, not peaceful: a player who has
 *   crafted or placed a crystallizer (1.7's ore acidizer stat; {@code enableStatReRegistering} was on by default),
 *   has at least {@code maskmanMinRad} (50) RAD and is more than three blocks below the height map counts one
 *   second; at {@code maskmanDelay - 60} a red warning, at {@code maskmanDelay} (1200 s) the mask man spawns
 *   gaussian-20 around them on the height map; otherwise the count resets.</li>
 *   <li><b>FBI raids</b> ({@code enableRaids}, off in 1.7): every {@code raidDelay} ticks a 1 in {@code raidChance}
 *   roll picks a player of the level whose {@code fbiMark} lies in the past; {@code raidAmount} agents and
 *   {@code raidDrones} drones (10 higher) come from {@code raidAttackDistance} away. 1.7 odd thing kept:
 *   {@link #markFBI} (using a reactor, RBMK, missile assembly, radiolysis or Soyuz console) pushes the mark 20
 *   minutes <i>ahead</i>, so an active operator is the one player a raid skips.</li>
 *   <li><b>Radiation beasts</b> ({@code enableElementals}): every {@code elementalDelay} ticks a 1 in
 *   {@code elementalChance} roll picks a player; if a reactor meltdown marked them ({@code radMark}) the mark is
 *   cleared and {@code elementalAmount} beasts (the first a leader) spawn {@code raidAttackDistance} away (1.7 used
 *   the raid distance, not {@code elementalDistance}).</li>
 *   <li><b>Ghosts</b>: every 20 ticks a 1 in 5 roll picks a player; with any digamma a ghost spawns 75 blocks away.</li>
 * </ul>
 * 1.7's {@code world.rand} is the level's random here; {@code getHeightValue} is the {@code MOTION_BLOCKING} height and,
 * as in 1.7, 0 for a column whose chunk is not loaded. 1.7 asked Forge's {@code canEntitySpawn} event, whose default
 * allows the spawn without the mob's own spawn rules; the port spawns directly and runs the triggered
 * {@code finalizeSpawn} (1.7 {@code onSpawnWithEgg}).
 */
public final class BossSpawnHandler {

    private BossSpawnHandler() {}

    // because some dimwit keeps resetting the world rand
    private static final Random meteorRand = new Random();

    /** Ticks left of the running meteor shower; 0 when none is running. */
    public static int meteorShower = 0;

    private static boolean registered;

    /** Hooks the per-level roll into the server tick. */
    public static void init() {
        if (registered) return;
        registered = true;
        Lifecycle.onServerTick(BossSpawnHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) rollTheDice(level);
    }

    /** 1.7 {@code rollTheDice(world)}. */
    public static void rollTheDice(ServerLevel level) {
        long time = level.getGameTime();
        // 1.7 provider.isSurfaceWorld(): the overworld, unless an addon names more surface worlds (hbm-space's planets
        // are WorldProviderSurface subclasses) through com.hbm.api.environment.Environment.SURFACE
        boolean surface = com.hbm.api.environment.Environment.isSurface(level);
        RandomSource rand = level.getRandom();

        /*
         * Spawns every 20 minutes if
         * - the player is 3 blocks below the surface
         * - the player has at least 50 RAD
         * - the player has either crafted or placed an ore acidizer before
         */
        if (MobConfig.enableMaskman && time % 20 == 0 && surface && level.getDifficulty() != Difficulty.PEACEFUL) {
            for (ServerPlayer player : List.copyOf(level.players())) {
                if (maskManConditions(level, player)) {
                    EntityHbmProps data = HbmPlayerProps.getData(player);
                    data.maskManTimer++;

                    if (data.maskManTimer == MobConfig.maskmanDelay - 60) {
                        player.sendSystemMessage(Component.literal("The mask man draws near.").withStyle(ChatFormatting.RED));
                    }

                    if (data.maskManTimer >= MobConfig.maskmanDelay) {
                        data.maskManTimer = 0;
                        double spawnX = player.getX() + rand.nextGaussian() * 20;
                        double spawnZ = player.getZ() + rand.nextGaussian() * 20;
                        double spawnY = heightValue(level, spawnX, spawnZ);
                        if (trySpawn(level, (float) spawnX, (float) spawnY, (float) spawnZ, ModNtmEntities.MASK_MAN.get())) {
                            player.sendSystemMessage(Component.literal("The mask man is about to claim another victim.").withStyle(ChatFormatting.RED));
                        } else {
                            player.sendSystemMessage(Component.literal("Seems like mask man couldn't come today.").withStyle(ChatFormatting.BLUE));
                        }
                    }
                } else {
                    HbmPlayerProps.getData(player).maskManTimer = 0;
                }
            }
        }

        if (MobConfig.enableRaids && MobConfig.raidDelay > 0 && time % MobConfig.raidDelay == 0) {
            if (rand.nextInt(Math.max(1, MobConfig.raidChance)) == 0 && !level.players().isEmpty() && surface) {
                ServerPlayer player = level.players().get(rand.nextInt(level.players().size()));
                if (HbmPlayerProps.getData(player).fbiMark < time) {
                    raid(level, player);
                }
            }
        }

        if (MobConfig.enableElementals && MobConfig.elementalDelay > 0 && time % MobConfig.elementalDelay == 0) {
            if (rand.nextInt(Math.max(1, MobConfig.elementalChance)) == 0 && !level.players().isEmpty() && surface) {
                ServerPlayer player = level.players().get(rand.nextInt(level.players().size()));
                EntityHbmProps data = HbmPlayerProps.getData(player);
                if (data.radMark) {
                    data.radMark = false;
                    elementals(level, player);
                }
            }
        }

        if (WorldConfig.enableMeteorStrikes) meteorUpdate(level);

        if (time % 20 == 0) {
            if (rand.nextInt(5) == 0 && !level.players().isEmpty() && surface) {
                ServerPlayer player = level.players().get(rand.nextInt(level.players().size()));
                if (HbmLivingProps.getDigamma(player) > 0) ghost(level, player);
            }
        }
    }

    /** The three mask man conditions of 1.7 {@code rollTheDice} for one player. */
    public static boolean maskManConditions(ServerLevel level, ServerPlayer player) {
        Item acidizer = ModMachineBlocks.machine_crystallizer.get().asItem();
        // 1.7: 1.33_enableStatReRegistering off skips the stat condition (the stats were unreliable without the fix)
        boolean acidizerStat = !com.hbm.config.GeneralConfig.enableStatReRegistering
                || player.getStats().getValue(Stats.ITEM_CRAFTED.get(acidizer)) > 0
                || player.getStats().getValue(Stats.ITEM_USED.get(acidizer)) > 0;
        boolean hasRads = HbmLivingProps.getRadiation(player) >= MobConfig.maskmanMinRad;
        boolean underground = heightValue(level, player.getX(), player.getZ()) > player.getY() + 3 || !MobConfig.maskmanUnderground;
        return acidizerStat && hasRads && underground;
    }

    /** One FBI raid on {@code player} (1.7's raid body, after the roll). */
    public static void raid(ServerLevel level, ServerPlayer player) {
        RandomSource rand = level.getRandom();
        player.sendSystemMessage(Component.literal("FBI, OPEN UP!").withStyle(ChatFormatting.RED));
        Vec3 vec = new Vec3(MobConfig.raidAttackDistance, 0, 0).yRot((float) (Math.PI * 2) * rand.nextFloat());

        for (int i = 0; i < MobConfig.raidAmount; i++) {
            double spawnX = player.getX() + vec.x + rand.nextGaussian() * 5;
            double spawnZ = player.getZ() + vec.z + rand.nextGaussian() * 5;
            double spawnY = heightValue(level, (int) spawnX, (int) spawnZ);
            trySpawn(level, (float) spawnX, (float) spawnY, (float) spawnZ, ModNtmEntities.FBI.get());
        }

        for (int i = 0; i < MobConfig.raidDrones; i++) {
            double spawnX = player.getX() + vec.x + rand.nextGaussian() * 5;
            double spawnZ = player.getZ() + vec.z + rand.nextGaussian() * 5;
            double spawnY = heightValue(level, (int) spawnX, (int) spawnZ);
            trySpawn(level, (float) spawnX, (float) spawnY + 10, (float) spawnZ, ModNtmEntities.FBI_DRONE.get());
        }
    }

    /** The radiation beast pack of 1.7's elemental branch; each beast gets its own heading, the first leads. */
    public static void elementals(ServerLevel level, ServerPlayer player) {
        RandomSource rand = level.getRandom();
        player.sendSystemMessage(Component.literal("You hear a faint clicking...").withStyle(ChatFormatting.YELLOW));
        Vec3 vec = new Vec3(MobConfig.raidAttackDistance, 0, 0);

        for (int i = 0; i < MobConfig.elementalAmount; i++) {
            // 1.7 rotated the same vector again for every beast
            vec = vec.yRot((float) (Math.PI * 2) * rand.nextFloat());
            double spawnX = player.getX() + vec.x + rand.nextGaussian();
            double spawnZ = player.getZ() + vec.z + rand.nextGaussian();
            double spawnY = heightValue(level, (int) spawnX, (int) spawnZ);
            EntityRADBeast rad = Entities.createTriggered(ModNtmEntities.RAD_BEAST.get(), level);
            if (rad == null) continue;
            if (i == 0) rad.makeLeader();
            trySpawn(level, (float) spawnX, (float) spawnY, (float) spawnZ, rad);
        }
    }

    /** A digamma ghost 75 blocks from {@code player}. */
    public static void ghost(ServerLevel level, ServerPlayer player) {
        RandomSource rand = level.getRandom();
        Vec3 vec = new Vec3(75, 0, 0).yRot((float) (Math.PI * 2) * rand.nextFloat());
        double spawnX = player.getX() + vec.x + rand.nextGaussian();
        double spawnZ = player.getZ() + vec.z + rand.nextGaussian();
        double spawnY = heightValue(level, (int) spawnX, (int) spawnZ);
        trySpawn(level, (float) spawnX, (float) spawnY, (float) spawnZ, ModNtmEntities.GHOST.get());
    }

    /** 1.7 {@code World.getHeightValue}: 0 when the column's chunk is not loaded. */
    private static int heightValue(ServerLevel level, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level.hasChunk(bx >> 4, bz >> 4)) return 0;
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
    }

    private static boolean trySpawn(ServerLevel level, float x, float y, float z, EntityType<? extends Mob> type) {
        Mob mob = Entities.createTriggered(type, level);
        return mob != null && trySpawn(level, x, y, z, mob);
    }

    /**
     * 1.7 {@code trySpawn}: placed with a random yaw, spawned, initialised as an egg spawn. 1.7's
     * {@code spawnEntityInWorld} refused a (non-player) entity whose chunk was not loaded; so does this.
     */
    private static boolean trySpawn(ServerLevel level, float x, float y, float z, Mob e) {
        e.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.hasChunk(net.minecraft.util.Mth.floor(x) >> 4, net.minecraft.util.Mth.floor(z) >> 4)) return false;
        if (!level.addFreshEntity(e)) return false;
        Spawns.finalizeTriggered(e, level);
        return true;
    }

    /** 1.7 {@code markFBI}: the player is safe from raids for the next 20 minutes. */
    public static void markFBI(Player player) {
        if (!player.level().isClientSide()) {
            HbmPlayerProps.getData(player).fbiMark = player.level().getGameTime() + 20 * 60 * 20;
        }
    }

    private static void meteorUpdate(ServerLevel level) {
        int chance = meteorShower > 0 ? WorldConfig.meteorShowerChance : WorldConfig.meteorStrikeChance;
        if (chance > 0 && meteorRand.nextInt(chance) == 0) {
            List<ServerPlayer> players = level.players();
            if (!players.isEmpty()) {
                ServerPlayer p = players.get(meteorRand.nextInt(players.size()));
                // 1.7 p.dimension == 0; hbm-space: p.worldObj.provider instanceof WorldProviderSurface
                if (p != null && com.hbm.api.environment.Environment.isSurface(p.level())) {
                    strike(level, p);
                }
            }
        }

        if (meteorShower > 0) {
            meteorShower--;
        }

        if (WorldConfig.meteorStrikeChance > 0 && meteorRand.nextInt(WorldConfig.meteorStrikeChance * 100) == 0
                && WorldConfig.enableMeteorShowers) {
            meteorShower = (int) (WorldConfig.meteorShowerDuration * 0.75 + WorldConfig.meteorShowerDuration * 0.25 * meteorRand.nextFloat());
        }
    }

    /** The charm checks of one strike on {@code p}, then the meteor. Returns the meteor, or null when a Meteor Charm stopped it. */
    public static EntityMeteor strike(ServerLevel level, Player p) {
        boolean repell = false;
        boolean strike = true;

        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack armor = p.getItemBySlot(slot);
            if (armor.isEmpty() || !ArmorModHandler.hasMods(armor)) continue;
            ItemStack[] mods = ArmorModHandler.pryMods(armor);
            for (int j = 0; j < 8; j++) { // 1.7 read slots 0..7: the battery slot came later
                ItemStack mod = mods[j];
                if (mod.isEmpty()) continue;
                if (mod.is(ModArmorModItems.protection_charm.get())) repell = true;
                if (mod.is(ModArmorModItems.meteor_charm.get())) strike = false;
            }
        }

        // only check if either charm is not present
        if (!repell || strike) {
            int x = (int) Math.floor(p.getX());
            int z = (int) Math.floor(p.getZ());
            List<PedestalEntry> entries = BlockPedestal.getEntriesForDimension(level.dimension());
            if (entries != null) {
                for (PedestalEntry entry : entries) {
                    if (Math.abs(entry.pos().getX() - x) <= 100 && Math.abs(entry.pos().getZ() - z) <= 100) {
                        if (entry.type() == PedestalEntryType.CHARM_OF_PROTECTION) repell = true;
                        if (entry.type() == PedestalEntryType.METEORITE_CHARM) strike = false;
                    }
                }
            }
        }

        return strike ? spawnMeteorAtPlayer(p, repell) : null;
    }

    /** 1.7 {@code spawnMeteorAtPlayer}: also the meteor remote's call. */
    public static EntityMeteor spawnMeteorAtPlayer(Player player, boolean repell) {
        EntityMeteor meteor = new EntityMeteor(player.level());
        meteor.setPos(player.getX() + meteorRand.nextInt(201) - 100, 384, player.getZ() + meteorRand.nextInt(201) - 100);

        Vec3 vec;
        if (repell) {
            vec = new Vec3(meteor.getX() - player.getX(), 0, meteor.getZ() - player.getZ()).normalize();
            double vel = meteorRand.nextDouble();
            vec = new Vec3(vec.x * vel, 0, vec.z * vel);
            meteor.safe = true;
        } else {
            vec = new Vec3(meteorRand.nextDouble() - 0.5D, 0, 0);
            vec = vec.yRot((float) (Math.PI * meteorRand.nextDouble()));
        }

        meteor.setDeltaMovement(vec.x, -2.5, vec.z);
        player.level().addFreshEntity(meteor);
        return meteor;
    }
}
