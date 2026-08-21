package dev.caffeine.dungeons.ability;

public class AbilityData {
    public String name;
    public int cooldown; // seconds
    public String color = "#FFFFFF";

    public String buffLabel;
    public String buffColor;
    public int buffDuration; // seconds, 0 = use cooldown

    public boolean hasBuff() {
        return buffLabel != null && !buffLabel.isBlank();
    }

    public String getBuffColorHex() {
        return buffColor != null ? buffColor : color;
    }

    public int getBuffDurationSeconds() {
        return buffDuration > 0 ? buffDuration : cooldown;
    }

    public int getArgb() {
        try {
            String hex = color.startsWith("#") ? color.substring(1) : color;
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}