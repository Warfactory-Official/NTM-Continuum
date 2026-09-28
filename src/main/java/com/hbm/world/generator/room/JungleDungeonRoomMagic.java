package com.hbm.world.generator.room;

import com.hbm.blocks.generic.TrappedBrick.Trap;
import com.hbm.lib.ForgeDirection;
import com.hbm.world.gen.LegacyDungeonBlocks.Blocks;
import com.hbm.world.gen.LegacyDungeonBlocks.ModBlocks;
import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.component.LegacyBlock;
import com.hbm.world.generator.CellularDungeon;
import com.hbm.world.generator.CellularDungeonRoom;
import com.hbm.world.generator.DungeonToolbox;
import com.hbm.world.generator.DungeonToolbox.MetaBlock;
import com.hbm.world.generator.JungleDungeon;
import com.hbm.world.generator.TimedGenerator.ITimedJob;

import java.util.ArrayList;
import java.util.List;



/** 1.7 {@code com.hbm.world.generator.room.JungleDungeonRoomMagic}, on {@link LegacyDungeonWorld}; jobs go to the site's queue. */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class JungleDungeonRoomMagic extends JungleDungeonRoom {

	public JungleDungeonRoomMagic(CellularDungeon parent) {
		super(parent);
	}

	public void generateMain(final LegacyDungeonWorld world, final int x, final int y, final int z) {
		super.generateMain(world, x, y, z);
		
		ITimedJob job = new ITimedJob() {

			@Override
			public void work() {

				int ix = world.rand.nextInt(3) + 1;
				int iz = world.rand.nextInt(3) + 1;
				
				LegacyBlock bl = world.getBlock(x + ix, y, z + iz);
				if(bl == ModBlocks.brick_jungle || bl == ModBlocks.brick_jungle_cracked || bl == ModBlocks.brick_jungle_lava) {
					world.setBlock(x + ix, y, z + iz, ModBlocks.brick_jungle_trap, Trap.MAGIC_CONVERSTION.ordinal(), 3);
				}
			}
		};
		
		parent.timed.addOp(job);
	}
}
