package net.fabricmc.fabric.api.event;

import java.lang.reflect.Array;
import java.util.function.Function;

public final class EventFactory {

    private EventFactory() {
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, Function<T[], T> invokerFactory) {
        return new ArrayBackedEvent<>(type, invokerFactory);
    }

    private static final class ArrayBackedEvent<T> implements Event<T> {
        private final Class<T> type;
        private final Function<T[], T> invokerFactory;
        private volatile T[] listeners;

        ArrayBackedEvent(Class<T> type, Function<T[], T> invokerFactory) {
            this.type = type;
            this.invokerFactory = invokerFactory;
            this.listeners = newArray(0);
        }

        @Override
        public void register(T listener) {
            T[] old = listeners;
            T[] next = newArray(old.length + 1);
            System.arraycopy(old, 0, next, 0, old.length);
            next[old.length] = listener;
            listeners = next;
        }

        @Override
        public T invoker() {
            return invokerFactory.apply(listeners);
        }

        @SuppressWarnings("unchecked")
        private T[] newArray(int size) {
            return (T[]) Array.newInstance(type, size);
        }
    }
}
