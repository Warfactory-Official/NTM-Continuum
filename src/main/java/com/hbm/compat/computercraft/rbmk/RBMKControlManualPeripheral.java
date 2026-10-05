package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.RBMKColor;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

/** 1.7 {@code TileEntityRBMKControlManual}: the inherited {@code rbmk_control_rod} component plus the rod's colour. */
public final class RBMKControlManualPeripheral extends RBMKControlPeripheral<BlockEntityRBMKControlManual> {

    private static final int COLOR = CONTROL_SLOTS;

    public RBMKControlManualPeripheral(BlockEntityRBMKControlManual machine) {
        super(machine, CONTROL_SLOTS + 1);
    }

    @Override
    protected void capture(BlockEntityRBMKControlManual machine) {
        super.capture(machine);
        put(COLOR, machine.color == null ? -1 : machine.color.ordinal());
    }

    @LuaFunction
    public final Object[] getColor() {
        // 1.7 :162 dereferences an undyed rod's null colour
        return read(s -> s.intAt(COLOR) < 0 ? OpenComputers.unknownError() : new Object[]{s.intAt(COLOR)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColor(double color) {
        BlockEntityRBMKControlManual machine = machine();
        machine.color = RBMKColor.VALUES[Mth.clamp((int) color, 0, 4)];
        machine.setChanged();
        return new Object[]{true};
    }
}
//?}
