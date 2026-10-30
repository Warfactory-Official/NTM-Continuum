package com.hbm.gametest.deimport;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.deimport.engine.ChunkBlocks;
import com.hbm.deimport.engine.RegionIo;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.storage.RegionFile;

/**
 * Writes NTM: Decentralized Edition saves as DE 1.0.0-beta.12 writes them (shapes from DE's source: nested tanks,
 * {@code battery_pack_} items, multiblock cell blocks, per-chunk duct fluids, sealed crates, its chunk stamp), for the
 * importer's GameTests and harness scenario. Dev builds only.
 */
public final class DeFixtures {
    public static final String DE_VERSION = "1.0.0-beta.12";
    /** The fixture world's AES key for DE's sealed crates. */
    static final byte[] KEY = new byte[32];

    static {
        for (int i = 0; i < KEY.length; i++) KEY[i] = (byte) (i * 7 + 3);
    }

    /** A multiblock core the fixture placed, for laying DE cells over its footprint. */
    public record Core(BlockPos pos, String continuumBlock, String facing) {}

    private DeFixtures() {}

    public static CompoundTag block(String name, String... props) {
        CompoundTag entry = new CompoundTag();
        entry.putString("Name", name);
        if (props.length > 0) {
            CompoundTag p = new CompoundTag();
            for (int i = 0; i < props.length; i += 2) p.putString(props[i], props[i + 1]);
            entry.put("Properties", p);
        }
        return entry;
    }

    public static CompoundTag stack(String id, int count) {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", id);
        stack.putInt("count", count);
        return stack;
    }

    public static CompoundTag slot(int slot, CompoundTag stack) {
        CompoundTag copy = stack.copy();
        copy.putByte("Slot", (byte) slot);
        return copy;
    }

    public static CompoundTag inventory(CompoundTag... slots) {
        ListTag items = new ListTag();
        for (CompoundTag s : slots) items.add(s);
        CompoundTag inv = new CompoundTag();
        inv.put("Items", items);
        return inv;
    }

    static CompoundTag withComponent(CompoundTag stack, String id, Tag value) {
        CompoundTag components = stack.getCompound("components");
        components.put(id, value);
        stack.put("components", components);
        return stack;
    }

    static ListTag containerOf(CompoundTag stack) {
        CompoundTag entry = new CompoundTag();
        entry.putInt("slot", 0);
        entry.put("item", stack);
        ListTag list = new ListTag();
        list.add(entry);
        return list;
    }

    /** DE's {@code FluidTankNTM.serialize}: one compound per tank. */
    public static CompoundTag tank(String fluid, int fill, int capacity) {
        CompoundTag tank = new CompoundTag();
        tank.putInt("fill", fill);
        tank.putInt("capacity", capacity);
        tank.putInt("pressure", 0);
        tank.putString("type", fluid);
        if (fill > 0) tank.putString("held", fluid);
        return tank;
    }

    public static CompoundTag blockEntity(String id, BlockPos pos) {
        CompoundTag be = new CompoundTag();
        be.putString("id", id);
        be.putInt("x", pos.getX());
        be.putInt("y", pos.getY());
        be.putInt("z", pos.getZ());
        return be;
    }

    /** A ship holding one chunk as Sable saves it: sections by index from the level's lowest, the plot at grid square 0. */
    public static CompoundTag ship(CompoundTag chunk) {
        CompoundTag sections = new CompoundTag();
        for (Tag t : chunk.getList("sections", Tag.TAG_COMPOUND)) {
            CompoundTag section = ((CompoundTag) t).copy();
            int index = section.getByte("Y") + 4;
            section.remove("Y");
            section.remove("biomes");
            sections.put(Integer.toString(index), section);
        }
        CompoundTag stored = new CompoundTag();
        stored.put("sections", sections);
        stored.put("block_entities", chunk.getList("block_entities", Tag.TAG_COMPOUND).copy());
        CompoundTag chunks = new CompoundTag();
        chunks.put(Long.toString(ChunkPos.asLong(0, 0)), stored);
        CompoundTag plot = new CompoundTag();
        plot.putInt("plot_x", 0);
        plot.putInt("plot_z", 0);
        plot.putInt("log_size", 7);
        plot.put("chunks", chunks);
        CompoundTag ship = new CompoundTag();
        ship.put("plot", plot);
        return ship;
    }

