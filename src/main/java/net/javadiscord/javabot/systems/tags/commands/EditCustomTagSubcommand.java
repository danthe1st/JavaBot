package net.javadiscord.javabot.systems.tags.commands;

import com.dynxsty.dih4jda.interactions.ComponentIdBuilder;
import com.dynxsty.dih4jda.interactions.commands.AutoCompletable;
import com.dynxsty.dih4jda.interactions.components.ModalHandler;
import com.dynxsty.dih4jda.util.AutoCompleteUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.InteractionCallbackAction;
import net.javadiscord.javabot.Bot;
import net.javadiscord.javabot.data.h2db.DbHelper;
import net.javadiscord.javabot.systems.tags.CustomTagManager;
import net.javadiscord.javabot.systems.tags.dao.CustomTagRepository;
import net.javadiscord.javabot.systems.tags.model.CustomTag;
import net.javadiscord.javabot.util.Checks;
import net.javadiscord.javabot.util.Responses;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <h3>This class represents the /customcommands-admin command.</h3>
 */
public class EditCustomTagSubcommand extends CustomTagsSubcommand implements AutoCompletable, ModalHandler {
	/**
	 * The constructor of this class, which sets the corresponding {@link SubcommandData}.
	 */
	public EditCustomTagSubcommand() {
		setSubcommandData(new SubcommandData("edit", "Edits a single Custom Tag.")
				.addOption(OptionType.STRING, "name", "The tag's name.", true, true)
		);
	}

	@Override
	public InteractionCallbackAction<?> handleCustomTagsSubcommand(@NotNull SlashCommandInteractionEvent event) {
		OptionMapping nameMapping = event.getOption("name");
		if (nameMapping == null) {
			return Responses.error(event, "Missing required arguments.");
		}
		if (!Checks.checkGuild(event)) {
			return Responses.error(event, "This command may only be used inside a server.");
		}
		Set<CustomTag> tags = Bot.customCommandManager.getLoadedCommands(event.getGuild().getIdLong());
		Optional<CustomTag> tagOptional = tags.stream()
				.filter(t -> t.getName().equalsIgnoreCase(nameMapping.getAsString()))
				.findFirst();
		if (tagOptional.isPresent()) {
			return event.replyModal(buildEditTagModal(tagOptional.get()));
		}
		return Responses.error(event, String.format("Could not find tag with name: `%s`", nameMapping.getAsString()));
	}

	private @NotNull Modal buildEditTagModal(@NotNull CustomTag tag) {
		TextInput responseField = TextInput.create("tag-response", "Tag Response", TextInputStyle.PARAGRAPH)
				.setPlaceholder("""
						According to all known laws
						of aviation,
						      
						there is no way a bee
						should be able to fly...
						""")
				.setValue(tag.getResponse())
				.setMaxLength(2000)
				.setRequired(true)
				.build();
		TextInput replyField = TextInput.create("tag-reply", "Should the tag reply to your message?", TextInputStyle.SHORT)
				.setPlaceholder("true")
				.setValue(String.valueOf(tag.isReply()))
				.setMaxLength(5)
				.setRequired(true)
				.build();
		TextInput embedField = TextInput.create("tag-embed", "Should the tag be embedded?", TextInputStyle.SHORT)
				.setPlaceholder("true")
				.setValue(String.valueOf(tag.isReply()))
				.setMaxLength(5)
				.setRequired(true)
				.build();
		return Modal.create(ComponentIdBuilder.build("tag-edit", tag.getName()),
						String.format("Edit \"%s\"", tag.getName().length() > 90 ? tag.getName().substring(0, 87) + "..." : tag.getName()))
				.addActionRows(ActionRow.of(responseField), ActionRow.of(replyField), ActionRow.of(embedField))
				.build();
	}

	private @NotNull MessageEmbed buildEditTagEmbed(@NotNull Member createdBy, @NotNull CustomTag command) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getUser().getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle("Custom Tag Edited")
				.addField("Id", String.format("`%s`", command.getId()), true)
				.addField("Name", String.format("`/%s`", command.getName()), true)
				.addField("Created by", createdBy.getAsMention(), true)
				.addField("Response", String.format("```\n%s\n```", command.getResponse()), false)
				.addField("Reply?", String.format("`%s`", command.isReply()), true)
				.addField("Embed?", String.format("`%s`", command.isEmbed()), true)
				.setColor(Responses.Type.DEFAULT.getColor())
				.setTimestamp(Instant.now())
				.build();
	}

	@Override
	public void handleAutoComplete(@NotNull CommandAutoCompleteInteractionEvent event, @NotNull AutoCompleteQuery target) {
		event.replyChoices(AutoCompleteUtils.handleChoices(event, e -> CustomTagManager.replyTags(Objects.requireNonNull(e.getGuild())))).queue();
	}

	@Override
	public void handleModal(@NotNull ModalInteractionEvent event, @NotNull List<ModalMapping> values) {
		String[] id = ComponentIdBuilder.split(event.getModalId());
		ModalMapping responseMapping = event.getValue("customcmd-response");
		ModalMapping replyMapping = event.getValue("customcmd-reply");
		ModalMapping embedMapping = event.getValue("customcmd-embed");
		if (responseMapping == null || replyMapping == null || embedMapping == null) {
			Responses.error(event.getHook(), "Missing required arguments.").queue();
			return;
		}
		if (!event.isFromGuild() || event.getGuild() == null || event.getMember() != null) {
			Responses.error(event.getHook(), "This may only be used inside servers.").queue();
			return;
		}
		// build the CustomCommand object
		CustomTag update = new CustomTag();
		update.setGuildId(event.getGuild().getIdLong());
		update.setCreatedBy(event.getUser().getIdLong());
		update.setName(id[0]);
		update.setResponse(responseMapping.getAsString());
		update.setReply(Boolean.parseBoolean(replyMapping.getAsString()));
		update.setEmbed(Boolean.parseBoolean(embedMapping.getAsString()));

		event.deferReply(true).queue();
		DbHelper.doDaoAction(CustomTagRepository::new, dao -> {
			Optional<CustomTag> tagOptional = dao.findByName(event.getGuild().getIdLong(), update.getName());
			if (tagOptional.isEmpty()) {
				Responses.error(event.getHook(), String.format("Could not find Custom Tag with name `/%s`.", update.getName())).queue();
				return;
			}
			if (Bot.customCommandManager.editCommand(event.getGuild().getIdLong(), tagOptional.get(), update)) {
				event.getHook().sendMessageEmbeds(buildEditTagEmbed(event.getMember(), update)).queue();
				return;
			}
			Responses.error(event.getHook(), "Could not edit Custom Command. Please try again.").queue();
		});
	}
}
