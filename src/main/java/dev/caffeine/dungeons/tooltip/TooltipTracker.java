package dev.caffeine.dungeons.tooltip;

/**
 * Stores the screen bounds of the most recently rendered tooltip.
 * Updated every frame by DrawContextMixin before the tooltip is drawn.
 * Reset each frame at the start of HUD rendering so stale data isn't used.
 */
public final class TooltipTracker {

    private static int x, y, width, height;
    private static boolean active = false;

    private TooltipTracker() {}

    public static void set(int x, int y, int width, int height) {
        TooltipTracker.x      = x;
        TooltipTracker.y      = y;
        TooltipTracker.width  = width;
        TooltipTracker.height = height;
        TooltipTracker.active = true;
    }

    public static void clear()       { active = false; }
    public static boolean isActive() { return active; }
    public static int getX()         { return x; }
    public static int getY()         { return y; }
    public static int getWidth()     { return width; }
    public static int getHeight()    { return height; }
}