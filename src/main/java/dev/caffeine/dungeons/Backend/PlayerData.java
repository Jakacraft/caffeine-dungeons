package dev.caffeine.dungeons.Backend;

import com.google.gson.annotations.SerializedName;

public class PlayerData {

    public String uuid;
    public String username;

    @SerializedName("last_seen")
    public String lastSeen;

    @SerializedName("has_mod")
    public boolean hasMod = true;
}