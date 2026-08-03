package dev.caffeine.dungeons.skillxp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillXpTracker {

    public static final SkillXpTracker INSTANCE = new SkillXpTracker();

    private static final Pattern XP_PATTERN = Pattern.compile(
            ".*\\+(\\d[\\d,]*)\\s+(.+?)\\s+exp\\s*\\((\\d[\\d,]*)/(\\d[\\d,]*)\\).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private SkillXpEntry current;

    private SkillXpTracker() {}

    public SkillXpEntry getCurrent() {
        return current;
    }

    public boolean onActionBarMessage(String plainText) {
        Matcher m = XP_PATTERN.matcher(plainText);
        if (!m.matches()) return false;

        long gain = parseLong(m.group(1));
        String skillName = capitalize(m.group(2).trim());
        long cur = parseLong(m.group(3));
        long max = parseLong(m.group(4));

        long now = System.currentTimeMillis();
        if (current != null && current.skillName.equalsIgnoreCase(skillName)) {
            current.update(cur, max, gain, now);
        } else {
            current = new SkillXpEntry(skillName, cur, max, gain, now);
        }
        return true;
    }

    private long parseLong(String s) {
        try {
            return Long.parseLong(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String capitalize(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean startOfWord = true;
        for (char c : s.toCharArray()) {
            if (Character.isWhitespace(c)) {
                startOfWord = true;
                out.append(c);
            } else {
                out.append(startOfWord ? Character.toUpperCase(c) : c);
                startOfWord = false;
            }
        }
        return out.toString();
    }

    public void tick() {
        if (current == null) return;
        current.tickAnimation();
        if (current.isExpired(System.currentTimeMillis())) {
            current = null;
        }
    }

    public void clear() {
        current = null;
    }
}