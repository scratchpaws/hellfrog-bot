package hellfrog.commands.scenes;

import hellfrog.common.CommonUtils;
import hellfrog.common.WorldometersParser;
import hellfrog.common.worldometers.CovidStatistic;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

public class CovidStatisticScene extends OneShotScenario {

    private static final String PREFIX = "covid";
    private static final String DESCRIPTION = "Displays COVID-19 incidence statistics by country. Two letters of the country required.";

    public CovidStatisticScene() {
        super(PREFIX, DESCRIPTION);
        super.enableStrictByChannels();
    }

    @Override
    protected void onPrivate(@NotNull MessageCreateEvent event, @NotNull PrivateChannel privateChannel, @NotNull User user, boolean isBotOwner) {
        execute(event);
    }

    @Override
    protected void onServer(@NotNull MessageCreateEvent event, @NotNull Server server, @NotNull ServerTextChannel serverTextChannel, @NotNull User user, boolean isBotOwner) {
        execute(event);
    }

    private void execute(@NotNull final MessageCreateEvent event) {

        final String avlCode = super.getReadableMessageContentWithoutPrefix(event);
        if (CommonUtils.isTrStringEmpty(avlCode)) {
            showErrorMessage("Required two letters of country name", event);
            return;
        }

        final String code = avlCode.strip().toUpperCase();
        if (code.length() != 2) {
            showErrorMessage("Required two letters of country name", event);
            return;
        }

        final Map<String, CovidStatistic> statisticMap = WorldometersParser.getActualCovidStatistic();
        if (statisticMap.isEmpty()) {
            showErrorMessage("Unable to load statistic from https://www.worldometers.info", event);
            return;
        }

        CovidStatistic covidStatistic = statisticMap.get(code);
        if (covidStatistic == null) {
            showErrorMessage("Unable to find statistic for " + code, event);
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setTimestampToNow()
                .setFooter("Statistics provided by " + WorldometersParser.getCovidPage())
                .setDescription("Coronavirus statistic for " + covidStatistic.getCountry());

        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();

        symbols.setGroupingSeparator(' ');
        formatter.setDecimalFormatSymbols(symbols);


        if (covidStatistic.getTotalCases() > 0L) {
            embed.addField("Total cases:", formatter.format(covidStatistic.getTotalCases()), true);
        }
        if (covidStatistic.getNewCases() > 0L) {
            embed.addField("New cases:", "+" + formatter.format(covidStatistic.getNewCases()), true);
        }
        if (covidStatistic.getTotalDeaths() > 0L) {
            embed.addField("Total deaths:", formatter.format(covidStatistic.getTotalDeaths()), true);
        }
        if (covidStatistic.getNewDeaths() > 0L) {
            embed.addField("New deaths:", "+" + formatter.format(covidStatistic.getNewDeaths()), true);
        }
        if (covidStatistic.getTotalRecovered() > 0L) {
            embed.addField("Total recovered:", formatter.format(covidStatistic.getTotalRecovered()), true);
        }
        if (covidStatistic.getActiveCases() > 0L) {
            embed.addField("Active cases:", formatter.format(covidStatistic.getActiveCases()), true);
        }
        if (covidStatistic.getSeriousCritical() > 0L) {
            embed.addField("Serious, Critical:", formatter.format(covidStatistic.getSeriousCritical()), true);
        }
        if (covidStatistic.getTotalCases1MPop() > 0L) {
            embed.addField("Total cases/1M pop:", formatter.format(covidStatistic.getTotalCases1MPop()), true);
        }
        if (covidStatistic.getDeaths1MPop() > 0L) {
            embed.addField("Deaths/1M pop:", formatter.format(covidStatistic.getDeaths1MPop()), true);
        }
        if (covidStatistic.getTotalTests() > 0L) {
            embed.addField("Total tests:", formatter.format(covidStatistic.getTotalTests()), true);
        }
        if (covidStatistic.getTests1MPop() > 0L) {
            embed.addField("Tests/1M pop:", formatter.format(covidStatistic.getTests1MPop()), true);
        }
        if (covidStatistic.getPopulation() > 0L) {
            embed.addField("Population:", formatter.format(covidStatistic.getPopulation()), true);
        }
        new MessageBuilder().setEmbed(embed)
                .send(event.getChannel());
    }
}
