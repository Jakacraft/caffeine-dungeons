package dev.caffeine.dungeons.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "caffeine_dungeons")
public class CaffeineConfig implements ConfigData {

    // -------------------------------------------------------------------------
    // Rarity Indicator
    // -------------------------------------------------------------------------

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public IndicatorStyle rarityIndicatorStyle = IndicatorStyle.CIRCLE;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
    public int rarityIndicatorAlpha = 55;  // Percent — 55 = 55% opacity

    // -------------------------------------------------------------------------

    public enum IndicatorStyle {
        CIRCLE,
        SQUARE,
        BORDER
    }
}
