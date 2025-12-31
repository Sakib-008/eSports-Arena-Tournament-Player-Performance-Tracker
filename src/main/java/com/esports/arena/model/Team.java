package com.esports.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {
    @JsonProperty("id")
    private int id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("tag")
    private String tag;

    @JsonProperty("region")
    private String region;

    @JsonProperty("createdDate")
    private LocalDate createdDate;

    @JsonProperty("wins")
    private int wins;

    @JsonProperty("losses")
    private int losses;

    @JsonProperty("draws")
    private int draws;

    @JsonProperty("leaderId")
    private Integer leaderId;

    @JsonProperty("players")
    private List<Player> players;

    public Team() {
        this.createdDate = LocalDate.now();
        this.players = new ArrayList<>();
    }

    public Team(String name, String tag, String region) {
        this();
        this.name = name;
        this.tag = tag;
        this.region = region;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getDraws() { return draws; }
    public void setDraws(int draws) { this.draws = draws; }

    public Integer getLeaderId() { return leaderId; }
    public void setLeaderId(Integer leaderId) { this.leaderId = leaderId; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
            player.setTeamId(this.id);
            if (players.size() == 1) {
                this.leaderId = player.getId();
            }
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
        player.setTeamId(null);
        if (leaderId != null && leaderId.equals(player.getId())) {
            this.leaderId = null;
        }
    }

    public Player getLeader() {
        if (leaderId == null) return null;
        return players.stream()
                .filter(p -> p.getId() == leaderId)
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public List<Player> getAvailablePlayers() {
        return players.stream()
                .filter(Player::isAvailable)
                .toList();
    }

    @JsonIgnore
    public int getAvailablePlayerCount() {
        return (int) players.stream()
                .filter(Player::isAvailable)
                .count();
    }

    public int getTotalMatches() {
        return wins + losses + draws;
    }

    public double getWinRate() {
        int total = getTotalMatches();
        return total == 0 ? 0 : (double) wins / total * 100;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return id == team.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return tag + " - " + name;
    }
}