package dev.blancocl.jevmc.paper;

import dev.blancocl.jevmc.JevClient;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

/**
 * Example plugin bundled with JevMC: chat moderation powered by Jev.
 *
 * <p>The library jar works directly as a plugin: drop it into the
 * {@code plugins/} folder and configure the API key in
 * {@code plugins/JevMCModeration/config.yml} or via the
 * {@code TYPESAFE_API_KEY} environment variable.
 *
 * <p>API key loading (in priority order):
 * <ol>
 *   <li>{@code api-key} in {@code config.yml}</li>
 *   <li>{@code TYPESAFE_API_KEY} environment variable</li>
 * </ol>
 */
public class ModerationPlugin extends JavaPlugin {

    private JevClient jev;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String apiKey = loadApiKey();
        if (apiKey == null) {
            getLogger().severe("Missing TypeSafe.ai API key: set 'api-key' in "
                    + "config.yml or the TYPESAFE_API_KEY environment variable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        jev = new JevClient(apiKey)
                .withRequestTimeout(Duration.ofSeconds(
                        getConfig().getLong("request-timeout-seconds", 30)));

        getServer().getPluginManager()
                .registerEvents(new ChatModerationListener(this, jev), this);

        getLogger().info("JevMC chat moderation enabled.");
    }

    private String loadApiKey() {
        String apiKey = getConfig().getString("api-key", "");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        String envKey = System.getenv("TYPESAFE_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        return null;
    }
}
