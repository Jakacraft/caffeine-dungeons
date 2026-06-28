package dev.caffeine.dungeons.config;

import dev.caffeine.dungeons.hud.HudPosition;
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
    public int rarityIndicatorAlpha = 50;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public CooldownHudStyle cooldownHudStyle = CooldownHudStyle.PANELS;

    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public BuffHudStyle buffHudStyle = BuffHudStyle.PANELS;

    @ConfigEntry.Gui.Excluded
    public HudPosition buffHudPos = new HudPosition();
    @ConfigEntry.Gui.Excluded
    public HudPosition cooldownHudPos = new HudPosition();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = false)
    public DevSettings dev = new DevSettings();

    public enum IndicatorStyle { CIRCLE, SQUARE, BORDER }

    public enum CooldownHudStyle { PANELS, TEXT }

    public enum BuffHudStyle { PANELS, TEXT }

    public static class DevSettings implements ConfigData {
        @ConfigEntry.Gui.Tooltip
        public String supabaseUrl = "https://doplhwkpsbzgnzzkymfb.supabase.co";

        @ConfigEntry.Gui.Tooltip
        public String supabaseAnonKey = "sb_publishable_LoVp8WNIjCyrU14_Zu8g1A_1MGjxquT";
    }
}