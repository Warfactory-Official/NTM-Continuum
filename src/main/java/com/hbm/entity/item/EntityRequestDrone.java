package com.hbm.entity.item;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.ModNtmEntities;
import com.hbm.api.block.IDroneContainer;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.tool.ItemDrone;
import com.hbm.items.tool.ItemDrone.EnumDroneType;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.network.RequestNetwork.Demand;
import com.hbm.util.InventoryUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.data.Codecs;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code entity/item/EntityRequestDrone}: the courier. Runs a queued program of waypoints, one pickup and
 * one delivery, then docks. With nothing left to do it self-destructs back into a drone item, which is what
 * keeps a confused drone from being lost forever.
 *
 * <p>1.7: the program was a raw {@code List<Object>} of {@code BlockPos}, {@code AStack} or {@code DroneProgram}
 * constants, and neither {@code AStack} branch survived its own NBT round trip - {@code :234}/{@code :237} read
 * from the root tag while {@code :273-278} wrote into {@code data}, so a reloaded drone lost its fetch step and
 * returned to the dock empty. {@link Step} makes the three cases a sealed choice and {@link Demand} replaces
 * {@code AStack}; both of 1.7's subclasses collapse onto it, and it serialises, so the port does not reproduce
 * that loss.
 */
public class EntityRequestDrone extends EntityDroneBase implements StoredItems {

    /** 1.7's cargo scan reached four blocks straight down for the node it was hovering over. */
    private static final double NODE_REACH = 4;
    private static final int ACTION_DELAY = 5;

