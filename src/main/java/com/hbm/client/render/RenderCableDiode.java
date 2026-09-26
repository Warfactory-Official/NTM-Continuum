package com.hbm.client.render;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.network.TileEntityDiode;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * The cable arms of 1.7's {@code RenderDiode}: the slab and the coil core are the block's own model; what
 * a model cannot know is which neighbours are cables, so the {@code cable_neo.obj} arms are drawn here per
 * connected side (with the original's Z quirk: NORTH draws {@code posZ}, SOUTH draws {@code negZ}).
 */
public class RenderCableDiode extends GemBER<TileEntityDiode, CableRenderState> {

    private static final String[] ARMS = {"posX", "negX", "posY", "negY", "posZ", "negZ"};

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of("hbm", "models/blocks/cable_neo.obj"));
    private final Surface surface = Surface.cutout("hbm:textures/models/blocks/cable_neo.png");

    @Override
    protected void setup(Gem.Model<CableRenderState> declaration) {
        for (String name : ARMS) {
            declaration.part(model.part(name), surface).visibleIf(state -> state.parts.contains(name)).translate(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    public CableRenderState createRenderState() {
        return new CableRenderState();
    }

    @Override
    public void extract(TileEntityDiode be, CableRenderState state, float partialTicks, Vec3 cameraPosition) {
        Level level = be.getLevel();
        if (level == null) {
            state.parts = Set.of();
            return;
        }
        BlockPos pos = be.getBlockPos();
        Set<String> parts = new HashSet<>();
        if (connects(level, pos, Direction.EAST)) parts.add("posX");
        if (connects(level, pos, Direction.WEST)) parts.add("negX");
        if (connects(level, pos, Direction.UP)) parts.add("posY");
        if (connects(level, pos, Direction.DOWN)) parts.add("negY");
        if (connects(level, pos, Direction.NORTH)) parts.add("posZ");
        if (connects(level, pos, Direction.SOUTH)) parts.add("negZ");
        state.parts = parts;
    }

    /** {@code Library.canConnect}: the neighbour's block or block entity takes a cable on the face towards us. */
    private static boolean connects(Level level, BlockPos pos, Direction dir) {
        BlockPos neighbour = pos.relative(dir);
        ForgeDirection side = ForgeDirection.getOrientation(dir.getOpposite());
        if (level.getBlockState(neighbour).getBlock() instanceof IEnergyConnectorBlock block
                && block.canConnect(level, neighbour, side.toDirection())) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(neighbour);
        return be instanceof IEnergyConnectorMK2 connector && connector.canConnect(side.toDirection());
    }
}
