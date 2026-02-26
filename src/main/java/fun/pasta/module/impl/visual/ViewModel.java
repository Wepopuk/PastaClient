package fun.pasta.module.impl.visual;

import fun.pasta.module.api.Category;
import fun.pasta.module.api.Module;
import fun.pasta.module.settings.SliderOption;

public class ViewModel extends Module {

    public final SliderOption right_x = new SliderOption("Правая X", 0.0F, -2.0f, 2.0f, 0.1F);
    public final SliderOption right_y = new SliderOption("Правая Y", 0.0F, -2.0f, 2.0f, 0.1F);
    public final SliderOption right_z = new SliderOption("Правая Z", 0.0F, -2.0f, 2.0f, 0.1F);
    public final SliderOption left_x = new SliderOption("Левая X", 0.0F, -2.0f, 2.0f, 0.1F);
    public final SliderOption left_y = new SliderOption("Левая Y", 0.0F, -2.0f, 2.0f, 0.1F);
    public final SliderOption left_z = new SliderOption("Левая Z", 0.0F, -2.0f, 2.0f, 0.1F);


    public ViewModel() {
        super("ViewModel", Category.VISUAL);
        add(right_x, right_y, right_z, left_x, left_y, left_z);
    }
}