package com.hbm.blocks.generic;

import com.hbm.blocks.ModDecoBlocks;
import com.hbm.advancement.HbmCriteria;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorSetItems;
import com.hbm.items.ModItems;
import com.hbm.sound.ModDoorSounds;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * 1.7 {@code BlockKeyhole}, the "Strange Stone": plain stone with a keyhole on its sides. The red key, or a
 * cracked red key (used up), turned in a side face carves the Red Room four blocks behind it: a 9x5x9 shell of
 * Red Room bricks (marked faces turned inward, plain on the edges), eight torches, a keyhole brick in one wall,
 * a one-in-four chance each of cobwebs, red concrete pillars, burning corners, a red concrete circle and lava
 * channels, and on the floor a pedestal with a red-room treasure - or, one time in twenty, a pile of power armor.
 * A red door replaces the keyhole and the block under it, and every dropped item inside is cleared.
 *
 * <p>1.7 built this on {@code BlockStone}, so the block itself keeps stone's hardness-less defaults: no hardness
 * (1.7 set none), drops cobblestone, top and bottom are plain stone. Turning the key grants the Red Room
 * advancement ({@code hbm:red_room}), as 1.7 did.
 */
public class BlockKeyhole extends CompatBlock {

    public BlockKeyhole(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return useKey(level, pos, player, stack, hit, false);
    }

    /** The shared key turn of both keyholes; {@code black} picks the Red Room brick keyhole's black room. */
    static InteractionResult useKey(Level level, BlockPos pos, Player player, ItemStack stack, BlockHitResult hit,
                                    boolean black) {
        boolean cracked = stack.is(ModItems.key_red_cracked.get());
        Direction side = hit.getDirection();
        if ((!stack.is(ModItems.key_red.get()) && !cracked) || side.getAxis() == Direction.Axis.Y) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (cracked) stack.shrink(1);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        int x = pos.getX() - side.getStepX() * 4, y = pos.getY() - 2, z = pos.getZ() - side.getStepZ() * 4;
        if (black) BlockRedBrickKeyhole.generateRoom(level, x, y, z, side);
        else generateRoom(level, x, y, z);
        // 1.7 ItemModDoor.placeDoorBlock with the door turned to face into the room
        placeDoor(level, pos.below(), side.getOpposite(), ModDoorBlocks.DOOR_RED.get());
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModDoorSounds.LOCK_OPEN.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        // 1.7 achRedRoom, for the player who turned the key (both keyholes)
        if (player instanceof ServerPlayer server) {
            HbmCriteria.ROOM_OPENED.fire(server);
        }
        return InteractionResult.SUCCESS;
    }

    static BlockState redBrick(Direction marked) {
        return ModDecoContent.brick_red.get().defaultBlockState().setValue(BlockRedBrick.FACING, marked);
    }

    static BlockState redBrickPlain() {
        return ModDecoContent.brick_red.get().defaultBlockState().setValue(BlockRedBrick.PLAIN, true);
    }

