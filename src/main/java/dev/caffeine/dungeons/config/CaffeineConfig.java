package dev.caffeine.dungeons.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "caffeine_dungeons")
public class CaffeineConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public IndicatorStyle rarityIndicatorStyle = IndicatorStyle.CIRCLE;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int rarityIndicatorAlpha = 55;

    @ConfigEntry.Gui.Tooltip
    public String supabaseUrl = "https://doplhwkpsbzgnzzkymfb.supabase.co";

    @ConfigEntry.Gui.Tooltip
    public String supabaseAnonKey = "sb_publishable_LoVp8WNIjCyrU14_Zu8g1A_1MGjxquT";

    public enum IndicatorStyle {
        CIRCLE,
        SQUARE,
        BORDER
    }
}