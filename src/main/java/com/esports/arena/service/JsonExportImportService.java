package com.esports.arena.service;

import com.esports.arena.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JsonExportImportService {
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    public JsonExportImportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.executor = Executors.newFixedThreadPool(3);
    }

    // Export Data Classes
    public static class ExportData {
        private List<Player> players;
        private List<Team> teams;
        private List<Tournament> tournaments;
        private List<Match> matches;
        private List<PlayerMatchStats> stats;

        public ExportData() {}

        public List<Player> getPlayers() { return players; }
        public void setPlayers(List<Player> players) { this.players = players; }

        public List<Team> getTeams() { return teams; }
        public void setTeams(List<Team> teams) { this.teams = teams; }

        public List<Tournament> getTournaments() { return tournaments; }
        public void setTournaments(List<Tournament> tournaments) { this.tournaments = tournaments; }

        public List<Match> getMatches() { return matches; }
        public void setMatches(List<Match> matches) { this.matches = matches; }

        public List<PlayerMatchStats> getStats() { return stats; }
        public void setStats(List<PlayerMatchStats> stats) { this.stats = stats; }
    }

    // Export all data to JSON file (async)
    public CompletableFuture<Boolean> exportAllDataAsync(ExportData data, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                objectMapper.writeValue(new File(filePath), data);
                System.out.println("Data exported successfully to: " + filePath);
                return true;
            } catch (IOException e) {
                System.err.println("Error exporting data: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }

    // Export single player to JSON
    public CompletableFuture<Boolean> exportPlayerAsync(Player player, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                objectMapper.writeValue(new File(filePath), player);
                return true;
            } catch (IOException e) {
                System.err.println("Error exporting player: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    // Export single team to JSON
    public CompletableFuture<Boolean> exportTeamAsync(Team team, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                objectMapper.writeValue(new File(filePath), team);
                return true;
            } catch (IOException e) {
                System.err.println("Error exporting team: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    // Export tournament to JSON
    public CompletableFuture<Boolean> exportTournamentAsync(Tournament tournament, String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                objectMapper.writeValue(new File(filePath), tournament);
                return true;
            } catch (IOException e) {
                System.err.println("Error exporting tournament: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    // Import all data from JSON file (async)
    public CompletableFuture<ExportData> importAllDataAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ExportData data = objectMapper.readValue(new File(filePath), ExportData.class);
                System.out.println("Data imported successfully from: " + filePath);
                return data;
            } catch (IOException e) {
                System.err.println("Error importing data: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, executor);
    }

    // Import single player from JSON
    public CompletableFuture<Player> importPlayerAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return objectMapper.readValue(new File(filePath), Player.class);
            } catch (IOException e) {
                System.err.println("Error importing player: " + e.getMessage());
                return null;
            }
        }, executor);
    }

    // Import single team from JSON
    public CompletableFuture<Team> importTeamAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return objectMapper.readValue(new File(filePath), Team.class);
            } catch (IOException e) {
                System.err.println("Error importing team: " + e.getMessage());
                return null;
            }
        }, executor);
    }

    // Import tournament from JSON
    public CompletableFuture<Tournament> importTournamentAsync(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return objectMapper.readValue(new File(filePath), Tournament.class);
            } catch (IOException e) {
                System.err.println("Error importing tournament: " + e.getMessage());
                return null;
            }
        }, executor);
    }

    // Export players list to JSON
    public boolean exportPlayers(List<Player> players, String filePath) {
        try {
            objectMapper.writeValue(new File(filePath), players);
            return true;
        } catch (IOException e) {
            System.err.println("Error exporting players: " + e.getMessage());
            return false;
        }
    }

    // Import players list from JSON
    public List<Player> importPlayers(String filePath) {
        try {
            return objectMapper.readValue(
                    new File(filePath),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Player.class)
            );
        } catch (IOException e) {
            System.err.println("Error importing players: " + e.getMessage());
            return null;
        }
    }

    // Export teams list to JSON
    public boolean exportTeams(List<Team> teams, String filePath) {
        try {
            objectMapper.writeValue(new File(filePath), teams);
            return true;
        } catch (IOException e) {
            System.err.println("Error exporting teams: " + e.getMessage());
            return false;
        }
    }

    // Import teams list from JSON
    public List<Team> importTeams(String filePath) {
        try {
            return objectMapper.readValue(
                    new File(filePath),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Team.class)
            );
        } catch (IOException e) {
            System.err.println("Error importing teams: " + e.getMessage());
            return null;
        }
    }

    // Convert object to JSON string
    public String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (IOException e) {
            System.err.println("Error converting to JSON: " + e.getMessage());
            return null;
        }
    }

    // Convert JSON string to object
    public <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            return null;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}