    /** An empty full chunk as 1.21.1 saves it, already stamped by DE. */
    public static CompoundTag emptyChunk(int cx, int cz) {
        CompoundTag chunk = new CompoundTag();
        chunk.putInt("DataVersion", 3955);
        chunk.putInt("xPos", cx);
        chunk.putInt("zPos", cz);
        chunk.putInt("yPos", -4);
        chunk.putString("Status", "minecraft:full");
        ListTag sections = new ListTag();
        for (int y = -4; y < 20; y++) {
            CompoundTag section = new CompoundTag();
            section.putByte("Y", (byte) y);
            CompoundTag states = new CompoundTag();
            ListTag palette = new ListTag();
            palette.add(block("minecraft:air"));
            states.put("palette", palette);
            section.put("block_states", states);
            CompoundTag biomes = new CompoundTag();
            ListTag biomePalette = new ListTag();
            biomePalette.add(StringTag.valueOf("minecraft:plains"));
            biomes.put("palette", biomePalette);
            section.put("biomes", biomes);
            sections.add(section);
        }
        chunk.put("sections", sections);
        chunk.put("block_entities", new ListTag());
        stamp(chunk);
        return chunk;
    }

    public static void stamp(CompoundTag chunk) {
        chunk.putInt("hbm_data_version", 7);
    }

    public static void setBlock(CompoundTag chunk, BlockPos pos, CompoundTag state) {
        if (!ChunkBlocks.set(chunk, pos, state)) throw new IllegalArgumentException("no section for " + pos);
    }

    public static void addBlockEntity(CompoundTag chunk, CompoundTag be) {
        ListTag list = chunk.getList("block_entities", Tag.TAG_COMPOUND);
        list.add(be);
        chunk.put("block_entities", list);
    }

    public static void ductFluid(CompoundTag chunk, BlockPos pos, String fluid) {
        CompoundTag ducts = chunk.getCompound("hbm_duct_fluids");
        long[] old = ducts.getLongArray("pos");
        long[] positions = new long[old.length + 1];
        System.arraycopy(old, 0, positions, 0, old.length);
        positions[old.length] = pos.asLong();
        ListTag fluids = ducts.getList("fluid", Tag.TAG_STRING);
        fluids.add(StringTag.valueOf(fluid));
        ducts.putLongArray("pos", positions);
        ducts.put("fluid", fluids);
        chunk.put("hbm_duct_fluids", ducts);
    }

