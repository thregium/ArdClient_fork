package haven.automation;

import haven.Config;
import haven.purus.pbot.PBotDiscord;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordBot {
    static JDA jda;
    static DiscordListener discordListener = new DiscordListener();

    public static JDA getJda() {
        if (jda == null) {
            try {
                JDA jda = JDABuilder.createDefault(Config.discordtoken).build();
                jda.setAutoReconnect(true);
                jda.addEventListener(discordListener);
                jda.getPresence().setActivity(Activity.competing("Haven and Hearth"));
                jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
                jda.awaitReady();
            } catch (Exception e) {
                System.out.println("DiscordBot getJDA: Invalid Token");
//                e.printStackTrace();
            }
        }
        return jda;
    }

    public static class DiscordListener extends ListenerAdapter {
        @Override
        public void onReady(ReadyEvent event) {
            jda = event.getJDA();
        }

        @Override
        public void onDisconnect(@NotNull DisconnectEvent event) {
            jda = null;
        }

        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            PBotDiscord.messageListeners.forEach((s, l) -> l.add(event));
        }
    }
}