    public ItemStack heldItem = ItemStack.EMPTY;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (visitor.visit(heldItem) && heldItem.isEmpty()) heldItem = ItemStack.EMPTY;
    }
    public final List<Step> program = new ArrayList<>();
    private int nextActionTimer;

    public EntityRequestDrone(EntityType<? extends EntityRequestDrone> type, Level level) {
        super(type, level);
    }

    public EntityRequestDrone(Level level) {
        super(ModNtmEntities.REQUEST_DRONE.get(), level);
    }

    /** 1.7 hovered one block above the node it was working. */
    @Override
    public void setTarget(double x, double y, double z) {
        super.setTarget(x, y + 1, z);
    }

    @Override
    public double getSpeed() {
        return 0.625D;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (isRemoved() || !(attacker instanceof Player) || !(level() instanceof ServerLevel server)) {
            return false;
        }

        discard();
        if (!this.heldItem.isEmpty()) spawnAtLocation(server, this.heldItem);
        spawnAtLocation(server, ModEntityItems.drone.stack(EnumDroneType.REQUEST));
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;
        if (relativeMotion().length() >= 0.01) return;

        if (this.nextActionTimer > 0) {
            this.nextActionTimer--;
            return;
        }

        if (this.program.isEmpty()) {
            selfDestruct(server); // 1.7: no further operations pending
            return;
        }

        // instanceof chain rather than a record-pattern switch: the 1.20.1 node compiles at Java 17
        Step step = this.program.remove(0);
        if (step instanceof Step.Goto go) {
            BlockPos pos = go.pos();
            setTarget(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        } else if (step instanceof Step.Load load) {
            if (this.heldItem.isEmpty()) load(server, load.want());
            this.nextActionTimer = ACTION_DELAY;
        } else if (step instanceof Step.Unload) {
            if (!this.heldItem.isEmpty()) unload(server);
            this.nextActionTimer = ACTION_DELAY;
        } else if (step instanceof Step.Dock) {
            dock(server);
        }
    }

    /** 1.7: pull the first stack matching the request out of the provider below. */
    private void load(ServerLevel server, Demand want) {
        if (!(nodeBelow(server) instanceof IDroneContainer.Provider provider)) return;

        Container inventory = provider.droneOfferInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !want.test(stack)) continue;

            this.heldItem = stack.copy();
            setAppearance(1);
            playUnpack(server);
            inventory.setItem(i, ItemStack.EMPTY);
            inventory.setChanged();
            return;
        }
    }

    /** 1.7: top up matching stacks first, then take the first free slot. */
    private void unload(ServerLevel server) {
        if (!(nodeBelow(server) instanceof IDroneContainer.Requester requester)) return;

        Container inventory = requester.droneDeliveryInventory();

        this.heldItem = addToContainer(inventory, 0, inventory.getContainerSize() - 1, this.heldItem);

        if (this.heldItem.isEmpty()) {
            setAppearance(0);
            playUnpack(server);
        }

        inventory.setChanged();
    }

    /**
     * 1.7: park in the first free dock slot, or stack onto an identical parked drone, shelving the cargo in
     * the slot after. Failing to dock is not fatal - the drone falls back to self-destructing into its item.
     */
    private void dock(ServerLevel server) {
        BlockEntity node = nodeBelow(server);

        if (node instanceof IDroneContainer.Dock dock) {
            Container inventory = dock.droneDockInventory();
            ItemStack drone = ModEntityItems.drone.stack(EnumDroneType.REQUEST);

            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                boolean free = slot.isEmpty();
                boolean stackable = ItemStacks.sameItemAndData(slot, drone)
                        && slot.getCount() < slot.getMaxStackSize();
                if (!free && !stackable) continue;

                discard();

                if (!this.heldItem.isEmpty() && i + 1 < inventory.getContainerSize()
                        && inventory.getItem(i + 1).isEmpty()) {
                    inventory.setItem(i + 1, this.heldItem.copy());
                    this.heldItem = ItemStack.EMPTY;
                }

                // 1.7:164-193 destroys cargo the successor slot cannot take: setDead runs before the drop
                // branch that guards on it, so that branch can never see a docked drone.
                if (!this.heldItem.isEmpty()) {
                    spawnAtLocation(server, this.heldItem);
                    this.heldItem = ItemStack.EMPTY;
                }

                if (free) inventory.setItem(i, drone);
                else slot.grow(1);

                inventory.setChanged();
                // 1.7 played this at the dock's own centre, which the drone hovers a block or more above
                server.playSound(null, node.getBlockPos(), ModSounds.CRATE_CLOSE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                break;
            }
        }

        if (!isRemoved()) selfDestruct(server);
    }

    private void selfDestruct(ServerLevel server) {
        discard();
        if (!this.heldItem.isEmpty()) spawnAtLocation(server, this.heldItem);
        spawnAtLocation(server, ModEntityItems.drone.stack(EnumDroneType.REQUEST));
    }

    /**
     * 1.7 ray-traced straight down rather than reading the block under the drone, to make damn sure a drone
     * hovering slightly off never missed its node.
     */
    private @Nullable BlockEntity nodeBelow(ServerLevel server) {
        Vec3 from = position();
        Vec3 to = from.subtract(0, NODE_REACH, 0);
        BlockHitResult hit = server.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return server.getBlockEntity(hit.getBlockPos());
    }

    private void playUnpack(ServerLevel server) {
        server.playSound(null, getX(), getY(), getZ(), ModSounds.ITEM_UNPACK.get(), SoundSource.NEUTRAL, 0.5F, 0.75F);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.heldItem = input.read("held", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.program.clear();
        this.program.addAll(input.read("program", Step.CODEC.listOf()).orElse(List.of()));
        this.nextActionTimer = ACTION_DELAY; // 1.7 re-armed the delay on load
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (!this.heldItem.isEmpty()) output.store("held", ItemStack.CODEC, this.heldItem);
        output.store("program", Step.CODEC.listOf(), List.copyOf(this.program));
    }

    /**
     * One queued operation. 1.7 held these as bare Objects in a raw list and tagged them with a "type" string
     * on the way to NBT; the dispatch codec below is that same tagging, made total.
     */
    public sealed interface Step {

        Codec<Step> CODEC = Codecs.dispatch(Codec.STRING, "type", Step::type, Step::codecFor);

        static MapCodec<? extends Step> codecFor(String type) {
            return switch (type) {
                case "pos" -> Goto.CODEC;
                case "want" -> Load.CODEC;
                case "dock" -> MapCodec.unit(new Dock());
                default -> MapCodec.unit(new Unload());
            };
        }

        String type();

        record Goto(BlockPos pos) implements Step {
            static final MapCodec<Goto> CODEC = RecordCodecBuilder.mapCodec(i ->
                    i.group(BlockPos.CODEC.fieldOf("pos").forGetter(Goto::pos)).apply(i, Goto::new));

            @Override
            public String type() {
                return "pos";
            }
        }

        record Load(Demand want) implements Step {
            static final MapCodec<Load> CODEC = RecordCodecBuilder.mapCodec(i ->
                    i.group(Demand.CODEC.fieldOf("want").forGetter(Load::want)).apply(i, Load::new));

            @Override
            public String type() {
                return "want";
            }
        }

        record Unload() implements Step {
            @Override
            public String type() {
                return "unload";
            }
        }

        record Dock() implements Step {
            @Override
            public String type() {
                return "dock";
            }
        }
    }

    /** 1.7 tryAddItemToInventory over a plain Container: top up matching stacks first, then the first free slot. */
    private static ItemStack addToContainer(Container inventory, int start, int end, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = start; i <= end && !rest.isEmpty(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty() || !ItemStacks.sameItemAndData(slot, rest)) continue;
            int room = Math.min(slot.getMaxStackSize(), inventory.getMaxStackSize()) - slot.getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, rest.getCount());
            slot.grow(moved);
            rest.shrink(moved);
        }
        for (int i = start; i <= end && !rest.isEmpty(); i++) {
            if (!inventory.getItem(i).isEmpty()) continue;
            inventory.setItem(i, rest);
            rest = ItemStack.EMPTY;
        }
        return rest;
    }
}
