package com.javadiscord.javabot.commands.jam.subcommands.admin;

import com.javadiscord.javabot.Bot;
import com.javadiscord.javabot.commands.jam.dao.JamThemeRepository;
import com.javadiscord.javabot.commands.jam.model.Jam;
import com.javadiscord.javabot.commands.jam.model.JamTheme;
import com.javadiscord.javabot.commands.jam.subcommands.ActiveJamSubcommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class ListThemesSubcommand extends ActiveJamSubcommand {
	public ListThemesSubcommand() {
		super(true);
	}

	@Override
	protected void handleJamCommand(SlashCommandEvent event, Jam activeJam, Connection con) throws Exception {
		List<JamTheme> themes = new JamThemeRepository(con).getThemes(activeJam);
		EmbedBuilder embedBuilder = new EmbedBuilder()
				.setTitle("Themes for Jam " + activeJam.getId())
				.setColor(Color.decode(Bot.getProperty("jamEmbedColor")));
		for (JamTheme theme : themes) {
			embedBuilder.addField(theme.getName(), theme.getDescription(), false);
		}
		event.getHook().sendMessageEmbeds(embedBuilder.build()).queue();
	}
}