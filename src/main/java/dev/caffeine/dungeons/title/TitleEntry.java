package dev.caffeine.dungeons.title;

public record TitleEntry(String id, String titleText, String colorHex) {
    public boolean isChroma() {
        return "chroma".equalsIgnoreCase(colorHex);
    }
}