package com.hbm.tileentity.turret;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemTurretBiometry;
import com.hbm.inventory.container.ContainerTurretBase;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.Tags;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Raycasts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;

/**
 * The shared turret brain — target acquisition, line of sight, aiming and the ammo/chip inventory.
 * Ported from 1.12.2's {@code TileEntityTurretBaseNT}.
 *
 * <p>Eleven slots: the biometry chip (0), nine ammo slots (1–9) and a battery (10). A turret only
 * acts while switched on and powered; each tick it decays its search timer, reacquires when it has
 * no target, aims at whatever it holds, and lets {@link #updateFiringTick()} decide whether to shoot.
 *
 * <p><b>Deviations from the fork.</b> The 1.12.2 target filter also matches {@code EntityBomber},
 * {@code EntityMissileCustom} and {@code EntityRailCarBase}; the current {@link EntityMissileBaseNT}
 * hierarchy covers the missile branch, while the bomber and rail-car branches are not ported yet.
 * Spent casings are likewise absent: this port's
 * {@code BulletConfig} carries no casing, so a turret that throws brass simply does not.
 *
 * <p>The fork's redstone-over-radio surface is ten functions; this port carries the seven whose state
 * exists here. The other three — {@code toggleBlacklistMobFilter}, {@code addMobFilter} and
 * {@code removeMobFilter} — drive a per-entity-id target filter that is not ported, so they are absent
 * rather than accepted and dropped. {@code addWhitelist} also guards its list where 1.12.2 does not:
 * {@link #getWhitelist} answers null both for a missing chip and for a chip with an empty roster, so the
 * fork throws on the first name written to a fresh chip. Adding on null is the reading that makes the
 * command work, and a turret with no chip is still a no-op because {@link #addName} checks for one.
 */
