package net.javadiscord.javabot.systems.staff;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import net.javadiscord.javabot.command.SlashCommandHandler;

/**
 * Command that lets staff-members redeploy the bot.
 *
 * This only works if the way the bot is hosted is set up correctly, for example with a bash script that handles
 * compilation and a service set up with that bash script running before the bot gets started.
 *
 * I have explained how we do it in https://github.com/Java-Discord/JavaBot/pull/195
 */
@Slf4j
public class RedeployCommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		log.warn("Redeploying... Requested by: " + event.getUser().getAsTag());
		event.reply("Redeploying... this can take up to 2 Minutes.").queue();
		System.exit(0);
		return null;
	}
}