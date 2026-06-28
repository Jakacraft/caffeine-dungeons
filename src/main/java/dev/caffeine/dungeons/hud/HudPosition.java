package dev.caffeine.dungeons.hud;

/**
 * Stores the screen position and scale of a moveable HUD element.
 * Serialized to config via Gson. x/y == -1 means "use computed default on first render".
 */
public class HudPosition {

    public int x = -1;
    public int y = -1;
    public float scale = 1.0f;

    /**
     * Returns x, initializing to defaultX on first call.
     * Writes back so the default is persisted on next save.
     */
    public int getX(int defaultX) {
        if (x == -1) x = defaultX;
        return x;
    }

    public int getY(int defaultY) {
        if (y == -1) y = defaultY;
        return y;
    }
}