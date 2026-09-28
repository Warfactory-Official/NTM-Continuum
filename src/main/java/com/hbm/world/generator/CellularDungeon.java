package com.hbm.world.generator;

import com.hbm.lib.ForgeDirection;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.generator.DungeonToolbox.MetaBlock;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code com.hbm.world.generator.CellularDungeon}: a grid of rooms grown from the middle cell by random branching,
 * on {@link LegacyDungeonWorld}. The composition is 1.7's call for call and draw for draw.
 */
public class CellularDungeon {

    //a buffer "map" of the rooms being generated before being spawned in
    CellularDungeonRoom[][] cells;
    ForgeDirection[][] doors;
    //the order in which the buffer should be processed
    List<int[]> order = new ArrayList<>();

    //the size of the cell array x
    int dimX;
    //the size of the cell array z
    int dimZ;
    //the base width (and length) of a room
    public int width;
    //the height of a room
    public int height;
    //list of random floor blocks with equal weight
    public List<MetaBlock> floor = new ArrayList<>();
    //list of random ceiling blocks with equal weight
    public List<MetaBlock> ceiling = new ArrayList<>();
    //list of random wall blocks with equal weight
    public List<MetaBlock> wall = new ArrayList<>();
    //the rooms that the dungeon can use
    public List<CellularDungeonRoom> rooms = new ArrayList<>();
    int tries;
    int branches;
    boolean isGenerating;
    /** 1.7's static TimedGenerator queue; one per dungeon site in the port (see {@link TimedGenerator}). */
    public final TimedGenerator timed = new TimedGenerator();

    public CellularDungeon(int width, int height, int dimX, int dimZ, int tries, int branches) {

        this.dimX = dimX;
        this.dimZ = dimZ;
        this.width = width;
        this.height = height;
        this.tries = tries;
        this.branches = branches;
    }

    public CellularDungeon(int width, int height, int dimX, int dimZ, int tries, int branches, MetaBlock floor, MetaBlock ceiling, MetaBlock wall) {

        this.dimX = dimX;
        this.dimZ = dimZ;
        this.width = width;
        this.height = height;
        this.tries = tries;
        this.branches = branches;
        this.floor.add(floor);
        this.ceiling.add(ceiling);
        this.wall.add(wall);
    }

    public void generate(LegacyDungeonWorld world, int x, int y, int z, RandomSource rand) {

        if(isGenerating) return;

        isGenerating = true;

        x -= dimX * width / 2;
        z -= dimZ * width / 2;

        compose(rand);

        for(int[] coord : order) {

            if(coord == null || coord.length != 2)
                continue;

            int dx = coord[0];
            int dz = coord[1];

            if(cells[dx][dz] != null) {

                if(doors[dx][dz] == null)
                    doors[dx][dz] = ForgeDirection.UNKNOWN;

                cells[dx][dz].generate(world, x + dx * (width - 1), y, z + dz * (width - 1), doors[dx][dz]);
            }
        }

        isGenerating = false;
    }

    int rec = 0;
    public void compose(RandomSource rand) {

        cells = new CellularDungeonRoom[dimX][dimZ];
        doors = new ForgeDirection[dimX][dimZ];
        order.clear();

        int startX = dimX / 2;
        int startZ = dimZ / 2;

        cells[startX][startZ] = DungeonToolbox.getRandom(rooms, rand);
        doors[startX][startZ] = ForgeDirection.UNKNOWN;
        order.add(new int[] { startX, startZ });

        rec = 0;
        addRoom(startX, startZ, rand, ForgeDirection.UNKNOWN, DungeonToolbox.getRandom(rooms, rand));
    }

    //if x and z are occupied, it will just use the next nearby random space
    private boolean addRoom(int x, int z, RandomSource rand, ForgeDirection door, CellularDungeonRoom room) {

        rec++;
        if(rec > tries)
            return false;

        if(x < 0 || z < 0 || x >= dimX || z >= dimZ)
            return false;

        if(cells[x][z] != null) {

            ForgeDirection dir = getRandomDir(rand);
            addRoom(x + dir.offsetX, z + dir.offsetZ, rand, dir.getOpposite(), DungeonToolbox.getRandom(rooms, rand));
            return false;
        }

        if(room.daisyChain == null || addRoom(x + room.daisyDirection.offsetX, z + room.daisyDirection.offsetZ, rand, ForgeDirection.UNKNOWN, room.daisyChain)) {
            cells[x][z] = room;
            doors[x][z] = door;
            order.add(new int[] { x, z });
        }

        for(int i = 0; i < branches; i++) {
            ForgeDirection dir = getRandomDir(rand);
            addRoom(x + dir.offsetX, z + dir.offsetZ, rand, dir.getOpposite(), DungeonToolbox.getRandom(rooms, rand));
        }

        return true;
    }

    public static ForgeDirection getRandomDir(RandomSource rand) {

        return ForgeDirection.getOrientation(rand.nextInt(4) + 2);
    }
}
