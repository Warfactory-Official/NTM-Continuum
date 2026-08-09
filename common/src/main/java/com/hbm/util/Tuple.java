package com.hbm.util;

/**
 * HBM tuple utility. Ported incrementally — currently just the {@link ObjectLongPair} used by the
 * power network. More pair types are added as later content is ported.
 */
public class Tuple {

    public static class ObjectLongPair<T> {

        private final T key;
        private final long value;

        public ObjectLongPair(T x, long y) {
            this.key = x;
            this.value = y;
        }

        public T getKey() {
            return key;
        }

        public long getValue() {
            return value;
        }
    }
}
