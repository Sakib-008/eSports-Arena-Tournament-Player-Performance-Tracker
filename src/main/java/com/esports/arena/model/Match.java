package com.esports.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Match {
    @JsonProperty("id")
    private int id;

    @JsonProperty("tournamentId")
    private int tournamentId;

    @JsonProperty("team1Id")
    private int team1Id;

    @JsonProperty("team2Id")
    private int team2Id;

    @JsonProperty("team1Score")
    private int team1Score;

    @JsonProperty("team2Score")
    private int team2Score;

    @JsonProperty("scheduledTime")
    private LocalDateTime scheduledTime;

    @JsonProperty("actualStartTime")
    private LocalDateTime actualStartTime;

    @JsonProperty("actualEndTime")
    private LocalDateTime actualEndTime;

    @JsonProperty("status")
    private MatchStatus status;

    @JsonProperty("round")
    private String round;

    @JsonProperty("winnerId")
    private Integer winnerId;

    @JsonProperty("playerStats")
    private List<PlayerMatchStats> playerStats;

    public enum MatchStatus {
        SCHEDULED, LIVE, COMPLETED, POSTPONED, CANCELLED
    }

    public Match() {
        this.playerStats = new ArrayList<>();
        this.status = MatchStatus.SCHEDULED;
    }

    public Match(int tournamentId, int team1Id, int team2Id,
                 LocalDateTime scheduledTime, String round) {
        this();
        this.tournamentId = tournamentId;
        this.team1Id = team1Id;
        this.team2Id = team2Id;
        this.scheduledTime = scheduledTime;
        this.round = round;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTournamentId() { return tournamentId; }
    public void setTournamentId(int tournamentId) { this.tournamentId = tournamentId; }

    public int getTeam1Id() { return team1Id; }
    public void setTeam1Id(int team1Id) { this.team1Id = team1Id; }

    public int getTeam2Id() { return team2Id; }
    public void setTeam2Id(int team2Id) { this.team2Id = team2Id; }

    public int getTeam1Score() { return team1Score; }
    public void setTeam1Score(int team1Score) { this.team1Score = team1Score; }

    public int getTeam2Score() { return team2Score; }
    public void setTeam2Score(int team2Score) { this.team2Score = team2Score; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }

    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }

    public Integer getWinnerId() { return winnerId; }
    public void setWinnerId(Integer winnerId) { this.winnerId = winnerId; }

    public List<PlayerMatchStats> getPlayerStats() { return playerStats; }
    public void setPlayerStats(List<PlayerMatchStats> playerStats) { this.playerStats = playerStats; }

    public void startMatch() {
        this.status = MatchStatus.LIVE;
        this.actualStartTime = LocalDateTime.now();
    }

    public void endMatch(int winnerId) {
        this.status = MatchStatus.COMPLETED;
        this.actualEndTime = LocalDateTime.now();
        this.winnerId = winnerId;
    }

    public void addPlayerStats(PlayerMatchStats stats) {
        playerStats.add(stats);
    }

    public boolean isCompleted() {
        return status == MatchStatus.COMPLETED;
    }

    public boolean isLive() {
        return status == MatchStatus.LIVE;
    }

    public String getScoreDisplay() {
        return team1Score + " - " + team2Score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return id == match.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Match #" + id + " - " + round;
    }
}