    static void set(Level level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    protected static void generateRoom(Level level, int x, int y, int z) {
        RandomSource rand = level.getRandom();
        int size = 9;
        int height = 5;
        int width = size / 2;
        BlockState plain = redBrickPlain();

        // Outer edges, top and bottom
        for (int i = -width; i <= width; i++) {
            set(level, x + i, y, z + width, plain);
            set(level, x + i, y, z - width, plain);
            set(level, x + width, y, z + i, plain);
            set(level, x - width, y, z + i, plain);
            set(level, x + i, y + height - 1, z + width, plain);
            set(level, x + i, y + height - 1, z - width, plain);
            set(level, x + width, y + height - 1, z + i, plain);
            set(level, x - width, y + height - 1, z + i, plain);
        }

        for (int i = 1; i <= height - 2; i++) {
            // Outer edges, sides
            set(level, x + width, y + i, z + width, plain);
            set(level, x + width, y + i, z - width, plain);
            set(level, x - width, y + i, z + width, plain);
            set(level, x - width, y + i, z - width, plain);

            // Walls, marked faces inward (1.7 metas 4, 5, 2, 3)
            for (int j = -width + 1; j <= width - 1; j++) {
                set(level, x + width, y + i, z + j, redBrick(Direction.WEST));
                set(level, x - width, y + i, z + j, redBrick(Direction.EAST));
                set(level, x + j, y + i, z + width, redBrick(Direction.NORTH));
                set(level, x + j, y + i, z - width, redBrick(Direction.SOUTH));
            }
        }

        // 1.7 rand.nextInt(1) == 0: always
        if (rand.nextInt(1) == 0) {
            int r = rand.nextInt(4);
            BlockState keyhole = ModDungeonBlocks.stone_keyhole_meta.get().defaultBlockState();
            if (r == 0) set(level, x + width, y + 2, z, keyhole.setValue(BlockRedBrickKeyhole.FACING, Direction.WEST));
            if (r == 1) set(level, x - width, y + 2, z, keyhole.setValue(BlockRedBrickKeyhole.FACING, Direction.EAST));
            if (r == 2) set(level, x, y + 2, z + width, keyhole.setValue(BlockRedBrickKeyhole.FACING, Direction.NORTH));
            if (r == 3) set(level, x, y + 2, z - width, keyhole.setValue(BlockRedBrickKeyhole.FACING, Direction.SOUTH));
        }

        for (int i = -width + 1; i <= width - 1; i++) {
            for (int j = -width + 1; j <= width - 1; j++) {
                // Floor (1.7 meta 1, marked face up) and ceiling (meta 0, marked face down)
                set(level, x + i, y, z + j, redBrick(Direction.UP));
                set(level, x + i, y + height - 1, z + j, redBrick(Direction.DOWN));
                for (int k = 1; k <= height - 2; k++) set(level, x + i, y + k, z + j, Blocks.AIR.defaultBlockState());
            }
        }

        // Torches: 1.7 placed plain torches, which attached themselves to the wall beside them
        int torchDist = width - 1;
        int torchOff = torchDist - 1;
        set(level, x + torchDist, y + 2, z + torchOff, wallTorch(Direction.WEST));
        set(level, x + torchDist, y + 2, z - torchOff, wallTorch(Direction.WEST));
        set(level, x - torchDist, y + 2, z + torchOff, wallTorch(Direction.EAST));
        set(level, x - torchDist, y + 2, z - torchOff, wallTorch(Direction.EAST));
        set(level, x + torchOff, y + 2, z + torchDist, wallTorch(Direction.NORTH));
        set(level, x - torchOff, y + 2, z + torchDist, wallTorch(Direction.NORTH));
        set(level, x + torchOff, y + 2, z - torchDist, wallTorch(Direction.SOUTH));
        set(level, x - torchOff, y + 2, z - torchDist, wallTorch(Direction.SOUTH));

        // Cobwebs
        if (rand.nextInt(4) == 0) {
            for (int i = -width + 1; i <= width - 1; i++) {
                for (int j = -width + 1; j <= width - 1; j++) {
                    if (rand.nextBoolean()) set(level, x + i, y + height - 2, z + j, Blocks.COBWEB.defaultBlockState());
                }
            }
        }

        BlockState redConcrete = ModDecoBlocks.CONCRETE_COLORED.get().stateForVariant(14);

        // Pillars
        if (rand.nextInt(4) == 0) {
            for (int i = 1; i <= height - 2; i++) {
                set(level, x + width - 2, y + i, z + width - 2, redConcrete);
                set(level, x + width - 2, y + i, z - width + 2, redConcrete);
                set(level, x - width + 2, y + i, z + width - 2, redConcrete);
                set(level, x - width + 2, y + i, z - width + 2, redConcrete);
            }
        }

        // Fire
        if (rand.nextInt(4) == 0) {
            BlockState netherrack = Blocks.NETHERRACK.defaultBlockState();
            BlockState fire = Blocks.FIRE.defaultBlockState();
            set(level, x + width - 1, y, z + width - 1, netherrack);
            set(level, x + width - 1, y, z - width + 1, netherrack);
            set(level, x - width + 1, y, z + width - 1, netherrack);
            set(level, x - width + 1, y, z - width + 1, netherrack);
            set(level, x + width - 1, y + 1, z + width - 1, fire);
            set(level, x + width - 1, y + 1, z - width + 1, fire);
            set(level, x - width + 1, y + 1, z + width - 1, fire);
            set(level, x - width + 1, y + 1, z - width + 1, fire);
        }

        // Circle
        if (rand.nextInt(4) == 0) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) set(level, x + i, y, z + j, redConcrete);
                }
            }
        }

        // Lava
        if (rand.nextInt(4) == 0) {
            BlockState lava = Blocks.LAVA.defaultBlockState();
            set(level, x + width - 2, y, z + width - 1, lava);
            set(level, x + width - 3, y, z + width - 1, lava);
            set(level, x - width + 2, y, z + width - 1, lava);
            set(level, x - width + 3, y, z + width - 1, lava);
            set(level, x + width - 2, y, z - width + 1, lava);
            set(level, x + width - 3, y, z - width + 1, lava);
            set(level, x - width + 2, y, z - width + 1, lava);
            set(level, x - width + 3, y, z - width + 1, lava);
            set(level, x + width - 1, y, z + width - 2, lava);
            set(level, x + width - 1, y, z + width - 3, lava);
            set(level, x + width - 1, y, z - width + 2, lava);
            set(level, x + width - 1, y, z - width + 3, lava);
            set(level, x - width + 1, y, z + width - 2, lava);
            set(level, x - width + 1, y, z + width - 3, lava);
            set(level, x - width + 1, y, z - width + 2, lava);
            set(level, x - width + 1, y, z - width + 3, lava);
        }

        if (rand.nextInt(20) == 0) {
            set(level, x, y + 1, z, ModDungeonBlocks.deco_loot.get().defaultBlockState());
            if (level.getBlockEntity(new BlockPos(x, y + 1, z)) instanceof BlockLoot.TileEntityLoot loot) {
                if (rand.nextInt(5) == 0) {
                    loot.addItem(new ItemStack(ModArmorItems.ncrpa_helmet.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(ModArmorItems.ncrpa_plate.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(ModArmorItems.ncrpa_legs.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(ModArmorItems.ncrpa_boots.get()), 0, 0, 0);
                } else {
                    loot.addItem(new ItemStack(ModArmorSetItems.trenchmaster_helmet.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(ModArmorSetItems.trenchmaster_plate.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(ModArmorSetItems.trenchmaster_legs.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(ModArmorSetItems.trenchmaster_boots.get()), 0, 0, 0);
                }
                loot.sync();
            }
        } else {
            spawnPedestalItem(level, x, y + 1, z);
        }

        clearDrops(level, x, y, z, size, height);
    }

    static BlockState wallTorch(Direction facing) {
        return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
    }

    /** 1.7: every item entity in the room's column is removed, the loose drops the carving knocked out. */
    static void clearDrops(Level level, int x, int y, int z, int size, int height) {
        AABB box = new AABB(x + 0.5, y, z + 0.5, x + 0.5, y + height, z + 0.5).inflate(size / 2D, 0, size / 2D);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box);
        for (ItemEntity item : items) item.discard();
    }

    /** 1.7 {@code spawnPedestalItem}: a pedestal holding one red-room treasure (the template, no count roll). */
    public static void spawnPedestalItem(Level level, int x, int y, int z) {
        BlockPos at = new BlockPos(x, y, z);
        level.setBlock(at, ModDungeonBlocks.pedestal.get().defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof BlockPedestal.TileEntityPedestal pedestal) {
            pedestal.setItem(ItemPool.getUnit(ItemPools.POOL_RED_PEDESTAL, level.getRandom()));
        }
    }

    /**
     * 1.7 {@code ItemModDoor.placeDoorBlock}: both halves of a door at {@code lower}, the hinge put on the side with
     * fewer solid blocks beside it (or next to a neighbouring door of the same kind).
     */
    static void placeDoor(Level level, BlockPos lower, Direction facing, Block door) {
        Direction left = facing.getCounterClockWise();
        BlockPos l = lower.relative(left), r = lower.relative(left.getOpposite());
        int i1 = (level.getBlockState(l).isRedstoneConductor(level, l) ? 1 : 0)
                + (level.getBlockState(l.above()).isRedstoneConductor(level, l.above()) ? 1 : 0);
        int j1 = (level.getBlockState(r).isRedstoneConductor(level, r) ? 1 : 0)
                + (level.getBlockState(r.above()).isRedstoneConductor(level, r.above()) ? 1 : 0);
        boolean doorLeft = level.getBlockState(l).is(door) || level.getBlockState(l.above()).is(door);
        boolean doorRight = level.getBlockState(r).is(door) || level.getBlockState(r.above()).is(door);
        boolean mirrored = (doorLeft && !doorRight) || j1 > i1;

        BlockState base = door.defaultBlockState().setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, mirrored ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
        level.setBlock(lower, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
        level.setBlock(lower.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
    }
}
