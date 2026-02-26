package fun.pasta.altManager;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.io.*;
import java.nio.file.Files;

public class
LastAccountConfig {
    // Убираем статическую инициализацию, используем ленивую загрузку
    private static File file = null;

    // Метод для получения файла с отложенной инициализацией
    private static File getFile() {
        if (file == null) {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().gameDir != null) {
                // Используем File.separator для кроссплатформенности
                file = new File(Minecraft.getInstance().gameDir,
                        "Pasta" + File.separator + "files" + File.separator + "lastAcc.pasta");
            } else {
                // Альтернативный путь, если Minecraft еще не инициализирован
                file = new File(System.getProperty("user.dir"),
                        "Pasta" + File.separator + "files" + File.separator + "lastAcc.pasta");
            }
        }
        return file;
    }

    public void init() throws Exception {
        File configFile = getFile();
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            configFile.createNewFile();
        } else {
            readAlts();
        }
    }

    public void updateFile() {
        try {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().session != null) {
                Files.write(getFile().toPath(), Minecraft.getInstance().session.getUsername().getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readAlts() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new DataInputStream(
                                    new FileInputStream(getFile().getAbsolutePath())
                            )
                    )
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Проверяем, что Minecraft уже инициализирован
                if (Minecraft.getInstance() != null) {
                    Minecraft.getInstance().session = new Session(line, "", "", "mojang");
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}