    /**
     * The machines and stacks every DE fixture holds around {@code origin} (y above ground, x and z near a chunk's low
     * corner): a centrifuge with stacks, a fluid tank with oil, a ZIRNOX, a duct with its fluid, split and renamed
     * blocks, a removed machine whose stacks must come back, and a chest of DE stacks with DE components.
     */
    public static List<Core> standardContent(CompoundTag chunk, BlockPos origin) {
        List<Core> cores = new ArrayList<>();
        BlockPos centrifuge = origin;
        setBlock(chunk, centrifuge, block("hbm:machine_centrifuge", "facing", "north"));
        for (int dy = 1; dy <= 3; dy++) setBlock(chunk, centrifuge.above(dy), block("hbm:multiblock_cell_h5_r6"));
        cores.add(new Core(centrifuge, "hbm:machine_centrifuge", "north"));
        CompoundTag cbe = blockEntity("hbm:centrifuge", centrifuge);
        CompoundTag battery = withComponent(stack("hbm:battery_pack_battery_lead", 1), "hbm:battery_charge", LongTag.valueOf(1234L));
        cbe.put("inventory", inventory(slot(0, stack("hbm:canned_ass", 3)), slot(1, battery)));
        cbe.putLong("power", 5000L);
        addBlockEntity(chunk, cbe);

        BlockPos tankPos = origin.offset(5, 0, 1);
        setBlock(chunk, tankPos, block("hbm:machine_fluidtank", "facing", "north"));
        cores.add(new Core(tankPos, "hbm:machine_fluidtank", "north"));
        CompoundTag tbe = blockEntity("hbm:fluid_tank", tankPos);
        tbe.put("tank", tank("hbm:oil", 16000, 256000));
        addBlockEntity(chunk, tbe);

        BlockPos zirnox = origin.offset(10, 0, 10);
        setBlock(chunk, zirnox, block("hbm:reactor_zirnox", "facing", "east"));
        cores.add(new Core(zirnox, "hbm:machine_zirnox", "east"));

        BlockPos duct = origin.south(4);
        setBlock(chunk, duct, block("hbm:fluid_pipe"));
        ductFluid(chunk, duct, "hbm:oil");

        BlockPos deco = origin.south(6);
        setBlock(chunk, deco, block("hbm:concrete_black"));
        setBlock(chunk, deco.east(), block("hbm:lamp_tritium_blue", "lit", "true"));
        setBlock(chunk, deco.east(2), block("hbm:sellafield", "level", "3"));
        setBlock(chunk, deco.east(3), block("hbm:pwr_block", "part", "fuel"));
        setBlock(chunk, deco.east(4), block("hbm:icf_block", "part", "turbocharger"));

        BlockPos turbine = origin.south(9);
        setBlock(chunk, turbine, block("hbm:machine_turbine"));
        CompoundTag turbineBe = blockEntity("hbm:turbine", turbine);
        turbineBe.put("inventory", inventory(slot(0, stack("hbm:ingot_steel", 7))));
        addBlockEntity(chunk, turbineBe);

        BlockPos chest = origin.south(9).east(3);
        setBlock(chunk, chest, block("minecraft:chest", "facing", "north", "type", "single", "waterlogged", "false"));
        CompoundTag chestBe = blockEntity("minecraft:chest", chest);
        ListTag items = new ListTag();
        items.add(slot(0, stack("hbm:capacitor_copper", 2)));
        items.add(slot(1, stack("hbm:test_item", 1)));
        items.add(slot(2, withComponent(stack("minecraft:shulker_box", 1), "minecraft:container",
                containerOf(stack("hbm:crashed_nuke", 1)))));
        items.add(slot(3, stack("hbm:wire_fine_mingrade", 16)));
        CompoundTag diesel = new CompoundTag();
        diesel.putString("type", "hbm:diesel");
        diesel.putLong("amount", 1000L);
        items.add(slot(4, withComponent(stack("hbm:canister", 1), "hbm:fluid_content", diesel)));
        items.add(slot(5, withComponent(stack("hbm:designator", 1), "hbm:target_designator",
                LongTag.valueOf((123L << 32) | (-456L & 0xFFFFFFFFL)))));
        CompoundTag identifier = new CompoundTag();
        identifier.putString("primary", "hbm:watz_mud");
        identifier.putString("secondary", "hbm:oil");
        items.add(slot(6, withComponent(stack("hbm:fluid_identifier_multi", 1), "hbm:fluid_identifier", identifier)));
        items.add(slot(7, withComponent(stack("hbm:machine_battery_redd", 1), "hbm:redd_charge", StringTag.valueOf("123456789"))));
        items.add(slot(8, withComponent(stack("hbm:crate_iron", 1), "hbm:locked_contents",
                seal(containerOf(stack("hbm:ingot_uranium", 12)), KEY))));
        CompoundTag gun = new CompoundTag();
        gun.putFloat("wear_0", 50F);
        gun.putInt("mode_0", 1);
        gun.putInt("magtype0", 2);
        gun.putInt("magcount0", 6);
        gun.putInt("timer_0", 12);
        items.add(slot(9, withComponent(stack("hbm:gun_pepperbox", 1), "hbm:gun_state", gun)));
        chestBe.put("Items", items);
        addBlockEntity(chunk, chestBe);
        return cores;
    }

    public static void writeKey(Path world) throws IOException {
        Files.createDirectories(world.resolve("data"));
        // as DE's SavedDataCompat writes it, inside vanilla's own wrapper
        CompoundTag data = new CompoundTag();
        data.putByteArray("data", KEY);
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", 3955);
        root.put("data", data);
        NbtIo.writeCompressed(root, world.resolve("data/hbm_locked_contents_key.dat"));
    }

    /** DE's {@code LockedContents.seal}: AES-GCM over gzip NBT, with the crate's metadata as additional data. */
    static CompoundTag seal(ListTag items, byte[] key) {
        try {
            CompoundTag plain = new CompoundTag();
            plain.putInt("DataVersion", 3955);
            plain.put("items", items);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(plain, bytes);
            byte[] nonce = new byte[12];
            for (int i = 0; i < nonce.length; i++) nonce[i] = (byte) (i + 1);
            ByteBuffer aad = ByteBuffer.allocate(14);
            aad.put((byte) 0).putInt(Float.floatToIntBits(0F)).putLong(Long.MAX_VALUE).put((byte) 0);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad.array());
            byte[] body = cipher.doFinal(bytes.toByteArray());
            byte[] sealed = new byte[12 + body.length];
            System.arraycopy(nonce, 0, sealed, 0, 12);
            System.arraycopy(body, 0, sealed, 12, body.length);
            CompoundTag locked = new CompoundTag();
            locked.putByteArray("sealed", sealed);
            locked.putBoolean("heavy", false);
            locked.putFloat("radiation", 0F);
            return locked;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** DE cell blocks over each core's footprint inside the chunk: DE's footprints match Continuum's. */
    static void layCells(CompoundTag chunk, List<Core> cores) {
        ChunkPos chunkPos = new ChunkPos(chunk.getInt("xPos"), chunk.getInt("zPos"));
        for (Core core : cores) {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(core.continuumBlock()));
            if (!(block instanceof BlockMultiblockCore machine)) continue;
            machine.visitPlacedCells(EmptyBlockGetter.INSTANCE, core.pos(), Direction.byName(core.facing()), (cell, mask) -> {
                if (new ChunkPos(cell).equals(chunkPos)) setBlock(chunk, cell.immutable(), block("hbm:multiblock_cell_h5_r6"));
            });
        }
    }

