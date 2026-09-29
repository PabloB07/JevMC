package dev.blancocl.jevmc.paper;

import dev.blancocl.jevmc.Choice;
import dev.blancocl.jevmc.ChoiceAnswer;
import dev.blancocl.jevmc.JevClient;
import dev.blancocl.jevmc.Noul;
import dev.blancocl.jevmc.NoulAnswer;
import dev.blancocl.jevmc.Score;
import dev.blancocl.jevmc.ScoreAnswer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

/**
 * Example of using jevmc in a PaperMC plugin: automatic chat moderation.
 * Each message is evaluated with Noul (toxicity), Score (severity) and
 * Choice (suggested action) in a single API call.
 *
 * <p>Reads {@code toxicity-threshold} and {@code cancel-message} from the
 * plugin's config.yml. Register the listener from your main plugin class:
 * <pre>{@code
 * JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
 * Bukkit.getPluginManager().registerEvents(new ChatModerationListener(this, jev), this);
 * }</pre>
 */
public class ChatModerationListener implements Listener {

    private final Plugin plugin;
    private final JevClient jev;
    private final double toxicityThreshold;
    private final boolean cancelMessage;

    public ChatModerationListener(Plugin plugin, JevClient jev) {
        this.plugin = plugin;
        this.jev = jev;
        double threshold = plugin.getConfig().getDouble("toxicity-threshold", 0.8);
        this.toxicityThreshold = Math.max(0.0, Math.min(1.0, threshold));
        this.cancelMessage = plugin.getConfig().getBoolean("cancel-message", false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String player = event.getPlayer().getName();

        // AsyncChatEvent already runs off the main thread: safe to call directly.
        jev.evaluateAsync(
                Map.of("player", player, "message", message),
                Map.of(
                        "toxic", new Noul("The message is toxic, insulting or aggressive"),
                        "severity", new Score("Severity of the message",
                                List.of("Harmless", "Annoying", "Offensive", "Severe")),
                        "action", new Choice("Suggested action for staff",
                                Map.of("none", "The message is harmless",
                                       "warn", "Warn the player via private message",
                                       "punish", "Apply mute or ban"))
                )
        ).whenComplete((response, error) -> {
            if (error != null) {
                plugin.getLogger().warning("jevmc failed: " + error.getMessage());
                return;
            }

            boolean toxic = response.get("toxic", NoulAnswer.class)
                    .isLikelyTrue(toxicityThreshold);

            // Hop back to the main thread before touching the Bukkit API.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (toxic) {
                    if (cancelMessage) {
                        event.setCancelled(true);
                    }
                    plugin.getLogger().warning(String.format(
                            "[jevmc] Message from %s flagged: severity=%s action=%s",
                            player,
                            response.get("severity", ScoreAnswer.class).closestLevel(),
                            response.get("action", ChoiceAnswer.class).choice()));
                }
            });
        });
    }
}
