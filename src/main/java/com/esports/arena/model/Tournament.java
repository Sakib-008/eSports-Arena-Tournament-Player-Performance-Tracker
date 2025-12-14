package com.esports.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Tournament {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("game")
    private String game;

    @JsonProperty("format")
    private String format;

    @JsonProperty("startDate")
    private LocalDate startDate;

    @JsonProperty("endDate")
    private LocalDate endDate;

    @JsonProperty("prizePool")
    private double prizePool;

    @JsonProperty("status")
    private TournamentStatus status;

    @JsonProperty("maxTeams")
    private int maxTeams;

    @JsonProperty("registeredTeams")
    private List<Team> registeredTeams;

    @JsonProperty("matches")
    private List<Match> matches;

    public enum TournamentStatus {
        UPCOMING, REGISTRATION_OPEN, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public Tournament() {
        this.registeredTeams = new ArrayList<>();
        this.matches = new ArrayList<>();
        this.status = TournamentStatus.UPCOMING;
    }

    public Tournament(String name, String game, String format, LocalDate startDate,
                      LocalDate endDate, double prizePool, int maxTeams) {
        this();
        this.name = name;
        this.game = game;
        this.format = format;
        this.startDate = startDate;
        this.endDate = endDate;
        this.prizePool = prizePool;
        this.maxTeams = maxTeams;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public double getPrizePool() { return prizePool; }
    public void setPrizePool(double prizePool) { this.prizePool = prizePool; }

    public TournamentStatus getStatus() { return status; }
    public void setStatus(TournamentStatus status) { this.status = status; }

    public int getMaxTeams() { return maxTeams; }
    public void setMaxTeams(int maxTeams) { this.maxTeams = maxTeams; }

    public List<Team> getRegisteredTeams() { return registeredTeams; }
    public void setRegisteredTeams(List<Team> registeredTeams) { this.registeredTeams = registeredTeams; }

    public List<Match> getMatches() { return matches; }
    public void setMatches(List<Match> matches) { this.matches = matches; }

    public boolean canRegisterTeam() {
        return registeredTeams.size() < maxTeams &&
                status == TournamentStatus.REGISTRATION_OPEN;
    }

    public void registerTeam(Team team) {
        if (canRegisterTeam() && !registeredTeams.contains(team)) {
            registeredTeams.add(team);
        }
    }

    public void addMatch(Match match) {
        matches.add(match);
    }

    public int getRegisteredTeamCount() {
        return registeredTeams.size();
    }

    public boolean isFull() {
        return registeredTeams.size() >= maxTeams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tournament that = (Tournament) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + game + ")";
    }
}