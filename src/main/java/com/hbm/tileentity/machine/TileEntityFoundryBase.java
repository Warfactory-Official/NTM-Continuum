package com.hbm.tileentity.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

/**
 * Base class for all foundry blocks (channels, casters, basins, outlets) — the 1.12.2
 * {@code TileEntityFoundryBase}. Holds exactly ONE molten material at a time ({@link #type} +
 * {@link #amount} quanta) and provides the standard accept/add logic used by pour and flow. The
 * {@link ICrucibleAcceptor} contract lives on the <b>Block</b>, which delegates here.
 */
public abstract class TileEntityFoundryBase extends TileEntityLoadedBase {

    public @Nullable NTMMaterial type;
    public int amount;

    public TileEntityFoundryBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract int getCapacity();

    /**
     * Standard check for whether this stack can be added: type matching + amount below capacity.
     */
    public boolean standardCheck(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        if (this.type != null && this.type != stack.material && this.amount > 0) return false;
        return this.amount < this.getCapacity();
    }

    /**
     * Standard adding via pouring or flowing: adopts the material, adds up to capacity, returns the
     * leftover (null = fully added).
     */
    public @Nullable MaterialStack standardAdd(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        this.type = stack.material;

        if (stack.amount + this.amount <= this.getCapacity()) {
            this.amount += stack.amount;
            setChanged();
            return null;
        }

        int required = this.getCapacity() - this.amount;
        this.amount = this.getCapacity();
        stack.amount -= required;
        setChanged();
        return stack;
    }

    // --- the ICrucibleAcceptor default behaviour (blocks delegate to these) ---

    /** Standard check with no additional limitations. */
    public boolean canAcceptPartialFlow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        return this.standardCheck(world, p, side, stack);
    }

    public @Nullable MaterialStack flow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        return this.standardAdd(world, p, side, stack);
    }

    /** Standard check, but the only valid pour direction is from above. */
    public boolean canAcceptPartialPour(Level world, BlockPos p, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        if (side != ForgeDirection.UP) return false;
        return this.standardCheck(world, p, side, stack);
    }

    public @Nullable MaterialStack pour(Level world, BlockPos p, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        return this.standardAdd(world, p, side, stack);
    }

    /** On destruction, drop the molten content as scraps (old breakBlock). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        if (amount > 0 && type != null) {
            com.hbm.items.machine.ItemScraps.spawnMats(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new MaterialStack(type, amount));
            amount = 0;
        }
    }

    // --- sync (BufPacket) + persistence (1.12.2 keys) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(this.type == null ? -1 : this.type.id);
        buf.writeInt(this.amount);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        int id = buf.readInt();
        this.type = id < 0 ? null : Mats.matById.get(id);
        this.amount = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("type", this.type == null ? -1 : this.type.id);
        output.putInt("amount", this.amount);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        int id = input.getIntOr("type", -1);
        this.type = id < 0 ? null : Mats.matById.get(id);
        this.amount = input.getIntOr("amount", 0);
    }
}
