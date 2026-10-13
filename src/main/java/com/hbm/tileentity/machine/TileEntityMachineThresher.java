package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Plants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.generic.BlockTallPlant;
import com.hbm.blocks.machine.MachineThresher;
import com.hbm.sound.ModProductionSounds;
import com.hbm.tileentity.ModProductionBlockEntities;

/**
 * The automatic thresher, ported from the original {@code TileEntityMachineThresher}: a fuelled arm that
 * folds out over a 7-wide strip two segments ahead, harvesting and re-planting crops, cutting cane and
 * cactus, and threshing tall grass and sunflowers now and then.
 *
 * <p>1.7's {@code BlockTallPlant} (hemp, mustard willow) is not in this tree, so that branch has no block
 * to name; vanilla's two-block plants take its place under the double-plant rule.
 */
public class TileEntityMachineThresher extends TileEntityLoadedBase implements IFluidStandardReceiverMK2 {

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.WOODOIL, 100);

    public boolean isOn;
    public boolean isSuspended;
    public int delay;

    private int turnProgress;
    public float syncAngle;
    public float angle;
    public float prevAngle;

    // 0: waiting, 1: extending, 2: retracting
    private int state = 0;

    public float spin;
    public float lastSpin;
    private @Nullable AudioWrapper audio;

    public TileEntityMachineThresher(BlockPos pos, BlockState state) {
        super(ModProductionBlockEntities.THRESHER.get(), pos, state);
    }

    private static ForgeDirection facing(BlockState state) {
        Direction facing = state.hasProperty(MachineThresher.FACING) ? state.getValue(MachineThresher.FACING) : Direction.NORTH;
        return ForgeDirection.getOrientation(facing);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, TileEntityMachineThresher te) {
        if (!(level instanceof ServerLevel server)) return;

        ForgeDirection dir = facing(blockState).getOpposite();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        if (!te.isSuspended && TickPhase.every(level, pos, 20L)) {
            if (te.tank.getFill() > 0) {
                te.tank.setFill(te.tank.getFill() - 1);
                te.isOn = true;
            } else {
                te.isOn = false;
            }
            te.trySubscribe(te.tank.getTankType(), level, pos.getX() + rot.offsetX, pos.getY(), pos.getZ() + rot.offsetZ, rot);
            te.trySubscribe(te.tank.getTankType(), level, pos.getX() - rot.offsetX, pos.getY(), pos.getZ() - rot.offsetZ, rot.getOpposite());
            te.trySubscribe(te.tank.getTankType(), level, pos.getX(), pos.getY() - 1, pos.getZ(), ForgeDirection.DOWN);
        }

        if (te.isOn && !te.isSuspended) {

            if (te.state == 0) {
                te.delay--;
                if (te.delay <= 0) te.state = 1;
            }

            if (te.state == 1) {
                te.angle += 82.5F / 60F;

                if (te.angle >= 82.5F) {
                    te.angle = 82.5F;
                    te.state = 2;
                }
            } else if (te.state == 2) {
                te.angle -= 82.5F / 60F;

                if (te.angle <= 0F) {
                    te.angle = 0F;
                    te.state = 0;
                    te.delay = 200 + level.getRandom().nextInt(100);
                }
            }

            if (te.angle != 0) {
                Vec3 pivot = new Vec3(pos.getX() + 0.5 - dir.offsetX, pos.getY() + 0.5, pos.getZ() + 0.5 - dir.offsetZ);
                Vec3 upperArm = new Vec3(-dir.offsetX * 4, 0, -dir.offsetZ * 4);
                Vec3 lowerArm = new Vec3(-dir.offsetX * 4, 0, -dir.offsetZ * 4);
                if (dir.offsetZ != 0) {
                    upperArm = upperArm.xRot((float) Math.toRadians(82.5 - te.angle));
                    lowerArm = lowerArm.xRot((float) -Math.toRadians(82.5 - te.angle));
                }
                if (dir.offsetX != 0) {
                    upperArm = upperArm.zRot((float) Math.toRadians(82.5 - te.angle));
                    lowerArm = lowerArm.zRot((float) -Math.toRadians(82.5 - te.angle));
                }
                Vec3 armTip = new Vec3(-dir.offsetX * 2, 0, -dir.offsetZ * 2);

                double endX = pivot.x + upperArm.x + lowerArm.x + armTip.x;
                double endZ = pivot.z + upperArm.z + lowerArm.z + armTip.z;

                for (int i = -3; i <= 3; i++) {
                    int hitX = (int) Math.floor(endX + rot.offsetX * i);
                    int hitZ = (int) Math.floor(endZ + rot.offsetZ * i);
                    BlockPos hit = new BlockPos(hitX, pos.getY(), hitZ);
                    if (!level.hasChunkAt(hit)) continue;

                    BlockState b = level.getBlockState(hit);

                    if (b.isCollisionShapeFullBlock(level, hit)) {
                        te.state = 2;
                        break;
                    }

                    if (b.getBlock() instanceof DoublePlantBlock) {
                        // sunflower
                        if (b.is(Blocks.SUNFLOWER) && b.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER
                                && level.getRandom().nextInt(250) == 0) {
                            level.levelEvent(2001, hit, Block.getId(b));
                            te.dropItem(level, new ItemStack(Blocks.SUNFLOWER, 1));
                        }
                        // tall grass
                        if (b.is(Blocks.TALL_GRASS) && b.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER
                                && level.getRandom().nextInt(100) == 0) {
                            level.levelEvent(2001, hit, Block.getId(b));
                            te.dropItem(level, new ItemStack(Items.WHEAT_SEEDS, 1));
                        }
                        continue;
                    }

                    if (b.is(Blocks.SUGAR_CANE) || b.is(Blocks.CACTUS)) {
                        te.cutCane(server, b.getBlock(), hit);
                        continue;
                    }
                    // IGrowable also covers anything that accepts bone
                    // meal, so we have to handle actual crops last
                    if (b.getBlock() instanceof BonemealableBlock && !shouldIgnore(level, hit, b)) te.cutCrop(server, b, hit);
                }

                AABB box = new AABB(endX, pos.getY() + 0.5, endZ, endX, pos.getY() + 0.5, endZ)
                        .inflate(Math.abs(dir.offsetX * 0.5) + Math.abs(rot.offsetX * 4.5), 0.5,
                                Math.abs(dir.offsetZ * 0.5) + Math.abs(rot.offsetZ * 4.5));
                List<LivingEntity> affected = level.getEntitiesOfClass(LivingEntity.class, box);

                for (LivingEntity e : affected) {
                    if (e.isAlive() && Compat.hurt(e, server, ModDamageSources.turbofan(level), 100F)) {
                        if (e instanceof Enemy && !e.isAlive()) te.dropItem(level, new ItemStack(ModItems.nitra_small.get()));
                        level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                                SoundSource.BLOCKS, 2.0F, 0.95F + level.getRandom().nextFloat() * 0.2F);
                        int count = Math.min((int) Math.ceil(e.getMaxHealth() / 4), 250);
                        ParticleEffects.sawmillDamage(level, e, count * 4);
                    }
                }
            }
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState blockState, TileEntityMachineThresher te) {
        te.lastSpin = te.spin;

        if (te.isOn && !te.isSuspended) {
            if (te.angle > 0) te.spin += 15F;

            ForgeDirection dir = facing(blockState).getOpposite();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + dir.offsetX * 0.8125 + rot.offsetX * 0.375,
                    pos.getY() + 1.5625, pos.getZ() + 0.5 + dir.offsetZ * 0.8125 + rot.offsetZ * 0.375, 0, 0, 0);
        }

        te.tickAudio(level, pos);

        if (te.spin >= 360F) {
            te.spin -= 360F;
            te.lastSpin -= 360F;
        }

        te.prevAngle = te.angle;

        if (te.turnProgress > 0) {
            double d0 = wrapDegrees(te.syncAngle - te.angle);
            te.angle = (float) (te.angle + d0 / te.turnProgress);
            --te.turnProgress;
        } else {
            te.angle = te.syncAngle;
        }
    }

    /** 1.7: the diesel engine loop within 15 blocks of the player, kept alive while the arm works. */
    private void tickAudio(Level level, BlockPos pos) {
        // Typed as LocalPlayer on purpose (as TileEntityMachinePyroOven does): widening it to Player would make the
        // bytecode verifier load the client-only class when this class links on a dedicated server.
        LocalPlayer me = Minecraft.getInstance().player;
        boolean wanted = isOn && !isSuspended && me != null
                && me.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 15 * 15;
        if (wanted) {
            if (audio == null || !audio.isPlaying()) {
                audio = AudioSystem.getLoopedSound(ModProductionSounds.ENGINE.get(), SoundSource.BLOCKS,
                        pos.getX(), pos.getY(), pos.getZ(), 1.0F, 10F, 1.0F + level.getRandom().nextFloat() * 0.1F, 10);
                if (audio != null) audio.startSound();
            }
            if (audio != null) {
                audio.keepAlive();
                audio.updateVolume(this.getVolume(1F));
            }
        } else if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360D;
        if (wrapped >= 180D) wrapped -= 360D;
        if (wrapped < -180D) wrapped += 360D;
        return wrapped;
    }

    /** 1.7 {@code IGrowable.func_149851_a}: a plant that still accepts bone meal is left to grow. */
    public static boolean shouldIgnore(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof BonemealableBlock growable) {
            return Plants.isValidBonemealTarget(growable, level, pos, state);
        }
        return false;
    }

    /** Removes the top two blocks from a three block crop like cacti and sugar cane. */
    protected void cutCane(ServerLevel level, Block target, BlockPos base) {
        // people may be inclined to incorrectly place this thing one block above
        // the intended operating level, so we compensate for that
        int offset = level.getBlockState(base.below()).is(target) ? -1 : 0;

        // top to bottom
        for (int i = 2 + offset; i > offset; i--) {
            BlockPos at = base.above(i);
            BlockState b = level.getBlockState(at);
            if (b.isAir()) continue;
            level.levelEvent(2001, at, Block.getId(b));
            for (ItemStack drop : Block.getDrops(b, level, at, level.getBlockEntity(at), null, ItemStack.EMPTY)) dropItem(level, drop);
            level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Harvests and re-plants crops like wheat. */
    protected void cutCrop(ServerLevel level, BlockState b, BlockPos at) {
        level.levelEvent(2001, at, Block.getId(b));

        BlockState replacement = Blocks.AIR.defaultBlockState();
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(b, level, at, level.getBlockEntity(at), null, ItemStack.EMPTY));
        boolean replanted = false;

        for (ItemStack drop : drops) {
            if (!replanted && !drop.isEmpty()) {
                BlockState plant = plantStateFromDrop(level, at, drop);
                if (plant != null) {
                    replacement = plant;
                    replanted = true;
                    drop.shrink(1);
                }
            }
            if (!drop.isEmpty()) dropItem(level, drop);
        }

        // Apparently, until 1.14 full-grown wheat could sometimes drop no seeds at all
        // This is a quick and dirty workaround for that.
        if (b.is(Blocks.WHEAT) && !replanted) {
            replacement = Blocks.WHEAT.defaultBlockState();
        }

        level.setBlock(at, replacement, 3);
    }

    /** The old {@code IPlantable} test: a dropped seed whose plant can stand on the soil below. */
    private static @Nullable BlockState plantStateFromDrop(ServerLevel level, BlockPos pos, ItemStack drop) {
        if (!(drop.getItem() instanceof BlockItem blockItem)) return null;
        BlockState candidate = blockItem.getBlock().defaultBlockState();
        if (!(candidate.getBlock() instanceof CropBlock || candidate.getBlock() instanceof BonemealableBlock)) return null;
        if (!candidate.canSurvive(level, pos)) return null;
        return candidate;
    }

    protected void dropItem(Level level, ItemStack drop) {
        ForgeDirection dir = facing(getBlockState());
        double spawnX = worldPosition.getX() + 0.5 - dir.offsetX * 0.75;
        double spawnZ = worldPosition.getZ() + 0.5 - dir.offsetZ * 0.75;

        ItemEntity entityItem = new ItemEntity(level, spawnX, worldPosition.getY(), spawnZ, drop);
        entityItem.setPickUpDelay(10);
        // The old motion (dir.offsetX * -0.2 + 0.2, 0, dir.offsetZ * -0.2), including its X-only bias.
        entityItem.setDeltaMovement(dir.offsetX * -0.2 + 0.2, 0, dir.offsetZ * -0.2);
        level.addFreshEntity(entityItem);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.isOn);
        buf.writeBoolean(this.isSuspended);
        buf.writeFloat(this.angle);
        this.tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.isOn = buf.readBoolean();
        this.isSuspended = buf.readBoolean();
        this.syncAngle = buf.readFloat();
        this.turnProgress = 3; //use 3-ply for extra smoothness
        this.tank.deserialize(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.isOn = input.getBooleanOr("isOn", false);
        this.isSuspended = input.getBooleanOr("isSuspended", false);
        this.angle = input.getFloatOr("angle", 0F);
        this.state = input.getIntOr("state", 0);
        this.tank.readFromNBT(input, "t");
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isOn", this.isOn);
        output.putBoolean("isSuspended", this.isSuspended);
        output.putFloat("angle", this.angle);
        output.putInt("state", this.state);
        tank.writeToNBT(output, "t");
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
}
