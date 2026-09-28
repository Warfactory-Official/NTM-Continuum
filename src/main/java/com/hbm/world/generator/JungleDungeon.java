package com.hbm.world.generator;

import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.generator.DungeonToolbox.MetaBlock;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code com.hbm.world.generator.JungleDungeon}: jungle-brick rooms 5 wide and 5 high on a 25 x 25 grid, and the
 * "hole" bookkeeping between the three floors {@code HbmWorldGen} stacks at y 20, 24 and 28.
 *
 * <p>Ported with 1.7's quirk: a floor's hole/circle job runs from the shared queue after that floor's rooms but reads
 * {@code cells} when it runs, and by then the dungeon object has been recomposed for the floors generated after it
 * (1.7 used one static instance for all three). So the bottom floor's brick circle lands on a cell of the top floor's
 * layout, at the bottom floor's height. The port keeps one instance for the three floors of a site, which reproduces
 * that.
 */
public class JungleDungeon extends CellularDungeon {

    public boolean hasHole = false;

    public JungleDungeon(int width, int height, int dimX, int dimZ, int tries, int branches) {
        super(width, height, dimX, dimZ, tries, branches);

        this.floor.add(new MetaBlock(ModBlocks.brick_jungle));
        this.floor.add(new MetaBlock(ModBlocks.brick_jungle_cracked));

        for(int i = 0; i < 50; i++) {
            this.wall.add(new MetaBlock(ModBlocks.brick_jungle));
            this.wall.add(new MetaBlock(ModBlocks.brick_jungle_cracked));
        }
        for(int i = 0; i < 16; i++) {
            this.wall.add(new MetaBlock(ModBlocks.brick_jungle_glyph, i));
        }

        this.ceiling.add(new MetaBlock(ModBlocks.brick_jungle));
        this.ceiling.add(new MetaBlock(ModBlocks.brick_jungle_cracked));
    }

    @Override
    public void generate(final LegacyDungeonWorld world, final int x, final int y, final int z, final RandomSource rand) {
        super.generate(world, x, y, z, rand);

        timed.addOp(() -> {

            JungleDungeon that = JungleDungeon.this;

            //A hole has not been made -> this is the bottom floor
            if(!that.hasHole) {

                List<int[]> rooms = new ArrayList<>();

                for(int i = 0; i < that.cells.length; i++) {
                    for(int j = 0; j < that.cells[0].length; j++) {

                        if(that.cells[i][j] != null)
                            rooms.add(new int[]{i, j});
                    }
                }

                if(!rooms.isEmpty()) {

                    int ix = x - dimX * width / 2;
                    int iz = z - dimZ * width / 2;

                    int[] room = rooms.get(world.rand.nextInt(rooms.size()));
                    world.setBlock(ix + room[0] * (width - 1) + width / 2, y, iz + room[1] * (width - 1) + width / 2, ModBlocks.brick_jungle_circle);
                }
            }

            that.hasHole = false;
        });

        //since all the building is timed jobs, this has to be as well. timed jobs are ordered so this works!
        //is it shitty coding? is it not? who knows?
        //future bob here: yes it is, go fuck yourself
    }

}
