package com.hbm.world.gen.nbt;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** 1.7 {@code JigsawPool}: a set of pieces with weights. */
public class JigsawPool {

    public record Weighted(JigsawPiece piece, int weight) {}

    // Weighted list of pieces to pick from
    List<Weighted> pieces = new ArrayList<>();
    int totalWeight = 0;

    public String fallback;

    private boolean isClone;

    public void add(JigsawPiece piece, int weight) {
        if (weight <= 0) throw new IllegalStateException("JigsawPool spawn weight must be positive!");
        pieces.add(new Weighted(piece, weight));
        totalWeight += weight;
    }

    public int getAverageWeight() {
        if (pieces.isEmpty()) return 1;
        return totalWeight / pieces.size();
    }

    @Override
    protected JigsawPool clone() {
        JigsawPool clone = new JigsawPool();
        clone.pieces = new ArrayList<>(this.pieces);
        clone.fallback = this.fallback;
        clone.totalWeight = this.totalWeight;
        clone.isClone = true;

        return clone;
    }

    // If from a clone, will remove from the pool
    public JigsawPiece get(RandomSource rand) {
        if (totalWeight <= 0) return null;
        int weight = rand.nextInt(totalWeight);

        for (int i = 0; i < pieces.size(); i++) {
            Weighted pair = pieces.get(i);
            weight -= pair.weight();

            if (weight < 0) {
                if (isClone) {
                    pieces.remove(i);
                    totalWeight -= pair.weight();
                }

                return pair.piece();
            }
        }

        return null;
    }
}
