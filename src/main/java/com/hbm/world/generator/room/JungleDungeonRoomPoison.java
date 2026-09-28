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



/** 1.7 {@code com.hbm.world.generator.room.JungleDungeonRoomPoison}, on {@link LegacyDungeonWorld}; jobs go to the site's queue. */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class JungleDungeonRoomPoison extends JungleDungeonRoom {

	public JungleDungeonRoomPoison(CellularDungeon parent) {
		super(parent);
	}

	public void generateMain(final LegacyDungeonWorld world, final int x, final int y, final int z) {
		super.generateMain(world, x, y, z);
		
		ITimedJob job = new ITimedJob() {

			@Override
			public void work() {
				
				ITimedJob job = new ITimedJob() {

					@Override
					public void work() {
				
						for(int i = 1; i < 4; i++) {
							LegacyBlock bl = world.getBlock(x, y + 2, z + i);
							if(bl == ModBlocks.brick_jungle || bl == ModBlocks.brick_jungle_cracked || bl == ModBlocks.brick_jungle_lava) {
								world.setBlock(x, y + 2, z + i, ModBlocks.brick_jungle_trap, Trap.POISON_DART.ordinal(), 3);
							}
						}
						
						for(int i = 1; i < 4; i++) {
							LegacyBlock bl = world.getBlock(x + parent.width - 1, y + 2, z + i);
							if(bl == ModBlocks.brick_jungle || bl == ModBlocks.brick_jungle_cracked || bl == ModBlocks.brick_jungle_lava) {
								world.setBlock(x + parent.width - 1, y + 2, z + i, ModBlocks.brick_jungle_trap, Trap.POISON_DART.ordinal(), 3);
							}
						}
						
						for(int i = 1; i < 4; i++) {
							LegacyBlock bl = world.getBlock(x + i, y + 2, z);
							if(bl == ModBlocks.brick_jungle || bl == ModBlocks.brick_jungle_cracked || bl == ModBlocks.brick_jungle_lava) {
								world.setBlock(x + i, y + 2, z, ModBlocks.brick_jungle_trap, Trap.POISON_DART.ordinal(), 3);
							}
						}
						
						for(int i = 1; i < 4; i++) {
							LegacyBlock bl = world.getBlock(x + i, y + 2, z + parent.width - 1);
							if(bl == ModBlocks.brick_jungle || bl == ModBlocks.brick_jungle_cracked || bl == ModBlocks.brick_jungle_lava) {
								world.setBlock(x + i, y + 2, z + parent.width - 1, ModBlocks.brick_jungle_trap, Trap.POISON_DART.ordinal(), 3);
							}
						}
					}
				};
				
				parent.timed.addOp(job);
			}
		};
		
		parent.timed.addOp(job);
	}
}
