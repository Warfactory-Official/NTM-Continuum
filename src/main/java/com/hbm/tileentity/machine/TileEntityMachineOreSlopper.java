package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.particle.ParticleGiblet;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.inventory.container.ContainerOreSlopper;
import com.hbm.tileentity.ModProduction2BlockEntities;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * 1.7 {@code TileEntityMachineOreSlopper}: the bedrock ore processor. Slots: 0 battery, 1 fluid identifier,
 * 2 bedrock ore input, 3-8 outputs, 9-10 upgrades. Each cycle turns one {@code bedrock_ore_base} chunk
 * plus 1,000 mB of water into slop and a fractional amount of each of the six graded BASE ores, paid out
 * whenever a whole item accumulates.
 */
public class TileEntityMachineOreSlopper extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    public long power;
    public static final long maxPower = 100_000;

    public static final int waterUsedBase = 1_000;
    public int waterUsed = waterUsedBase;
    public static final long consumptionBase = 200;
    public long consumption = consumptionBase;

    public float progress;
    public boolean processing;

    public SlopperAnimation animation = SlopperAnimation.LOWERING;
    public float slider;
    public float prevSlider;
    public float bucket;
    public float prevBucket;
    public float blades;
    public float prevBlades;
    public float fan;
    public float prevFan;
    public int delay;

    public final FluidTankNTM[] tanks;
    /** Accumulated ore per registered type ({@code BedrockOreType.ordinal()}; an addon's types included). */
    public final double[] ores = new double[BedrockOreType.values().size()];

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    private static final int[] SLOT_ACCESS = new int[] {2, 3, 4, 5, 6, 7, 8};

    public TileEntityMachineOreSlopper(BlockPos pos, BlockState state) {
        super(ModProduction2BlockEntities.ORE_SLOPPER.get(), pos, state, 11);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.WATER, 16_000);
        tanks[1] = new FluidTankNTM(Fluids.SLOP, 16_000);
    }

    @Override
    public String getDefaultName() {
        return "container.machineOreSlopper";
    }

    public enum SlopperAnimation {
        LOWERING, LIFTING, MOVE_SHREDDER, DUMPING, MOVE_BUCKET
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineOreSlopper te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        ForgeDirection dir = MachineCore.facing(state);

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);

        te.setTankTypeFromSlot(1);
        FluidType conversion = te.getFluidOutput(te.tanks[0].getTankType());
        if (conversion != null) te.tanks[1].setTankType(conversion);

        te.processing = false;

        te.upgradeManager.checkSlots(te.inventory, 9, 10);
        int speed = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int efficiency = te.upgradeManager.getLevel(UpgradeType.EFFECT);

        te.consumption = consumptionBase + (consumptionBase * speed) / 2 + (consumptionBase * efficiency);

        if (te.canSlop()) {
            te.power -= te.consumption;
            te.progress += 1F / (600 - speed * 150);
            te.processing = true;
            boolean markDirty = false;

            while (te.progress >= 1F && te.canSlop()) {
                te.progress -= 1F;

                ItemStack input = te.inventory.getStackInSlot(2);
                // hbm-space: the types the raw ore carries (its body's), else HBM's six
                for (BedrockOreType type : ItemBedrockOreBase.types(input)) {
                    if (type.ordinal() >= te.ores.length) continue;
                    te.ores[type.ordinal()] += (ItemBedrockOreBase.getOreAmount(input, type) * (1D + efficiency * 0.1));
                }

                te.inventory.extractItem(2, 1, false);
                te.tanks[0].setFill(te.tanks[0].getFill() - te.waterUsed);
                te.tanks[1].setFill(te.tanks[1].getFill() + te.waterUsed);
                markDirty = true;
            }

            if (markDirty) te.setChanged();

            if (level instanceof ServerLevel serverLevel) {
                int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                List<Entity> entities = level.getEntitiesOfClass(Entity.class,
                        new AABB(x - 0.5, y + 1, z - 0.5, x + 1.5, y + 3, z + 1.5).move(dir.offsetX, 0, dir.offsetZ));

                for (Entity e : entities) {
                    Compat.hurt(e, serverLevel, ModDamageSources.turbofan(level), 1000F);

                    if (!e.isAlive() && e instanceof LivingEntity) {
                        ParticleCreators.giblets(serverLevel, e, ParticleGiblet.TYPE_MEAT, 5);
                        // 1.7 NTMSounds.VANILLA_GIB = mob.zombie.woodbreak
                        level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                                SoundSource.BLOCKS, 2.0F, 0.95F + level.getRandom().nextFloat() * 0.2F);
                    }
                }
            }
        } else {
            te.progress = 0;
        }

        for (BedrockOreType type : BedrockOreType.values()) {
            if (type.ordinal() >= te.ores.length || te.ores[type.ordinal()] < 1) continue;
            ItemStack output = new ItemStack(ModProduction2Blocks.bedrockOreBase(type));
            outer:
            while (te.ores[type.ordinal()] >= 1) {
                for (int i = 3; i <= 8; i++) {
                    ItemStack slot = te.inventory.getStackInSlot(i);
                    if (!slot.isEmpty() && slot.getItem() == output.getItem() && slot.getCount() < output.getMaxStackSize()) {
                        slot.grow(1);
                        te.inventory.setStackInSlot(i, slot);
                        te.ores[type.ordinal()] -= 1F;
                        continue outer;
                    }
                }
                for (int i = 3; i <= 8; i++) {
                    if (te.inventory.getStackInSlot(i).isEmpty()) {
                        te.inventory.setStackInSlot(i, output.copy());
                        te.ores[type.ordinal()] -= 1F;
                        continue outer;
                    }
                }
                break;
            }
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineOreSlopper te) {
        ForgeDirection dir = MachineCore.facing(state);

        te.prevSlider = te.slider;
        te.prevBucket = te.bucket;
        te.prevBlades = te.blades;
        te.prevFan = te.fan;

        if (te.processing) {

            te.blades += 15F;
            te.fan += 35F;

            if (te.blades >= 360) {
                te.blades -= 360;
                te.prevBlades -= 360;
            }

            if (te.fan >= 360) {
                te.fan -= 360;
                te.prevFan -= 360;
            }

            // 1.7: only when the local player is within 50 blocks; the nearest client-side player stands in for "me".
            if (te.animation == SlopperAnimation.DUMPING
                    && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 4, pos.getZ() + 0.5, 50, false) != null) {
                var rand = level.getRandom();
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.IRON_BLOCK.defaultBlockState()),
                        pos.getX() + 0.5 + dir.offsetX + rand.nextGaussian() * 0.25,
                        pos.getY() + 4.25,
                        pos.getZ() + 0.5 + dir.offsetZ + rand.nextGaussian() * 0.25,
                        0, -0.2D, 0);
            }

            if (te.delay > 0) {
                te.delay--;
                return;
            }

            switch (te.animation) {
                case LOWERING -> {
                    te.bucket += 1F / 40F;
                    if (te.bucket >= 1F) {
                        te.bucket = 1F;
                        te.animation = SlopperAnimation.LIFTING;
                        te.delay = 20;
                    }
                }
                case LIFTING -> {
                    te.bucket -= 1F / 40F;
                    if (te.bucket <= 0) {
                        te.bucket = 0F;
                        te.animation = SlopperAnimation.MOVE_SHREDDER;
                        te.delay = 10;
                    }
                }
                case MOVE_SHREDDER -> {
                    te.slider += 1 / 50F;
                    if (te.slider >= 1F) {
                        te.slider = 1F;
                        te.animation = SlopperAnimation.DUMPING;
                        te.delay = 60;
                    }
                }
                case DUMPING -> te.animation = SlopperAnimation.MOVE_BUCKET;
                case MOVE_BUCKET -> {
                    te.slider -= 1 / 50F;
                    if (te.slider <= 0F) {
                        te.animation = SlopperAnimation.LOWERING;
                        te.delay = 10;
                    }
                }
            }
        }
    }

    /** 1.7 {@code FluidTank.setType(slot, slots)}: a fluid identifier in the slot retypes the tank. */
    private void setTankTypeFromSlot(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.getItem() instanceof ItemFluidId) {
            FluidType type = ItemFluidId.getFluid(stack);
            if (type != tanks[0].getTankType()) tanks[0].setTankType(type);
        }
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX * 3, y, z + dir.offsetZ * 3, dir),
                    PortDef.make(x - dir.offsetX * 3, y, z - dir.offsetZ * 3, dir.getOpposite()),
                    PortDef.make(x + rot.offsetX, y, z + rot.offsetZ, rot),
                    PortDef.make(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                    // 1.7 mixes up the Z terms of these two, so they sit elsewhere for each facing
                    PortDef.make(x + dir.offsetX * 2 + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ * 2, rot),
                    PortDef.make(x + dir.offsetX * 2 - rot.offsetX, y, z + dir.offsetZ - rot.offsetZ * 2, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, rot),
                    PortDef.make(x - dir.offsetX * 2 - rot.offsetX, y, z - dir.offsetZ * 2 - rot.offsetZ, rot.getOpposite()),
            };
        }
        return cachedPorts;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.getItem() instanceof ItemFluidId;
        if (slot == 9 || slot == 10) return Library.isMachineUpgrade(stack);
        if (slot >= 3 && slot <= 8) return false;
        return slot == 2 && stack.is(ModOreBlocks.bedrock_ore_base.get());
    }

    @Override public int[] getSlotsForFace(Direction side) { return SLOT_ACCESS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return SLOT_ACCESS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 2 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 3 && slot <= 8; }

    @Override protected int[] getUpgradeSlotRange() { return new int[]{9, 10}; }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(consumption);
        buf.writeFloat(progress);
        buf.writeBoolean(processing);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.consumption = buf.readLong();
        this.progress = buf.readFloat();
        this.processing = buf.readBoolean();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);
        this.power = nbt.getLongOr("power", 0L);
        this.progress = nbt.getFloatOr("progress", 0F);
        tanks[0].readFromNBT(nbt, "water");
        tanks[1].readFromNBT(nbt, "slop");
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);
        nbt.putLong("power", power);
        nbt.putFloat("progress", progress);
        tanks[0].writeToNBT(nbt, "water");
        tanks[1].writeToNBT(nbt, "slop");
    }

    public boolean canSlop() {
        if (this.getFluidOutput(tanks[0].getTankType()) == null) return false;
        if (tanks[0].getFill() < waterUsed) return false;
        if (tanks[1].getFill() + waterUsed > tanks[1].getMaxFill()) return false;
        if (power < consumption) return false;

        ItemStack input = inventory.getStackInSlot(2);
        return !input.isEmpty() && input.is(ModOreBlocks.bedrock_ore_base.get());
    }

    public @Nullable FluidType getFluidOutput(FluidType input) {
        if (input == Fluids.WATER) return Fluids.SLOP;
        return null;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tanks[0]}; }

    @Override public Component getDisplayName() { return Component.translatable("container.machineOreSlopper"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerOreSlopper(containerId, playerInventory, this);
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.EFFECT;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModProduction2Blocks.MACHINE_ORE_SLOPPER.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_EFFICIENCY, "+" + (level * 10) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.EFFECT, 3);
        return upgrades;
    }
}
