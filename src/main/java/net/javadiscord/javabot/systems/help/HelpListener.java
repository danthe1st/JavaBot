package net.javadiscord.javabot.systems.help;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.javadiscord.javabot.annotations.AutoDetectableComponentHandler;
import net.javadiscord.javabot.data.config.BotConfig;
import net.javadiscord.javabot.data.config.guild.HelpConfig;
import net.javadiscord.javabot.data.h2db.DbActions;
import net.javadiscord.javabot.systems.help.dao.HelpAccountRepository;
import net.javadiscord.javabot.systems.help.dao.HelpTransactionRepository;
import net.javadiscord.javabot.util.ExceptionLogger;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import xyz.dynxsty.dih4jda.interactions.components.ButtonHandler;
import xyz.dynxsty.dih4jda.util.ComponentIdBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listens for all events related to the forum help channel system.
 */
@RequiredArgsConstructor
@AutoDetectableComponentHandler({HelpManager.HELP_THANKS_IDENTIFIER, HelpManager.HELP_CLOSE_IDENTIFIER, HelpManager.HELP_GUIDELINES_IDENTIFIER})
public class HelpListener extends ListenerAdapter implements ButtonHandler {

	/**
	 * A static Map that holds all messages that was sent in a specific reserved forum channel.
	 */
	protected static final Map<Long, List<Message>> HELP_POST_MESSAGES = new HashMap<>();
	private static final Set<Long> newThreadChannels;

	static {
		newThreadChannels = new HashSet<>();
	}

