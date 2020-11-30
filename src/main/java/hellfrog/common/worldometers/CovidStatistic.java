package hellfrog.common.worldometers;

public class CovidStatistic {

    private final String country;
    private final long totalCases;
    private final long newCases;
    private final long totalDeaths;
    private final long newDeaths;
    private final long totalRecovered;
    private final long activeCases;
    private final long seriousCritical;
    private final long totalCases1MPop;
    private final long deaths1MPop;
    private final long totalTests;
    private final long tests1MPop;
    private final long population;

    public CovidStatistic(String country,
                          long totalCases,
                          long newCases,
                          long totalDeaths,
                          long newDeaths,
                          long totalRecovered,
                          long activeCases,
                          long seriousCritical,
                          long totalCases1MPop,
                          long deaths1MPop,
                          long totalTests,
                          long tests1MPop,
                          long population) {

        this.country = country;
        this.totalCases = totalCases;
        this.newCases = newCases;
        this.totalDeaths = totalDeaths;
        this.newDeaths = newDeaths;
        this.totalRecovered = totalRecovered;
        this.activeCases = activeCases;
        this.seriousCritical = seriousCritical;
        this.totalCases1MPop = totalCases1MPop;
        this.deaths1MPop = deaths1MPop;
        this.totalTests = totalTests;
        this.tests1MPop = tests1MPop;
        this.population = population;
    }

    public String getCountry() {
        return country;
    }

    public long getTotalCases() {
        return totalCases;
    }

    public long getNewCases() {
        return newCases;
    }

    public long getTotalDeaths() {
        return totalDeaths;
    }

    public long getNewDeaths() {
        return newDeaths;
    }

    public long getTotalRecovered() {
        return totalRecovered;
    }

    public long getActiveCases() {
        return activeCases;
    }

    public long getSeriousCritical() {
        return seriousCritical;
    }

    public long getTotalCases1MPop() {
        return totalCases1MPop;
    }

    public long getDeaths1MPop() {
        return deaths1MPop;
    }

    public long getTotalTests() {
        return totalTests;
    }

    public long getTests1MPop() {
        return tests1MPop;
    }

    public long getPopulation() {
        return population;
    }

    @Override
    public String toString() {
        return "CovidStatistic{" +
                "country='" + country + '\'' +
                ", totalCases=" + totalCases +
                ", newCases=" + newCases +
                ", totalDeaths=" + totalDeaths +
                ", newDeaths=" + newDeaths +
                ", totalRecovered=" + totalRecovered +
                ", activeCases=" + activeCases +
                ", seriousCritical=" + seriousCritical +
                ", totalCases1MPop=" + totalCases1MPop +
                ", deaths1MPop=" + deaths1MPop +
                ", totalTests=" + totalTests +
                ", tests1MPop=" + tests1MPop +
                ", population=" + population +
                '}';
    }
}
