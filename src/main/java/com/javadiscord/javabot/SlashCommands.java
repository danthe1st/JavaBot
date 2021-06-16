package com.javadiscord.javabot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.CommandClient;
import com.javadiscord.javabot.commands.configuation.Config;
import com.javadiscord.javabot.commands.configuation.WelcomeImage;
import com.javadiscord.javabot.commands.custom_commands.CustomCommands;
import com.javadiscord.javabot.commands.moderation.*;
import com.javadiscord.javabot.commands.other.Question;
import com.javadiscord.javabot.commands.other.qotw.ClearQOTW;
import com.javadiscord.javabot.commands.other.qotw.Correct;
import com.javadiscord.javabot.commands.other.qotw.Leaderboard;
import com.javadiscord.javabot.commands.other.suggestions.Accept;
import com.javadiscord.javabot.commands.other.suggestions.Clear;
import com.javadiscord.javabot.commands.other.suggestions.Decline;
import com.javadiscord.javabot.commands.other.suggestions.Respond;
import com.javadiscord.javabot.commands.reaction_roles.ReactionRoles;
import com.javadiscord.javabot.commands.user_commands.*;
import com.javadiscord.javabot.other.Constants;
import com.javadiscord.javabot.other.Embeds;
import com.javadiscord.javabot.other.SlashEnabledCommand;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

