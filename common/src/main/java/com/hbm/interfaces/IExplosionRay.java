package com.hbm.interfaces;

import java.util.UUID;

public interface IExplosionRay {

    void update(int processTimeMs);

    void cancel();

    boolean isComplete();

    boolean isContained();

    void setDetonator(UUID detonator);
}
