package fun.pasta.drag;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import fun.pasta.module.api.Module;
import fun.pasta.util.IMinecraft;
import fun.pasta.util.math.MathUtil;

public class Dragging implements IMinecraft {
    @Expose
    @SerializedName("x")
    private float xPos;
    @Expose
    @SerializedName("y")
    private float yPos;

    public float initialXVal;
    public float initialYVal;

    private float startX, startY;
    public boolean dragging;

    private float width, height;

    @Expose
    @SerializedName("name")
    private final String name;

    private final Module module;

    public Dragging(Module module, String name, float initialXVal, float initialYVal) {
        this.module = module;
        this.name = name;
        this.xPos = initialXVal;
        this.yPos = initialYVal;
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;
    }

    public Module getModule() {
        return module;
    }

    public String getName() {
        return name;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getX() {
        return xPos;
    }

    public void setX(float x) {
        this.xPos = x;
    }

    public float getY() {
        return yPos;
    }

    public void setY(float y) {
        this.yPos = y;
    }

    public final void draw(int mouseX, int mouseY) {
        if (dragging) {

            xPos = (mouseX - startX);
            yPos = (mouseY - startY);
            if (xPos + width > sr.getScaledWidth()) {
                xPos = sr.getScaledHeight() - width;
            }
            if (yPos + height > sr.getScaledWidth()) {
                yPos = sr.getScaledHeight() - height;
            }
            if (xPos < 0) {
                xPos = 0;
            }
            if (yPos < 0) {
                yPos = 0;
            }
        }
    }

    public final boolean onClick(double mouseX, double mouseY, int button) {
        if (button == 0 && MathUtil.isHover((int) mouseX, (int) mouseY, xPos, yPos, width, height)) {
            dragging = true;
            startX = (int) (mouseX - xPos);
            startY = (int) (mouseY - yPos);
            return true;
        }
        return false;
    }

    public final void onRelease(int button) {
        if (button == 0) dragging = false;
    }


}