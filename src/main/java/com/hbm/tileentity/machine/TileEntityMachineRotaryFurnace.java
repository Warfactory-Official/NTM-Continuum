package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.CrucibleUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.container.ContainerMachineRotaryFurnace;
import com.hbm.tileentity.ModProductionBlockEntities;

/**
 * The rotary furnace core: three unordered item inputs, a fluid-identifier slot, a solid-fuel slot,
 * steam in / spent steam out, and a molten-metal output poured out of the side spout.
 *
 * <p>Ported 1:1 from the original {@code TileEntityMachineRotaryFurnace}. The old
 * {@code TileEntityMachinePolluting} base is inlined as the three smoke tanks; a smoke overflow only sets
 * {@code isVenting} because the port has no pollution store yet ({@link PollutionHandler}). The JSON
 * machine-config hooks ({@code IConfigurableMachine}) and the fluid copy tool are not carried.
 */
public class TileEntityMachineRotaryFurnace extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2 {

    public static final int maxOutput = MaterialShapes.BLOCK.quantity * 16;
    /** 1.7 {@code TileEntityMachinePolluting(5, 50)}. */
    private static final int SMOKE_BUFFER = 50;
    /** Old {@code AuxParticlePacketNT "foundry"} stream: kept on screen this long after the last pour. */
    private static final int STREAM_LIFETIME = 15;

    public final FluidTankNTM[] tanks;
    public final FluidTankNTM smoke = new FluidTankNTM(Fluids.SMOKE, SMOKE_BUFFER);
    public final FluidTankNTM smokeLeaded = new FluidTankNTM(Fluids.SMOKE_LEADED, SMOKE_BUFFER);
    public final FluidTankNTM smokePoison = new FluidTankNTM(Fluids.SMOKE_POISON, SMOKE_BUFFER);

    public boolean isProgressing;
    public float progress;
    public int burnTime;
    public double burnHeat = 1D;
    public int maxBurnTime;
    public int steamUsed = 0;
    public boolean isVenting;
    public @Nullable MaterialStack output;

    public int anim;
    public int lastAnim;

    /** Pour-stream sync for the renderer: molten colour, length and a lifetime that decays server-side. */
    public int pourColor = -1;
    public float pourLen;
    private int pourTimer;

    /** Given this has no heat, the heat mod instead affects the progress per fuel. */
    public static final ModuleBurnTime burnModule = new ModuleBurnTime()
            .setCokeTimeMod(1.25)
            .setRocketTimeMod(1.5)
            .setSolidTimeMod(1.5)
            .setBalefireTimeMod(1.5)

            .setSolidHeatMod(1.5)
            .setRocketHeatMod(3)
            .setBalefireHeatMod(10);

    public TileEntityMachineRotaryFurnace(BlockPos pos, BlockState state) {
        super(ModProductionBlockEntities.ROTARY_FURNACE.get(), pos, state, 5);
        tanks = new FluidTankNTM[3];
        tanks[0] = new FluidTankNTM(Fluids.NONE, 16_000);
        tanks[1] = new FluidTankNTM(Fluids.STEAM, 12_000);
        tanks[2] = new FluidTankNTM(Fluids.SPENTSTEAM, 120);
        RotaryFurnaceRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.machineRotaryFurnace";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRotaryFurnace te) {
        ForgeDirection dir = MachineCore.facing(state);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);

        te.setTankType(3);

        for (DirPos con : te.getSteamPos()) {
            if (te.tanks[2].getFill() > 0) te.tryProvide(te.tanks[2], level, con);
        }

        if (te.smoke.getFill() > 0) {
            for (DirPos con : te.getSmokePos()) te.tryProvide(te.smoke, level, con);
        }

        if (te.pourTimer > 0) te.pourTimer--; else te.pourColor = -1;

        if (te.output != null) {
            int prev = te.output.amount;
            Vec3[] impact = new Vec3[1];
            double sx = pos.getX() + 0.5D + rot.offsetX * 2.875D;
            double sz = pos.getZ() + 0.5D + rot.offsetZ * 2.875D;
            MaterialStack leftover = CrucibleUtil.pourSingleStack(level, sx, pos.getY() + 1.25D, sz, 6, true,
                    te.output, MaterialShapes.INGOT.quantity, impact);
            te.output = leftover;

            if (prev != te.output.amount) {
                te.pourColor = leftover.material.moltenColor;
                double impactY = impact[0] != null ? impact[0].y : pos.getY() - 5D;
                te.pourLen = Math.max(1F, pos.getY() + 1 - (float) (Math.ceil(impactY) - 1.125));
                te.pourTimer = STREAM_LIFETIME;
            }

            if (te.output.amount <= 0) te.output = null;
        }

        RotaryFurnaceRecipe recipe = RotaryFurnaceRecipes.getRecipe(
                te.inventory.getStackInSlot(0), te.inventory.getStackInSlot(1), te.inventory.getStackInSlot(2));
        te.isProgressing = false;

