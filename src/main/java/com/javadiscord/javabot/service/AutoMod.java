package com.javadiscord.javabot.service;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.moderation.Ban;
import com.javadiscord.javabot.commands.moderation.Mute;
import com.javadiscord.javabot.commands.moderation.Warn;
import com.javadiscord.javabot.utils.Misc;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class checks all incoming messages for potential spam/advertising and warns or mutes the potential offender.
 */
public class AutoMod extends ListenerAdapter {

    private static final Pattern inviteURL = Pattern.compile("discord(?:(\\.(?:me|io|gg)|sites\\.com)/.{0,4}|app\\.com.{1,4}(?:invite|oauth2).{0,5}/)\\w+");

    @SneakyThrows
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        Member member = event.getMember();
        if (canBypassAutomod(member)) return;
        checkNewMessageAutomod(event.getMessage());
    }

    @SneakyThrows
    @Override
    public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent event) {
        Member member = event.getMember();
        if (canBypassAutomod(member)) return;
        checkContentAutomod(event.getMessage());
    }

    /**
     * Checks if a member can bypass the automod system.
     *
     * @param member the {@link Member} to check
     * @return <code>true</code> if the member is allowed to bypass automod, else <code>false</code>
     */
    private boolean canBypassAutomod(Member member) {
        return member == null
                || member.getUser().isBot()
                || member.hasPermission(Permission.MESSAGE_MANAGE);
    }

    /**
     * Runs all automod checks that should be run when a message is sent.
     *
     * @param message the {@link Message} that should be checked
     */
    private void checkNewMessageAutomod(@NotNull Message message) throws FileNotFoundException {
        // mention spam
        if (message.getMentionedMembers().size() >= 5) {
            warn(message, message.getMember(), "Automod: Mention Spam");
        }

        // spam
        message.getChannel().getHistory().retrievePast(10).queue(messages -> {
            int spamCount = (int) messages.stream().filter(msg -> !msg.equals(message))
                    // filter for spam
                    .filter(msg -> msg.getAuthor().equals(message.getAuthor()) && !msg.getAuthor().isBot())
                    .filter(msg -> (message.getTimeCreated().toEpochSecond() - msg.getTimeCreated().toEpochSecond()) < 6).count();
            if (spamCount > 5) {
                handleSpam(message, message.getMember());
            }
        });
        checkContentAutomod(message);

    }

    /**
     * Runs all automod checks only depend on the message content.
     *
     * @param message the {@link Message} that should be checked
     */
    private void checkContentAutomod(@NotNull Message message) throws FileNotFoundException {
        // Advertising
        Matcher matcher = inviteURL.matcher(cleanString(message.getContentRaw()));
        if (matcher.find()) {
            warn(message, message.getMember(), "Automod: Advertising");
        }
        final String messageRaw = message.getContentRaw();
        if (messageRaw.startsWith("http://") || messageRaw.startsWith("https://")) {
            // only do it for a links, so it won't iterate for each message
            Scanner fileReader = new Scanner(new File(String.valueOf(getClass().getResourceAsStream("spams.txt"))));
            while (fileReader.hasNext()) {
                String domin = fileReader.next();
                domin = domin.split("\\.")[0];
                if (messageRaw.contains(domin)) {
                    new Ban().ban(message.getMember(), "Scam");
                }
            }
            fileReader.close();
        }
    }
    /**
     * Bans a user for the given reason
     * @param event the event
     * @param member the user to be banned
     * @param reason the reason for the ban
     */
    private void ban (@NotNull GuildMessageReceivedEvent event, Member member, String reason) {
        MessageEmbed eb = new EmbedBuilder()
                .setColor(Bot.config.get(event.getGuild()).getSlashCommand().getWarningColor())
                .setAuthor(member.getUser().getAsTag() + " | Ban", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + event.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

        event.getChannel().sendMessageEmbeds(eb).queue();
        Misc.sendToLog(event.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(eb).queue());

        try {
            new Ban().ban(member,reason);
        } catch (Exception e) {
            event.getChannel().sendMessage(e.getMessage()).queue();
        }

        event.getMessage().delete().queue();
    }


    /**
     * Handles potential spam messages
     *
     * @param msg    the message
     * @param member the member to be potentially warned
     */
    private void handleSpam(@NotNull Message msg, Member member) {
        // java files -> not spam
        if (!msg.getAttachments().isEmpty()
                && "java".equals(msg.getAttachments().get(0).getFileExtension())) return;

        Role muteRole = Bot.config.get(msg.getGuild()).getModeration().getMuteRole();
        if (member.getRoles().contains(muteRole)) return;

        var eb = new EmbedBuilder()
                .setColor(Bot.config.get(msg.getGuild()).getSlashCommand().getErrorColor())
                .setAuthor(member.getUser().getAsTag() + " | Mute", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + msg.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + "Automod: Spam" + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

        msg.getChannel().sendMessageEmbeds(eb).queue();
        Misc.sendToLog(msg.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(eb).queue());

        try {
            new Mute().mute(msg.getMember(), msg.getGuild()).queue(
                    success -> {
                    },
                    e -> msg.getChannel().sendMessage(e.getMessage()).queue());
        } catch (Exception e) {
            msg.getChannel().sendMessage(e.getMessage()).queue();
        }
    }

    /**
     * warns the user using the reason
     *
     * @param message the message
     * @param member  the member to be warned
     * @param reason  the reason for the warning
     */
    private void warn(Message message, Member member, String reason) {
        int warnPoints = new Warn().getWarnCount(member);

        MessageEmbed eb = new EmbedBuilder()
                .setColor(Bot.config.get(message.getGuild()).getSlashCommand().getWarningColor())
                .setAuthor(member.getUser().getAsTag() + " | Warn (" + (warnPoints + 1) + "/3)", null, member.getUser().getEffectiveAvatarUrl())
                .addField("Name", "```" + member.getUser().getAsTag() + "```", true)
                .addField("Moderator", "```" + message.getGuild().getSelfMember().getUser().getAsTag() + "```", true)
                .addField("ID", "```" + member.getId() + "```", false)
                .addField("Reason", "```" + reason + "```", false)
                .setFooter("ID: " + member.getId())
                .setTimestamp(Instant.now())
                .build();

        message.getChannel().sendMessageEmbeds(eb).queue();
        Misc.sendToLog(message.getGuild(), eb);
        member.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(eb).queue());

        try {
            new Warn().warn(message.getMember(), message.getGuild(), reason);
        } catch (Exception e) {
            message.getChannel().sendMessage(e.getMessage()).queue();
        }

        message.delete().queue();
    }


    /**
     * returns the original String cleaned up of unused code points and spaces
     *
     * @param input the input String
     * @return the cleaned-up String
     */
    private String cleanString(String input) {
        input = input.replaceAll("\\p{C}", "");
        input = input.replace(" ", "");
        return input;
    }
}

