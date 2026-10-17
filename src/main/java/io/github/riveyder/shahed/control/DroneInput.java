package io.github.riveyder.shahed.control;

/** Signed, normalized control rates. Rotation is integrated once per server tick. */
public record DroneInput(float forward, float strafe, float vertical, float yaw, float pitch, boolean brake) {
    public static final DroneInput IDLE = new DroneInput(0, 0, 0, 0, 0, false);

    public boolean isFinite() {
        return Float.isFinite(forward) && Float.isFinite(strafe) && Float.isFinite(vertical)
                && Float.isFinite(yaw) && Float.isFinite(pitch);
    }

    public DroneInput sanitized() {
        return new DroneInput(axis(forward), axis(strafe), axis(vertical), axis(yaw), axis(pitch), brake);
    }

    private static float axis(float value) {
        return Float.isFinite(value) ? Math.clamp(value, -1.0F, 1.0F) : 0;
    }
}
