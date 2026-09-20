package com.hbm.explosion;

import dev.golem.core.data.Tags;

import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 {@code explosion/ExplosionBalefire}: the balefire crater. Walks an outward square spiral one column per
 * {@link #update()}, shaving each column down to a distance-scaled depth, lighting balefire on the floor and
 * slaking the stone underneath. Driven by {@link com.hbm.entity.logic.EntityBalefire}.
 */
public class ExplosionBalefire {

    public int posX;
    public int posY;
    public int posZ;
    public int lastposX = 0;
    public int lastposZ = 0;
    public int radius;
    public int radius2;
    public Level level;
    private int n = 1;
    private int nlimit;
    private int shell;
    private int leg;
    private int element;

    public ExplosionBalefire(int x, int y, int z, Level level, int rad) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        this.level = level;

        this.radius = rad;
        this.radius2 = this.radius * this.radius;

        this.nlimit = this.radius2 * 4;
    }

    public void saveToNbt(DataOut output, String name) {
        output.putInt(name + "posX", posX);
        output.putInt(name + "posY", posY);
        output.putInt(name + "posZ", posZ);
        output.putInt(name + "lastposX", lastposX);
        output.putInt(name + "lastposZ", lastposZ);
        output.putInt(name + "radius", radius);
        output.putInt(name + "radius2", radius2);
        output.putInt(name + "n", n);
        output.putInt(name + "nlimit", nlimit);
        output.putInt(name + "shell", shell);
        output.putInt(name + "leg", leg);
        output.putInt(name + "element", element);
    }

    public void readFromNbt(DataIn input, String name) {
        posX = input.getIntOr(name + "posX", 0);
        posY = input.getIntOr(name + "posY", 0);
        posZ = input.getIntOr(name + "posZ", 0);
        lastposX = input.getIntOr(name + "lastposX", 0);
        lastposZ = input.getIntOr(name + "lastposZ", 0);
        radius = input.getIntOr(name + "radius", 0);
        radius2 = input.getIntOr(name + "radius2", 0);
        n = Math.max(input.getIntOr(name + "n", 1), 1); // prevents invalid read operation
        nlimit = input.getIntOr(name + "nlimit", 0);
        shell = input.getIntOr(name + "shell", 0);
        leg = input.getIntOr(name + "leg", 0);
        element = input.getIntOr(name + "element", 0);
    }

    public boolean update() {

        if (n == 0) return true;

        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        int shell2 = this.shell * 2;

        if (shell2 == 0) return true;

        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / (double) shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit;
    }

    private void breakColumn(int x, int z) {
        int dist = (int) (radius - Math.sqrt(x * x + z * z));

        if (dist <= 0) return;

        int pX = posX + x;
        int pZ = posZ + z;

        // 1.7 getHeightValue: first non-blocking column position from the top
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pX, pZ);
        int maxdepth = (int) (10 + radius * 0.25);
        int depth = (int) ((maxdepth * dist / (double) radius) + (Math.sin(dist * 0.15 + 2) * 2));

        depth = Math.max(y - depth, 0);

        while (y > depth) {

            BlockPos pos = new BlockPos(pX, y, pZ);
            BlockState state = level.getBlockState(pos);

            // a schrabidium cluster stops the column dead and may transmute instead
            if (isSchrabidiumCluster(state)) {
                if (level.getRandom().nextInt(10) == 0) {
                    level.setBlockAndUpdate(pos.above(), ModBombBlocks.balefire.get().defaultBlockState());
                    level.setBlock(pos, euphemiumClusterOf(state),
                            Block.UPDATE_ALL);
                }
                return;
            }

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            y--;
        }

        if (level.getRandom().nextInt(10) == 0) {
            level.setBlockAndUpdate(new BlockPos(pX, depth + 1, pZ),
                    ModBombBlocks.balefire.get().defaultBlockState());

            BlockPos floor = new BlockPos(pX, y, pZ);
            BlockState state = level.getBlockState(floor);
            if (isSchrabidiumCluster(state)) {
                level.setBlock(floor, euphemiumClusterOf(state),
                        Block.UPDATE_ALL);
            }
        }

        for (int i = depth; i > depth - 5; i--) {
            BlockPos pos = new BlockPos(pX, i, pZ);
            if (level.getBlockState(pos).is(Blocks.STONE)) {
                level.setBlock(pos, ModFalloutBlocks.sellafield_slaked.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * 1.7 carried the cluster's metadata across the swap; both clusters are pillars, so the axis is what
     * that metadata now spells.
     */
    private static BlockState copyPillarAxis(BlockState from, Block to) {
        BlockState result = to.defaultBlockState();
        if (from.hasProperty(BlockStateProperties.AXIS)
                && result.hasProperty(BlockStateProperties.AXIS)) {
            return result.setValue(BlockStateProperties.AXIS,
                    from.getValue(BlockStateProperties.AXIS));
        }
        return result;
    }

    /**
     * 1.7 {@code block_schrabidium_cluster}: this port has not registered the cluster blocks, so no column
     * stops on one and no euphemium cluster is minted (entity port manifest).
     */
    private static boolean isSchrabidiumCluster(BlockState state) {
        return false;
    }

    private static BlockState euphemiumClusterOf(BlockState state) {
        return state;
    }
}
