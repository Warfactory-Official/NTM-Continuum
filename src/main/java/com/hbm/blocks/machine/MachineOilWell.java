package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.IBlockExplodedVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.util.Owners;
import dev.golem.core.world.Protection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Oil Derrick block, 1.7's {@code MachineOilWell}: a 10-tall structure with a 1-high 3×3 base at ground level, a 9-high
 * 1×1 tower above the core, and the 4 base corners one block up. Core at the clicked position (offset 0). Caught in an
 * explosion with oil or gas in either tank, it blows up as 1.7's {@code onBlockExploded} does.
 */
public class MachineOilWell extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, IBlockExplodedVNT {

    public MachineOilWell(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineOilWell(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{9, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    private static final int[] PLINTH = {1, -1, 0, 0, 0, 0};
    private static final int[] TOWER = {8, 0, 1, 1, 1, 1};
    private static final int[] LEG = {-1, 1, 0, 0, 0, 0};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return MultiblockHandlerXR.checkSpace(level, core, PLINTH, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.above(), TOWER, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(1, 1, 1), LEG, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(1, 1, -1), LEG, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(-1, 1, 1), LEG, clicked, facing)
                && MultiblockHandlerXR.checkSpace(level, core.offset(-1, 1, -1), LEG, clicked, facing);
    }

    /**
     * No super: getDimensions is the placement envelope and is wider than what 1.7 actually fills; the
     * four edge-centre cells at ground level stay empty, which is what the derrick stands on.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, PLINTH, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(), TOWER, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(1, 1, 1), LEG, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(1, 1, -1), LEG, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-1, 1, 1), LEG, facing, visitor);
        MultiblockHandlerXR.visitBox(core.offset(-1, 1, -1), LEG, facing, visitor);
    }

    /** The whole port surface is the core's own four horizontals. */
    @Override public int coreMask() { return MASK_HORIZONTAL | MASK_DOWN; }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {0, 0, 0, ForgeDirection.EAST.ordinal()},
                {0, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, 0, ForgeDirection.SOUTH.ordinal()},
                {0, 0, 0, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }
    @Override
    protected boolean carriesData() {
        return true;
    }

    /** Cores whose struck cell is being removed right now, so that removal's own explosion hook stays plain. */
    private static final Set<BlockPos> REMOVING = new HashSet<>();

    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        TileEntityMachineOilWell well = state.getValue(CELL) ? null : fuelled(level, pos);
        if (well == null || !REMOVING.add(pos)) return super.onExplosion(state, level, pos, explosion, onHit);
        Protection.Source source = Owners.source(explosion.getDirectSourceEntity(), level, pos);
        try {
            forwardExplosionHit(state, level, pos, explosion, onHit);
        } finally {
            REMOVING.remove(pos);
        }
        blowUp(level, pos, well, source);
        return true;
    }

    @Override
    public void onExplosionHitAtCore(BlockState state, ServerLevel level, BlockPos core, Explosion explosion,
                                     BiConsumer<ItemStack, BlockPos> onHit) {
        TileEntityMachineOilWell well = fuelled(level, core);
        if (well == null || !REMOVING.add(core)) {
            super.onExplosionHitAtCore(state, level, core, explosion, onHit);
            return;
        }
        Protection.Source source = Owners.source(explosion.getDirectSourceEntity(), level, core);
        try {
            super.onExplosionHitAtCore(state, level, core, explosion, onHit);
        } finally {
            REMOVING.remove(core);
        }
        blowUp(level, core, well, source);
    }

    /**
     * HBM's own blasts reach the derrick here. With fuel the derrick removes itself before blowing up and skips the
     * blast's drop roll for this cell; without, the blast removes it as any block.
     */
    @Override
    public boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state, ExplosionVNT explosion) {
        BlockPos core = coreOf(level, pos, state);
        TileEntityMachineOilWell well = core == null ? null : fuelled(level, core);
        if (well == null) return false;
        Protection.Source source = Owners.source(explosion.exploder, level, core);
        level.removeBlock(pos, false);
        blowUp(level, core, well, source);
        return true;
    }

    private static @Nullable TileEntityMachineOilWell fuelled(ServerLevel level, BlockPos core) {
        return level.getBlockEntity(core) instanceof TileEntityMachineOilWell well
                && (well.tanks[0].getFill() > 0 || well.tanks[1].getFill() > 0) ? well : null;
    }

    /** 1.7 {@code onBlockExploded}, after the struck cell is gone: any fill at all becomes a fixed strength 15 blast. */
    private static void blowUp(ServerLevel level, BlockPos core, TileEntityMachineOilWell well, Protection.Source source) {
        well.tanks[0].setFill(0);
        well.tanks[1].setFill(0);

        double x = core.getX() + 0.5D;
        double y = core.getY() + 0.5D;
        double z = core.getZ() + 0.5D;
        ExplosionVNT xnt = new ExplosionVNT(level, x, y, z, 15F);
        xnt.setSource(source);
        xnt.setBlockAllocator(new BlockAllocatorStandard(24));
        xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        xnt.setEntityProcessor(new EntityProcessorStandard());
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        ExplosionCreator.composeEffect(level, x, y, z, 10, 2F, 0.5F, 25F, 5, 8, 20, 0.75F, 1F, -2F, 150F);
    }
}
