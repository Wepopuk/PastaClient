package fun.pasta.config;

import fun.pasta.Pasta;
import fun.pasta.module.api.Module;
import fun.pasta.ui.clickGui.settings.Setting;

import java.io.*;

public class Config {

    private final File file;

    public Config(File file) {
        this.file = file;
    }

    public void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        } catch (Exception ignored) {
        }

        try (PrintWriter writer = new PrintWriter(file)) {

            for (Module module : Pasta.instance.manager.modules) {
                writer.println("module " + module.name + " {");
                writer.println("enabled=" + module.enable);
                writer.println("key=" + module.key);

                for (Setting setting : module.settingList) {
                    writer.println(setting.getName() + "=" + setting.getValueAsString());
                }

                writer.println("}\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            Module current = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("module")) {
                    String name;
                    if (line.endsWith("{")) {
                        name = line.substring("module".length(), line.length() - 1).trim();
                    } else {
                        name = line.substring("module".length()).trim();
                    }
                    current = Pasta.instance.manager.modules.stream()
                            .filter(m -> m.name.equalsIgnoreCase(name))
                            .findFirst().orElse(null);
                    continue;
                }

                if (current == null) continue;

                if (line.equals("}")) {
                    current = null;
                    continue;
                }

                String[] split = line.split("=", 2);
                if (split.length != 2) continue;

                String key = split[0];
                String value = split[1];

                if (key.equals("enabled")) {
                    boolean state = Boolean.parseBoolean(value);
                    if (state != current.enable) current.toggle();
                } else if (key.equals("key")) {
                    current.key = Integer.parseInt(value);
                } else {
                    for (Setting setting : current.settingList) {
                        if (setting.getName().equalsIgnoreCase(key)) {
                            setting.setValueFromString(value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
