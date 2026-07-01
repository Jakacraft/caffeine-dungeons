package dev.caffeine.dungeons;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Runs before mod/game initialization — the earliest hook Fabric Loader
 * gives a mod. Forces IPv4-only networking system-wide for this JVM,
 * working around java.net.http.HttpClient failing to resolve/connect to
 * Supabase over IPv6 on some networks (UnresolvedAddressException) even
 * though the OS and browser resolve/connect fine.
 *
 * This system property is read once by the JVM's networking internals on
 * first use, then cached for the process's lifetime — setting it later
 * (e.g. in onInitializeClient) is unreliable if anything upstream has
 * already touched java.net classes. PreLaunch is the earliest a mod can
 * act, maximizing the chance this actually takes effect.
 */
public class CaffeinePreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }
}