        if (recipe != null) {
            ItemStack fuel = te.inventory.getStackInSlot(4);
            if (te.burnTime <= 0 && !fuel.isEmpty() && Compat.burnDuration(level, fuel) > 0) {
                te.burnHeat = burnModule.getHeatMod(fuel);
                te.maxBurnTime = te.burnTime = burnModule.getBurnTime(level, fuel) / 2;
                te.inventory.extractItem(4, 1, false);
                te.markChanged();
            }

            float processSpeed = Math.max((float) te.burnHeat, 1);
            float steamUseMult = (float) (10 * Math.log10(processSpeed) + 1);

            if (te.canProcess(recipe, steamUseMult)) {
                te.progress += processSpeed / recipe.duration;

                te.tanks[1].setFill((int) (te.tanks[1].getFill() - recipe.steam * steamUseMult));
                te.steamUsed += (int) (recipe.steam * steamUseMult);
                te.isProgressing = true;

                if (te.progress >= 1F) {
                    te.progress -= 1F;
                    te.consumeItems(recipe);

                    if (te.output == null) {
                        te.output = recipe.output.copy();
                    } else {
                        te.output.amount += recipe.output.amount;
                    }
                    te.setChanged();
                }

                if (te.burnTime > 0) {
                    te.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 10F);
                    te.burnTime--;
                }

            } else {
                te.progress = 0;
            }

