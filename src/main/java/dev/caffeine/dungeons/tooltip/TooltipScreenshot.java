package dev.caffeine.dungeons.tooltip;

import dev.caffeine.dungeons.CaffeineDungeons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TooltipScreenshot {

    private TooltipScreenshot() {}

    public static void capture() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!TooltipTracker.isActive()) {
            if (client.player != null)
                client.player.sendMessage(Text.literal("§cHover over an item first!"), true);
            return;
        }

        double scale  = client.getWindow().getScaleFactor();
        int fbX = (int) (TooltipTracker.getX()      * scale);
        int fbY = (int) (TooltipTracker.getY()      * scale);
        int fbW = (int) (TooltipTracker.getWidth()  * scale);
        int fbH = (int) (TooltipTracker.getHeight() * scale);

        int fbWidth  = client.getFramebuffer().textureWidth;
        int fbHeight = client.getFramebuffer().textureHeight;
        fbX = Math.max(0, fbX);
        fbY = Math.max(0, fbY);
        fbW = Math.min(fbW, fbWidth  - fbX);
        fbH = Math.min(fbH, fbHeight - fbY);

        if (fbW <= 0 || fbH <= 0) {
            if (client.player != null)
                client.player.sendMessage(Text.literal("§cTooltip out of bounds!"), true);
            return;
        }

        final int capX = fbX, capY = fbY, capW = fbW, capH = fbH;

        ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), full -> {
            try {
                NativeImage cropped = new NativeImage(capW, capH, false);
                for (int dy = 0; dy < capH; dy++)
                    for (int dx = 0; dx < capW; dx++)
                        cropped.setColorArgb(dx, dy, full.getColorArgb(capX + dx, capY + dy));

                Path screenshotsDir = client.runDirectory.toPath().resolve("screenshots");
                Files.createDirectories(screenshotsDir);
                String filename = "tooltip_" + System.currentTimeMillis() + ".png";
                Path outPath = screenshotsDir.resolve(filename);
                cropped.writeTo(outPath.toFile());
                cropped.close();

                if (client.player != null)
                    client.player.sendMessage(
                            Text.literal("§aTooltip saved to screenshots/" + filename), true
                    );
            } catch (IOException e) {
                CaffeineDungeons.LOGGER.error("[TooltipScreenshot] Failed to save", e);
                if (client.player != null)
                    client.player.sendMessage(Text.literal("§cFailed to save tooltip screenshot!"), true);
            }
        });
    }
}