	private final BotConfig botConfig;
	private final HelpAccountRepository helpAccountRepository;
	private final HelpTransactionRepository helpTransactionRepository;
	private final DbActions dbActions;

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		// listen for the posts initial message. Why this has to be done is further described in the JDA Discord:
		// https://canary.discord.com/channels/125227483518861312/1053705384466059354/1053705384466059354
		if (newThreadChannels.contains(event.getChannel().getIdLong())) {
			HelpConfig config = botConfig.get(event.getGuild()).getHelpConfig();
			ThreadChannel post = event.getChannel().asThreadChannel();
			// send post buttons
			post.sendMessageComponents(ActionRow.of(
					Button.primary(ComponentIdBuilder.build(HelpManager.HELP_CLOSE_IDENTIFIER, post.getIdLong()), "Close Post"),
					Button.secondary(ComponentIdBuilder.build(HelpManager.HELP_GUIDELINES_IDENTIFIER), "View Help Guidelines")
			)).queue(success -> post.sendMessageFormat(config.getReservedChannelMessageTemplate(), UserSnowflake.fromId(post.getOwnerId()).getAsMention(), config.getInactivityTimeoutMinutes()).queue());
			newThreadChannels.remove(event.getChannel().getIdLong());
			return;
		}
		if (event.getMessage().getAuthor().isSystem() || event.getMessage().getAuthor().isBot()) {
			return;
		}
		// check for forum post
		if (isInvalidForumPost(event.getChannel())) {
			return;
		}
		ThreadChannel post = event.getChannel().asThreadChannel();
		if (isInvalidHelpForumChannel(post.getParentChannel().asForumChannel())) {
			return;
		}
		// cache messages
		List<Message> messages = new ArrayList<>();
		messages.add(event.getMessage());
		if (HELP_POST_MESSAGES.containsKey(post.getIdLong())) {
			messages.addAll(HELP_POST_MESSAGES.get(post.getIdLong()));
		}
		HELP_POST_MESSAGES.put(post.getIdLong(), messages);
	}

	@Override
	public void onChannelCreate(@NotNull ChannelCreateEvent event) {
		if (isInvalidForumPost(event.getChannel())) {
			return;
		}
		HelpConfig config = botConfig.get(event.getGuild()).getHelpConfig();
		ThreadChannel post = event.getChannel().asThreadChannel();
		if (isInvalidHelpForumChannel(post.getParentChannel().asForumChannel())) {
			return;
		}
		// add thread id to a temporary cache to avoid potential missing author message
		// more info on why this has to be done: https://canary.discord.com/channels/125227483518861312/1053705384466059354/1053705384466059354 (JDA Discord)
		newThreadChannels.add(event.getChannel().getIdLong());
	}

	@Override
	public void handleButton(@NotNull ButtonInteractionEvent event, @NotNull Button button) {
		String[] id = ComponentIdBuilder.split(event.getComponentId());
		if (isInvalidForumPost(event.getChannel()) ||
				isInvalidHelpForumChannel(event.getChannel().asThreadChannel().getParentChannel().asForumChannel())
		) {
			Responses.error(event, "This button may only be used inside help forum threads.").queue();
			return;
		}
		ThreadChannel post = event.getChannel().asThreadChannel();
		HelpManager manager = new HelpManager(post, dbActions, botConfig, helpAccountRepository, helpTransactionRepository);
		switch (id[0]) {
			case HelpManager.HELP_THANKS_IDENTIFIER -> handleHelpThanksInteraction(event, manager, id);
			case HelpManager.HELP_GUIDELINES_IDENTIFIER ->
					handleReplyGuidelines(event, post.getParentChannel().asForumChannel());
			case HelpManager.HELP_CLOSE_IDENTIFIER -> handlePostClose(event, manager);
			default -> event.reply("Unknown interaction.").queue();
		}
	}

	private boolean isInvalidForumPost(@NotNull Channel channel) {
		return channel.getType() != ChannelType.GUILD_PUBLIC_THREAD ||
				((ThreadChannel) channel).getParentChannel().getType() != ChannelType.FORUM;
	}

	private boolean isInvalidHelpForumChannel(@NotNull ForumChannel forum) {
		HelpConfig config = botConfig.get(forum.getGuild()).getHelpConfig();
		return config.getHelpForumChannelId() != forum.getIdLong();
	}

	private void handleHelpThanksInteraction(@NotNull ButtonInteractionEvent event, @NotNull HelpManager manager, String @NotNull [] id) {
		ThreadChannel post = manager.getPostThread();
		HelpConfig config = botConfig.get(event.getGuild()).getHelpConfig();
		if (event.getUser().getIdLong() != post.getOwnerIdLong()) {
			Responses.warning(event, "Sorry, only the person who reserved this channel can thank users.").queue();
			return;
		}
		switch (id[2]) {
			case "done" -> handleThanksCloseButton(event, manager, post, config);
			case "cancel" -> event.getMessage().delete().queue();
			default -> {
				List<Button> thankButtons = event.getMessage().getButtons().stream()
						.filter(b -> b.getId() != null &&
								!ComponentIdBuilder.split(b.getId())[2].equals("done") &&
								!ComponentIdBuilder.split(b.getId())[2].equals("cancel")
						).toList();
				if (thankButtons.stream().filter(Button::isDisabled).count() == thankButtons.size() - 1) {
					handleThanksCloseButton(event, manager, post, config);
				} else {
					event.editButton(event.getButton().asDisabled()).queue();
				}
			}
		}
	}

	private void handleThanksCloseButton(@NotNull ButtonInteractionEvent event, HelpManager manager, ThreadChannel post, HelpConfig config) {
		List<Button> buttons = event.getMessage().getButtons();
		// immediately delete the message
		event.getMessage().delete().queue(s -> {
			// close post
			manager.close(event, false, null);
			// add experience
			try {
				HelpExperienceService service = new HelpExperienceService(botConfig, helpAccountRepository, helpTransactionRepository);
				Map<Long, Double> experience = HelpManager.calculateExperience(HELP_POST_MESSAGES.get(post.getIdLong()), post.getOwnerIdLong(), config);
				for (Map.Entry<Long, Double> entry : experience.entrySet()) {
					service.performTransaction(entry.getKey(), entry.getValue(), config.getGuild());
				}
			} catch (DataAccessException e) {
				ExceptionLogger.capture(e, getClass().getName());
			}
			// thank all helpers
			buttons.stream().filter(ActionComponent::isDisabled)
					.filter(b -> b.getId() != null)
					.forEach(b -> manager.thankHelper(event, post, Long.parseLong(ComponentIdBuilder.split(b.getId())[2])));
		});
	}

	private void handleReplyGuidelines(@NotNull IReplyCallback callback, @NotNull ForumChannel channel) {
		callback.replyEmbeds(new EmbedBuilder()
						.setTitle("Help Guidelines")
						.setDescription(channel.getTopic())
						.build()
				).setEphemeral(true)
				.queue();
	}

	private void handlePostClose(ButtonInteractionEvent event, @NotNull HelpManager manager) {
		if (manager.isForumEligibleToBeUnreserved(event)) {
			manager.close(event, event.getUser().getIdLong() == manager.getPostThread().getOwnerIdLong(), null);
		} else {
			Responses.warning(event, "Could not close this post", "You're not allowed to close this post.").queue();
		}
	}
}