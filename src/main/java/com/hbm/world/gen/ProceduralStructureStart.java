package com.hbm.world.gen;

import com.hbm.world.gen.component.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 1.7 {@code ProceduralStructureStart}: a structure grown room by room from a starting piece, with weighted room types,
 * instance limits and hard size/distance limits (the bunker). The assembly is 1.7's, call for call and random draw for
 * random draw; the finished pieces go into vanilla's {@link StructurePiecesBuilder} ({@link #addTo}).
 */
/** This makes so much more sense! OOP wasn't confusing sometimes, you were just retarded! */
public class ProceduralStructureStart {

    /** 1.7 {@code StructureStart.components}. */
    public final List<StructurePiece> components = new ArrayList<>();
    /** List of queued components to call buildComponent on randomly. Iterated over until hard limits reached. */
    public List<StructurePiece> queuedComponents = new ArrayList<>();
    /** List of the component weights for this particular structure -- weights removed as components are 'used up.' */
    protected List<Weight> componentWeightList; //Make sure to initialize the array list to the weight array size, since might as well

    private final int chunkX;
    private final int chunkZ;

    public ProceduralStructureStart(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    /** 'starter' is just the starting component, so like a village well. meant to be nice and convenient and not needing a super necessarily. */
    public ProceduralStructureStart buildStart(RandomSource rand, StructurePiece starter, Weight... weights) {
        prepareWeights(weights);

        components.add(starter);
        queuedComponents.add(starter);
        while (!queuedComponents.isEmpty()) {
            final int i = rand.nextInt(queuedComponents.size());
            StructurePiece component = queuedComponents.remove(i);
            if (component instanceof ProceduralComponent procedural)
                procedural.buildComponent(this, rand); //additional components are added to the list; the 'last component' is the caller already.
        }

        return this;
    }

    public void prepareWeights(Weight... weights) {
        componentWeightList = new ArrayList<>(weights.length);

        for (Weight weight : weights) {
            weight.instancesSpawned = 0;
            componentWeightList.add(weight);
        }
    }

    /** 1.7 {@code StructureStart.getBoundingBox} after {@code updateBoundingBox}: every component enclosed. */
    public BoundingBox getBoundingBox() {
        return StructurePiece.createBoundingBox(components.stream());
    }

    /**
     * 1.7 {@code StructureStart.markAvailableHeight}: sinks the whole structure so its top is somewhere below
     * {@code 63 - offset}. 1.7 hardcoded 63 and a world bottom of 0 (vanilla's modern {@code moveBelowSeaLevel} uses
     * the level's own sea level and bottom, which would move the bunker 64 blocks deeper here).
     */
    protected void markAvailableHeight(RandomSource rand, int offset) {
        BoundingBox box = getBoundingBox();
        int j = 63 - offset;
        int k = box.getYSpan() + 1;

        if (k < j) {
            k += rand.nextInt(j - k);
        }

        int l = k - box.maxY();

        for (StructurePiece component : components) component.move(0, l, 0);
    }

    /** Hands the assembled pieces to vanilla. */
    public void addTo(StructurePiecesBuilder builder) {
        for (StructurePiece component : components) builder.addPiece(component);
    }

    protected int getTotalWeight() {
        boolean flag = false;
        int totalWeight = 0;

        for (Weight weight : componentWeightList) { //Iterates over the entire list to find the total weight
            if (weight.instanceLimit >= 0 && weight.instancesSpawned < weight.instanceLimit) //can more structure pieces be added, in general?
                flag = true;
            totalWeight += weight.weight;
        }

        return flag ? totalWeight : -1;
    }

    protected @Nullable StructurePiece getWeightedComponent(StructurePiece last, RandomSource rand, int minX, int minY, int minZ, int coordMode, int componentType) {
        int totalWeight = getTotalWeight();

        if (totalWeight < 0)
            return null;

        for (int i = 0; i < 5; i++) {
            int value = rand.nextInt(totalWeight); //Pick a random value, based on how many parts there are already
            Iterator<Weight> iterator = componentWeightList.iterator();

            while (iterator.hasNext()) {
                Weight weight = iterator.next();
                value -= weight.weight; //Iterate over the list until the value is less than 0

                if (value < 0) {
                    if (!weight.canSpawnStructure(componentType, coordMode, last)) //Additional checks based on game state info preventing spawn? start from beginning
                        break;

                    StructurePiece component = weight.lambda.findValidPlacement(components, rand, minX, minY, minZ, coordMode, componentType); //Construct the chosen component

                    if (component != null) { //If it has been constructed, add it
                        weight.instancesSpawned++;

                        if (!weight.canSpawnMoreStructures()) //Structure can no longer be spawned regardless of game state? remove as an option
                            componentWeightList.remove(weight);

                        return component;
                    }

                }
            }
        }

        return null;
    }

    //might remove these, add hard limits so subclasses can create their own implementations
    protected int sizeLimit = 50;
    protected int distanceLimit = 64;

    /** Gets the next valid component based on the structure start's members */
    protected @Nullable StructurePiece getNextValidComponent(StructurePiece last, RandomSource rand, int minX, int minY, int minZ, int coordMode, int componentType) {

        if (components.size() > sizeLimit) //Hard limit on amount of components
            return null;

        if (Math.abs(minX - (chunkX * 16 + 8)) <= distanceLimit && Math.abs(minZ - (chunkZ * 16 + 8)) <= distanceLimit) { //Hard limit on spread of structure

            StructurePiece structure = getWeightedComponent(last, rand, minX, minY, minZ, coordMode, componentType + 1); //Returns null if all checks fail

            if (structure != null) {
                this.components.add(structure); //Adds component to structure start list
                this.queuedComponents.add(structure); //Add it to the list of queued components waiting to be built
            }

            return structure;
        }

        return null;
    }

    /** Useful utility method to automatically construct the appropriate structure bounding box; based on the getNextComponents below!<br>
     *  posX, posY, posZ represent the original anchor point of the structurecomponent (minX, minY, minZ).<br>
     *  offsetX, offsetY, offsetZ all add onto that point, meaning that offsets <b>will always move that anchor towards +x, +y, +z (in terms of south).</b><br>
     *  maxX, maxY, maxZ additionally are added to get the maximum x, y, z (obviously) in terms of south. An offset of 1 is subtracted, so these do not start at 0, and
     *  <b>refer to the ACTUAL dimensions of the component.</b><br>
     *  The initial anchor point is kind of arbitrary based on where it is in the door, so what really matters is keeping it consistent.
     */
    public static BoundingBox getComponentToAddBoundingBox(int posX, int posY, int posZ, int offsetX, int offsetY, int offsetZ, int maxX, int maxY, int maxZ, int coordMode) {
        switch (coordMode) {
            default:
            case 0: return new BoundingBox(posX + offsetX, posY + offsetY, posZ + offsetZ, posX + maxX - 1 + offsetX, posY + maxY - 1 + offsetY, posZ + maxZ - 1 + offsetZ); //South
            case 1: return new BoundingBox(posX - maxZ + 1 - offsetZ, posY + offsetY, posZ + offsetX, posX - offsetZ, posY + maxY - 1 + offsetY, posZ + maxX - 1 + offsetX); //West
            case 2: return new BoundingBox(posX - maxX + 1 - offsetX, posY + offsetY, posZ - maxZ + 1 - offsetZ, posX - offsetX, posY + maxY - 1 + offsetY, posZ + offsetZ); //North
            case 3: return new BoundingBox(posX + offsetZ, posY + offsetY, posZ - maxX + 1 - offsetX, posX + maxZ - 1 + offsetZ, posY + maxY - 1 + offsetY, posZ - offsetX); //East
        }
    }

    /** no class-based multiple inheritance?  */
    public interface ProceduralComponent {

        default void buildComponent(ProceduralStructureStart start, RandomSource rand) { } //no class-based multiple inheritance?

        /** Gets next component in the direction this component is facing.<br>'original' refers to the initial starting component (hard distance limits), 'components' refers to the StructureStart list.<br>
         *  offset and offsetY are added to the <b>anchor point of the new component<br>, referring to the minX and minY respectively (in terms of south).<br>
         *  An offset of 1 is added to the minZ anchor point, relative to south.
         */
        default @Nullable StructurePiece getNextComponentNormal(ProceduralStructureStart start, Component caller, int coordMode, RandomSource rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, coordMode, caller.getComponentType()); //South
                case 1: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, coordMode, caller.getComponentType()); //West
                case 2: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, coordMode, caller.getComponentType()); //North
                case 3: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, coordMode, caller.getComponentType()); //East
                default: return null;
            }
        }

        /** Gets next component in the opposite direction this component is facing. */
        default @Nullable StructurePiece getNextComponentAntiNormal(ProceduralStructureStart start, Component caller, int coordMode, RandomSource rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2, caller.getComponentType()); //South
                case 1: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3, caller.getComponentType()); //West
                case 2: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0, caller.getComponentType()); //North
                case 3: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1, caller.getComponentType()); //East
                default: return null;
            }
        }

        //Keep in mind for these methods: a given room would have its *actual entrance* opposite the side it is facing.
        /** Gets next component, to the West (-X) <i>relative to this component. */
        default @Nullable StructurePiece getNextComponentWest(ProceduralStructureStart start, Component caller, int coordMode, RandomSource rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1, caller.getComponentType()); //South
                case 1: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2, caller.getComponentType()); //West
                case 2: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3, caller.getComponentType()); //North
                case 3: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0, caller.getComponentType()); //East
                default: return null;
            }
        }

        /** Gets next component, to the East (+X) <i>relative to this component. */
        default @Nullable StructurePiece getNextComponentEast(ProceduralStructureStart start, Component caller, int coordMode, RandomSource rand, int offset, int offsetY) {
            BoundingBox box = caller.getBoundingBox();
            switch (coordMode) {
                case 0: return start.getNextValidComponent(caller, rand, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3, caller.getComponentType()); //South
                case 1: return start.getNextValidComponent(caller, rand, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0, caller.getComponentType()); //West
                case 2: return start.getNextValidComponent(caller, rand, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1, caller.getComponentType()); //North
                case 3: return start.getNextValidComponent(caller, rand, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2, caller.getComponentType()); //East
                default: return null;
            }
        }
    }

    /** Returns a new instance of this structureComponent, or null if not able to be placed.<br>Based on bounding box checks. Please use a method reference in the component. */
    @FunctionalInterface
    public interface InstantiateStructure {
        @Nullable StructurePiece findValidPlacement(List<StructurePiece> components, RandomSource rand, int minX, int minY, int minZ, int coordMode, int componentType);
    }

    public static class Weight {

        public final InstantiateStructure lambda; //Read above

        public final int weight; //Weight of this component
        public int instancesSpawned; //How many components spawned?
        public int instanceLimit; //Limit on amount of components: -1 for no limit

        public Weight(int weight, int limit, InstantiateStructure lambda) {
            this.weight = weight;
            this.instanceLimit = limit;
            this.lambda = lambda;
        }

        //Checks if another structure can be spawned based on input data
        public boolean canSpawnStructure(int componentAmount, int coordMode, StructurePiece component) {
            return this.instanceLimit < 0 || this.instancesSpawned < this.instanceLimit;
        }

        //Checks if another structure can be spawned at all (used to flag for removal from the list)
        public boolean canSpawnMoreStructures() {
            return this.instanceLimit < 0 || this.instancesSpawned < this.instanceLimit;
        }

    }
}