            if (te.steamUsed >= 100) {
                int steamReturn = te.steamUsed / 100;
                int canReturn = te.tanks[2].getMaxFill() - te.tanks[2].getFill();
                int doesReturn = Math.min(steamReturn, canReturn);
                te.steamUsed -= doesReturn * 100;
                te.tanks[2].setFill(te.tanks[2].getFill() + doesReturn);
            }

        } else {
            te.progress = 0;
        }

        // 1.7 clears the venting flag BEFORE packing, so the client never sees it and the exhaust smoke
        // particle below never fires. Kept exactly so; the flag still records the overflow server-side.
        te.isVenting = false;

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRotaryFurnace te) {
        ForgeDirection dir = MachineCore.facing(state);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);

        if (te.burnTime > 0 && net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 25 * 25) {
            var rand = level.getRandom();
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5 + dir.offsetX * 0.5 + rot.offsetX + rand.nextGaussian() * 0.25,
                    pos.getY() + 0.375,
                    pos.getZ() + 0.5 + dir.offsetZ * 0.5 + rot.offsetZ + rand.nextGaussian() * 0.25, 0, 0, 0);
        }

        if (te.isVenting && level.getGameTime() % 2 == 0) {
            CompoundTag fx = new CompoundTag();
            fx.putFloat("lift", 10F);
            fx.putFloat("base", 0.25F);
            fx.putFloat("max", 2.5F);
            fx.putInt("life", 100 + level.getRandom().nextInt(20));
            fx.putInt("color", 0x202020);
            HbmEffectNT.Tower.summonParticle(level, pos.getX() + 0.5 + rot.offsetX, pos.getY() + 5, pos.getZ() + 0.5 + rot.offsetZ, fx);
        }

        te.lastAnim = te.anim;
        if (te.isProgressing) {
            te.anim += (int) Math.max(burnModule.getHeatMod(te.inventory.getStackInSlot(4)), 1);
        }
    }

    /** 1.7 {@code FluidTank.setType(slot, slots)}: a fluid identifier in the slot retypes the tank and empties it on change. */
    private void setTankType(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidId id)) return;
        FluidType type = id.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        if (type != null && tanks[0].getTankType() != type) {
            tanks[0].setTankType(type);
            tanks[0].setFill(0);
        }
    }

    public DirPos[] getSteamPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x - dir.offsetX * 2 - rot.offsetX * 2, y, z - dir.offsetZ * 2 - rot.offsetZ * 2, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 - rot.offsetX, y, z - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite())
        };
    }

    public DirPos[] getFluidPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.offsetX + rot.offsetX * 3, y, z + dir.offsetZ + rot.offsetZ * 3, rot),
                new DirPos(x - dir.offsetX + rot.offsetX * 3, y, z - dir.offsetZ + rot.offsetZ * 3, rot)
        };
    }

    /** 1.7 writes this one inline: {@code tryProvide(smoke, x + rot, y + 5, z + rot, POS_Y)} above the exhaust cell. */
    public DirPos[] getSmokePos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + rot.offsetX, y + 5, z + rot.offsetZ, ForgeDirection.UP)
        };
    }

    public boolean canProcess(RotaryFurnaceRecipe recipe, float steamUseMult) {
        if (this.burnTime <= 0) return false;

        if (recipe.fluid != null) {
            if (this.tanks[0].getTankType() != recipe.fluid.type) return false;
            if (this.tanks[0].getFill() < recipe.fluid.fill) return false;
        }

        if (tanks[1].getFill() < recipe.steam * steamUseMult) return false;
        if (tanks[2].getMaxFill() - tanks[2].getFill() < recipe.steam * steamUseMult / 100) return false;
        if (this.steamUsed > 100) return false;

        if (this.output != null) {
            if (this.output.material != recipe.output.material) return false;
            if (this.output.amount + recipe.output.amount > maxOutput) return false;
        }

        return true;
    }

    public void consumeItems(RotaryFurnaceRecipe recipe) {
        for (AStack aStack : recipe.ingredients) {
            for (int i = 0; i < 3; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty() && aStack.matchesRecipe(stack, true) && stack.getCount() >= aStack.stacksize) {
                    inventory.extractItem(i, aStack.stacksize, false);
                    break;
                }
            }
        }

        if (recipe.fluid != null) {
            this.tanks[0].setFill(tanks[0].getFill() - recipe.fluid.fill);
        }
    }

    /** 1.7 {@code TileEntityMachinePolluting.pollute}, with the rotary furnace's venting override. */
    public void pollute(PollutionType type, float amount) {
        FluidTankNTM tank = type == PollutionType.SOOT ? smoke : type == PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;

        int fluidAmount = (int) Math.ceil(amount * 100);
        tank.setFill(tank.getFill() + fluidAmount);

        if (tank.getFill() > tank.getMaxFill()) {
            int overflow = tank.getFill() - tank.getMaxFill();
            tank.setFill(tank.getMaxFill());
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);
            this.isVenting = true;
        }
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return new int[0]; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return slot < 3 || slot == 4; }

    /**
     * The original's position-aware sided access: the three back cells feed the inputs (red / yellow /
     * green) from the back face, and the fuel cell takes fuel from the front face.
     */
    public int[] getAccessibleSlotsFromSide(BlockPos cell, Direction side) {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos core = worldPosition;

        // Red
        if (side == dir.getOpposite().toDirection() && cell.equals(core.offset(-dir.offsetX - rot.offsetX * 2, 0, -dir.offsetZ - rot.offsetZ * 2))) return new int[]{0};
        // Yellow
        if (side == dir.getOpposite().toDirection() && cell.equals(core.offset(-dir.offsetX - rot.offsetX, 0, -dir.offsetZ - rot.offsetZ))) return new int[]{1};
        // Green
        if (side == dir.getOpposite().toDirection() && cell.equals(core.offset(-dir.offsetX, 0, -dir.offsetZ))) return new int[]{2};
        // Fuel
        if (side == dir.toDirection() && cell.equals(core.offset(dir.offsetX - rot.offsetX, 0, dir.offsetZ - rot.offsetZ))) return new int[]{4};

        return new int[]{};
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tanks[0], tanks[1], tanks[2], smoke}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[2], smoke}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0], tanks[1]}; }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }
    @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineRotaryFurnace(containerId, playerInventory, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && com.hbm.inventory.container.Containers.inRangeOfMultiblock(level, worldPosition, player);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        tanks[2].serialize(buf);
        buf.writeBoolean(isVenting);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
        buf.writeInt(burnTime);
        buf.writeInt(maxBurnTime);

        if (this.output != null) {
            buf.writeBoolean(true);
            buf.writeInt(this.output.material.id);
            buf.writeInt(this.output.amount);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(pourColor);
        buf.writeFloat(pourLen);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        tanks[2].deserialize(buf);
        isVenting = buf.readBoolean();
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
        burnTime = buf.readInt();
        maxBurnTime = buf.readInt();

        if (buf.readBoolean()) {
            this.output = new MaterialStack(Mats.matById.get(buf.readInt()), buf.readInt());
        } else {
            this.output = null;
        }
        pourColor = buf.readInt();
        pourLen = buf.readFloat();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.tanks[0].readFromNBT(input, "t0");
        this.tanks[1].readFromNBT(input, "t1");
        this.tanks[2].readFromNBT(input, "t2");
        this.smoke.readFromNBT(input, "smoke0");
        this.smokeLeaded.readFromNBT(input, "smoke1");
        this.smokePoison.readFromNBT(input, "smoke2");
        this.progress = input.getFloatOr("prog", 0F);
        this.burnTime = input.getIntOr("burn", 0);
        this.burnHeat = input.getDoubleOr("heat", 1D);
        this.maxBurnTime = input.getIntOr("maxBurn", 0);
        int outType = input.getIntOr("outType", -1);
        if (outType >= 0 && Mats.matById.get(outType) != null) {
            this.output = new MaterialStack(Mats.matById.get(outType), input.getIntOr("outAmount", 0));
        } else {
            this.output = null;
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        this.tanks[0].writeToNBT(output, "t0");
        this.tanks[1].writeToNBT(output, "t1");
        this.tanks[2].writeToNBT(output, "t2");
        this.smoke.writeToNBT(output, "smoke0");
        this.smokeLeaded.writeToNBT(output, "smoke1");
        this.smokePoison.writeToNBT(output, "smoke2");
        output.putFloat("prog", progress);
        output.putInt("burn", burnTime);
        output.putDouble("heat", burnHeat);
        output.putInt("maxBurn", maxBurnTime);
        if (this.output != null) {
            output.putInt("outType", this.output.material.id);
            output.putInt("outAmount", this.output.amount);
        }
    }
}
