package net.javadiscord.javabot.systems.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.ISlashCommand;
import net.javadiscord.javabot.data.config.guild.SlashCommandConfig;

import java.time.Instant;

/**
 * Command that allows users to convert discord ids into a human-readable timestamp.
 */
public class IdCalculatorCommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var idOption = event.getOption("id");
		if (idOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		long id = idOption.getAsLong();
		var config = Bot.config.get(event.getGuild()).getSlashCommand();
		return event.replyEmbeds(buildIdCalcEmbed(event.getUser(), id, IdCalculatorCommand.getUnixTimestampFromSnowflakeId(id), config));
	}

	public static long getUnixTimestampFromSnowflakeId(long id) {
		return id / 4194304 + 1420070400000L;
	}

	private MessageEmbed buildIdCalcEmbed(User author, long id, long unixTimestamp, SlashCommandConfig config) {
		Instant instant = Instant.ofEpochMilli(unixTimestamp / 1000);
		return new EmbedBuilder()
				.setAuthor(author.getAsTag(), null, author.getEffectiveAvatarUrl())
				.setTitle("ID-Calculator")
				.setColor(config.getDefaultColor())
				.addField("Input", String.format("`%s`", id), false)
				.addField("Unix-Timestamp", String.format("`%s`", unixTimestamp), true)
				.addField("Unix-Timestamp (+ milliseconds)", String.format("`%s`", unixTimestamp / 1000), true)
				.addField("Date", String.format("<t:%s:F>", instant.getEpochSecond()), false)
				.build();
	}
}