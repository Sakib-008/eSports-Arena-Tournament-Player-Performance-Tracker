package com.esports.arena.dao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.esports.arena.model.Player;
import com.esports.arena.service.RealtimeDatabaseService;
import com.fasterxml.jackson.core.type.TypeReference;

public class PlayerDAO {
    private static final String COLLECTION = "players";

    private final ExecutorService executor;

    public PlayerDAO() {
        this.executor = Executors.newFixedThreadPool(4);
    }

    public CompletableFuture<Integer> createPlayerAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> createPlayer(player), executor);
    }

    public int createPlayer(Player player) {
        try {
            long nextId = RealtimeDatabaseService.nextId("counters/players");
            int id = Math.toIntExact(nextId);
            player.setId(id);
            if (player.getJoinDate() == null) {
                player.setJoinDate(LocalDate.now());
            }
            RealtimeDatabaseService.write(path(id), player);
            return id;
        } catch (Exception e) {
            System.err.println("Error creating player: " + e.getMessage());
            return -1;
        }
    }

    public CompletableFuture<Player> getPlayerByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getPlayerById(id), executor);
    }

    public Player getPlayerById(int id) {
        try {
            return RealtimeDatabaseService.read(path(id), Player.class);
        } catch (Exception e) {
            System.err.println("Error getting player: " + e.getMessage());
            return null;
        }
    }

    public Player getPlayerByUsername(String username) {
        return getAllPlayers().stream()
                .filter(p -> p.getUsername() != null && p.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    public CompletableFuture<List<Player>> getAllPlayersAsync() {
        return CompletableFuture.supplyAsync(this::getAllPlayers, executor);
    }

    public List<Player> getAllPlayers() {
        try {
            Map<String, Player> map = RealtimeDatabaseService.read(COLLECTION,
                    new TypeReference<Map<String, Player>>() {});
            if (map == null) {
                return new ArrayList<>();
            }
            return map.values().stream()
                    .sorted(Comparator.comparing(Player::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting all players: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public CompletableFuture<List<Player>> getPlayersByTeamAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getPlayersByTeam(teamId), executor);
    }

    public List<Player> getPlayersByTeam(int teamId) {
        return getAllPlayers().stream()
                .filter(p -> p.getTeamId() != null && p.getTeamId() == teamId)
                .sorted(Comparator.comparing(Player::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    public List<Player> getAvailablePlayersByTeam(int teamId) {
        return getPlayersByTeam(teamId).stream()
                .filter(Player::isAvailable)
                .collect(Collectors.toList());
    }

    public CompletableFuture<Boolean> updatePlayerAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> updatePlayer(player), executor);
    }

    public boolean updatePlayer(Player player) {
        try {
            RealtimeDatabaseService.write(path(player.getId()), player);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating player: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePlayerStats(int playerId, int kills, int deaths, int assists, boolean won) {
        Player player = getPlayerById(playerId);
        if (player == null) {
            return false;
        }
        player.setTotalKills(player.getTotalKills() + kills);
        player.setTotalDeaths(player.getTotalDeaths() + deaths);
        player.setTotalAssists(player.getTotalAssists() + assists);
        player.setMatchesPlayed(player.getMatchesPlayed() + 1);
        player.setMatchesWon(player.getMatchesWon() + (won ? 1 : 0));
        return updatePlayer(player);
    }

    public CompletableFuture<Boolean> updateAvailabilityAsync(int playerId, boolean available, String reason) {
        return CompletableFuture.supplyAsync(() -> updateAvailability(playerId, available, reason), executor);
    }

    public boolean updateAvailability(int playerId, boolean available, String reason) {
        Player player = getPlayerById(playerId);
        if (player == null) {
            return false;
        }
        player.setAvailable(available);
        player.setAvailabilityReason(reason);
        return updatePlayer(player);
    }

    public boolean deletePlayer(int id) {
        try {
            RealtimeDatabaseService.delete(path(id));
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting player: " + e.getMessage());
            return false;
        }
    }

    private String path(int id) {
        return COLLECTION + "/" + id;
    }

    public void shutdown() {
        executor.shutdown();
    }
}