import ListenersAndCommands.EventListener;
import ListenersAndCommands.SlashCommands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        JDABuilder builder = JDABuilder.createDefault(System.getenv("DISCORD_API_KEY"));
        builder.addEventListeners(new EventListener());
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        JDA bot = builder.build();
        bot.awaitReady();
        new SlashCommands(bot);
    }
}
