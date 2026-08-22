package dev.caffeine.dungeons.buff;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoosterTablistReader {

    private static final Pattern BOOSTER_LINE =
            Pattern.compile("^.*?([A-Za-z][A-Za-z ]*?)\\s+Booster:\\s*(ACTIVE|INACTIVE)\\s*$");

    private BoosterTablistReader() {}

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;

        Set<String> active = new HashSet<>();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            Matcher m = BOOSTER_LINE.matcher(displayName.getString());
            if (m.matches() && "ACTIVE".equals(m.group(2))) {
                active.add(m.group(1).trim());
            }
        }
        BuffTracker.getInstance().setActiveBoosterNames(active);
    }
}