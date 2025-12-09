package com.esports.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class PlayerMatchStats {
    @JsonProperty("id")
    private int id;

    @JsonProperty("matchId")
    private int matchId;

    @JsonProperty("playerId")
    private int playerId;

    @JsonProperty("kills")
    private int kills;

    @JsonProperty("deaths")
    private int deaths;

    @JsonProperty("assists")
    private int assists;

    @JsonProperty("damageDealt")
    private int damageDealt;

    @JsonProperty("damageTaken")
    private int damageTaken;

    @JsonProperty("goldEarned")
    private int goldEarned;

    @JsonProperty("mvp")
    private boolean mvp; // Most Valuable Player flag

    public PlayerMatchStats() {}

    public PlayerMatchStats(int matchId, int playerId) {
        this.matchId = matchId;
        this.playerId = playerId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }

    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }

    public int getDamageDealt() { return damageDealt; }
    public void setDamageDealt(int damageDealt) { this.damageDealt = damageDealt; }

    public int getDamageTaken() { return damageTaken; }
    public void setDamageTaken(int damageTaken) { this.damageTaken = damageTaken; }

    public int getGoldEarned() { return goldEarned; }
    public void setGoldEarned(int goldEarned) { this.goldEarned = goldEarned; }

    public boolean isMvp() { return mvp; }
    public void setMvp(boolean mvp) { this.mvp = mvp; }

    public double getKdRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getKdaRatio() {
        return deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerMatchStats that = (PlayerMatchStats) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("K/D/A: %d/%d/%d", kills, deaths, assists);
    }
}