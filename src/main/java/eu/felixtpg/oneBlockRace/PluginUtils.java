package eu.felixtpg.oneBlockRace;

import org.bukkit.Material;

public class PluginUtils {

    public static String getReadableName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

}
