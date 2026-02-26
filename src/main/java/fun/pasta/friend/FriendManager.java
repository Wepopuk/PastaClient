package fun.pasta.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FriendManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private final List<String> friends = new ArrayList<>();
    private File friendFile;

    public FriendManager() {
        load();
    }

    public List<String> getFriends() {
        return Collections.unmodifiableList(friends);
    }

    public void addFriend(String friend) {
        if (friend == null || friend.isEmpty() || isFriend(friend)) {
            return;
        }
        friends.add(friend);
        save();
    }

    public void removeFriend(String friend) {
        if (friend == null || friend.isEmpty()) {
            return;
        }
        friends.removeIf(name -> name.equalsIgnoreCase(friend));
        save();
    }

    public void clear() {
        friends.clear();
        save();
    }

    public boolean isFriend(Entity entity) {
        return entity != null && isFriend(entity.getName().getString());
    }

    public boolean isFriend(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (String friend : friends) {
            if (friend.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private File getFriendFile() {
        if (friendFile == null) {
            File baseDir = Minecraft.getInstance() != null && Minecraft.getInstance().gameDir != null
                    ? Minecraft.getInstance().gameDir
                    : new File(System.getProperty("user.dir"));
            friendFile = new File(baseDir, "pasta" + File.separator + "files" + File.separator + "friends.pasta");
        }
        return friendFile;
    }

    private void save() {
        File file = getFriendFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            Files.writeString(file.toPath(), GSON.toJson(friends));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        File file = getFriendFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            return;
        }
        try {
            String json = Files.readString(file.toPath());
            List<String> loaded = GSON.fromJson(json, LIST_TYPE);
            friends.clear();
            if (loaded != null) {
                friends.addAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
