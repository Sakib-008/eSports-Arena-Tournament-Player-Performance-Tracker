package com.esports.arena.model;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Player {
    @JsonProperty("id")
    private int id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("realName")
    private String realName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("teamId")
    private Integer teamId;

    @JsonProperty("role")
    private String role;

    @JsonProperty("joinDate")
    private LocalDate joinDate;

    @JsonProperty("totalKills")
    private int totalKills;

    @JsonProperty("totalDeaths")
    private int totalDeaths;

    @JsonProperty("totalAssists")
    private int totalAssists;

    @JsonProperty("matchesPlayed")
    private int matchesPlayed;

    @JsonProperty("matchesWon")
    private int matchesWon;

    @JsonProperty("available")
    private boolean available;

    @JsonProperty("availabilityReason")
    private String availabilityReason;

    public Player() {
        this.joinDate = LocalDate.now();
        this.available = true;
    }

    public Player(String username, String realName, String email, String role) {
        this();
        this.username = username;
        this.realName = realName;
        this.email = email;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getTeamId() { return teamId; }
    public void setTeamId(Integer teamId) { this.teamId = teamId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }

    public int getTotalKills() { return totalKills; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }

    public int getTotalDeaths() { return totalDeaths; }
    public void setTotalDeaths(int totalDeaths) { this.totalDeaths = totalDeaths; }

    public int getTotalAssists() { return totalAssists; }
    public void setTotalAssists(int totalAssists) { this.totalAssists = totalAssists; }

    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }

    public int getMatchesWon() { return matchesWon; }
    public void setMatchesWon(int matchesWon) { this.matchesWon = matchesWon; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getAvailabilityReason() { return availabilityReason; }
    public void setAvailabilityReason(String availabilityReason) { this.availabilityReason = availabilityReason; }

    public double getKdRatio() {
        return totalDeaths == 0 ? totalKills : (double) totalKills / totalDeaths;
    }

    public double getWinRate() {
        return matchesPlayed == 0 ? 0 : (double) matchesWon / matchesPlayed * 100;
    }

    public String getAvailabilityStatus() {
        return available ? "Available" : "Unavailable" + (availabilityReason != null && !availabilityReason.isEmpty() ?
                        " (" + availabilityReason + ")" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id == player.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return username + " (" + realName + ")";
    }
}