package fun.pasta.util.render;

import com.google.common.collect.Lists;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

public class SmartScissor {

    private static class State implements Cloneable {
        public boolean enable;
        public int transX;
        public int transY;
        public int x;
        public int y;
        public int width;
        public int height;

        @Override
        public State clone() {
            try {
                return (State) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private static State state = new State();
    private static List<State> stateList = Lists.newArrayList();

    public static void push() {
        stateList.add(state.clone());
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
    }

    public static void pop() {
        state = stateList.remove(stateList.size() - 1);
        GL11.glPopAttrib();
    }

    public static void unSet() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        state.enable = false;
    }

    public static void set(float x, float y, float width, float height) {
        MainWindow sr = Minecraft.getInstance().getMainWindow();
        int scaleFactor = (int) sr.getGuiScaleFactor();

        int screenX = (int) (x * scaleFactor);
        int screenY = (int) (y * scaleFactor);
        int screenWidth = (int) (width * scaleFactor);
        int screenHeight = (int) (height * scaleFactor);

        screenY = sr.getHeight() - screenY - screenHeight;

        Rectangle screen = new Rectangle(0, 0, sr.getWidth(), sr.getHeight());
        Rectangle current;

        if (state.enable) {
            current = new Rectangle(state.x, state.y, state.width, state.height);
        } else {
            current = screen;
        }

        Rectangle target = new Rectangle((int) (screenX + state.transX), (int) (screenY + state.transY), (int) screenWidth, (int) screenHeight);
        Rectangle result = current.intersection(target);

        result = result.intersection(screen);

        if (result.width < 0) result.width = 0;
        if (result.height < 0) result.height = 0;

        state.enable = true;
        state.x = result.x;
        state.y = result.y;
        state.width = result.width;
        state.height = result.height;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(result.x, result.y, result.width, result.height);
    }
}