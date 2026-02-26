package fun.pasta.ui.backGround;

import net.minecraft.util.ResourceLocation;
import fun.pasta.Pasta;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.render.RenderUtil2D;

import java.util.ArrayList;
import java.util.List;

public class BackGroundManager implements IMinecraft {

    List<BackGround> backGrounds = new ArrayList<>();

    public String currentBackResource = "pasta/images/backGround/backGround_1.jpg";

    public void drawBackGroundInSelect() {
        RenderUtil2D.drawImage(new ResourceLocation(currentBackResource), 0, 0, sr.getScaledWidth(), sr.getScaledHeight(), -1);
    }

    public void init() {
        backGrounds.clear();

        backGrounds.add(new BackGround("Фон 1", "pasta/images/backGround/backGround_1.jpg"));
        backGrounds.add(new BackGround("Фон 2", "pasta/images/backGround/backGround_2.jpg"));
        backGrounds.add(new BackGround("Фон 3", "pasta/images/backGround/backGround_3.jpg"));
    }
}