package net.javadiscord.javabot.systems.help.commands.subcommands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.command.ResponseException;
import net.javadiscord.javabot.command.Responses;
import net.javadiscord.javabot.command.interfaces.SlashCommand;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.HelpExperienceService;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.systems.help.model.HelpAccount;
import net.javadiscord.javabot.systems.help.model.HelpTransaction;
import net.javadiscord.javabot.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Handles commands to show information about how a user has been thanked for
 * their help.
 */
public class HelpAccountSubcommand implements SlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		User user = event.getOption("user", event::getUser, OptionMapping::getAsUser);
		long totalThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ?",
				s -> s.setLong(1, user.getIdLong())
		);
		long weekThanks = DbActions.count(
				"SELECT COUNT(id) FROM help_channel_thanks WHERE helper_id = ? AND thanked_at > DATEADD('week', -1, CURRENT_TIMESTAMP(0))",
				s -> s.setLong(1, user.getIdLong())
		);
		try {
			HelpAccount account = new HelpExperienceService(Bot.dataSource).getOrCreateAccount(user.getIdLong());
			return event.replyEmbeds(this.buildHelpAccountEmbed(account, user, totalThanks, weekThanks));
		} catch (SQLException e) {
			return Responses.error(event, e.getMessage());
		}
	}

	// TODO: add roles
	private MessageEmbed buildHelpAccountEmbed(HelpAccount account, User user, long totalThanks, long weekThanks) {
		// placeholder
		double maxXp = 1000;
		return new EmbedBuilder()
				.setAuthor(user.getAsTag(), null, user.getEffectiveAvatarUrl())
				.setTitle("Help Account")
				.setThumbnail(user.getEffectiveAvatarUrl())
				.setDescription("Here are some statistics about how you've helped others here.")
				.addField("Experience (BETA)", String.format("%.2f XP / %.2f XP (%.2f%%)\n%s\n\n**Recent Transactions**\n```%s```",
						account.getExperience(),
						maxXp,
						(account.getExperience() / maxXp) * 100,
						StringUtils.buildProgressBar(account.getExperience(), maxXp, "\u2B1B", "\uD83D\uDFE5", 15),
						this.formatTransactionHistory(user.getIdLong())), false)
				.addField("Total Times Thanked", String.format("**%s**", totalThanks), true)
				.addField("Times Thanked This Week", String.format("**%s**", weekThanks), true)
				.build();
	}

	private String formatTransactionHistory(long userId) {
		StringBuilder sb = new StringBuilder();
		try (Connection con = Bot.dataSource.getConnection()) {
			HelpTransactionRepository repo = new HelpTransactionRepository(con);
			for (HelpTransaction t : repo.getTransactions(userId, 3)) {
				sb.append(t.format()).append("\n\n");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sb.toString().length() <= 0 ? "None" : sb.toString();
	}
}
