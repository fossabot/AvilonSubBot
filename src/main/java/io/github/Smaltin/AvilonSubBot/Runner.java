package io.github.Smaltin.AvilonSubBot;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import io.github.Smaltin.AvilonSubBot.Commands.*;
import io.github.Smaltin.AvilonSubBot.Commands.Music.PlayCommand;
import io.github.Smaltin.AvilonSubBot.Commands.Music.SkipCommand;
import io.github.Smaltin.AvilonSubBot.MusicUtilities.MusicUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kodehawa.lib.imageboards.ImageBoard;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static io.github.Smaltin.AvilonSubBot.Configuration.*;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES;

public class Runner extends ListenerAdapter {
    public static final HashMap<String, AbstractCommand> commands = new HashMap<>();
    public static final HashMap<String, AbstractCommand> commandsAlias = new HashMap<>();
    public static final String CODE_VERSION = "0.0.2";
    public static JDA holder;
    public static String postedSubCt;


    public static void main(String[] args) throws LoginException, InterruptedException {
        MusicUtilities.musicManagers = new HashMap<>();
        MusicUtilities.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(MusicUtilities.playerManager);
        if (args.length >= 1) DEVELOPER_MODE = (args[0].equals("true"));
        try { //If it's not developer mode from command line, check the environment variables
            if (!DEVELOPER_MODE) {
                DEVELOPER_MODE = System.getenv("dev").equals("true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            if (isInternetWorking()) break;
        }
        updateEnv();
        ImageBoard.setUserAgent("Mozilla/5.0 (compatible; "+ "AvilonSubBot/" + CODE_VERSION + "; +github.com/Smaltin/AvilonSubBot)");
        if (DEVELOPER_MODE) System.out.println("Hello nerd. Imagine being a programmer, couldn't be me... wait.."); else System.out.println("AvilonSubBot running code v" + CODE_VERSION + " :)");
        holder = JDABuilder.createDefault(Configuration.BOTKEY, GUILD_MESSAGES, GUILD_VOICE_STATES).addEventListeners(new Runner()).build();
        holder.awaitReady();
        loadCommands();
        holder.getPresence().setActivity(Activity.listening(!DEVELOPER_MODE ? "Avilon's Music" : "Smaltin's sick 'grammer beats"));
        SubCountRenamer.SubCounter thread = new SubCountRenamer.SubCounter();
        thread.start();
    }

    /**
     * Checks if the internet is working by connecting to http://google.com
     *
     * @return true if you can connect to http://google.com
     * @author Smaltin
     */
    public static boolean isInternetWorking() {
        try {
            URL url = new URL("https://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Loads up all the commands from commands put into the list
     *
     * @author jojo2357
     */
    public static void loadCommands() {
        if (commands.keySet().size() > 0) return;
        List<Class<? extends AbstractCommand>> classes = Arrays.asList(RestartCommand.class, CatCommand.class, VerifyCommand.class, PaciCommand.class, PatCommand.class, PingCommand.class, MemeCommand.class, KickCommand.class, BanCommand.class, HugCommand.class, KissCommand.class, AviTimeCommand.class, HowPogCommand.class, HelpCommand.class, PlayCommand.class, SkipCommand.class);
        for (Class<? extends AbstractCommand> s : classes) { //TODO add "eject" and sus commands
            //TODO make music betterer
            try {
                if (Modifier.isAbstract(s.getModifiers())) {
                    continue;
                }
                AbstractCommand c = s.getConstructor().newInstance();
                if (!commands.containsKey(c.getCommand())) {
                    commands.put(c.getCommand(), c);
                }
                for (String alias : c.getAliases()) {
                    if (!commandsAlias.containsKey(alias)) {
                        commandsAlias.put(alias, c);
                    }
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Enters into the settings.env file and pulls out the value you set
     *
     * @param key The key to check under
     * @return the string value from the key
     */
    public static String getEnv(String key) {
        try {
            Properties loadProps = new Properties();
            loadProps.load(new FileInputStream((DEVELOPER_MODE ? "dev-" : "") + "settings.env"));
            return loadProps.getProperty(key);
        } catch (Exception e) {
            e.printStackTrace();
            return "No, you're bad.";
        }
    }

    /**
     * Called by {@link JDA} when a new message is sent and visible to the discord bot
     *
     * @param message the incoming message
     * @author jojo2357
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent message) {
        if (message.getAuthor().isBot()) return;
        if (!message.isFromGuild()) return;

        Message msg = message.getMessage();

        if (!(msg.getContentRaw().length() > 0)) {
            return;
        }

        if (msg.getContentRaw().startsWith(PREFIX) && getCommand(msg.getContentRaw()) != null) {
            System.out.println("Recieved Message: " + message.getMessage().getContentRaw());
            AbstractCommand command = getCommand(message.getMessage().getContentRaw());
            if (command == null) return;
            command.runCommand(holder, message, msg);
        }
    }

    /**
     * Searches through registered commands and gets the command matching incomming message
     *
     * @param message either the full incomming message or target string that a command should be extracted from
     * @return a command fitting the passed string, null if no matches
     * @author jojo2357
     */
    @Nullable
    public static AbstractCommand getCommand(String message) {
        message = message.replace(PREFIX, "").split(" ")[0];
        if (commands.containsKey(message)) return commands.get(message);
        if (commandsAlias.containsKey(message)) return commandsAlias.get(message);
        return null;
    }
}