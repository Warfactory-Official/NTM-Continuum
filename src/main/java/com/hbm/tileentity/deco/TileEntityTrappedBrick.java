package com.hbm.tileentity.deco;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.generic.TrappedBrick;
import com.hbm.blocks.generic.TrappedBrick.Trap;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.tileentity.ModDungeonBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.7 {@code TileEntityTrappedBrick}: the detector half of the trapped enargite bricks. Every server tick it looks
 * for a player inside the trap's detection box and springs the trap once one is there.
 *
 * <p>The box is built lazily on the first tick, as 1.7 did: the falling rocks and the spiders watch the 3x3 column
 * three blocks below, the pillar a narrow column below, the zombie the 3x3 layer above; the arrow traps pick a
 * random horizontal side that is air and watch a thin 3-block beam out of it (and fall back to the brick's own
 * cell if every side is closed). The side is not persisted, again as in 1.7.
 */
public class TileEntityTrappedBrick extends CompatBlockEntity {

    private AABB detector = null;
    private Direction dir = null;

    public TileEntityTrappedBrick(BlockPos pos, BlockState state) {
        super(ModDungeonBlockEntities.TRAPPED_BRICK.get(), pos, state);
    }

    private Trap trap() {
        return getBlockState().getBlock() instanceof TrappedBrick brick ? brick.trap : Trap.FIRE;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityTrappedBrick brick) {
        if (brick.detector == null) brick.setDetector();
        if (!level.getEntitiesOfClass(Player.class, brick.detector).isEmpty()) brick.trigger();
    }

    private void trigger() {
        ServerLevel level = (ServerLevel) this.level;
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        switch (trap()) {
            case FALLING_ROCKS -> {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        EntityRubble rubble = new EntityRubble(level, x - 0.5 + i, y - 0.5, z - 0.5 + j);
                        rubble.setBlockState(ModBlocks.reinforced_stone.get().defaultBlockState());
                        level.addFreshEntity(rubble);
                    }
                }
            }
            case ARROW, FLAMING_ARROW -> {
                Arrow arrow = new Arrow(EntityType.ARROW, level);
                int ox = dir == null ? 0 : dir.getStepX();
                int oz = dir == null ? 0 : dir.getStepZ();
                arrow.setPos(x + 0.5 + ox, y + 0.5, z + 0.5 + oz);
                arrow.setDeltaMovement(ox, 0, oz);
                if (trap() == Trap.FLAMING_ARROW) Effects.ignite(arrow, 60);
                level.addFreshEntity(arrow);
            }
            case PILLAR -> {
                for (int i = 0; i < 3; i++)
                    level.setBlockAndUpdate(worldPosition.below(1 + i), ModDecoContent.concrete_pillar.get().defaultBlockState());
            }
            case POISON_DART -> {
                // 1.7: "TBI" - never implemented, the trap only clicks
            }
            case ZOMBIE -> {
                Zombie zombie = Entities.createTriggered(EntityType.ZOMBIE, level);
                if (zombie != null) {
                    zombie.setPos(x + 0.5, y + 1, z + 0.5);
                    ItemStack held = switch (level.getRandom().nextInt(3)) {
                        case 0 -> new ItemStack(ModMeleeKitItems.chernobylsign.get());
                        case 1 -> new ItemStack(ModAbilityToolItems.cobalt_sword.get());
                        default -> new ItemStack(ModAbilityToolItems.cmb_hoe.get());
                    };
                    zombie.setItemSlot(EquipmentSlot.MAINHAND, held);
                    zombie.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
                    level.addFreshEntity(zombie);
                }
            }
            case SPIDERS -> {
                for (int i = 0; i < 3; i++) {
                    Mob spider = Entities.createTriggered(EntityType.CAVE_SPIDER, level);
                    if (spider == null) continue;
                    spider.setPos(x + 0.5, y - 1, z + 0.5);
                    level.addFreshEntity(spider);
                }
            }
            default -> { }
        }

        TrappedBrick.spring(level, worldPosition);
    }

    private void setDetector() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        switch (trap()) {
            case FALLING_ROCKS, SPIDERS -> detector = new AABB(x - 1, y - 3, z - 1, x + 2, y, z + 2);
            case PILLAR -> detector = new AABB(x + 0.2, y - 3, z + 0.2, x + 0.8, y, z + 0.8);
            case ARROW, FLAMING_ARROW, POISON_DART -> setDetectorDirectional();
            case ZOMBIE -> detector = new AABB(x - 1, y + 1, z - 1, x + 2, y + 2, z + 2);
            default -> detector = new AABB(x, y, z, x + 1, y + 1, z + 1);
        }
    }

    private void setDetectorDirectional() {
        List<Direction> dirs = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        Collections.shuffle(dirs);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        for (Direction d : dirs) {
            if (level.getBlockState(worldPosition.relative(d)).isAir()) {
                double minX = x + 0.4, minY = y + 0.4, minZ = z + 0.4;
                double maxX = x + 0.6, maxY = y + 0.6, maxZ = z + 0.6;
                if (d.getStepX() > 0) maxX += 3;
                else if (d.getStepX() < 0) minX -= 3;
                if (d.getStepZ() > 0) maxZ += 3;
                else if (d.getStepZ() < 0) minZ -= 3;
                detector = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
                this.dir = d;
                return;
            }
        }
        detector = new AABB(x, y, z, x + 1, y + 1, z + 1);
    }
}
