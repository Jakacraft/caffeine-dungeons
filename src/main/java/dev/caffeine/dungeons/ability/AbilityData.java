package dev.caffeine.dungeons.ability;

public class AbilityData {
    public String name;
    public int cooldown; // seconds
    public String color = "#FFFFFF";

    public int getArgb() {
        try {
            String hex = color.startsWith("#") ? color.substring(1) : color;
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }
}