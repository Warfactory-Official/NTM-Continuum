package com.hbm.explosion;

import dev.golem.core.data.Tags;

import com.hbm.blocks.ModBlocks;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 {@code explosion/ExplosionTom}: the TOM impact crater. Same outward square spiral as
 * {@link ExplosionBalefire}, but each column is carved to an analytic bowl-plus-peak-ring-plus-rim profile,
 * floored with tektite and flooded with lava below sea level. Driven by
 * {@link com.hbm.entity.logic.EntityTomBlast}.
 */
public class ExplosionTom {

    /**
     * How far from the impact the crater floods the ground below the old terrain with lava (ADDON-API-INVENTORY V-174).
     * 1.7: everywhere the crater reaches; hbm-space gated it, like the clearing above, at 500 blocks (NTM: Space sets
     * 500), so outside the rim the ejecta does not turn into a lava sheet.
     */
    public static double lavaRadius = Double.POSITIVE_INFINITY;


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

    public ExplosionTom(int x, int y, int z, Level level, int rad) {
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
        n = input.getIntOr(name + "n", 1);
        nlimit = input.getIntOr(name + "nlimit", 0);
        shell = input.getIntOr(name + "shell", 0);
        leg = input.getIntOr(name + "leg", 0);
        element = input.getIntOr(name + "element", 0);
    }

    public boolean update() {
        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        int shell2 = this.shell * 2;
        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / (double) shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit;
    }

    private void breakColumn(int x, int z) {
        int dist = this.radius2 - (x * x + z * z);

        if (dist <= 0) return;

        int pX = posX + x;
        int pZ = posZ + z;
        double X = Math.pow(this.posX - pX, 2);
        double Z = Math.pow(this.posZ - pZ, 2);
        double distance = Math.sqrt(X + Z); // distance calculations used for crater rim stuff

        int y = 256;
        int terrain = 63;
        double planar = Math.sqrt(x * x + z * z);

        // basic crater bowl shape
        double cA = (terrain - Math.pow(Math.E, -Math.pow(planar, 2) / 40000) * 13) + level.getRandom().nextInt(2);
        // crater peak ring
        double cB = cA + Math.pow(Math.E, -Math.pow(planar - 200, 2) / 400) * 13;
        // crater rim
        int craterFloor = (int) (cB + Math.pow(Math.E, -Math.pow(planar - 500, 2) / 2000) * 37);

        for (int i = 256; i > 0; i--) {
            if (i == craterFloor || !level.getBlockState(new BlockPos(pX, i, pZ)).isAir()) {
                y = i;
                break;
            }
        }

        // Visit the full lava band even when the old surface lies below sea level.
        y = Math.max(y, terrain + 1);

        int height = terrain - 14;
        int offset = 20;
        int threshold = (int) (planar * (height + offset) / (float) this.radius)
                + level.getRandom().nextInt(2) - offset;

        while (y > threshold) {

            if (y == 0) break;

            if (y <= craterFloor) {

                BlockPos pos = new BlockPos(pX, y, pZ);
                if (level.getRandom().nextInt(200) == 0) {
                    level.setBlock(pos, ModBlocks.ore_tektite_osmiridium.get().defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                } else {
                    level.setBlock(pos, ModBlocks.tektite.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                }

            } else if (y > terrain + 1) {
                // used so that old terrain inside the crater rim is destroyed, while rim material
                // "floods" terrain outside
                if (distance < 500) {
                    for (int i = -2; i < 3; i++) {
                        for (int j = -2; j < 3; j++) {
                            for (int k = -2; k < 3; k++) {
                                BlockPos near = new BlockPos(pX + i, y + j, pZ + k);
                                if (isWashedAway(level.getBlockState(near))) {
                                    level.setBlock(near, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                                    level.setBlock(new BlockPos(pX, y, pZ), Blocks.AIR.defaultBlockState(),
                                            Block.UPDATE_ALL);
                                }
                            }
                        }
                    }
                    // 1.7 flag 2, but its breakBlock still ran: keep the removal hook on 26.1
                    if (level instanceof ServerLevel server) {
                        Compat.setBlockWithRemoval(server, new BlockPos(pX, y, pZ),
                                Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else {
                        level.setBlock(new BlockPos(pX, y, pZ), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            } else if (distance < lavaRadius) {
                for (int i = -2; i < 3; i++) {
                    for (int j = -2; j < 3; j++) {
                        for (int k = -2; k < 3; k++) {
                            // 1.7 tests (i,y+j,k) for water/ice but (i,y,k) for air - the asymmetry is 1.7's
                            BlockState offsetState = level.getBlockState(new BlockPos(pX + i, y + j, pZ + k));
                            boolean liquid = offsetState.is(Blocks.WATER) || offsetState.is(BlockTags.ICE);
                            boolean air = level.getBlockState(new BlockPos(pX + i, y, pZ + k)).isAir();

                            if (liquid || air) {
                                level.setBlock(new BlockPos(pX + i, y, pZ + k),
                                        Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
                                level.setBlock(new BlockPos(pX, y, pZ),
                                        Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
                            }
                        }
                    }
                }
                level.setBlock(new BlockPos(pX, y, pZ), Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
            }

            y--;
        }
    }

    /**
     * 1.7 tested Material water/ice/snow plus {@code getCanBurn()}. 26.1 has no Material, so the three are
     * tags and burnability is the flammability the fire block already reads.
     */
    private static boolean isWashedAway(BlockState state) {
        return state.is(Blocks.WATER)
                || state.is(BlockTags.ICE)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.ignitedByLava();
    }
}
