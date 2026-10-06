package com.hbm.world.gen.nbt;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockSkeletonHolder;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import dev.golem.core.data.NbtFiles;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The 1.7 tile-entity data of the bundled 1.7 structure files, carried into the port's block entities when a structure is
 * built (what 1.7's {@code NBTStructure.buildTileEntity} did with {@code readFromNBT}, plus its {@code itemPalette}
 * relinking of numeric item ids).
 *
 * <ul>
 *   <li><b>Key-compatible</b>: the port kept 1.7's NBT keys for these ({@code primed}/{@code waiting}, a fluid tank's
 *   {@code tank}/{@code tank_max}/{@code tank_type}/{@code tank_p}, {@code mode}, {@code exploded}, {@code type},
 *   {@code priority}/{@code redLow}/{@code redHigh}, {@code rotation}/{@code isOn}, {@code phase}, {@code color},
 *   {@code channel}, telex {@code tx}/{@code rx}, turret target flags, ...): the 1.7 compound (without its item list and
 *   position) is loaded as it is.</li>
 *   <li><b>Renamed</b>: the diesel generator's 1.7 tank was {@code fuel*}; the port's is {@code tank*}.</li>
 *   <li><b>Items</b>: {@code items} (HBM, {@code slot}) and {@code Items} (vanilla, {@code Slot}) lists, numeric ids
 *   resolved through the file's {@code itemPalette} (1.7 name, {@code hbm:item.x} = {@code hbm:x}, a material item's
 *   damage = its {@code Mats} id), put into the container's slots; the skeleton holder's {@code item}; a book and quill
 *   keeps its pages.</li>
 *   <li><b>Block swaps</b>: a 1.7 flower pot held its plant in the tile entity, a 1.7 skull its type and floor rotation;
 *   the modern block carries both, so the placed block is replaced.</li>
 *   <li><b>Nothing to carry</b>: tiles whose 1.7 NBT in these files holds only defaults (cable, geiger, charger,
 *   satellite receiver, steel roof, crab spawner marker, sign with no text, ...).</li>
 * </ul>
 * Returns false for a 1.7 id not listed here, which the caller logs as not migrated.
 */
public final class LegacyStructureTiles {

    private LegacyStructureTiles() {}

    /** 1.7 ids whose compound loads into the port block entity unchanged (apart from items and position). */
    private static final Set<String> KEY_COMPATIBLE = Set.of(
            "tileentity_landmine", "tileentity_fluid_barrel", "tileentity_ntm_bobblehead", "tileentity_battery",
            "tileentity_floodlight", "tileentity_ntm_dungeon_spawner", "tileentity_connector_redwire",
            "tileentity_radio_receiver", "tileentity_rtty_telex", "tileentity_fluid_tank",
            "tileentity_turret_howard_damaged", "tileentity_turret_sentry_damaged", "tileentity_pipe_gauge",
            "tileentity_pipe_base", "tileentity_electric_furnace", "tileentity_microwave", "tileentity_funnel",
            "tileentity_tesla_coil", "tileentity_supply_crate", "tileentity_crate_iron", "tileentity_rotary_furnace");

    /** 1.7 ids with nothing but defaults in the bundled files (or data the port keeps elsewhere). */
    private static final Set<String> NOTHING_TO_CARRY = Set.of(
            "tileentity_cable", "tileentity_geiger", "tileentity_ntm_charger", "tileentity_satellitereceicer",
            "tileentity_deco", "tileentity_deco_f", "tileentity_crabs", "tileentity_ntm_loot", "tileentity_data",
            "tileentity_proxy_combo", "Sign", "Comparator");