public abstract class TileEntityTurretBaseNT extends TileEntityMachineBase
        implements IEnergyReceiverMK2, IControlReceiver, MenuProvider, IRORInteractive {

    private static final Identifier TURRET_GUI_BASE = turretGui("base");

    public static final int SLOT_CHIP = 0;
    public static final int SLOT_AMMO_FIRST = 1;
    public static final int SLOT_AMMO_LAST = 9;
    public static final int SLOT_BATTERY = 10;
    public static final int SLOT_COUNT = 11;
    private static final int MAX_CONTROL_NAME_LENGTH = 25;
    private static final int MAX_WHITELIST_ENTRIES = 64;
    private static final int MAX_MOB_FILTER_ENTRIES = 1024;
    private static final java.util.Set<String> MISC_MOB_FILTER_IDS = java.util.Set.of(
            "minecraft:iron_golem", "minecraft:snow_golem", "minecraft:villager", "minecraft:wandering_trader");

    private static final int[] AMMO_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    public double rotationYaw;
    public double rotationPitch;
    public double lastRotationYaw;
    public double lastRotationPitch;
    public double syncRotationYaw;
    public double syncRotationPitch;
    public boolean isOn;
    public boolean aligned;
    public int searchTimer;
    public long power;
    public boolean targetPlayers;
    public boolean targetAnimals;
    public boolean targetMobs = true;
    public boolean targetMachines = true;
    public boolean isBlacklistMobFilter = true;
    public int stattrak;
    public int casingDelay;
    public int connectorMask;
    public @Nullable Entity target;
    public @Nullable Vec3 tPos;
    public final List<String> mobFilter = new ArrayList<>();
    protected @Nullable List<ItemStack> ammoStacks;

    private static final Codec<List<String>> MOB_FILTER_CODEC = Codec.STRING.listOf();

    protected TileEntityTurretBaseNT(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    public abstract void updateFiringTick();

    protected abstract @Nullable List<BulletConfig> getAmmoList();

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityTurretBaseNT turret) {
        turret.tickServer();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityTurretBaseNT turret) {
        turret.tickClient();
    }

    protected void tickServer() {
        aligned = false;

        if (target != null && !target.isAlive()) {
            target = null;
            stattrak++;
        }

        if (target != null && !entityInLOS(target)) target = null;

        tPos = target != null ? getEntityPos(target) : null;

        if (isOn() && hasPower()) {
            if (tPos != null) alignTurret();
        } else {
            target = null;
            tPos = null;
        }

        if (target != null && !target.isAlive()) {
            target = null;
            tPos = null;
            stattrak++;
        }

        if (isOn() && hasPower()) {
            searchTimer--;
            setPower(getPower() - getConsumption());

            if (searchTimer <= 0) {
                searchTimer = getDetectorInterval();
                if (target == null) seekNewTarget();
            }
        } else {
            searchTimer = 0;
        }

        if (aligned) updateFiringTick();

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, getMaxPower());

        if (level instanceof ServerLevel server) connectorMask = computeConnectorMask(server);

        setChanged();
        networkPackNT();
    }

    protected void tickClient() {
        lastRotationPitch = rotationPitch;
        lastRotationYaw = rotationYaw;
        rotationPitch = syncRotationPitch;
        rotationYaw = syncRotationYaw;

        if (Math.abs(lastRotationYaw - rotationYaw) > Math.PI) {
            if (lastRotationYaw < rotationYaw) lastRotationYaw += Math.PI * 2;
            else lastRotationYaw -= Math.PI * 2;
        }
    }

    protected void seekNewTarget() {
        Vec3 pos = getTurretPos();
        double range = getDetectorRange();
        Entity found = null;
        double closest = range;

        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(pos, pos).inflate(range))) {
            double dist = getEntityPos(entity).subtract(pos).length();
            if (dist > range) continue;
            if (!entityAcceptableTarget(entity)) continue;
            if (!entityInLOS(entity)) continue;

            if (dist < closest) {
                closest = dist;
                found = entity;
            }
        }

        target = found;
        if (target != null) tPos = getEntityPos(target);
    }

    protected void alignTurret() {
        turnTowards(tPos);
    }

    public void turnTowards(Vec3 ent) {
        Vec3 delta = ent.subtract(getTurretPos());
        double targetPitch = Math.asin(delta.y / delta.length());
        double targetYaw = -Math.atan2(delta.x, delta.z);
        turnTowardsAngle(targetPitch, targetYaw);
    }

    public void turnTowardsAngle(double targetPitch, double targetYaw) {
        double turnYaw = Math.toRadians(getTurretYawSpeed());
        double turnPitch = Math.toRadians(getTurretPitchSpeed());
        double pi2 = Math.PI * 2;

        if (Math.abs(rotationPitch - targetPitch) < turnPitch
                || Math.abs(rotationPitch - targetPitch) > pi2 - turnPitch) {
            rotationPitch = targetPitch;
        } else if (targetPitch > rotationPitch) {
            rotationPitch += turnPitch;
        } else {
            rotationPitch -= turnPitch;
        }

        double deltaYaw = (targetYaw - rotationYaw) % pi2;
        int dir = 0;
        if (deltaYaw < -Math.PI) dir = 1;
        else if (deltaYaw < 0) dir = -1;
        else if (deltaYaw > Math.PI) dir = -1;
        else if (deltaYaw > 0) dir = 1;

        if (Math.abs(rotationYaw - targetYaw) < turnYaw || Math.abs(rotationYaw - targetYaw) > pi2 - turnYaw) {
            rotationYaw = targetYaw;
        } else {
            rotationYaw += turnYaw * dir;
        }

        double deltaPitch = targetPitch - rotationPitch;
        deltaYaw = targetYaw - rotationYaw;
        double deltaAngle = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);

        rotationYaw = rotationYaw % pi2;
        rotationPitch = rotationPitch % pi2;

        if (deltaAngle <= Math.toRadians(getAcceptableInaccuracy())) aligned = true;
    }

    public boolean entityInLOS(Entity e) {
        if (!e.isAlive()) return false;
        // 1.7 predates spectators. A spectator is not in the world physically: never acquire or keep one.
        // Checked here, not only in entityAcceptableTarget, because this is the one test every target
        // passes - on acquisition, on the per-tick retention check, and in the damaged turrets whose
        // entityAcceptableTarget replaces the base one - so a player who switches mode mid-lock is dropped.
        if (e.isSpectator()) return false;
        if (!hasThermalVision() && e instanceof LivingEntity living && living.hasEffect(MobEffects.INVISIBILITY)) {
            return false;
        }

        Vec3 pos = getTurretPos();
        Vec3 ent = getEntityPos(e);
        Vec3 delta = ent.subtract(pos);
        double length = delta.length();

        if (length < getDetectorGrace() || length > getDetectorRange() * 1.1) return false;

        Vec3 unit = delta.normalize();
        double pitchDeg = Math.toDegrees(Math.asin(unit.y / unit.length()));
        if (pitchDeg < -getTurretDepression() || pitchDeg > getTurretElevation()) return false;

        return !isObstructedOpaque(ent, pos);
    }

    protected boolean isObstructedOpaque(Vec3 from, Vec3 to) {
        return Raycasts.isObstructed(level, from, to, null);
    }

    public boolean entityAcceptableTarget(Entity e) {
        if (!e.isAlive()) return false;
        // 1.7 base turrets exclude no game mode: creative players are targeted (their invulnerability
        // ignores the damage). Only the damaged Howard/Sentry skip creative players. Spectators: entityInLOS.
        if (e.isSpectator()) return false;

        List<String> whitelist = getWhitelist();
        if (whitelist != null) {
            if (e instanceof Player player) {
                if (whitelist.contains(player.getScoreboardName())) return false;
            } else if (e instanceof Mob mob) {
                net.minecraft.network.chat.Component name = mob.getCustomName();
                if (name != null && whitelist.contains(name.getString())) return false;
            }
        }

        if (!mobFilter.isEmpty() && e instanceof Mob) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            if (id != null) {
                boolean listed = mobFilter.contains(id.toString());
                if (isBlacklistMobFilter ? listed : !listed) return false;
            }
        }

        if (targetAnimals) {
            if (e instanceof Animal || e instanceof AmbientCreature) return true;
            if (e instanceof Npc) return true;
        }

        if (targetMobs) {
            if (e instanceof EnderDragon) return false;
            if (e instanceof EnderDragonPart) return true;
            if (e instanceof Enemy) return true;
        }

        if (targetMachines) {
            if (e instanceof IRadarDetectableNT detectable && !detectable.canBeSeenBy(this)) return false;
            if (e instanceof EntityMissileBaseNT missile) return missile.getDeltaMovement().y < 0D;
            if (e instanceof AbstractMinecart) return true;
        }

        if (targetPlayers && e instanceof Player) return true;

        return false;
    }

    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;

        List<ItemStack> stacks = new ArrayList<>();
        List<BulletConfig> list = getAmmoList();
        if (list != null) {
            for (BulletConfig config : list) {
                if (config != null && config.ammoItem() != null) stacks.add(config.toStack());
            }
        }

        ammoStacks = stacks;
        return ammoStacks;
    }

    /** Slot order wins over config order, as upstream. */
    public @Nullable BulletConfig getFirstConfigLoaded() {
        List<BulletConfig> list = getAmmoList();
        if (list == null || list.isEmpty()) return null;

        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            for (BulletConfig config : list) if (config.matchesAmmo(stack)) return config;
        }

        return null;
    }

    protected Vec3 alongBarrel(double forward, double up, double side) {
        return new Vec3(forward, up, side)
                .zRot((float) -rotationPitch)
                .yRot((float) -(rotationYaw + Math.PI * 0.5));
    }

    protected Vec3 barrelTip() {
        return getTurretPos().add(alongBarrel(getBarrelLength(), 0, 0));
    }

    /** 1.12.2's {@code largeexplode}: the flash size rides the x velocity slot. */
    protected void muzzleFlash(Vec3 at, float size, int count) {
        if (!(level instanceof ServerLevel server)) return;
        if (count <= 1) {
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 0, size, 0D, 0D, 1D);
        } else {
            server.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, count, size, 0D, 0D, 0D);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerTurretBase(containerId, playerInventory, this);
    }

    /**
     * 1.12.2 spells the background and label colour as a {@code GUITurret*} subclass per turret; the
     * layout never varies, so the pair lives here and one screen serves every turret.
     */
    public Identifier guiTexture() {
        return TURRET_GUI_BASE;
    }

    public int guiFontColor() {
        return 0x808080;
    }

    protected static Identifier turretGui(String name) {
        return dev.golem.core.Ids.of(com.hbm.Constants.MOD_ID, "textures/gui/weapon/gui_turret_" + name + ".png");
    }

    /** The tank a fuel-burning turret shows in its GUI, or null for the rest. */
    public @Nullable FluidTankNTM turretTank() {
        return null;
    }

    public void manualSetup() {}

    public void spawnBullet(BulletConfig bullet, float baseDamage) {
        Vec3 tip = barrelTip();

        EntityBulletBaseMK4 projectile = new EntityBulletBaseMK4(level, bullet, baseDamage, bullet.spread,
                (float) rotationYaw, (float) rotationPitch);
        projectile.setPos(tip.x, tip.y, tip.z);
        level.addFreshEntity(projectile);

        if (usesCasings() && casingDelay() > 0) casingDelay = casingDelay();
    }

    public boolean usesCasings() {
        return false;
    }

    public int casingDelay() {
        return 0;
    }

    public void consumeAmmo(BulletConfig ammo) {
        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            if (ammo.matchesAmmo(getItem(i))) {
                removeItem(i, 1);
                return;
            }
        }
        setChanged();
    }

    public @Nullable List<String> getWhitelist() {
        ItemStack chip = getItem(SLOT_CHIP);
        if (!chip.is(ModMachineItems.turret_chip.get())) return null;
        String[] names = ItemTurretBiometry.getNames(chip);
        return names == null ? null : Arrays.asList(names);
    }

    public void addName(String name) {
        if (name.isEmpty() || name.length() > MAX_CONTROL_NAME_LENGTH) return;
        ItemStack chip = getItem(SLOT_CHIP);
        if (!chip.is(ModMachineItems.turret_chip.get())) return;
        String[] names = ItemTurretBiometry.getNames(chip);
        if (names != null && names.length >= MAX_WHITELIST_ENTRIES) return;
        ItemTurretBiometry.addName(chip, name);
    }

    public void removeName(int index) {
        ItemStack chip = getItem(SLOT_CHIP);
        if (!chip.is(ModMachineItems.turret_chip.get())) return;

        String[] array = ItemTurretBiometry.getNames(chip);
        if (array == null) return;

        List<String> names = new ArrayList<>(Arrays.asList(array));
        ItemTurretBiometry.clearNames(chip);
        if (index >= 0 && index < names.size()) names.remove(index);
        for (String name : names) ItemTurretBiometry.addName(chip, name);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("del")) removeName(Tags.getIntOr(data, "del", 0));
        else if (data.contains("name")) addName(Tags.getStringOr(data, "name", ""));
        else if (data.contains("addMobFilter")) {
            String id = Tags.getStringOr(data, "addMobFilter", "");
            if (isValidMobFilter(id) && mobFilter.size() < MAX_MOB_FILTER_ENTRIES && !mobFilter.contains(id)) {
                mobFilter.add(id);
            }
        } else if (data.contains("removeMobFilter")) {
            mobFilter.remove(Tags.getStringOr(data, "removeMobFilter", ""));
        } else if (data.contains("toggle")) {
            // The old AuxButtonPacket carries button 0 in value and the actual button in meta.
            handleButtonPacket(0, Tags.getIntOr(data, "toggle", -1));
        }
    }

    private static boolean isValidMobFilter(String id) {
        if (id.isEmpty() || id.length() > 128) return false;
        try {
            net.minecraft.world.entity.EntityType<?> type = dev.golem.core.registry.Entities.byId(id);
            return isFilterableMob(id, type);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** 26.1 reports {@code Entity.class} from EntityType, so category plus the vanilla MISC mobs is authoritative. */
    public static boolean isFilterableMob(String id, net.minecraft.world.entity.EntityType<?> type) {
        return type != null && (type.getCategory() != net.minecraft.world.entity.MobCategory.MISC
                || MISC_MOB_FILTER_IDS.contains(id));
    }

    @Override
    public boolean hasPermission(Player player) {
        // Turret controls intentionally retain the old proximity-based remote policy.
        return stillValid(player);
    }

    @Override
    public void handleButtonPacket(int value, int meta) {
        switch (meta) {
            case 0 -> isOn = !isOn;
            case 1 -> targetPlayers = !targetPlayers;
            case 2 -> targetAnimals = !targetAnimals;
            case 3 -> targetMobs = !targetMobs;
            case 4 -> targetMachines = !targetMachines;
            case 6 -> isBlacklistMobFilter = !isBlacklistMobFilter;
            default -> {
            }
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_FUNCTION + "setActive" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "targetPlayers" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetAnimals" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetMobs" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "targetMachines" + NAME_SEPARATOR + "enabled (0 or 1)",
                PREFIX_FUNCTION + "addWhitelist" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "removeWhitelist" + NAME_SEPARATOR + "name"};
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setActive").equals(name) && params.length > 0) {
            isOn = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            markChanged();
        }
        if ((PREFIX_FUNCTION + "targetPlayers").equals(name) && params.length > 0) {
            targetPlayers = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            markChanged();
        }
        if ((PREFIX_FUNCTION + "targetAnimals").equals(name) && params.length > 0) {
            targetAnimals = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            markChanged();
        }
        if ((PREFIX_FUNCTION + "targetMobs").equals(name) && params.length > 0) {
            targetMobs = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            markChanged();
        }
        if ((PREFIX_FUNCTION + "targetMachines").equals(name) && params.length > 0) {
            targetMachines = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            markChanged();
        }
        if ((PREFIX_FUNCTION + "addWhitelist").equals(name) && params.length > 0) {
            List<String> whitelist = getWhitelist();
            if (whitelist == null || !whitelist.contains(params[0])) addName(params[0]);
            markChanged();
        }
        if ((PREFIX_FUNCTION + "removeWhitelist").equals(name) && params.length > 0) {
            List<String> whitelist = getWhitelist();
            if (whitelist != null && whitelist.contains(params[0])) {
                removeName(whitelist.indexOf(params[0]));
            }
            markChanged();
        }
        return null;
    }

    public double getAcceptableInaccuracy() {
        return 5D;
    }

    public double getTurretYawSpeed() {
        return 4.5D;
    }

    public double getTurretPitchSpeed() {
        return 3D;
    }

    public double getTurretDepression() {
        return 30D;
    }

    public double getTurretElevation() {
        return 30D;
    }

    public int getDetectorInterval() {
        return 10;
    }

    public double getDetectorRange() {
        return 32D;
    }

    public double getDetectorGrace() {
        return 3D;
    }

    public double getHeightOffset() {
        return 1.5D;
    }

    public double getBarrelLength() {
        return 1.0D;
    }

    public boolean hasThermalVision() {
        return true;
    }

    public Vec3 getTurretPos() {
        Vec3 offset = getHorizontalOffset();
        return new Vec3(worldPosition.getX() + offset.x, worldPosition.getY() + getHeightOffset(),
                worldPosition.getZ() + offset.z);
    }

    public Vec3 getHorizontalOffset() {
        BlockState state = getBlockState();
        if (!state.hasProperty(BlockMultiblockCore.FACING)) return new Vec3(0.5D, 0D, 0.5D);
        Direction facing = state.getValue(BlockMultiblockCore.FACING);
        double x = facing == Direction.NORTH || facing == Direction.WEST ? 1D : 0D;
        double z = facing == Direction.NORTH || facing == Direction.EAST ? 1D : 0D;
        return new Vec3(x, 0D, z);
    }

    protected boolean hasConnectorPlugs() {
        return true;
    }

    /**
     * The eight pad-edge plugs, in the order 1.12.2's {@code renderConnectors} draws them. The graph
     * only answers on the server, so the mask is computed there and synced for the renderer.
     */
    /**
     * Where a cable may feed the turret: the eight pad-edge cells plus the four beneath the footprint.
     * The first eight are also the plugs the renderer draws, in 1.12.2's {@code renderConnectors}
     * order, which is why the order matters and the underside comes last.
     */
    public DirPos[] getConPos() {
        Vec3 offset = getHorizontalOffset();
        int x = worldPosition.getX() + (int) offset.x;
        int y = worldPosition.getY();
        int z = worldPosition.getZ() + (int) offset.z;

        return new DirPos[]{
                new DirPos(x - 2, y, z, Library.NEG_X), new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 1, Library.POS_Z), new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x + 1, y, z, Library.POS_X), new DirPos(x + 1, y, z - 1, Library.POS_X),
                new DirPos(x, y, z - 2, Library.NEG_Z), new DirPos(x - 1, y, z - 2, Library.NEG_Z),
                new DirPos(x, y - 1, z, Library.NEG_Y), new DirPos(x - 1, y - 1, z, Library.NEG_Y),
                new DirPos(x, y - 1, z - 1, Library.NEG_Y), new DirPos(x - 1, y - 1, z - 1, Library.NEG_Y),
        };
    }

    private int computeConnectorMask(ServerLevel world) {
        if (!hasConnectorPlugs()) return 0;

        DirPos[] ports = getConPos();
        int mask = 0;
        for (int i = 0; i < 8 && i < ports.length; i++) {
            if (Library.canConnect(world, ports[i].getPos(), ports[i].getDir())) mask |= 1 << i;
        }
        return mask;
    }

    public Vec3 getEntityPos(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
    }

    public boolean hasPower() {
        return getPower() >= getConsumption();
    }

    public boolean isOn() {
        return isOn;
    }

    public long getConsumption() {
        return 100L;
    }

    public int getPowerScaled(int scale) {
        return (int) (power * scale / getMaxPower());
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = Math.max(0L, Math.min(power, getMaxPower()));
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return AMMO_SLOTS;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(tPos != null);
        if (tPos != null) {
            buf.writeDouble(tPos.x);
            buf.writeDouble(tPos.y);
            buf.writeDouble(tPos.z);
        }
        buf.writeDouble(rotationPitch);
        buf.writeDouble(rotationYaw);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
        buf.writeBoolean(targetPlayers);
        buf.writeBoolean(targetAnimals);
        buf.writeBoolean(targetMobs);
        buf.writeBoolean(targetMachines);
        buf.writeInt(stattrak);
        buf.writeByte(connectorMask);
        buf.writeBoolean(isBlacklistMobFilter);
        buf.writeInt(mobFilter.size());
        for (String id : mobFilter) buf.writeUtf(id);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tPos = buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        syncRotationPitch = buf.readDouble();
        syncRotationYaw = buf.readDouble();
        power = buf.readLong();
        isOn = buf.readBoolean();
        targetPlayers = buf.readBoolean();
        targetAnimals = buf.readBoolean();
        targetMobs = buf.readBoolean();
        targetMachines = buf.readBoolean();
        stattrak = buf.readInt();
        connectorMask = buf.readByte() & 0xFF;
        isBlacklistMobFilter = buf.readBoolean();
        mobFilter.clear();
        int filterSize = Math.max(0, buf.readInt());
        for (int i = 0; i < filterSize; i++) mobFilter.add(buf.readUtf());
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putBoolean("targetPlayers", targetPlayers);
        output.putBoolean("targetAnimals", targetAnimals);
        output.putBoolean("targetMobs", targetMobs);
        output.putBoolean("targetMachines", targetMachines);
        output.putBoolean("isBlacklistFilter", isBlacklistMobFilter);
        output.store("mobFilter", MOB_FILTER_CODEC, mobFilter);
        output.putInt("stattrak", stattrak);
    }

    public void openInventory() {
        playLidSound(ModSounds.OPEN_C.get());
    }

    public void closeInventory() {
        playLidSound(ModSounds.CLOSE_C.get());
    }

    private void playLidSound(net.minecraft.sounds.SoundEvent sound) {
        if (level == null || level.isClientSide()) return;
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        isOn = input.getBooleanOr("isOn", false);
        targetPlayers = input.getBooleanOr("targetPlayers", false);
        targetAnimals = input.getBooleanOr("targetAnimals", false);
        targetMobs = input.getBooleanOr("targetMobs", false);
        targetMachines = input.getBooleanOr("targetMachines", false);
        isBlacklistMobFilter = input.getBooleanOr("isBlacklistFilter", true);
        mobFilter.clear();
        input.read("mobFilter", MOB_FILTER_CODEC).orElse(List.of()).stream()
                .filter(TileEntityTurretBaseNT::isValidMobFilter)
                .distinct().limit(MAX_MOB_FILTER_ENTRIES)
                .forEach(mobFilter::add);
        stattrak = input.getIntOr("stattrak", 0);
    }
}
