package fun.pasta.module.impl.move;

import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.BooleanOption;

public class AirStuck extends Module {

    public final BooleanOption rw = new BooleanOption("ReallyWorld", false);

    public AirStuck() {
        super("AirStuck", Category.MOVE);
        add(rw);
    }
}