    public static boolean apply(LevelAccessor level, BlockPos pos, @Nullable BlockEntity be, CompoundTag nbt,
                                Map<Integer, String> itemPalette, int coordBaseMode) {
        String id = Tags.getStringOr(nbt, "id", "");

        if (id.equals("FlowerPot")) return flowerPot(level, pos, nbt, itemPalette);
        if (id.equals("Skull")) return skull(level, pos, nbt, coordBaseMode);
        if (NOTHING_TO_CARRY.contains(id)) return true;
        if (be == null) return false;

        if (id.equals("tileentity_ntm_skeleton")) {
            if (be instanceof BlockSkeletonHolder.TileEntitySkeletonHolder holder && Tags.contains(nbt, "item")) {
                holder.setItem(stack(NbtFiles.compoundOrNull(nbt, "item"), itemPalette));
            }
            return true;
        }

        CompoundTag data;
        if (KEY_COMPATIBLE.contains(id)) {
            data = nbt.copy();
        } else if (id.equals("tileentity_diesel_generator")) {
            data = nbt.copy();
            for (String suffix : new String[]{"", "_max", "_type", "_p"}) {
                if (Tags.contains(nbt, "fuel" + suffix)) data.put("tank" + suffix, nbt.get("fuel" + suffix));
            }
        } else if (id.equals("Chest")) {
            data = new CompoundTag();
        } else {
            return false;
        }
        legacyFluidIds(id, data);
        data.remove("items");
        data.remove("Items");
        data.remove("x");
        data.remove("y");
        data.remove("z");
        data.remove("id");
        if (!data.isEmpty()) BlockEntities.loadWithComponents(be, data, level.registryAccess());

        if (be instanceof Container container) {
            for (String key : new String[]{"items", "Items"}) {
                ListTag list = NbtFiles.list(nbt, key);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag item = Tags.getCompoundOrEmpty(list, i);
                    int slot = Tags.contains(item, "slot") ? Tags.getIntOr(item, "slot", -1) : Tags.getIntOr(item, "Slot", -1);
                    ItemStack stack = stack(item, itemPalette);
                    if (slot >= 0 && slot < container.getContainerSize() && !stack.isEmpty()) container.setItem(slot, stack);
                }
            }
        }
        return true;
    }

    /**
     * The bundled files are verbatim 1.7 data, so their fluids are 1.7 numbers: every tank's {@code <name>_type}
     * (1.7 {@code FluidTank.writeToNBT}, recognised by its {@code <name>_max} sibling) and a pipe's {@code type}. The
     * port saves a fluid only by registry name (History section 19ej-ids), so they are translated here through the
     * frozen 1.7 table ({@link Fluids#byLegacyId}) - the one place a 1.7 fluid number is still read.
     */
    private static void legacyFluidIds(String id, CompoundTag data) {
        for (String key : Tags.keys(data)) {
            if (key.endsWith("_type") && Tags.hasInt(data, key)
                    && Tags.contains(data, key.substring(0, key.length() - "_type".length()) + "_max")) {
                Fluids.write(data, key, Fluids.byLegacyId(Tags.getIntOr(data, key, 0)));
            }
        }
        if ((id.equals("tileentity_pipe_base") || id.equals("tileentity_pipe_gauge")) && Tags.hasInt(data, "type")) {
            Fluids.write(data, "type", Fluids.byLegacyId(Tags.getIntOr(data, "type", 0)));
        }
        // 1.7 enum ordinals the port now saves by name: the battery's ConnectionPriority, the dungeon spawner's type
        if (id.equals("tileentity_battery") && Tags.hasInt(data, "priority")) {
            com.hbm.util.SavedIds.putEnum(data, "priority", com.hbm.util.EnumUtil.grabEnumSafely(
                    com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority.class, Tags.getIntOr(data, "priority", 0)));
        }
        // the bobblehead's figure: 1.7 saved the BobbleType ordinal as a byte, the port its name (History §19es-itemvariant)
        if (id.equals("tileentity_ntm_bobblehead") && Tags.hasInt(data, "type")) {
            com.hbm.util.SavedIds.putEnum(data, "type",
                    com.hbm.blocks.generic.BlockBobble.BobbleType.of(Tags.getIntOr(data, "type", 0)));
        }
        if (id.equals("tileentity_ntm_dungeon_spawner") && Tags.hasInt(data, "type")) {
            com.hbm.util.SavedIds.putEnum(data, "type", com.hbm.util.EnumUtil.grabEnumSafely(
                    com.hbm.blocks.generic.DungeonSpawner.EnumSpawnerType.class, Tags.getIntOr(data, "type", 0)));
        }
    }

    /** A 1.7 item compound ({@code id} numeric, {@code Count}, {@code Damage}, {@code tag}) as a port stack. */
    public static ItemStack stack(@Nullable CompoundTag item, Map<Integer, String> itemPalette) {
        if (item == null) return ItemStack.EMPTY;
        int numeric = Tags.getIntOr(item, "id", 0);
        String name = itemPalette.getOrDefault(numeric, VANILLA_ITEMS.getOrDefault(numeric, ""));
        int count = Math.max(1, Tags.getIntOr(item, "Count", 1));
        int damage = Tags.getIntOr(item, "Damage", 0);
        Item resolved = resolve(name, damage);
        if (resolved == null) {
            Constants.LOG.info("[NBTStructure] 1.7 item {} (id {}, damage {}) has no port counterpart", name, numeric, damage);
            return ItemStack.EMPTY;
        }
        if (resolved == Items.WRITABLE_BOOK) {
            CompoundTag tag = NbtFiles.compoundOrNull(item, "tag");
            List<String> pages = new ArrayList<>();
            ListTag list = tag == null ? new ListTag() : NbtFiles.list(tag, "pages");
            for (int i = 0; i < list.size(); i++) pages.add(NbtFiles.stringAt(list, i));
            return ItemStacks.writableBook(pages);
        }
        return new ItemStack(resolved, count);
    }

    /** 1.7 registry name + damage to a port item: {@code hbm:item.x} is {@code hbm:x}, a material item {@code hbm:x_<mat>}. */
    private static @Nullable Item resolve(String name, int damage) {
        if (name.isEmpty()) return null;
        String path = name;
        if (name.startsWith("hbm:item.")) path = "hbm:" + name.substring("hbm:item.".length());
        else if (name.startsWith("hbm:tile.")) path = "hbm:" + name.substring("hbm:tile.".length());
        if (path.startsWith("hbm:") && damage > 0) {
            NTMMaterial mat = Mats.matById.get(damage);
            if (mat != null) {
                Item item = item(path + "_" + mat.name);
                if (item != null) return item;
            }
        }
        return item(path);
    }

    private static @Nullable Item item(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0) return null;
        var key = dev.golem.core.Ids.of(id.substring(0, colon), id.substring(colon + 1));
        if (!BuiltInRegistries.ITEM.containsKey(key)) return null;
        Item item = BuiltInRegistries.ITEM.getValue(key);
        return item == Items.AIR ? null : item;
    }

    /** The vanilla numeric item ids the bundled 1.7 files use without an itemPalette entry. */
    private static final Map<Integer, String> VANILLA_ITEMS = Map.of(
            76, "minecraft:redstone_torch", 386, "minecraft:writable_book", 387, "minecraft:written_book",
            6, "minecraft:oak_sapling", 31, "minecraft:short_grass", 32, "minecraft:dead_bush",
            37, "minecraft:dandelion", 38, "minecraft:poppy", 39, "minecraft:brown_mushroom", 40, "minecraft:red_mushroom");

    /** 1.7 {@code TileEntityFlowerPot}: {@code Item} (numeric) + {@code Data}; the modern pot is a block per plant. */
    private static boolean flowerPot(LevelAccessor level, BlockPos pos, CompoundTag nbt, Map<Integer, String> itemPalette) {
        int item = Tags.getIntOr(nbt, "Item", 0);
        int data = Tags.getIntOr(nbt, "Data", 0);
        Block potted = switch (item) {
            case 6 -> switch (data & 7) {
                case 1 -> Blocks.POTTED_SPRUCE_SAPLING;
                case 2 -> Blocks.POTTED_BIRCH_SAPLING;
                case 3 -> Blocks.POTTED_JUNGLE_SAPLING;
                case 4 -> Blocks.POTTED_ACACIA_SAPLING;
                case 5 -> Blocks.POTTED_DARK_OAK_SAPLING;
                default -> Blocks.POTTED_OAK_SAPLING;
            };
            case 31 -> Blocks.POTTED_FERN;
            case 32 -> Blocks.POTTED_DEAD_BUSH;
            case 37 -> Blocks.POTTED_DANDELION;
            case 38 -> switch (data) {
                case 1 -> Blocks.POTTED_BLUE_ORCHID;
                case 2 -> Blocks.POTTED_ALLIUM;
                case 3 -> Blocks.POTTED_AZURE_BLUET;
                case 4 -> Blocks.POTTED_RED_TULIP;
                case 5 -> Blocks.POTTED_ORANGE_TULIP;
                case 6 -> Blocks.POTTED_WHITE_TULIP;
                case 7 -> Blocks.POTTED_PINK_TULIP;
                case 8 -> Blocks.POTTED_OXEYE_DAISY;
                default -> Blocks.POTTED_POPPY;
            };
            case 39 -> Blocks.POTTED_BROWN_MUSHROOM;
            case 40 -> Blocks.POTTED_RED_MUSHROOM;
            case 81 -> Blocks.POTTED_CACTUS;
            default -> null;
        };
        if (potted != null && level.getBlockState(pos).is(Blocks.FLOWER_POT)) {
            level.setBlock(pos, potted.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /**
     * 1.7 {@code TileEntitySkull}: {@code SkullType} (0 skeleton, 1 wither, 2 zombie, 3 player, 4 creeper) and, for a skull
     * standing on the floor (block meta 1), {@code Rot} in sixteenths; the wall skulls kept their facing in the metadata,
     * which the palette already turned into the placed state.
     */
    private static boolean skull(LevelAccessor level, BlockPos pos, CompoundTag nbt, int coordBaseMode) {
        BlockState placed = level.getBlockState(pos);
        int type = Tags.getIntOr(nbt, "SkullType", 0);
        boolean wall = placed.hasProperty(net.minecraft.world.level.block.WallSkullBlock.FACING);
        Block block = switch (type) {
            case 1 -> wall ? Blocks.WITHER_SKELETON_WALL_SKULL : Blocks.WITHER_SKELETON_SKULL;
            case 2 -> wall ? Blocks.ZOMBIE_WALL_HEAD : Blocks.ZOMBIE_HEAD;
            case 3 -> wall ? Blocks.PLAYER_WALL_HEAD : Blocks.PLAYER_HEAD;
            case 4 -> wall ? Blocks.CREEPER_WALL_HEAD : Blocks.CREEPER_HEAD;
            default -> wall ? Blocks.SKELETON_WALL_SKULL : Blocks.SKELETON_SKULL;
        };
        BlockState state;
        if (wall) {
            Direction facing = placed.getValue(net.minecraft.world.level.block.WallSkullBlock.FACING);
            state = block.defaultBlockState().setValue(net.minecraft.world.level.block.WallSkullBlock.FACING, facing);
        } else {
            state = block.defaultBlockState().setValue(SkullBlock.ROTATION, Tags.getIntOr(nbt, "Rot", 0) & 15)
                    .rotate(rotationOf(coordBaseMode));
        }
        if (placed.getBlock() != state.getBlock() || !placed.equals(state)) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }

    private static Rotation rotationOf(int coordBaseMode) {
        return switch (coordBaseMode & 3) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
