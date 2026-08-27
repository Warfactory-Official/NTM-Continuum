package com.hbm.tileentity.machine;

import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.items.ModItems;
import com.hbm.entity.projectile.EntitySawblade;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.sound.ModSounds;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.ModDamageSources;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Heat-powered sawmill core, ported from the old three-slot TileEntitySawmill. */
public class TileEntitySawmill extends TileEntityMachineBase implements WorldlyContainer {

    public static final int PROCESSING_TIME = 600;
    public static final double DIFFUSION = 0.1D;

    private static final int[] ACCESSIBLE_SLOTS = {0, 1, 2};

    public int heat;
    public int progress;
    public boolean hasBlade = true;
    public float spin;
    public float lastSpin;

    private int overspeed;
    private int warnCooldown;
    private int syncHeat;

    private static final AABB BLADE_BOX = new AABB(-1D, 0.375D, -1D, -0.875D, 2.375D, 1D);

    public TileEntitySawmill(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SAWMILL.get(), pos, state, 3);
    }

    @Override
    protected NTMItemHandler createInventory(int slotCount) {
        return new NTMItemHandler(slotCount) {
            @Override
            public int getSlotLimit(int slot) {
                return slot == 0 ? 1 : super.getSlotLimit(slot);
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                if (slot == 0 && stack.getCount() > 1) stack = stack.copyWithCount(1);
                super.setStackInSlot(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                TileEntitySawmill.this.setChanged();
                TileEntitySawmill.this.onSlotContentsChanged(slot, getStackInSlot(slot));
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileEntitySawmill.this.isItemValidForSlot(slot, stack);
            }
        };
    }

    @Override
    public String getDefaultName() {
        return "container.sawmill";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySawmill sawmill) {
        if (sawmill.hasBlade) {
            sawmill.tryPullHeat(level, pos);

            if (sawmill.warnCooldown > 0) sawmill.warnCooldown--;

            if (sawmill.heat >= 100) {
                ItemStack result = sawmill.getOutput(level, sawmill.inventory.getStackInSlot(0));
                if (!result.isEmpty()) {
                    sawmill.progress += sawmill.heat / 10;
                    if (sawmill.progress >= PROCESSING_TIME) {
                        sawmill.progress = 0;
                        sawmill.inventory.setStackInSlot(0, ItemStack.EMPTY);
                        sawmill.inventory.setStackInSlot(1, result);

                        if (!result.is(ModItems.powder_sawdust.get())) {
                            int chance = result.is(net.minecraft.world.item.Items.STICK) ? 10 : 50;
                            if (level.getRandom().nextInt(100) < chance) {
                                sawmill.inventory.setStackInSlot(2,
                                        new ItemStack(com.hbm.items.ModItems.powder_sawdust.get()));
                            }
                        }
                        sawmill.setChanged();
                    }
                } else {
                    sawmill.progress = 0;
                }

                damageOperatingBlade(level, pos, state);

                if (sawmill.heat > 300) {
                    sawmill.overspeed++;
                    if (sawmill.overspeed > 60 && sawmill.warnCooldown == 0) {
                        sawmill.warnCooldown = 100;
                        level.playSound(null, pos, ModSounds.WARN_OVERSPEED.get(),
                                net.minecraft.sounds.SoundSource.BLOCKS, 2F, 1F);
                    }
                    if (sawmill.overspeed > 300) {
                        sawmill.hasBlade = false;
                        level.explode(null, pos.getX() + 0.5D, pos.getY() + 1D, pos.getZ() + 0.5D,
                                5F, false, Level.ExplosionInteraction.NONE);
                        int orientation = state.getValue(com.hbm.blocks.BlockDummyable.META)
                                - com.hbm.blocks.BlockDummyable.offset;
                        ForgeDirection direction = ForgeDirection.getOrientation(orientation);
                        ForgeDirection rotation = direction.getRotation(ForgeDirection.DOWN);
                        EntitySawblade blade = new EntitySawblade(level,
                                pos.getX() + 0.5D + direction.offsetX,
                                pos.getY() + 1D,
                                pos.getZ() + 0.5D + direction.offsetZ, orientation);
                        blade.setDeltaMovement(rotation.offsetX,
                                1D + (sawmill.heat - 100) * 0.0001D, rotation.offsetZ);
                        level.addFreshEntity(blade);
                        sawmill.setChanged();
                        sawmill.overspeed = 0;
                    }
                } else {
                    sawmill.overspeed = 0;
                }
            } else {
                sawmill.overspeed = 0;
                sawmill.warnCooldown = 0;
            }
        }

        sawmill.syncHeat = sawmill.heat;
        sawmill.networkPackNT();
        sawmill.heat = 0;
    }

    /** The running blade's fixed damage volume, rotated exactly like the old BlockDummyable helper. */
    private static void damageOperatingBlade(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel server)) return;

        ForgeDirection facing = ForgeDirection.getOrientation(
                state.getValue(com.hbm.blocks.BlockDummyable.META) - com.hbm.blocks.BlockDummyable.offset);
        ForgeDirection rotation = facing.getRotation(ForgeDirection.UP);
        AABB box = switch (rotation) {
            case NORTH -> BLADE_BOX;
            case EAST -> new AABB(-BLADE_BOX.maxZ, BLADE_BOX.minY, BLADE_BOX.minX,
                    -BLADE_BOX.minZ, BLADE_BOX.maxY, BLADE_BOX.maxX);
            case SOUTH -> new AABB(-BLADE_BOX.maxX, BLADE_BOX.minY, -BLADE_BOX.maxZ,
                    -BLADE_BOX.minX, BLADE_BOX.maxY, -BLADE_BOX.minZ);
            case WEST -> new AABB(BLADE_BOX.minZ, BLADE_BOX.minY, -BLADE_BOX.maxX,
                    BLADE_BOX.maxZ, BLADE_BOX.maxY, -BLADE_BOX.minX);
            default -> BLADE_BOX;
        };
        box = box.move(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (entity.isAlive() && Compat.hurt(entity, server, ModDamageSources.turbofan(server), 100F)) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F,
                        0.95F + level.getRandom().nextFloat() * 0.2F);
                int count = Math.min((int) Math.ceil(entity.getMaxHealth() / 4F), 250) * 4;
                ParticleEffects.sawmillDamage(level, entity, count);
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntitySawmill sawmill) {
        float momentum = sawmill.heat * 25F / 300F;
        sawmill.lastSpin = sawmill.spin;
        sawmill.spin += momentum;
        if (sawmill.spin >= 360F) {
            sawmill.spin -= 360F;
            sawmill.lastSpin -= 360F;
        }
    }

    private void tryPullHeat(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below instanceof IHeatSource source) {
            int heatSource = (int) (source.getHeatStored() * DIFFUSION);
            if (heatSource > 0) {
                source.useUpHeat(heatSource);
                heat += heatSource;
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    public ItemStack getOutput(Level level, ItemStack input) {
        return SawmillRecipes.getOutput(level, input);
    }

    public boolean canAcceptInput(ItemStack input) {
        if (!inventory.getStackInSlot(0).isEmpty() || !inventory.getStackInSlot(1).isEmpty()
                || !inventory.getStackInSlot(2).isEmpty() || !SawmillRecipes.isValidInput(input)) {
            return false;
        }
        if (input.is(ItemTags.LOGS)) {
            // Clients do not have the server recipe access used by getOutput; the server rechecks it.
            return level != null && (level.isClientSide() || !getOutput(level, input).isEmpty());
        }
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && canAcceptInput(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot > 0;
    }

    public int getProgressScaled(int width) {
        return progress * width / PROCESSING_TIME;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(syncHeat);
        buf.writeInt(progress);
        buf.writeBoolean(hasBlade);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        progress = buf.readInt();
        hasBlade = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("progress", progress);
        output.putBoolean("hasBlade", hasBlade);
        output.putInt("overspeed", overspeed);
        output.putInt("warnCooldown", warnCooldown);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        progress = input.getIntOr("progress", 0);
        hasBlade = input.getBooleanOr("hasBlade", true);
        overspeed = input.getIntOr("overspeed", 0);
        warnCooldown = input.getIntOr("warnCooldown", 0);
    }
}
