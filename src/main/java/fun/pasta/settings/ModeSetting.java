    package fun.pasta.settings;

    import java.util.Arrays;
    import java.util.List;

    public class ModeSetting extends Setting {

        private List<String> modes;
        private String mode;

        public ModeSetting(String name, String defaultMode, String... modes) {
            super(name);
            this.mode = defaultMode;
            this.modes = Arrays.asList(modes);
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            if (modes.contains(mode)) {
                this.mode = mode;
            }
        }

        public void cycle() {
            int index = modes.indexOf(mode);
            index++;
            if (index >= modes.size()) index = 0;
            mode = modes.get(index);
        }

        public List<String> getModes() {
            return modes;
        }
    }
