package fun.pasta.util.math;

public class Vector4i {

    public int x, y, z, w;

    public Vector4i(int x, int y, int z, int w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4i(int x, int y) {
        this.x = x;
        this.y = x;
        this.z = y;
        this.w = y;
    }

}