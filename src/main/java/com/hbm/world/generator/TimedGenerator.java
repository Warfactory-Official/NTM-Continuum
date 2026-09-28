package com.hbm.world.generator;

import java.util.ArrayDeque;

/**
 * 1.7 {@code com.hbm.world.generator.TimedGenerator}: a FIFO of jobs the jungle dungeon's rooms queue, worked off over
 * the following ticks (10 ms of jobs per world tick). The jungle dungeon depends on its order - a room's second-stage
 * jobs (arrow and dart traps) are appended behind every job queued before them, including the later floors', and the
 * hole/boss-circle job of a floor runs after that floor's rooms - so the port keeps the queue and its order, one queue
 * per dungeon run, and drains it at the end of the run instead of across ticks.
 */
public class TimedGenerator {

    private final ArrayDeque<ITimedJob> operations = new ArrayDeque<>();

    public void addOp(ITimedJob job) {
        operations.addLast(job);
    }

    /** 1.7 {@code automaton}, without the 10 ms budget: every queued job, in order, including the ones they queue. */
    public void drain() {
        while (!operations.isEmpty()) operations.pollFirst().work();
    }

    public interface ITimedJob {
        void work();
    }
}