import static com.javadiscord.javabot.events.Startup.mongoClient;
import static com.mongodb.client.model.Filters.eq;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class SlashCommands extends ListenerAdapter {
    /**
     * Maps every command name and alias to an instance of the command, for
     * constant-time lookup.
     */
    private final Map<String, SlashEnabledCommand> commandsIndex;

    public SlashCommands(CommandClient commandClient) {
        this.commandsIndex = new HashMap<>();
        this.registerSlashCommands(commandClient);
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (event.getGuild() == null) return;

        var command = this.commandsIndex.get(event.getName());
        if (command != null) {
            command.execute(event);
            return;
        }

        String reason;
        boolean bool;
        Member member;

        switch (event.getName()) {

            // USER COMMANDS

            case "avatar":

                OptionMapping option = event.getOption("user");
                User user = option == null ? event.getUser() : option.getAsUser();

                Avatar.execute(event, user);
                break;

            case "botinfo":

                BotInfo.execute(event);
                break;

            case "changemymind":

                ChangeMyMind.execute(event, event.getOption("text").getAsString());
                break;

            case "help":
                Help.execute(event);
                break;

            case "idcalc":

                long idInput = 0;
                try {
                    idInput = event.getOption("id").getAsLong();
                } catch (Exception e) {
                    idInput = event.getUser().getIdLong();
                }

                IDCalc.execute(event, idInput);
                break;

            case "lmgtfy":

                Lmgtfy.execute(event, event.getOption("text").getAsString());
                break;

            case "ping":

                Ping.execute(event);
                break;

            case "profile":

                OptionMapping profileOption = event.getOption("user");
                member = profileOption == null ? event.getMember() : profileOption.getAsMember();

                Profile.execute(event, member);
                break;

            case "serverinfo":

                ServerInfo.execute(event);
                break;

            case "uptime":

                Uptime.execute(event);
                break;

            // MODERATION

            case "ban":

                try {
                    reason = event.getOption("reason").getAsString();
                } catch (NullPointerException e) {
                    reason = "None";
                }

                Ban.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser(), reason);
                break;

            case "clearwarns":

                ClearWarns.execute(event,
                        event.getOption("user").getAsMember());
                break;

            case "clearqotw":

                ClearQOTW.execute(event,
                        event.getOption("user").getAsMember());
                break;

            case "editembed":

                EditEmbed.execute(event,
                        event.getOption("messageid").getAsString(),
                        event.getOption("title").getAsString(),
                        event.getOption("description").getAsString());
                break;

            case "embed":

                OptionMapping embedOption;
                embedOption = event.getOption("title");
                String title = embedOption == null ? null : embedOption.getAsString();

                embedOption = event.getOption("description");
                String description = embedOption == null ? null : embedOption.getAsString();

                embedOption = event.getOption("author-name");
                String authorname = embedOption == null ? null : embedOption.getAsString();

                embedOption = event.getOption("author-url");
                String url = embedOption == null ? null : embedOption.getAsString();

                embedOption = event.getOption("author-iconurl");
                String iconurl = embedOption == null ? null : embedOption.getAsString();

                embedOption = event.getOption("thumbnail-url");
                String thumb = embedOption == null ? null : embedOption.getAsString();

                embedOption = event.getOption("image-url");
                String img = embedOption == null ? null : embedOption.getAsString();

                embedOption = event.getOption("color");
                String color = embedOption == null ? null : embedOption.getAsString();

                Embed.execute(event, title, description, authorname, url, iconurl, thumb, img, color);
                break;

            case "kick":

                try {
                    reason = event.getOption("reason").getAsString();
                } catch (NullPointerException e) {
                    reason = "None";
                }

                Kick.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser(), reason);
                break;

            case "mute":

                Mute.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser());
                break;

            case "mutelist":

                Mutelist.execute(event);
                break;

            case "purge":

                try {
                    bool = event.getOption("nuke-channel").getAsBoolean();
                } catch (NullPointerException e) {
                    bool = false;
                }

                Purge.execute(event,
                        (int) event.getOption("amount").getAsLong(),
                        bool);
                break;

            case "report":

                try {
                    reason = event.getOption("reason").getAsString();
                } catch (NullPointerException e) {
                    reason = "None";
                }

                Report.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser(), reason);
                break;

            case "unban":

                Unban.execute(event,
                        event.getOption("id").getAsString(),
                        event.getUser());
                break;

            case "unmute":

                Unmute.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser());
                break;

            case "warn":

                try {
                    reason = event.getOption("reason").getAsString();

                } catch (NullPointerException e) {
                    reason = "None";
                }

                Warn.execute(event,
                        event.getOption("user").getAsMember(),
                        event.getUser(), reason);
                break;

            case "warns":

                OptionMapping warnsOption = event.getOption("user");
                member = warnsOption == null ? event.getMember() : warnsOption.getAsMember();

                Warns.execute(event, member);
                break;

            case "config":

                if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    switch (event.getSubcommandName()) {

                        case "list":
                            Config.getList(event);
                            break;

                        case "leave-message":
                            Config.setLeaveMessage(event, event.getOption("message").getAsString());
                            break;

                        case "welcome-message":
                            Config.setWelcomeMessage(event, event.getOption("message").getAsString());
                            break;

                        case "welcome-channel":
                            Config.setWelcomeChannel(event, event.getOption("channel").getAsMessageChannel());
                            break;

                        case "stats-category":
                            Config.setStatsCategory(event, event.getOption("id").getAsString());
                            break;

                        case "stats-message":
                            Config.setStatsMessage(event, event.getOption("message").getAsString());
                            break;

                        case "report-channel":
                            Config.setReportChannel(event, event.getOption("channel").getAsMessageChannel());
                            break;

                        case "log-channel":
                            Config.setLogChannel(event, event.getOption("channel").getAsMessageChannel());
                            break;

                        case "suggestion-channel":
                            Config.setSuggestionChannel(event, event.getOption("channel").getAsMessageChannel());
                            break;

                        case "submission-channel":
                            Config.setSubmissionChannel(event, event.getOption("channel").getAsMessageChannel());
                            break;

                        case "mute-role":
                            Config.setMuteRole(event, event.getOption("role").getAsRole());
                            break;

                        case "dm-qotw":
                            Config.setDMQOTWStatus(event, event.getOption("enabled").getAsBoolean());
                            break;

                        case "lock":
                            Config.setLockStatus(event, event.getOption("locked").getAsBoolean());
                            break;
                    }
                } else {
                    event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
                }

                break;

            case "welcome-image":

                if (event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    switch (event.getSubcommandName()) {

                        case "list":
                            WelcomeImage.getList(event);
                            break;

                        case "image-width":
                            WelcomeImage.setImageWidth(event, (int) event.getOption("width").getAsLong());
                            break;

                        case "image-height":
                            WelcomeImage.setImageHeight(event, (int) event.getOption("height").getAsLong());
                            break;

                        case "overlay-url":
                            WelcomeImage.setOverlayURL(event, event.getOption("url").getAsString());
                            break;

                        case "background-url":
                            WelcomeImage.setBackgroundURL(event, event.getOption("url").getAsString());
                            break;

                        case "primary-color":
                            WelcomeImage.setPrimaryColor(event, event.getOption("color").getAsString());
                            break;

                        case "secondary-color":
                            WelcomeImage.setSecondaryColor(event, event.getOption("color").getAsString());
                            break;

                        case "avatar-x":
                            WelcomeImage.setAvatarX(event, (int) event.getOption("x").getAsLong());
                            break;

                        case "avatar-y":
                            WelcomeImage.setAvatarY(event, (int) event.getOption("y").getAsLong());
                            break;

                        case "avatar-width":
                            WelcomeImage.setAvatarWidth(event, (int) event.getOption("width").getAsLong());
                            break;

                        case "avatar-height":
                            WelcomeImage.setAvatarHeight(event, (int) event.getOption("height").getAsLong());
                            break;
                    }
                } else {
                    event.replyEmbeds(Embeds.permissionError("ADMINISTRATOR", event)).setEphemeral(Constants.ERR_EPHEMERAL).queue();
                }

                break;

            case "customcommand":

                switch (event.getSubcommandName()) {

                    case "list":
                        CustomCommands.list(event);
                        break;

                    case "create":
                        CustomCommands.create(event,
                                event.getOption("name").getAsString(),
                                event.getOption("text").getAsString());

                        break;

                    case "edit":
                        CustomCommands.edit(event,
                                event.getOption("name").getAsString(),
                                event.getOption("text").getAsString());

                        break;

                    case "delete":
                        CustomCommands.delete(event,
                                event.getOption("name").getAsString());

                        break;
                }

                break;

            case "reactionrole":

                switch (event.getSubcommandName()) {

                    case "list":
                        ReactionRoles.list(event);
                        break;

                    case "create":
                        ReactionRoles.create(event,
                                event.getOption("channel").getAsMessageChannel(),
                                event.getOption("messageid").getAsString(),
                                event.getOption("emote").getAsString(),
                                event.getOption("role").getAsRole());

                        break;

                    case "delete":
                        ReactionRoles.delete(event,
                                event.getOption("messageid").getAsString(),
                                event.getOption("emote").getAsString());
                        break;
                }

                break;

            case "leaderboard":

                try {
                    bool = event.getOption("old").getAsBoolean();
                } catch (NullPointerException e) {
                    bool = false;
                }

                Leaderboard.execute(event, bool);
                break;

            case "question":

                Question.execute(event, (int) event.getOption("amount").getAsLong());
                break;

            case "accept":

                Accept.execute(event, event.getOption("message-id").getAsString());
                break;

            case "clear":

                Clear.execute(event, event.getOption("message-id").getAsString());
                break;

            case "decline":

                Decline.execute(event, event.getOption("message-id").getAsString());
                break;

            case "respond":

                Respond.execute(event, event.getOption("message-id").getAsString(), event.getOption("text").getAsString());
                break;

            default:

                try {

                    BasicDBObject criteria = new BasicDBObject()
                            .append("guild_id", event.getGuild().getId())
                            .append("commandname", event.getName());

                    MongoDatabase database = mongoClient.getDatabase("other");
                    MongoCollection<Document> collection = database.getCollection("customcommands");
                    
                    Document it = collection.find(criteria).first();

                    JsonObject Root = JsonParser.parseString(it.toJson()).getAsJsonObject();
                    String value = Root.get("value").getAsString();

                    event.replyEmbeds(new EmbedBuilder().setColor(Constants.GRAY).setDescription(value).build()).queue();

                } catch (Exception e) {
                    event.reply("Oops, this command isnt registered, yet").queue();
                }
        }
    }

    /**
     * Registers all slash commands in the command index.
     *
     * @param commandClient The command client to register commands from.
     */
    private void registerSlashCommands(CommandClient commandClient) {
        for (var cmd : commandClient.getCommands()) {
            if (cmd instanceof SlashEnabledCommand) {
                var slashCommand = (SlashEnabledCommand) cmd;
                this.commandsIndex.put(slashCommand.getName(), slashCommand);
                for (var alias : slashCommand.getAliases()) {
                    this.commandsIndex.put(alias, slashCommand);
                }
            }
        }
    }

    public static void registerSlashCommands(Guild guild) {

        CommandListUpdateAction commands = guild.updateCommands();

        commands.addCommands(

                // USER COMMANDS

                new CommandData("avatar", "Shows your or someone else's profile picture")
                        .addOption(USER, "user", "If given, shows the profile picture of the given user", false),

                new CommandData("botinfo", "Shows some information about the Bot"),

                new CommandData("changemymind", "Generates the \"change my mind\" meme from your given input")
                        .addOption(STRING, "text", "your text-input", true),

                new CommandData("help", "Sends you a DM with all Commands"),

                new CommandData("idcalc", "Generates a human-readable timestamp out of a Discord-ID")
                        .addOption(STRING, "id", "The id the bot will convert into a human-readable timestamp", true),

                new CommandData("lmgtfy", "Turns your text-input into a lmgtfy-link")
                        .addOption(STRING, "text", "Text that will be converted into a lmgtfy-link", true),

                new CommandData("ping", "Checks the Bot's Gateway Ping"),

                new CommandData("profile", "Shows your or someon else's profile")
                        .addOption(USER, "user", "If given, shows the profile of the user", false),

                new CommandData("serverinfo", "Shows some information about the current guild"),

                new CommandData("uptime", "Checks the Bot's uptime"),

                // MODERATION

                new CommandData("ban", "Bans a member.")
                        .addOption(USER, "user", "The user you want to ban", true)
                        .addOption(STRING, "reason", "The reason", false),

                new CommandData("clearwarns", "Clears all warns from the given user.")
                        .addOption(USER, "user", "The user you want to clear all warns from", true),

                new CommandData("clearqotw", "Clears all QOTW-points from the given user.")
                        .addOption(USER, "user", "The user you want to clear all QOTW-points from", true),

                new CommandData("editembed", "Edits an embed")
                        .addOption(STRING, "messageid", "The message ID of the embed you want to edit", true)
                        .addOption(STRING, "title", "New title of the embed", true)
                        .addOption(STRING, "description", "New description of the embed", true),

                new CommandData("embed", "Sends an embed")
                        .addOption(STRING, "title", "Title of the embed", false)
                        .addOption(STRING, "description", "Description of the embed", false)
                        .addOption(STRING, "author-name", "Author name of the embed", false)
                        .addOption(STRING, "author-url", "Author url of the embed", false)
                        .addOption(STRING, "author-iconurl", "Author iconurl of the embed", false)
                        .addOption(STRING, "thumbnail-url", "Thumbnail url of the embed", false)
                        .addOption(STRING, "image-url", "Image url of the embed", false)
                        .addOption(STRING, "color", "Color of the embed (e.g. #ff0000)", false),

                new CommandData("kick", "Kicks a member")
                        .addOption(USER, "user", "The user you want to kick", true)
                        .addOption(STRING, "reason", "The reason", false),

                new CommandData("mute", "Mutes a member")
                        .addOption(USER, "user", "The user you want to mute", true),

                new CommandData("mutelist", "Lists all muted members"),

                new CommandData("purge", "Deletes the given amount of messages in a channel")
                        .addOption(OptionType.INTEGER, "amount", "the amount of messages you want to delete (2-100)", true)
                        .addOption(OptionType.BOOLEAN, "nuke-channel", "If true, creates a copy- and deletes the current channel", false),

                new CommandData("report", "Reports a member")
                        .addOption(USER, "user", "The user you want to report", true)
                        .addOption(STRING, "reason", "The reason", false),

                new CommandData("unban", "Unbans a member")
                        .addOption(STRING, "id", "The ID of the user you want to unban", true),

                new CommandData("unmute", "Unmutes a member")
                        .addOption(USER, "user", "The user you want to unmute", true),

                new CommandData("warn", "Warns a member")
                        .addOption(USER, "user", "The user you want to warn", true)
                        .addOption(STRING, "reason", "The reason", false),

                new CommandData("warns", "Shows your warn count")
                        .addOption(USER, "user", "If given, shows the warn count of the given user", false),

                // CONFIGURATION

                new CommandData("config", "Shows the config for the current guild")
                        .addSubcommands(
                                new SubcommandData("list", "Shows the current config"),
                                new SubcommandData("leave-message", "Changes the leave message").addOption(STRING, "message", "New leave message", true),
                                new SubcommandData("welcome-message", "Changes the welcome message").addOption(STRING, "message", "New welcome message", true),
                                new SubcommandData("welcome-channel", "Changes the welcome channel").addOption(CHANNEL, "channel", "New welcome channel", true),
                                new SubcommandData("stats-category", "Changes the id of the stats category").addOption(STRING, "id", "ID of the new stats category", true),
                                new SubcommandData("stats-message", "Changes the message of the stats category").addOption(STRING, "message", "New text of the stats category", true),
                                new SubcommandData("report-channel", "Changes the report channel").addOption(CHANNEL, "channel", "New report channel", true),
                                new SubcommandData("log-channel", "Changes the log channel").addOption(CHANNEL, "channel", "the new log channel", true),
                                new SubcommandData("suggestion-channel", "Changes the suggestion channel").addOption(CHANNEL, "channel", "New suggestion channel", true),
                                new SubcommandData("submission-channel", "Changes the submission channel").addOption(CHANNEL, "channel", "New submission channel", true),
                                new SubcommandData("mute-role", "Changes the mute role").addOption(ROLE, "role", "New mute role", true),
                                new SubcommandData("dm-qotw", "Changes the state of dm-qotw").addOption(BOOLEAN, "enabled", "State of dm-qotw", true),
                                new SubcommandData("lock", "Changes the state of the server lock").addOption(BOOLEAN, "locked", "State of the server lock", true)),

                new CommandData("welcome-image", "edits the welcome image config")
                        .addSubcommands(
                                new SubcommandData("list", "Sends the current welcome image config"),
                                new SubcommandData("image-width", "Changes the welcome image width").addOption(INTEGER, "width", "New welcome image width", true),
                                new SubcommandData("image-height", "Changes the welcome image height").addOption(INTEGER, "height", "New welcome image height", true),
                                new SubcommandData("overlay-url", "Changes the welcome image overlay url").addOption(STRING, "url", "New welcome image url", true),
                                new SubcommandData("background-url", "Changes the welcome image background url").addOption(STRING, "url", "New welcome image background url", true),
                                new SubcommandData("primary-color", "Changes the primary color (tag)").addOption(STRING, "color", "New primary color (e.g. ff0000)", true),
                                new SubcommandData("secondary-color", "Changes the secondary color (member count)").addOption(STRING, "color", "New secondary color (e.g. ff0000)", true),
                                new SubcommandData("avatar-x", "Changes the x-position of the avatar image").addOption(INTEGER, "x", "New x-position of the avatar image", true),
                                new SubcommandData("avatar-y", "Changes the y-position of the avatar image").addOption(INTEGER, "y", "New y-position of the avatar image", true),
                                new SubcommandData("avatar-width", "Changes the width of the avatar image").addOption(INTEGER, "width", "New width of the avatar image", true),
                                new SubcommandData("avatar-height", "Changes the height of the avatar image").addOption(INTEGER, "height", "New height of the avatar image", true)),

                new CommandData("customcommand", "lists, creates, edits or deletes custom slash commands")
                        .addSubcommands(
                                new SubcommandData("list", "Lists all custom slash commands"),
                                new SubcommandData("create", "Creates a custom slash command")
                                        .addOption(STRING, "name", "Name of the custom slash command", true)
                                        .addOption(STRING, "text", "Content of the custom slash command", true),
                                new SubcommandData("edit", "Edits a custom slash command")
                                        .addOption(STRING, "name", "Name of the custom slash command", true)
                                        .addOption(STRING, "text", "Content of the custom slash command", true),
                                new SubcommandData("delete", "Deletes a custom slash command")
                                        .addOption(STRING, "name", "Name of the custom slash command", true)),


                new CommandData("reactionrole", "lists, creates or deletes reaction roles")
                        .addSubcommands(
                                new SubcommandData("list", "Lists all reaction roles"),
                                new SubcommandData("create", "Creates a reaction role")
                                        .addOption(CHANNEL, "channel", "Channel, the reaction role should be created in", true)
                                        .addOption(STRING, "messageid", "Message, the reaction role should be created on", true)
                                        .addOption(STRING, "emote", "Emote, the reaction role should use", true)
                                        .addOption(ROLE, "role", "Role, the reaction role should add", true),
                                new SubcommandData("delete", "Deletes a reaction role")
                                        .addOption(STRING, "messageid", "Message, the reaction role is on", true)
                                        .addOption(STRING, "emote", "Emote, the reaction role is using", true)),


                new CommandData("leaderboard", "Generates the question of the week leaderboard")
                        .addOption(BOOLEAN, "old", "If true, uses the old design", false),

                new CommandData("question", "Displays the given amount of questions in a random order")
                        .addOption(INTEGER, "amount", "Aamount of questions", true),

                new CommandData("accept", "Accepts the given submissions")
                        .addOption(STRING, "message-id", "ID of the submission", true),

                new CommandData("clear", "clears the given submissions")
                        .addOption(STRING, "message-id", "ID of the submission", true),

                new CommandData("decline", "declines the given submissions")
                        .addOption(STRING, "message-id", "ID of the submission", true),

                new CommandData("respond", "Adds a response to the given submissions")
                        .addOption(STRING, "message-id", "ID of the submission", true)
                        .addOption(STRING, "text", "Text of the response", true));

        MongoDatabase database = mongoClient.getDatabase("other");
        MongoCollection<Document> collection = database.getCollection("customcommands");
        MongoCursor<Document> it = collection.find(eq("guild_id", guild.getId())).iterator();

        while (it.hasNext()) {

            JsonObject Root = JsonParser.parseString(it.next().toJson()).getAsJsonObject();
            String commandName = Root.get("commandname").getAsString();
            String value = Root.get("value").getAsString();

            if (value.length() > 100) value = value.substring(0, 97) + "...";
            commands.addCommands(new CommandData(commandName, value));
        }

        commands.queue();
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {

        String[] id = event.getComponentId().split(":");
        String authorId = id[0];
        String type = id[1];

        if (!authorId.equals(event.getUser().getId()) && !(type.startsWith("submission"))) return;

        event.deferEdit().queue();
        switch (type) {
                
            case "submission":

                try {

                    MongoDatabase database = mongoClient.getDatabase("other");
                    MongoCollection<Document> submission_messages = database.getCollection("submission_messages");

                    BasicDBObject submission_criteria = new BasicDBObject()
                            .append("guild_id", event.getGuild().getId())
                            .append("channel_id", event.getChannel().getId())
                            .append("message_id", event.getMessageId());

                    String JSON = submission_messages.find(submission_criteria).first().toJson();

                    JsonObject root = JsonParser.parseString(JSON).getAsJsonObject();
                    String userID = root.get("user_id").getAsString();

                    if (id[2].equals("approve")) {

                        Correct.correct(event, event.getGuild().getMemberById(userID));

                        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                                .setActionRows(ActionRow.of(
                                        Button.success(authorId + ":submission:approve", "Approved by " + event.getMember().getUser().getAsTag()).asDisabled())
                                )
                                .queue();

                    } else if (id[2].equals("decline")) {

                        event.getHook().editOriginalEmbeds(event.getMessage().getEmbeds().get(0))
                                .setActionRows(ActionRow.of(
                                        Button.danger(authorId + ":submission:decline", "Declined by " + event.getMember().getUser().getAsTag()).asDisabled())
                                )
                                .queue();
                    }

                    submission_messages.deleteOne(submission_criteria);

                } catch (Exception e) { e.printStackTrace(); }
                break;
        }
    }
}


