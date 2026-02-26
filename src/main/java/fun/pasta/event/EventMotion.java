    package fun.pasta.event;

    public class EventMotion {

        public double x, y, z;
        public float yaw, pitch;
        public boolean onGround;

        public Runnable postMotion;
    }