package com.hbm.tileentity.machine;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.sound.ModProductionSounds;
import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Plants;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.MachineAudio;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/** The old autosaw machine logic, with modern block tags and API boundaries made explicit. */
public class TileEntityMachineAutosaw extends TileEntityLoadedBase implements IFluidStandardReceiverMK2 {

    private static final int MIN_DIST = 2;
    private static final int MAX_DIST = 9;
    private static final int FELL_HORIZONTAL_RANGE = 10;
    private static final int FELL_BFS_RADIUS = MAX_DIST + FELL_HORIZONTAL_RANGE;
    private static final int FELL_VERTICAL_RANGE = 32;
    private static final int FELL_MAX_BASE_DEPTH = FELL_VERTICAL_RANGE / 2;

    // 6 face neighbours + 12 edge neighbours; corners are excluded.
    private static final int[][] EIGHTEEN_DIRS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 1, 0}, {1, -1, 0}, {-1, 1, 0}, {-1, -1, 0},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, 1, 1}, {0, 1, -1}, {0, -1, 1}, {0, -1, -1}
    };

    /**
     * The fuels the autosaw and the thresher burn: a live view of the {@code hbm:autosaw_fuel} fluid data map
     * ({@link com.hbm.api.data.FluidDataMaps#AUTOSAW_FUEL}; 1.7: wood oil, ethanol, fish oil, heavy oil).
     */
    public static final Set<FluidType> acceptedFuels = new AbstractSet<>() {
        @Override
        public boolean contains(Object o) {
            return o instanceof FluidType type && type != Fluids.NONE
                    && Boolean.TRUE.equals(FluidDataMaps.AUTOSAW_FUEL.get(Fluids.toVanilla(type)));
        }

        @Override
        public Iterator<FluidType> iterator() {
            List<FluidType> list = new ArrayList<>();
            FluidDataMaps.AUTOSAW_FUEL.byId().forEach((id, on) -> {
                if (Boolean.TRUE.equals(on) && Fluids.byRegistryName(id) != Fluids.NONE) list.add(Fluids.byRegistryName(id));
            });
            return list.iterator();
        }

        @Override
        public int size() {
            int n = 0;
            for (Iterator<FluidType> it = iterator(); it.hasNext(); it.next()) n++;
            return n;
        }
    };

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.WOODOIL, 100);

    public boolean isOn;
    public boolean isSuspended;
    private int forceSkip;
    public float syncYaw;
    public float rotationYaw;
    public float prevRotationYaw;
    public float syncPitch;
    public float rotationPitch;
    public float prevRotationPitch;

    // 0 = searching, 1 = extending, 2 = retracting.
    private int state;
    private int turnProgress;

    public float spin;
    public float lastSpin;

    public TileEntityMachineAutosaw(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_AUTOSAW.get(), pos, state);
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   TileEntityMachineAutosaw saw) {
        if (!(level instanceof ServerLevel server)) return;

        saw.setupFluidPorts(saw.getPorts());
        saw.updatePortFIFO();

        if (!saw.isSuspended && TickPhase.every(level, pos, 20L)) {
            if (saw.tank.getFill() > 0) {
                saw.tank.setFill(saw.tank.getFill() - 1);
                saw.isOn = true;
            } else {
                saw.isOn = false;
            }
            saw.setChanged();
        }

        if (saw.isOn && !saw.isSuspended) {
            Vec3 pivot = new Vec3(pos.getX() + 0.5D, pos.getY() + 1.75D, pos.getZ() + 0.5D);
            Vec3 upperArm = new Vec3(0D, 0D, -4D)
                    .xRot((float) Math.toRadians(80D - saw.rotationPitch))
                    .yRot((float) -Math.toRadians(saw.rotationYaw));
            Vec3 lowerArm = new Vec3(0D, 0D, -4D)
                    .xRot((float) -Math.toRadians(80D - saw.rotationPitch))
                    .yRot((float) -Math.toRadians(saw.rotationYaw));
            Vec3 armTip = new Vec3(0D, 0D, -2D)
                    .yRot((float) -Math.toRadians(saw.rotationYaw));

            double centerX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
            double centerY = pivot.y;
            double centerZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;
            saw.damageEntities(server, centerX, centerY, centerZ);

            if (saw.state == 0) {
                saw.rotationYaw += 1F;
                if (saw.rotationYaw >= 360F) saw.rotationYaw -= 360F;

                if (saw.forceSkip > 0) {
                    saw.forceSkip--;
                } else if (saw.detectTarget(server)) {
                    saw.state = 1;
                }
            }

            int hitY = (int) Math.floor(centerY);
            int hitX0 = (int) Math.floor(centerX - 0.5D);
            int hitZ0 = (int) Math.floor(centerZ - 0.5D);
            int hitX1 = (int) Math.floor(centerX + 0.5D);
            int hitZ1 = (int) Math.floor(centerZ + 0.5D);
            saw.tryInteract(server, new BlockPos(hitX0, hitY, hitZ0));
            saw.tryInteract(server, new BlockPos(hitX1, hitY, hitZ0));
            saw.tryInteract(server, new BlockPos(hitX0, hitY, hitZ1));
            saw.tryInteract(server, new BlockPos(hitX1, hitY, hitZ1));

            if (saw.state == 1) {
                saw.rotationPitch += 2F;
                if (saw.rotationPitch > 80F) {
                    saw.rotationPitch = 80F;
                    saw.state = 2;
                }
            }
            if (saw.state == 2) {
                saw.rotationPitch -= 2F;
                if (saw.rotationPitch <= 0F) {
                    saw.rotationPitch = 0F;
                    saw.state = 0;
                }
            }
        }

        saw.networkPackNT();
    }

    private AudioWrapper audio;
    private float audioPitch;

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                   TileEntityMachineAutosaw saw) {
        saw.lastSpin = saw.spin;
        if (saw.isOn && !saw.isSuspended) {
            saw.spin += 15F;
            Vec3 smoke = new Vec3(0.625D, 0D, 1.625D)
                    .yRot((float) -Math.toRadians(saw.rotationYaw));
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D + smoke.x,
                    pos.getY() + 2.0625D, pos.getZ() + 0.5D + smoke.z,
                    0D, 0D, 0D);
        }

        if (saw.spin >= 360F) {
            saw.spin -= 360F;
            saw.lastSpin -= 360F;
        }

        if (saw.audioPitch == 0F) saw.audioPitch = 1.0F + level.getRandom().nextFloat() * 0.1F;
        saw.audio = MachineAudio.tick(saw.audio, saw.isOn && !saw.isSuspended, ModProductionSounds.ENGINE.get(),
                SoundSource.BLOCKS, pos, saw.getVolume(1F), 10F, saw.audioPitch, 10);

        saw.prevRotationYaw = saw.rotationYaw;
        saw.prevRotationPitch = saw.rotationPitch;
        if (saw.turnProgress > 0) {
            double yawDelta = wrapDegrees(saw.syncYaw - saw.rotationYaw);
            double pitchDelta = wrapDegrees(saw.syncPitch - saw.rotationPitch);
            saw.rotationYaw = (float) (saw.rotationYaw + yawDelta / saw.turnProgress);
            saw.rotationPitch = (float) (saw.rotationPitch + pitchDelta / saw.turnProgress);
            saw.turnProgress--;
        } else {
            saw.rotationYaw = saw.syncYaw;
            saw.rotationPitch = saw.syncPitch;
        }
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360D;
        if (wrapped >= 180D) wrapped -= 360D;
        if (wrapped < -180D) wrapped += 360D;
        return wrapped;
    }

    private boolean detectTarget(ServerLevel level) {
        for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
            for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                if (!AutosawMath.isInCutWindow(dx, dz, rotationYaw)) continue;

                BlockPos target = getBlockPos().offset(dx, 1, dz);
                BlockState targetState = loadedState(level, target);
                if (targetState == null || !isCuttable(targetState) || shouldIgnore(level, target, targetState)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isCuttable(BlockState state) {
        return isWood(state) || isLeaves(state) || isPlant(state);
    }

    private static boolean isWood(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    private static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    private static boolean isPlant(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.FLOWERS) || state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)
                || block instanceof VegetationBlock || state.is(Blocks.VINE) || state.is(Blocks.SUGAR_CANE);
    }

    /** Crops and bonemealable growth stages are ignored until their vanilla growth target is exhausted. */
    private static boolean shouldIgnore(Level level, BlockPos pos, BlockState blockState) {
        // The old plant_tall mustard-willow metadata range cannot be named here: that HBM plant family
        // is not present in the current source tree, and inventing a replacement would change behavior.
        if (blockState.getBlock() instanceof BonemealableBlock growable
                && Plants.isValidBonemealTarget(growable, level, pos, blockState)) {
            return true;
        }

        for (Property<?> property : blockState.getProperties()) {
            if (!"age".equals(property.getName())) continue;
            if (property.getPossibleValues().isEmpty()) return false;
            @SuppressWarnings("unchecked")
            Property<Integer> ageProperty = (Property<Integer>) property;
            int age = blockState.getValue(ageProperty);
            int max = ageProperty.getPossibleValues().stream().max(Integer::compareTo).orElse(age);
            return age < max;
        }
        return blockState.getBlock() instanceof SaplingBlock;
    }

    private void tryInteract(ServerLevel level, BlockPos target) {
        BlockState blockState = loadedState(level, target);
        if (blockState == null) return;

        if (!shouldIgnore(level, target, blockState)) {
            if (isLeaves(blockState) || isPlant(blockState)) {
                cutCrop(level, target, blockState);
            } else if (isWood(blockState)) {
                fellTree(level, target);
                if (state == 1) state = 2;
            }
        }

        BlockState after = loadedState(level, target);
        if (state == 1 && after != null && after.isCollisionShapeFullBlock(level, target)) {
            state = 2;
            forceSkip = 5;
        }
    }

    private static void cutCrop(ServerLevel level, BlockPos pos, BlockState blockState) {
        Block block = blockState.getBlock();
        level.levelEvent(2001, pos, Block.getId(blockState));

        BlockState replacement = Blocks.AIR.defaultBlockState();
        ArrayList<ItemStack> drops = new ArrayList<>(Block.getDrops(blockState, level, pos,
                level.getBlockEntity(pos), null, ItemStack.EMPTY));
        boolean replanted = false;

        for (ItemStack drop : drops) {
            if (!replanted && !drop.isEmpty()) {
                BlockState plant = plantStateFromDrop(level, pos, drop);
                if (plant != null) {
                    replacement = plant;
                    replanted = true;
                    drop.shrink(1);
                }
            }

            if (drop.isEmpty()) continue;
            double delta = 0.7D;
            double dx = level.getRandom().nextFloat() * delta + (1D - delta) * 0.5D;
            double dy = level.getRandom().nextFloat() * delta + (1D - delta) * 0.5D;
            double dz = level.getRandom().nextFloat() * delta + (1D - delta) * 0.5D;
            ItemEntity item = new ItemEntity(level, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, drop);
            item.setPickUpDelay(10);
            level.addFreshEntity(item);
        }

        if (block == Blocks.WHEAT && !replanted) replacement = Blocks.WHEAT.defaultBlockState();
        level.setBlock(pos, replacement, 3);
    }

    @Nullable
    private static BlockState plantStateFromDrop(ServerLevel level, BlockPos pos, ItemStack drop) {
        if (!(drop.getItem() instanceof BlockItem blockItem)) return null;
        BlockState candidate = blockItem.getBlock().defaultBlockState();
        if (!isPlant(candidate) || !candidate.canSurvive(level, pos)) return null;
        return candidate;
    }

    private void fellTree(ServerLevel level, BlockPos hitPos) {
        BlockPos hitColumn = new BlockPos(hitPos.getX(), -1, hitPos.getZ());
        Map<BlockPos, BlockPos> trunks = new HashMap<>();

        for (int dx = -MAX_DIST; dx <= MAX_DIST; dx++) {
            for (int dz = -MAX_DIST; dz <= MAX_DIST; dz++) {
                if (dx * dx + dz * dz > MAX_DIST * MAX_DIST) continue;

                int columnX = getBlockPos().getX() + dx;
                int columnZ = getBlockPos().getZ() + dz;
                BlockPos atHit = new BlockPos(columnX, hitPos.getY(), columnZ);
                BlockState atHitState = loadedState(level, atHit);
                if (atHitState == null || !isWood(atHitState)) continue;

                int baseY = hitPos.getY();
                while (hitPos.getY() - baseY < FELL_MAX_BASE_DEPTH) {
                    BlockPos below = new BlockPos(columnX, baseY - 1, columnZ);
                    BlockState belowState = loadedState(level, below);
                    if (belowState == null || !isWood(belowState)) break;
                    baseY--;
                }

                if (!canSupportSapling(level, new BlockPos(columnX, baseY - 1, columnZ))) continue;
                trunks.put(new BlockPos(columnX, -1, columnZ), new BlockPos(columnX, baseY, columnZ));
            }
        }

        if (!trunks.containsKey(hitColumn)) {
            int baseY = hitPos.getY();
            while (hitPos.getY() - baseY < FELL_MAX_BASE_DEPTH) {
                BlockPos below = new BlockPos(hitPos.getX(), baseY - 1, hitPos.getZ());
                BlockState belowState = loadedState(level, below);
                if (belowState == null || !isWood(belowState)) break;
                baseY--;
            }
            trunks.put(hitColumn, new BlockPos(hitPos.getX(), baseY, hitPos.getZ()));
        }

        Map<BlockPos, BlockPos> blockOwner = new HashMap<>();
        ArrayDeque<BlockPos[]> deque = new ArrayDeque<>();
        int hitColumnCount = 1;
        for (Map.Entry<BlockPos, BlockPos> trunk : trunks.entrySet()) {
            deque.addFirst(new BlockPos[]{trunk.getValue(), trunk.getKey()});
        }

        int minY = Math.max(level.getMinY(), hitPos.getY() - FELL_MAX_BASE_DEPTH);
        int maxY = Math.min(level.getMaxY() - 1, hitPos.getY() + FELL_VERTICAL_RANGE);

        while (!deque.isEmpty()) {
            BlockPos[] pair = deque.pollFirst();
            BlockPos current = pair[0];
            BlockPos currentColumn = pair[1];

            if (blockOwner.containsKey(current)) {
                if (currentColumn.equals(hitColumn)) {
                    hitColumnCount--;
                    if (hitColumnCount == 0) break;
                }
                continue;
            }
            blockOwner.put(current, currentColumn);

            for (int[] direction : EIGHTEEN_DIRS) {
                int neighbourX = current.getX() + direction[0];
                int neighbourY = current.getY() + direction[1];
                int neighbourZ = current.getZ() + direction[2];
                int dx = neighbourX - getBlockPos().getX();
                int dz = neighbourZ - getBlockPos().getZ();
                if (dx * dx + dz * dz > FELL_BFS_RADIUS * FELL_BFS_RADIUS
                        || neighbourY < minY || neighbourY > maxY) continue;

                BlockPos neighbour = new BlockPos(neighbourX, neighbourY, neighbourZ);
                if (blockOwner.containsKey(neighbour)) continue;
                BlockState neighbourState = loadedState(level, neighbour);
                if (neighbourState == null || (!isWood(neighbourState) && !isLeaves(neighbourState))) continue;

                BlockPos[] entry = new BlockPos[]{neighbour, currentColumn};
                boolean horizontal = direction[0] != 0 || direction[2] != 0;
                if (horizontal) deque.addLast(entry);
                else deque.addFirst(entry);
                if (currentColumn.equals(hitColumn)) hitColumnCount++;
            }

            if (currentColumn.equals(hitColumn)) {
                hitColumnCount--;
                if (hitColumnCount == 0) break;
            }
        }

        for (Map.Entry<BlockPos, BlockPos> entry : blockOwner.entrySet()) {
            if (!entry.getValue().equals(hitColumn)) continue;
            BlockPos target = entry.getKey();
            BlockState blockState = loadedState(level, target);
            if (blockState == null) continue;

            if (isWood(blockState) && isWithinWorkingArea(target)
                    && canSupportSapling(level, target.below())) {
                level.destroyBlock(target, true);
                // The old metadata fallback was oak for any unknown wood block; keep that oddity.
                level.setBlock(target, saplingForLog(blockState), 3);
            } else {
                level.destroyBlock(target, true);
            }
        }
    }

    private boolean isWithinWorkingArea(BlockPos pos) {
        int dx = pos.getX() - getBlockPos().getX();
        int dz = pos.getZ() - getBlockPos().getZ();
        int distance = dx * dx + dz * dz;
        return distance > MIN_DIST * MIN_DIST && distance <= MAX_DIST * MAX_DIST;
    }

    private static boolean canSupportSapling(ServerLevel level, BlockPos soilPos) {
        BlockPos plantPos = soilPos.above();
        return loadedState(level, soilPos) != null && loadedState(level, plantPos) != null
                && Blocks.OAK_SAPLING.defaultBlockState().canSurvive(level, plantPos);
    }

    private static BlockState saplingForLog(BlockState log) {
        if (log.is(Blocks.SPRUCE_LOG) || log.is(Blocks.SPRUCE_WOOD)) return Blocks.SPRUCE_SAPLING.defaultBlockState();
        if (log.is(Blocks.BIRCH_LOG) || log.is(Blocks.BIRCH_WOOD)) return Blocks.BIRCH_SAPLING.defaultBlockState();
        if (log.is(Blocks.JUNGLE_LOG) || log.is(Blocks.JUNGLE_WOOD)) return Blocks.JUNGLE_SAPLING.defaultBlockState();
        if (log.is(Blocks.ACACIA_LOG) || log.is(Blocks.ACACIA_WOOD)) return Blocks.ACACIA_SAPLING.defaultBlockState();
        if (log.is(Blocks.DARK_OAK_LOG) || log.is(Blocks.DARK_OAK_WOOD)) return Blocks.DARK_OAK_SAPLING.defaultBlockState();
        return Blocks.OAK_SAPLING.defaultBlockState();
    }

    @Nullable
    private static BlockState loadedState(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) ? level.getBlockState(pos) : null;
    }

    private void damageEntities(ServerLevel level, double centerX, double centerY, double centerZ) {
        AABB box = new AABB(centerX - 1D, centerY - 0.25D, centerZ - 1D,
                centerX + 1D, centerY + 0.25D, centerZ + 1D);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity.isAlive() && Compat.hurt(entity, level, ModDamageSources.turbofan(level), 100F)) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE,
                        2.0F, 0.95F + level.getRandom().nextFloat() * 0.2F);
                int count = Math.min((int) Math.ceil(entity.getMaxHealth() / 4F), 250) * 4;
                ParticleEffects.sawmillDamage(level, entity, count);
            }
        }
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(isSuspended);
        buf.writeFloat(rotationYaw);
        buf.writeFloat(rotationPitch);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        isSuspended = buf.readBoolean();
        syncYaw = buf.readFloat();
        syncPitch = buf.readFloat();
        turnProgress = 3;
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("isSuspended", isSuspended);
        output.putInt("skip", forceSkip);
        output.putFloat("yaw", rotationYaw);
        output.putFloat("pitch", rotationPitch);
        output.putInt("state", state);
        tank.writeToNBT(output, "t");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isOn = input.getBooleanOr("isOn", false);
        isSuspended = input.getBooleanOr("isSuspended", false);
        forceSkip = input.getIntOr("skip", 0);
        rotationYaw = input.getFloatOr("yaw", 0F);
        rotationPitch = input.getFloatOr("pitch", 0F);
        state = input.getIntOr("state", 0);
        tank.readFromNBT(input, "t");
    }
}
