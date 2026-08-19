package dev.caffeine.dungeons.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartyChatListener {

    public static final PartyChatListener INSTANCE = new PartyChatListener();

    // Matches "- [Tag] Username" or "- Username"
    private static final Pattern MEMBER_LINE = Pattern.compile("^-\\s+(?:\\[.*?\\]\\s+)?(.+)$");

    private final List<String> pendingMembers = new ArrayList<>();
    private boolean collecting = false;
    private PartyScreen activeScreen = null;

    private PartyChatListener() {}

    public void setActiveScreen(PartyScreen screen) {
        this.activeScreen = screen;
    }

    public void clearActiveScreen() {
        this.activeScreen = null;
        this.collecting = false;
        this.pendingMembers.clear();
    }

    public boolean onChatMessage(String rawText) {
        if (rawText.contains("[Party] Players in your party:")) {
            pendingMembers.clear();
            collecting = true;
            return false;
        }

        if (collecting) {
            Matcher matcher = MEMBER_LINE.matcher(rawText.trim());
            if (matcher.matches()) {
                pendingMembers.add(matcher.group(1).trim());
                return false;
            } else {
                collecting = false;
                if (activeScreen != null && !pendingMembers.isEmpty()) {
                    activeScreen.updateMembers(new ArrayList<>(pendingMembers));
                }
                pendingMembers.clear();
            }
        }

        return false;
    }
}