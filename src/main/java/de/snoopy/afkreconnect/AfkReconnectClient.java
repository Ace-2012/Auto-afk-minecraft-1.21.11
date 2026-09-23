package de.snoopy.afkreconnect;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Merkt sich den zuletzt verbundenen Server. Fliegt man raus (Timeout, Kick,
 * Verbindungsabbruch), wird nach ein paar Sekunden automatisch neu verbunden.
 * Sobald der Join fertig ist, wird automatisch "/afk" gesendet - man landet
 * also wieder in der AFK-Welt, ohne selbst etwas eintippen zu muessen.
 */
public class AfkReconnectClient implements ClientModInitializer {

    // ---------------- Einstellungen zum Anpassen ----------------
    private static final String AFK_COMMAND = "afk";       // ohne führenden "/"
    private static final int AFK_DELAY_TICKS = 60;          // 3s nach dem Join warten, bevor /afk gesendet wird
    private static final int RECONNECT_DELAY_TICKS = 100;   // 5s warten, bevor der erste Reconnect-Versuch startet
    private static final int BACKOFF_STEP_TICKS = 40;       // pro fehlgeschlagenem Versuch 2s länger warten
    private static final int MAX_RECONNECT_ATTEMPTS = 30;   // danach aufgeben (z.B. falls Server dauerhaft down/Ban)
    // --------------------------------------------------------------

    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("afkreconnect", "main"));
    private static KeyBinding toggleKey;
    private static boolean enabled = true;

    private static ServerInfo lastServerInfo;

    private static boolean waitingForReconnect = false;
    private static int reconnectCountdown = 0;
    private static int reconnectAttempts = 0;

    private static boolean waitingToSendAfk = false;
    private static int afkCountdown = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.afkreconnect.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // Standardmäßig nicht belegt -> im Steuerungsmenü selbst zuweisen
                CATEGORY
        ));

        // Erfolgreich (neu) verbunden -> Server merken, /afk vorbereiten
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            lastServerInfo = handler.getServerInfo();
            reconnectAttempts = 0;
            waitingForReconnect = false;

            if (enabled && lastServerInfo != null) {
                waitingToSendAfk = true;
                afkCountdown = AFK_DELAY_TICKS;
            }
        });

        // Verbindung verloren -> Reconnect vormerken (mit ansteigender Wartezeit)
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            waitingToSendAfk = false;

            if (enabled && lastServerInfo != null && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                waitingForReconnect = true;
                reconnectCountdown = RECONNECT_DELAY_TICKS + (reconnectAttempts * BACKOFF_STEP_TICKS);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        handleToggleKey(client);
        handleAfkCommand(client);
        handleReconnect(client);
    }

    private void handleToggleKey(MinecraftClient client) {
        if (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (!enabled) {
                waitingForReconnect = false;
                waitingToSendAfk = false;
            }
            if (client.player != null) {
                client.player.sendMessage(Text.literal(enabled
                        ? "§a[AFK-Reconnect] Aktiviert"
                        : "§c[AFK-Reconnect] Deaktiviert"), false);
            }
        }
    }

    private void handleAfkCommand(MinecraftClient client) {
        if (!waitingToSendAfk || client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        if (afkCountdown > 0) {
            afkCountdown--;
            return;
        }
        client.getNetworkHandler().sendChatCommand(AFK_COMMAND);
        client.player.sendMessage(Text.literal("§7[AFK-Reconnect] /" + AFK_COMMAND + " gesendet"), false);
        waitingToSendAfk = false;
    }

    private void handleReconnect(MinecraftClient client) {
        // client.world == null heißt: wir sind gerade in keiner laufenden Welt/Server-Session
        if (!waitingForReconnect || client.world != null || lastServerInfo == null) {
            return;
        }
        if (reconnectCountdown > 0) {
            reconnectCountdown--;
            return;
        }

        waitingForReconnect = false;
        reconnectAttempts++;

        ConnectScreen.connect(
                new TitleScreen(),
                client,
                ServerAddress.parse(lastServerInfo.address),
                lastServerInfo,
                false,
                null
        );
    }
}
