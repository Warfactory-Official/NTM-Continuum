package com.hbm.blocks.generic;

import com.hbm.config.ServerConfig;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.hbm.world.gen.nbt.NBTStructure;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 1.7 {@code BlockWandStructure} ({@code wand_structure}): the structure saving block (metadata 0) and structure
 * loading block (metadata 1), here the {@link #MODE} property with the variant picked by the item's
 * item {@code wand_structure_load} (1.7 metadata 1).
 *
 * <p>The saving block covers the box starting one above it ({@code sizeX/Y/Z}, drawn as a white wire box) and
 * saves it to {@code <game dir>/structures/<name>.nbt}, leaving out air and its blacklist; a block in hand toggles
 * that block on the blacklist. The loading block builds {@code <name>.nbt} one above it, wiping the box first, in
 * structure-debug mode unless it is powered, and then turns into a saving block for the loaded size.
 */
public class BlockWandStructure extends CompatBlock implements TickingBlock, ILookOverlay {

    public enum Mode implements StringRepresentable {
        SAVE("save"), LOAD("load");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    /** Client hooks that open the save and load screens; installed by the client registrations. */
    public static Consumer<TileEntityWandStructure> OPEN_SAVE = tile -> {};
    public static Consumer<TileEntityWandStructure> OPEN_LOAD = tile -> {};

    public BlockWandStructure(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MODE, Mode.SAVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWandStructure(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int variant = com.hbm.items.LegacyMeta.of(context.getItemInHand());
        return defaultBlockState().setValue(MODE, variant == 1 ? Mode.LOAD : Mode.SAVE);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandStructure structure)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!player.isShiftKeyDown()) {
            Block block = ModStructureWandBlocks.getBlockFromStack(held);
            if (block != null && !ModStructureWandBlocks.isStructureBlock(block, true)) {
                if (!level.isClientSide()) {
                    BlockState bm = block.defaultBlockState();

                    if (structure.blacklist.contains(bm)) {
                        structure.blacklist.remove(bm);
                    } else {
                        structure.blacklist.add(bm);
                    }
                    structure.setChanged();
                }
                return InteractionResult.SUCCESS;
            }

            if (level.isClientSide()) {
                if (state.getValue(MODE) == Mode.LOAD) OPEN_LOAD.accept(structure);
                else OPEN_SAVE.accept(structure);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public @Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this) || state.getValue(MODE) != Mode.SAVE) return null;
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandStructure structure)) return null;

        List<String> text = new ArrayList<>();

        text.add(ChatFormatting.GRAY + "Name: " + ChatFormatting.RESET + structure.name);

        text.add(ChatFormatting.GRAY + "Blacklist:");
        for (BlockState bm : structure.blacklist) {
            text.add(ChatFormatting.RED + "- " + bm.getBlock().getDescriptionId() + " : " + propertiesOf(bm));
        }

        return new Data(Component.translatable("block.hbm.wand_structure.save").getString(), 0xffff00, 0x404000, text);
    }

    /** What 1.7 printed as the metadata: the state's properties, or 0 for a block without any. */
    static String propertiesOf(BlockState state) {
        String text = LegacyStructurePalette.stateString(state);
        int bracket = text.indexOf('[');
        return bracket < 0 ? "0" : text.substring(bracket + 1, text.length() - 1);
    }

    public static class TileEntityWandStructure extends TileEntityLoadedBase implements IControlReceiver {

        public String name = "";

        public int sizeX = 1;
        public int sizeY = 1;
        public int sizeZ = 1;

        /** 1.7 kept (block, metadata) pairs; the flattened pair is one state (the ids are world-local, as 1.7's were). */
        public Set<BlockState> blacklist = new LinkedHashSet<>();

        public TileEntityWandStructure(BlockPos pos, BlockState state) {
            super(ModStructureWandBlocks.WAND_STRUCTURE_ENTITY.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWandStructure tile) {
            tile.networkPackNT();
        }

        public void saveStructure(Player player) {
            Level world = level;
            if (world == null) return;

            if (name.isEmpty()) {
                player.sendSystemMessage(Component.literal("Could not save: invalid name").withStyle(ChatFormatting.RED));
                return;
            }

            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                player.sendSystemMessage(Component.literal("Could not save: invalid dimensions").withStyle(ChatFormatting.RED));
                return;
            }

            BlockState air = Blocks.AIR.defaultBlockState();
            boolean hadAir = !blacklist.add(air);

            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            Path file = NBTStructure.quickSaveArea(name + ".nbt", world, x, y + 1, z, x + sizeX - 1, y + sizeY, z + sizeZ - 1, blacklist);

            if (!hadAir) blacklist.remove(air);

            if (file == null) {
                player.sendSystemMessage(Component.literal("Failed to save structure").withStyle(ChatFormatting.RED));
                return;
            }

            // 1.7 made the name a clickable OPEN_FILE link to the folder; the underlined name is kept
            Component fileText = Component.literal(file.getFileName().toString()).withStyle(ChatFormatting.UNDERLINE);
            player.sendSystemMessage(Component.literal("Saved structure as ").append(fileText));
        }

        public void loadStructure(Player player) {
            Level world = level;
            if (world == null) return;

            if (name.isEmpty()) {
                player.sendSystemMessage(Component.literal("Could not load: no filename specified").withStyle(ChatFormatting.RED));
                return;
            }

            Path structureFile = NBTStructure.structureDirectory().resolve(name + ".nbt");
            try {
                Files.createDirectories(structureFile.getParent());
            } catch (Exception ignored) {
                // 1.7 mkdir()
            }

            boolean debug = !world.hasNeighborSignal(worldPosition);
            boolean previousDebug = ServerConfig.STRUCTURE_DEBUG.get();
            ServerConfig.STRUCTURE_DEBUG.set(debug);

            try {
                if (!Files.isRegularFile(structureFile)) {
                    player.sendSystemMessage(Component.literal("Could not load: file not found").withStyle(ChatFormatting.RED));
                    return;
                }

                NBTStructure structure = new NBTStructure(structureFile);

                sizeX = structure.getSizeX();
                sizeY = structure.getSizeY();
                sizeZ = structure.getSizeZ();

                structure.build(world, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), 0, false, true);

                world.setBlock(worldPosition, getBlockState().setValue(MODE, Mode.SAVE), 3);
                setChanged();

                player.sendSystemMessage(Component.literal("Structure loaded"));

            } catch (Exception ex) {
                player.sendSystemMessage(Component.literal("Could not load: file not found").withStyle(ChatFormatting.RED));
            } finally {
                ServerConfig.STRUCTURE_DEBUG.set(previousDebug);
            }
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(name);

            buf.writeInt(sizeX);
            buf.writeInt(sizeY);
            buf.writeInt(sizeZ);

            buf.writeInt(blacklist.size());
            for (BlockState bm : blacklist) {
                buf.writeInt(Block.getId(bm));
                buf.writeInt(0);
            }
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            name = buf.readUtf();

            sizeX = buf.readInt();
            sizeY = buf.readInt();
            sizeZ = buf.readInt();

            int count = buf.readInt();
            blacklist = new LinkedHashSet<>();
            for (int i = 0; i < count; i++) {
                BlockState block = Block.stateById(buf.readInt());
                buf.readInt();
                blacklist.add(block);
            }
        }

        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putString("name", name);

            out.putInt("sizeX", sizeX);
            out.putInt("sizeY", sizeY);
            out.putInt("sizeZ", sizeZ);

            // block-state names, never global state ids (History section 19ej-ids)
            DataOut.ChildList blocks = out.childrenList("blacklist");
            for (BlockState state : blacklist) com.hbm.util.SavedIds.putState(blocks.addChild(), "state", state);
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            name = in.getStringOr("name", "");

            sizeX = in.getIntOr("sizeX", 0);
            sizeY = in.getIntOr("sizeY", 0);
            sizeZ = in.getIntOr("sizeZ", 0);

            blacklist = new LinkedHashSet<>();
            if (in.getIntArray("blocks").isPresent()) com.hbm.util.SavedIds.warnNumeric("block state id array", "blocks");
            for (DataIn entry : in.childrenListOrEmpty("blacklist")) {
                BlockState state = com.hbm.util.SavedIds.getState(entry, "state", null);
                if (state != null) blacklist.add(state);
            }
        }

        @Override
        public boolean hasPermission(Player player) {
            return true;
        }

        @Override
        public void receiveControl(CompoundTag data) {}

        /** The screens send the edited name (and sizes), plus {@code save}/{@code load} when their button closed them. */
        @Override
        public void receiveControl(Player player, CompoundTag nbt) {
            name = Tags.getStringOr(nbt, "name", name);
            if (Tags.hasInt(nbt, "sizeX")) sizeX = Tags.getIntOr(nbt, "sizeX", sizeX);
            if (Tags.hasInt(nbt, "sizeY")) sizeY = Tags.getIntOr(nbt, "sizeY", sizeY);
            if (Tags.hasInt(nbt, "sizeZ")) sizeZ = Tags.getIntOr(nbt, "sizeZ", sizeZ);
            setChanged();

            if (Tags.getBooleanOr(nbt, "save", false)) {
                saveStructure(player);
            }

            if (Tags.getBooleanOr(nbt, "load", false)) {
                loadStructure(player);
            }
        }
    }
}