    /** Copies a world folder (not its lock) so a fixture can be made from a world the game has saved. */
    public static void copyWorld(Path from, Path to) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(to.resolve(from.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().equals("session.lock"))
                    Files.copy(file, to.resolve(from.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Turns a saved world into a DE save: {@code hbm}'s version in {@code level.dat} becomes DE's, DE's key file
     * appears, the chunk at {@code origin} gets the standard content and the host player DE stacks and props.
     */
    public static void makeDeWorld(Path world, BlockPos origin) throws IOException {
        Path levelDat = world.resolve("level.dat");
        CompoundTag level = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
        CompoundTag fml = level.getCompound("fml");
        for (Tag t : fml.getList("LoadingModList", Tag.TAG_COMPOUND)) {
            CompoundTag mod = (CompoundTag) t;
            if (mod.getString("ModId").equals("hbm")) mod.putString("ModVersion", DE_VERSION);
        }
        CompoundTag player = level.getCompound("Data").getCompound("Player");
        if (!player.isEmpty()) dePlayer(player);
        NbtIo.writeCompressed(level, levelDat);
        writeKey(world);

        ChunkPos chunkPos = new ChunkPos(origin);
        Path file = world.resolve("region/r." + chunkPos.getRegionX() + "." + chunkPos.getRegionZ() + ".mca");
        CompoundTag chunk;
        try (RegionFile region = RegionIo.open(file)) {
            chunk = RegionIo.read(region, chunkPos);
        }
        if (chunk == null) chunk = emptyChunk(chunkPos.x, chunkPos.z);
        stamp(chunk);
        layCells(chunk, standardContent(chunk, origin));
        try (RegionFile region = RegionIo.open(file); DataOutputStream out = region.getChunkDataOutputStream(chunkPos)) {
            NbtIo.write(chunk, out);
        }

        // DE's radiation storage: the chunk east of the content, two pockets in the section holding origin.
        Path radiation = world.resolve("hbm/radiation/r.0.0.mca");
        Files.createDirectories(radiation.getParent());
        CompoundTag payload = new CompoundTag();
        payload.putByteArray("payload", radiationPayload(origin.getY() >> 4, 12.5, 30.0));
        try (RegionFile region = RegionIo.open(radiation); DataOutputStream out = region.getChunkDataOutputStream(new ChunkPos(1, 0))) {
            NbtIo.write(payload, out);
        }
    }

    /**
     * DE's format 8 radiation payload for one section at {@code sectionY}: a wall of resistant blocks at local
     * {@code x = 8} splits it into two pockets (x below 8 first in block order), with these densities.
     */
    public static byte[] radiationPayload(int sectionY, double west, double east) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + 4 + 1 + 2 + 2 + 64 * 8 + 2 * (2 + 8));
        buf.put((byte) 'N').put((byte) 'T').put((byte) 'X').put((byte) 8);
        buf.put((byte) 1);
        buf.putInt(sectionY);
        buf.put((byte) 1);
        buf.putShort((short) 2);
        buf.putShort((short) 2);
        long[] mask = new long[64];
        for (int i = 0; i < 4096; i++) if ((i & 15) == 8) mask[i >>> 6] |= 1L << (i & 63);
        for (long word : mask) buf.putLong(word);
        buf.putShort((short) 0).putDouble(west);
        buf.putShort((short) 1).putDouble(east);
        return buf.array();
    }

    /** DE stacks in the inventory and DE's two props attachments. */
    public static void dePlayer(CompoundTag player) {
        ListTag inventory = player.getList("Inventory", Tag.TAG_COMPOUND);
        inventory.add(slot(9, stack("hbm:apple_lead_nugget", 5)));
        inventory.add(slot(10, stack("hbm:cell_uf6", 2)));
        player.put("Inventory", inventory);
        CompoundTag attachments = player.getCompound("neoforge:attachments");
        CompoundTag living = new CompoundTag();
        living.putDouble("radiation", 250.0);
        living.putInt("asbestos", 40);
        living.putInt("bombTimer", 100);
        attachments.put("hbm:living_props", living);
        CompoundTag props = new CompoundTag();
        props.putFloat("shield", 5F);
        props.putInt("reputation", 3);
        attachments.put("hbm:player_props", props);
        player.put("neoforge:attachments", attachments);
    }
}
