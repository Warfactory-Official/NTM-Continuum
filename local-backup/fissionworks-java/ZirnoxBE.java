package fissionworks;

import fissionworks.sim.Zirnox;
import fissionworks.sim.Zirnox.Fuel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Arrays;

/** Owns the ZIRNOX model: steps it every tick, serves the fluid ports, handles console commands. */
public class ZirnoxBE extends BlockEntity implements Console {
    public static final int OP_LOAD = 0, OP_UNLOAD = 1, OP_RODS = 2, OP_VENT = 3, OP_CANISTER = 4, OP_BUCKET = 5;
    public static final int CANISTER_CO2 = 4000, BUCKET_WATER = 1000, STEAM_PUSH = 500;

    private Zirnox z = new Zirnox();
    private int ticks, lastHash;
    private final Port[] ports = {new Port(0), new Port(1), new Port(2)};

    public ZirnoxBE(BlockPos pos, BlockState state) {
        super(FissionWorks.ZIRNOX_BE.get(), pos, state);
    }

    public Zirnox model() {
        return z;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ZirnoxBE be) {
        if (level.isClientSide) {
            return;
        }
        be.z.step();
        be.pushSteam(level, pos);
        if (be.z.melted()) {
            level.removeBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6f,
                    Level.ExplosionInteraction.BLOCK);
        } else if (++be.ticks % 10 == 0 && Arrays.hashCode(be.z.toInts()) != be.lastHash) {
            be.sync();
        }
    }

    /** Pushes steam into whatever fluid handler sits on top, a little each tick. */
    private void pushSteam(Level level, BlockPos pos) {
        if (z.steam() <= 0) {
            return;
        }
        IFluidHandler out = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.above(), Direction.DOWN);
        if (out != null) {
            int n = out.fill(new FluidStack(ModFluids.STEAM.get(), Math.min(z.steam(), STEAM_PUSH)),
                    IFluidHandler.FluidAction.EXECUTE);
            z.drainSteam(n);
        }
    }

    @Override
    public void command(Player player, int op, int a, int b) {
        switch (op) {
            case OP_LOAD -> load(player, a);
            case OP_UNLOAD -> unload(player, a);
            case OP_RODS -> toggle(player);
            case OP_VENT -> z.vent();
            case OP_CANISTER -> {
                ItemStack held = player.getMainHandItem();
                if (held.is(FissionWorks.CANISTER.get()) && z.co2() + CANISTER_CO2 <= Zirnox.CO2_CAP) {
                    z.fillCo2(CANISTER_CO2);
                    held.shrink(1);
                }
            }
            case OP_BUCKET -> {
                ItemStack held = player.getMainHandItem();
                if (held.is(Items.WATER_BUCKET) && z.water() + BUCKET_WATER <= Zirnox.WATER_CAP) {
                    z.fillWater(BUCKET_WATER);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                }
            }
            default -> { }
        }
        sync();
    }

    private void toggle(Player player) {
        boolean want = !z.running();
        if (!z.setRunning(want) && want) {
            int why = z.melted() ? 5 : z.startBlock();
            player.displayClientMessage(Component.translatable("message.fissionworks.zirnox.block." + why), true);
        }
    }

    private void load(Player player, int slot) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof FuelRodItem rod
                && z.load(slot, rod.fuel(), held.getDamageValue() / (double) FuelRodItem.LIFE)) {
            if (!player.isCreative()) {
                held.shrink(1);
            }
        }
    }

    private void unload(Player player, int slot) {
        ItemStack out = stackFor(z.fuel(slot), z.burn(slot));
        if (!out.isEmpty() && z.unload(slot) != Fuel.NONE && !player.getInventory().add(out)) {
            player.drop(out, false);
        }
    }

    public void dropFuel(Level level, BlockPos pos) {
        for (int s = 0; s < Zirnox.SLOTS; s++) {
            ItemStack out = stackFor(z.fuel(s), z.burn(s));
            if (!out.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, out);
            }
        }
    }

    private static ItemStack stackFor(Fuel f, double burn) {
        ItemStack stack = switch (f) {
            case NATURAL_URANIUM -> new ItemStack(FissionWorks.ROD_NATURAL.get());
            case URANIUM -> new ItemStack(FissionWorks.ROD_URANIUM.get());
            case THORIUM -> new ItemStack(FissionWorks.ROD_THORIUM.get());
            case SPENT -> new ItemStack(FissionWorks.ROD_SPENT.get());
            default -> ItemStack.EMPTY;
        };
        if (!stack.isEmpty() && stack.getItem() instanceof FuelRodItem) {
            stack.setDamageValue(Math.min(FuelRodItem.LIFE - 1, (int) Math.round(burn * FuelRodItem.LIFE)));
        }
        return stack;
    }

    /** Fluid port for a side: water on the viewer's left, CO2 on the right, steam on top. */
    public IFluidHandler port(Direction side) {
        if (side == null) {
            return null;
        }
        Direction front = getBlockState().getValue(ZirnoxBlock.FACING);
        if (side == Direction.UP) {
            return ports[2];
        }
        if (side == front.getClockWise()) {
            return ports[0];
        }
        return side == front.getCounterClockWise() ? ports[1] : null;
    }

    private void sync() {
        setChanged();
        lastHash = Arrays.hashCode(z.toInts());
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("zirnox", z.toInts());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Zirnox loaded = Zirnox.fromInts(tag.getIntArray("zirnox"));
        z = loaded != null ? loaded : new Zirnox();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** One tank seen from outside: 0 water (fill only), 1 CO2 (fill only), 2 steam (drain only). */
    private final class Port implements IFluidHandler {
        private final int kind;

        Port(int kind) {
            this.kind = kind;
        }

        private int amount() {
            return kind == 0 ? z.water() : kind == 1 ? z.co2() : z.steam();
        }

        private int capacity() {
            return kind == 0 ? Zirnox.WATER_CAP : kind == 1 ? Zirnox.CO2_CAP : Zirnox.STEAM_CAP;
        }

        private net.minecraft.world.level.material.Fluid fluid() {
            return kind == 0 ? Fluids.WATER : kind == 1 ? ModFluids.CO2.get() : ModFluids.STEAM.get();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return new FluidStack(fluid(), amount());
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return kind != 2 && stack.is(fluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (kind == 2 || !resource.is(fluid())) {
                return 0;
            }
            int n = Math.min(resource.getAmount(), capacity() - amount());
            if (n > 0 && action.execute()) {
                if (kind == 0) z.fillWater(n); else z.fillCo2(n);
                setChanged();
            }
            return Math.max(0, n);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return kind == 2 && resource.is(fluid()) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int n = kind == 2 ? Math.min(maxDrain, z.steam()) : 0;
            if (n <= 0) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                z.drainSteam(n);
                setChanged();
            }
            return new FluidStack(fluid(), n);
        }
    }
}
