package com.hbm.tileentity.turret;

import com.hbm.api.block.IRadarCommandReceiver;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import dev.golem.core.multiblock.MultiblockCore;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * What the two big guns share: a queue of fire missions, and a line-of-sight test that answers over the
 * horizon.
 *
 * <p>Artillery does not need to see its target — {@link #doLOSCheck} turns the ray cast off and the
 * check becomes "is the target above the terrain height at its own column", which is how a shell lobbed
 * over a hill still finds something on the far side.
 */
public abstract class TileEntityTurretBaseArtillery extends TileEntityTurretBaseNT
        implements IRadarCommandReceiver {

    protected final List<Vec3> targetQueue = new ArrayList<>();

    protected TileEntityTurretBaseArtillery(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 1.12.2's artillery renderers extend the raw TESR, so they never draw the pad-edge plugs. */
    @Override
    protected boolean hasConnectorPlugs() {
        return false;
    }

    /**
     * The emplacement's whole outer wall, both courses: four cells a side, four sides, two levels.
     *
     * <p>Deviation from 1.12.2, which rotates the ring's <em>positions</em> with the placement but asks
     * every neighbour about a world-constant face — so a cable is asked about its south face whichever
     * way the gun points, and the two agree at only one of the four facings. The outward face is
     * derived from the same vectors the positions are, which is what 1.12.2 was reaching for; at the
     * facing where its constants line up, this is identical.
     */
    @Override
    public DirPos[] getConPos() {
        Direction facing = getBlockState().hasProperty(MultiblockCore.FACING)
                ? getBlockState().getValue(MultiblockCore.FACING)
                : Direction.SOUTH;
        Direction dir = facing.getOpposite();
        Direction rot = dir.getClockWise();

        ForgeDirection alongPositive = ForgeDirection.getOrientation(dir);
        ForgeDirection alongNegative = ForgeDirection.getOrientation(dir.getOpposite());
        ForgeDirection acrossPositive = ForgeDirection.getOrientation(rot);
        ForgeDirection acrossNegative = ForgeDirection.getOrientation(rot.getOpposite());

        DirPos[] ports = new DirPos[32];
        int at = 0;
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        for (int course = 0; course < 2; course++) {
            for (int step = 0; step < 4; step++) {
                int along = step - 1;
                int across = 1 - step;
                ports[at++] = new DirPos(x + dir.getStepX() * along + rot.getStepX() * -3, y + course,
                        z + dir.getStepZ() * along + rot.getStepZ() * -3, acrossNegative);
                ports[at++] = new DirPos(x + dir.getStepX() * along + rot.getStepX() * 2, y + course,
                        z + dir.getStepZ() * along + rot.getStepZ() * 2, acrossPositive);
                ports[at++] = new DirPos(x + dir.getStepX() * -2 + rot.getStepX() * across, y + course,
                        z + dir.getStepZ() * -2 + rot.getStepZ() * across, alongNegative);
                ports[at++] = new DirPos(x + dir.getStepX() * 3 + rot.getStepX() * across, y + course,
                        z + dir.getStepZ() * 3 + rot.getStepZ() * across, alongPositive);
            }
        }

        return ports;
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        enqueueTarget(x + 0.5D, y, z + 0.5D);
        return true;
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        enqueueTarget(target.getX(), target.getY(), target.getZ());
        return true;
    }

    /** Fire missions waiting. */
    public int queuedTargets() {
        return targetQueue.size();
    }

    /** Where one waiting mission aims — not derivable from the count, and what a test asserts on. */
    public Vec3 queuedTarget(int index) {
        return targetQueue.get(index);
    }

    public void enqueueTarget(double x, double y, double z) {
        Vec3 pos = getTurretPos();
        if (new Vec3(x - pos.x, y - pos.y, z - pos.z).length() <= getDetectorRange()) {
            targetQueue.add(new Vec3(x, y, z));
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
                PREFIX_FUNCTION + "removeWhitelist" + NAME_SEPARATOR + "name",
                PREFIX_FUNCTION + "enqueue" + NAME_SEPARATOR + "x" + PARAM_SEPARATOR + "y"
                        + PARAM_SEPARATOR + "z"};
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        super.runRORFunction(name, params);

        if ((PREFIX_FUNCTION + "enqueue").equals(name) && params.length > 2) {
            int x = IRORInteractive.parseInt(params[0], Integer.MIN_VALUE, Integer.MAX_VALUE);
            int y = IRORInteractive.parseInt(params[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
            int z = IRORInteractive.parseInt(params[2], Integer.MIN_VALUE, Integer.MAX_VALUE);
            sendCommandPosition(x, y, z);
            setChanged();
        }

        return null;
    }

    /**
     * What switching fire mode abandons.
     *
     * <p>Deviation from 1.12.2, which clears the aim point and the queue but leaves {@code target}
     * standing. A gun already tracking an entity would otherwise keep firing at it forever once
     * switched to manual: the tick re-derives {@code tPos} from the live target every tick, the empty
     * queue never pops it back to null, and manual mode skips the check that would drop it. Both guns
     * clear the other two halves of the engagement, so leaving this one is an oversight, not a rule.
     */
    protected void abandonEngagement() {
        target = null;
        tPos = null;
        targetQueue.clear();
    }

    public abstract boolean doLOSCheck();

    @Override
    public boolean entityInLOS(Entity e) {
        if (doLOSCheck()) return super.entityInLOS(e);

        Vec3 pos = getTurretPos();
        Vec3 ent = getEntityPos(e);
        double length = ent.subtract(pos).length();

        if (length < getDetectorGrace() || length > getDetectorRange() * 1.1D) return false;

        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(e.getX()), Mth.floor(e.getZ()));
        return height < e.getY() + e.getBbHeight();
    }

}
