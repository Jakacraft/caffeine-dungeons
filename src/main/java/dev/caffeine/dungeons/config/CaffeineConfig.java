package dev.caffeine.dungeons.config;

import dev.caffeine.dungeons.hud.HudPosition;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "caffeine_dungeons")
public class CaffeineConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public RarityIndicatorSettings rarityIndicator = new RarityIndicatorSettings();

    @ConfigEntry.Gui.CollapsibleObject
    public CooldownHudSettings cooldownHud = new CooldownHudSettings();

    @ConfigEntry.Gui.CollapsibleObject
    public BuffHudSettings buffHud = new BuffHudSettings();

    @ConfigEntry.Gui.CollapsibleObject
    public AccessoryHudSettings accessoryHud = new AccessoryHudSettings();

    @ConfigEntry.Gui.CollapsibleObject
    public PickupHudSettings pickupHud = new PickupHudSettings();

    @ConfigEntry.Gui.CollapsibleObject
    public SkillXpHudSettings skillXpHud = new SkillXpHudSettings();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = false)
    public DevSettings dev = new DevSettings();

    public enum IndicatorStyle { CIRCLE, SQUARE, BORDER }
    public enum CooldownHudStyle { PANELS, TEXT }
    public enum BuffHudStyle { PANELS, TEXT }
    public enum AccessoryHudStyle { PANELS, TEXT }
    public enum PickupHudStyle { PANELS, TEXT }
    public enum SkillXpHudStyle { PANELS, TEXT }

    public static class RarityIndicatorSettings implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public IndicatorStyle style = IndicatorStyle.CIRCLE;

        @ConfigEntry.BoundedDiscrete(min = 10, max = 100)
        public int alpha = 50;
    }

    public static class CooldownHudSettings implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public CooldownHudStyle style = CooldownHudStyle.PANELS;

        @ConfigEntry.Gui.Excluded
        public HudPosition pos = new HudPosition();
    }

    public static class BuffHudSettings implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public BuffHudStyle style = BuffHudStyle.PANELS;

        @ConfigEntry.Gui.Excluded
        public HudPosition pos = new HudPosition();
    }

    public static class AccessoryHudSettings implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public AccessoryHudStyle style = AccessoryHudStyle.PANELS;

        @ConfigEntry.Gui.Excluded
        public HudPosition pos = new HudPosition();
    }

    public static class PickupHudSettings implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public PickupHudStyle style = PickupHudStyle.PANELS;

        @ConfigEntry.Gui.Excluded
        public HudPosition pos = new HudPosition();
        { pos.scale = 0.7f; }
    }

    public static class SkillXpHudSettings implements ConfigData {
        public boolean enabled = true;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SkillXpHudStyle style = SkillXpHudStyle.PANELS;

        @ConfigEntry.Gui.Excluded
        public HudPosition pos = new HudPosition();
    }

    public static class DevSettings implements ConfigData {
        public String backendUrl = "https://caffeine-dungeons.duckdns.org";
        public String backendAdminKey = "";
    }
}