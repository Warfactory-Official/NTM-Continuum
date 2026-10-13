package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.util.CrucibleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Foundry outlet (1.12.2 {@code TileEntityFoundryOutlet}), a pass-through tap that accepts flow only from
 * the side opposite its facing, then raytraces 4 blocks down and re-pours into whatever acceptor it
 * finds. Filterable to a single material (right-click with scraps) and closed by a redstone signal.
 * Stores nothing (capacity 0).
 */
public class TileEntityFoundryOutlet extends TileEntityFoundryBase {

    public @Nullable NTMMaterial filter = null;
    /** inverts filter behaviour: lets everything BUT the filter material pass */
    public boolean invertFilter = false;
    /** inverts redstone behaviour: when TRUE the outlet is blocked by default and only opens with redstone */
    public boolean invertRedstone = false;

    // Pour-stream visual for the renderer (-1 = no stream this tick).
    public int pourColor = -1;
    public float pourLen;
    protected long lastPourTime;

    public TileEntityFoundryOutlet(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FOUNDRY_OUTLET.get(), pos, state);
    }

    protected TileEntityFoundryOutlet(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** How long the stream lingers after the last flow (ticks); the feeding channel only flows every 5,
     * so a short window would flicker. The old {@code ParticleFoundry} lived 20 ticks. */
    private static final int STREAM_LIFETIME = 15;

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFoundryOutlet te) {
        // The pour visual is set from flow() (event-driven); expire it once nothing has flowed recently.
        if (te.pourColor >= 0 && level.getGameTime() - te.lastPourTime > STREAM_LIFETIME) {
            te.pourColor = -1;
        }
        te.networkPackNT();
    }

    /** TRUE prevents flow through the outlet. */
    public boolean isClosed() {
        return invertRedstone ^ (level != null && level.hasNeighborSignal(worldPosition));
    }

    protected ForgeDirection getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return ForgeDirection.getOrientation(state.getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue());
        }
        return ForgeDirection.NORTH;
    }

    protected double getDropRange() {
        return 4;
    }

    @Override public boolean canAcceptPartialPour(Level world, BlockPos p, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) { return false; }
    @Override public @Nullable MaterialStack pour(Level world, BlockPos p, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) { return stack; }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        if (filter != null && (filter != stack.material ^ invertFilter)) return false;
        if (isClosed()) return false;
        if (side != getFacing().getOpposite()) return false;

        Vec3 start = new Vec3(p.getX() + 0.5, p.getY() - 0.125, p.getZ() + 0.5);
        Vec3 end = new Vec3(p.getX() + 0.5, p.getY() + 0.125 - getDropRange(), p.getZ() + 0.5);

        BlockHitResult[] mop = new BlockHitResult[1];
        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(world, start, end, mop);
        if (acc == null) return false;

        Vec3 hit = mop[0].getLocation();
        return acc.canAcceptPartialPour(world, mop[0].getBlockPos(), hit.x, hit.y, hit.z, ForgeDirection.UP, stack);
    }

    @Override
    public @Nullable MaterialStack flow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        Vec3 start = new Vec3(p.getX() + 0.5, p.getY() - 0.125, p.getZ() + 0.5);
        Vec3 end = new Vec3(p.getX() + 0.5, p.getY() + 0.125 - getDropRange(), p.getZ() + 0.5);

        BlockHitResult[] mop = new BlockHitResult[1];
        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(world, start, end, mop);
        if (acc == null) return stack;

        Vec3 hit = mop[0].getLocation();
        MaterialStack didPour = acc.pour(world, mop[0].getBlockPos(), hit.x, hit.y, hit.z, ForgeDirection.UP, stack);

        // stream visual: from the outlet's underside down to the impact
        this.pourColor = stack.material.moltenColor;
        this.pourLen = Math.max(1F, p.getY() - (float) (Math.ceil(mop[0].getBlockPos().getY() + 1) - 0.875));
        this.lastPourTime = world.getGameTime();

        return didPour;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    // --- sync + persistence (1.12.2 keys) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(filter == null ? -1 : filter.id);
        buf.writeBoolean(invertFilter);
        buf.writeBoolean(invertRedstone);
        buf.writeInt(pourColor);
        buf.writeFloat(pourLen);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        int id = buf.readInt();
        filter = id < 0 ? null : Mats.matById.get(id);
        invertFilter = buf.readBoolean();
        invertRedstone = buf.readBoolean();
        pourColor = buf.readInt();
        pourLen = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("invert", this.invertRedstone);
        output.putBoolean("invertFilter", this.invertFilter);
        Mats.write(output, "filter", this.filter); // by name, never the numeric id
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.invertRedstone = input.getBooleanOr("invert", false);
        this.invertFilter = input.getBooleanOr("invertFilter", false);
        this.filter = Mats.read(input, "filter");
    }
}
