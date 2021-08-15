package com.javadiscord.javabot.jam.subcommands.admin;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.Responses;
import com.javadiscord.javabot.commands.SlashCommandHandler;
import com.javadiscord.javabot.jam.dao.JamRepository;
import com.javadiscord.javabot.jam.model.Jam;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class PlanNewJamSubcommand implements SlashCommandHandler {
	@Override
	public ReplyAction handle(SlashCommandEvent event) {
		event.deferReply().queue();
		OptionMapping startOption = event.getOption("start-date");
		if (startOption == null) {
			return Responses.warning(event, "Missing start date.");
		}
		LocalDate startsAt;
		try {
			startsAt = LocalDate.parse(startOption.getAsString(), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		} catch (DateTimeParseException e) {
			return Responses.warning(event, "Invalid start date. Must be formatted as `dd-MM-yyyy`. For example, June 5th, 2017 is formatted as `05-06-2017`.");
		}
		if (startsAt.isBefore(LocalDate.now())) {
			return Responses.warning(event, "Invalid start date. The Jam cannot start in the past.");
		}
		long guildId = Objects.requireNonNull(event.getGuild()).getIdLong();
		String name = null;
		OptionMapping nameOption = event.getOption("name");
		if (nameOption != null) {
			name = nameOption.getAsString();
		}

		try {
			Connection con = Bot.dataSource.getConnection();
			JamRepository jamRepository = new JamRepository(con);

			Jam activeJam = jamRepository.getActiveJam(guildId);
			if (activeJam != null) {
				return Responses.warning(event, "There is already an active Jam (id = `" + activeJam.getId() + "`). Complete that Jam before planning a new one.");
			}

			Jam jam = new Jam();
			jam.setGuildId(guildId);
			jam.setName(name);
			jam.setStartedBy(event.getUser().getIdLong());
			jam.setStartsAt(startsAt);
			jam.setCompleted(false);
			jamRepository.saveNewJam(jam);
			return Responses.success(event, "Jam Created", "Jam has been created! *Jam ID = `" + jam.getId() + "`*. Use `/jam info` for more info.");
		} catch (Exception e) {
			return Responses.error(event, "Error occurred while creating the Jam: " + e.getMessage());
		}
	}
}
