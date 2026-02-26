package fun.pasta.drag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;

public class DragManager {
    public static LinkedHashMap<String, Dragging> draggables = new LinkedHashMap<>();

    private static File DRAG_DATA = null;
    private static File LEGACY_DRAG_DATA = null;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static File getDragDataFile() {
        if (DRAG_DATA == null) {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().gameDir != null) {
                DRAG_DATA = new File(Minecraft.getInstance().gameDir, "pasta" + File.separator + "files" + File.separator + "drag.pasta");
            } else {
                DRAG_DATA = new File(System.getProperty("user.dir"), "pasta" + File.separator + "files" + File.separator + "drag.pasta");
            }
        }
        return DRAG_DATA;
    }

    private static File getLegacyDragDataFile() {
        if (LEGACY_DRAG_DATA == null) {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().gameDir != null) {
                LEGACY_DRAG_DATA = new File(Minecraft.getInstance().gameDir, "pasta" + File.separator + "files" + File.separator + "drags.pasta");
            } else {
                LEGACY_DRAG_DATA = new File(System.getProperty("user.dir"), "pasta" + File.separator + "files" + File.separator + "drags.pasta");
            }
        }
        return LEGACY_DRAG_DATA;
    }

    public static void save() {
        File dragData = getDragDataFile();
        if (!dragData.exists()) {
            dragData.getParentFile().mkdirs();
        }
        try {
            Files.writeString(dragData.toPath(), GSON.toJson(draggables.values()));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void load() {
        File dragData = getDragDataFile();
        if (!dragData.exists()) {
            File legacy = getLegacyDragDataFile();
            if (legacy.exists()) {
                dragData = legacy;
            } else {
                dragData.getParentFile().mkdirs();
                return;
            }
        }
        Dragging[] draggings;

        try {
            draggings = GSON.fromJson(Files.readString(dragData.toPath()), Dragging[].class);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        if (draggings != null) {
            for (Dragging dragging : draggings) {
                if (dragging == null) continue;
                Dragging currentDrag = draggables.get(dragging.getName());
                if (currentDrag == null) continue;
                currentDrag.setX(dragging.getX());
                currentDrag.setY(dragging.getY());
                draggables.put(dragging.getName(), currentDrag);
            }
        }

        if (dragData.equals(getLegacyDragDataFile())) {
            save();
        }
    }
}
