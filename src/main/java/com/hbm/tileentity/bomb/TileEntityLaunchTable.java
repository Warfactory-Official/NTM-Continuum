package com.hbm.tileentity.bomb;

import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.inventory.container.ContainerLaunchTable;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityLaunchTable}: the 9x9 launch table. 100,000 l tanks, 100,000 solid, and a pad size the
 * GUI sets (size 10, 15 or 20 fuselage tops). Unlike the compact launcher it reports "can launch" without a
 * designator; the launch itself still needs a ready one.
 *
 * <p>{@code height} is 1.7's client-side scaffold height, which its renderer overwrote from the loaded missile and
 * kept once the missile left.
 */
public class TileEntityLaunchTable extends TileEntityCustomMissileLauncher implements MenuProvider {

    public static final int maxSolid = 100000;
    public PartSize padSize = PartSize.SIZE_10;
    public int height = 10;

    public TileEntityLaunchTable(BlockPos pos, BlockState state) {
        super(ModCustomMissileBlockEntities.LAUNCH_TABLE.get(), pos, state, 100000);
    }

    @Override public String getDefaultName() { return "container.launchTable"; }
    @Override public int getMaxSolid() { return maxSolid; }
    @Override public PartSize acceptedSize() { return padSize; }
    @Override protected int footprintRadius() { return 4; }
    @Override protected boolean requiresDesignatorToLaunch() { return false; }

    /** 1.7 {@code TileEntityLaunchTable.updateConnections}: nine positions along each side, five out. */
    @Override
    public DirPos[] connectionPositions() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        DirPos[] ports = new DirPos[36];
        int n = 0;
        for (int i = -4; i <= 4; i++) {
            ports[n++] = new DirPos(x + i, y, z + 5, Library.POS_Z);
            ports[n++] = new DirPos(x + i, y, z - 5, Library.NEG_Z);
            ports[n++] = new DirPos(x + 5, y, z + i, Library.POS_X);
            ports[n++] = new DirPos(x - 5, y, z + i, Library.NEG_X);
        }
        return ports;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UP && dir != ForgeDirection.DOWN && dir != ForgeDirection.UNKNOWN;
    }

    /** 1.7 AuxButtonPacket: the GUI's three pad-size buttons send the PartSize ordinal. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        if (value >= 0 && value < PartSize.values().length) {
            this.padSize = PartSize.values()[value];
            setChanged();
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte((byte) padSize.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.padSize = PartSize.values()[buf.readByte()];
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        SavedIds.putEnum(output, "padSize", padSize); // by name, never an ordinal
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        padSize = SavedIds.getEnum(input, "padSize", PartSize.SIZE_10);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerLaunchTable(containerId, playerInventory, this);
    }

    /** 1.7 {@code TileEntityLaunchTable}'s wider launch smoke. */
    @Override
    protected double smokeSpread() {
        return 0.65D;
    }
}
