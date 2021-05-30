package com.javadiscord.javabot;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.javadiscord.javabot.commands.configuation.Config;
import com.javadiscord.javabot.commands.configuation.WelcomeImage;
import com.javadiscord.javabot.commands.custom_commands.CustomCommands;
import com.javadiscord.javabot.commands.moderation.*;
import com.javadiscord.javabot.commands.other.GuildConfig;
import com.javadiscord.javabot.commands.other.Question;
import com.javadiscord.javabot.commands.other.Shutdown;
import com.javadiscord.javabot.commands.other.Version;
import com.javadiscord.javabot.commands.other.qotw.ClearQOTW;
import com.javadiscord.javabot.commands.other.qotw.Correct;
import com.javadiscord.javabot.commands.other.qotw.Leaderboard;
import com.javadiscord.javabot.commands.other.suggestions.Accept;
import com.javadiscord.javabot.commands.other.suggestions.Clear;
import com.javadiscord.javabot.commands.other.suggestions.Decline;
import com.javadiscord.javabot.commands.other.suggestions.Response;
import com.javadiscord.javabot.commands.other.testing.*;
import com.javadiscord.javabot.commands.reaction_roles.ReactionRoles;
import com.javadiscord.javabot.commands.user_commands.*;
import com.javadiscord.javabot.events.*;
import com.javadiscord.javabot.properties.MultiProperties;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.nio.file.Path;
import java.util.Properties;


public class Bot {

    public static JDA jda;
    public static EventWaiter waiter;

    private static final Properties properties = new MultiProperties(Path.of("bot.props"));

    public static void main(String[] args) throws Exception {
            waiter = new EventWaiter();

            CommandClientBuilder client = new CommandClientBuilder()
                    .setOwnerId("374328434677121036")
                    .setCoOwnerIds("299555811804315648", "620615131256061972")
                    .setPrefix("!")
                    .setEmojis("✅", "⚠️", "❌")
                    .useHelpBuilder(false)
                    .addCommands(

                            //UserCommands
                            new Avatar(),
                            new BotInfo(),
                            new ChangeMyMind(),
                            new Help(),
                            new IDCalc(),
                            new Lmgtfy(),
                            new Ping(),
                            new Profile(),
                            new ServerInfo(),
                            new Uptime(),

                            //ReactionRoles
                            new ReactionRoles(),

                            //CustomCommands
                            new CustomCommands(),

                            //Other
                            new Question(),
                            new Shutdown(),
                            new Version(),
                            new GuildConfig(),

                            //Other.Testing
                            new SampleSuggestion(),
                            new RefreshCategory(),
                            new MongoDBAddUser(),
                            new Image(),
                            new AddConfigFile(),
                            new UpdateUserFiles(),

                            //Other.Suggestions
                            new Accept(),
                            new Clear(),
                            new Decline(),
                            new Response(),

                            //Other.QOTW
                            new ClearQOTW(),
                            new Correct(),
                            new Leaderboard(),

                            //Commands.Moderation
                            new Ban(),
                            new ClearWarns(),
                            new EditEmbed(),
                            new Embed(),
                            new Kick(),
                            new Mute(),
                            new Mutelist(),
                            new Purge(),
                            new Report(),
                            new Unban(),
                            new Unmute(),
                            new Warn(),
                            new Warns(),

                            //Commands.Configuration
                            new Config(),
                            new WelcomeImage()
                    );


            jda = JDABuilder.createDefault(properties.getProperty("token", "null"))
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.ACTIVITY)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                    .build();

            jda.addEventListener(waiter, client.build());

            //EVENTS
            jda.addEventListener(new GuildJoin());
            jda.addEventListener(new UserJoin());
            jda.addEventListener(new UserLeave());
            jda.addEventListener(new Startup());
            jda.addEventListener(new StatusUpdate());
            jda.addEventListener(new ReactionListener());
            jda.addEventListener(new SuggestionListener());
            jda.addEventListener(new CstmCmdListener());
            jda.addEventListener(new AutoMod());
            jda.addEventListener(new SubmissionListener());
            jda.addEventListener(new SlashCommands());
            //jda.addEventListener(new StarboardListener());
    }

    /**
     * Gets the value of a property from the bot's loaded properties.
     * @see Properties#getProperty(String)
     * @param key The name of the property to get.
     * @return The value of the property, or <code>null</code> if none was found.
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets the value of a property from the bot's loaded properties.
     * @see Properties#getProperty(String, String)
     * @param key The name of the property to get.
     * @param defaultValue The value to return if no property was found.
     * @return The value of the property, or the default value.
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
