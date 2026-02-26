package fun.pasta.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private final Map<Class<?>, List<Method>> eventHandlers = new HashMap<>();

    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
                Class<?> eventType = method.getParameterTypes()[0];
                eventHandlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(method);
            }
        }
    }

    public static boolean blocked = false;
    private final List<fun.pasta.event.EventListener> listeners = new CopyOnWriteArrayList<>();

    public void register(EventListener listener) {
        listeners.add(listener);
    }

    public void unregister(EventListener listener) {
        listeners.remove(listener);
    }

    public void call(Event event) {
        if (blocked) return;
        listeners.forEach(listener -> {
            listener.onEvent(event);
        });
    }

    public void post(Object event) {
        List<Method> handlers = eventHandlers.get(event.getClass());
        if (handlers != null) {
            for (Method handler : handlers) {
                try {
                    handler.invoke(handler.getDeclaringClass().newInstance(), event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}