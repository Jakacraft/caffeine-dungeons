package dev.caffeine.dungeons.supabase;

import com.google.gson.annotations.SerializedName;

public class PlayerData {

    public String uuid;
    public String username;

    @SerializedName("last_seen")
    public String lastSeen;

    @SerializedName("has_mod")
    public boolean hasMod = true;

    // TODO: rename these to actual Dungeon Dodge class names
    @SerializedName("dungeon_level")
    public int dungeonLevel;

    @SerializedName("class_1_level")
    public int class1Level;

    @SerializedName("class_2_level")
    public int class2Level;

    @SerializedName("class_3_level")
    public int class3Level;

    @SerializedName("class_4_level")
    public int class4Level;

    @SerializedName("class_5_level")
    public int class5Level;

    @SerializedName("class_6_level")
    public int class6Level;

    @SerializedName("class_7_level")
    public int class7Level;

    @SerializedName("class_8_level")
    public int class8Level;
}