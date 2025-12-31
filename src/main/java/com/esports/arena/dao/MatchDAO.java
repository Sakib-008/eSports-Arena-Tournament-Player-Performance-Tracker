package com.esports.arena.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.esports.arena.model.Match;
import com.esports.arena.model.PlayerMatchStats;
import com.esports.arena.service.RealtimeDatabaseService;

public class MatchDAO {
    private static final String COLLECTION = "matches";

    private final ExecutorService executor;

    public MatchDAO() {
        this.executor = Executors.newFixedThreadPool(3);
    }

    public CompletableFuture<Integer> createMatchAsync(Match match) {
        return CompletableFuture.supplyAsync(() -> createMatch(match), executor);
    }

    public int createMatch(Match match) {
        try {
            long nextId = RealtimeDatabaseService.nextId("counters/matches");
            int id = Math.toIntExact(nextId);
            match.setId(id);
            if (match.getPlayerStats() == null) {
                match.setPlayerStats(new ArrayList<>());
            }
            RealtimeDatabaseService.write(path(id), match);
            return id;
        } catch (Exception e) {
            System.err.println("Error creating match: " + e.getMessage());
            return -1;
        }
    }

    public CompletableFuture<Match> getMatchByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getMatchById(id), executor);
    }

    public Match getMatchById(int id) {
        try {
            return RealtimeDatabaseService.read(path(id), Match.class);
        } catch (Exception e) {
            System.err.println("Error getting match: " + e.getMessage());
            return null;
        }
    }

    public CompletableFuture<List<Match>> getMatchesByTournamentAsync(int tournamentId) {
        return CompletableFuture.supplyAsync(() -> getMatchesByTournament(tournamentId), executor);
    }

    public List<Match> getMatchesByTournament(int tournamentId) {
        return getAllMatches().stream()
                .filter(m -> m.getTournamentId() == tournamentId)
                .sorted(Comparator.comparing(Match::getScheduledTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .collect(Collectors.toList());
    }

    public List<Match> getMatchesByStatus(Match.MatchStatus status) {
        return getAllMatches().stream()
                .filter(m -> m.getStatus() == status)
                .sorted(Comparator.comparing(Match::getScheduledTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .collect(Collectors.toList());
    }

    public List<Match> getAllMatches() {
        try {
            Map<String, Match> map = RealtimeDatabaseService.readCollection(COLLECTION, Match.class);
            if (map == null) {
                return new ArrayList<>();
            }
            return map.values().stream()
                    .sorted(Comparator.comparing(Match::getScheduledTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting all matches: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public CompletableFuture<Boolean> updateMatchAsync(Match match) {
        return CompletableFuture.supplyAsync(() -> updateMatch(match), executor);
    }

    public boolean updateMatch(Match match) {
        try {
            RealtimeDatabaseService.write(path(match.getId()), match);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating match: " + e.getMessage());
            return false;
        }
    }

    public boolean startMatch(int matchId) {
        Match match = getMatchById(matchId);
        if (match == null) {
            return false;
        }
        match.startMatch();
        return updateMatch(match);
    }

    public boolean endMatch(int matchId, int winnerId) {
        Match match = getMatchById(matchId);
        if (match == null) {
            return false;
        }
        match.endMatch(winnerId);
        return updateMatch(match);
    }

    public CompletableFuture<Boolean> addPlayerStatsAsync(PlayerMatchStats stats) {
        return CompletableFuture.supplyAsync(() -> addPlayerStats(stats), executor);
    }

    public boolean addPlayerStats(PlayerMatchStats stats) {
        Match match = getMatchById(stats.getMatchId());
        if (match == null) {
            return false;
        }
        try {
            long nextId = RealtimeDatabaseService.nextId("counters/playerMatchStats");
            stats.setId(Math.toIntExact(nextId));
            if (match.getPlayerStats() == null) {
                match.setPlayerStats(new ArrayList<>());
            }
            match.getPlayerStats().add(stats);
            return updateMatch(match);
        } catch (Exception e) {
            System.err.println("Error adding player stats: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePlayerStats(int matchId, int playerId, int kills, int deaths, int assists) {
        Match match = getMatchById(matchId);
        if (match == null) {
            return false;
        }

        if (match.getPlayerStats() == null) {
            match.setPlayerStats(new ArrayList<>());
        }

        PlayerMatchStats existing = match.getPlayerStats().stream()
                .filter(ps -> ps.getPlayerId() == playerId)
                .findFirst()
                .orElse(null);

        if (existing == null) {
            try {
                long nextId = RealtimeDatabaseService.nextId("counters/playerMatchStats");
                existing = new PlayerMatchStats();
                existing.setId(Math.toIntExact(nextId));
                existing.setMatchId(matchId);
                existing.setPlayerId(playerId);
                match.getPlayerStats().add(existing);
            } catch (Exception e) {
                System.err.println("Error updating player stats: " + e.getMessage());
                return false;
            }
        }

        existing.setKills(kills);
        existing.setDeaths(deaths);
        existing.setAssists(assists);

        return updateMatch(match);
    }

    public List<PlayerMatchStats> getPlayerStatsByMatch(int matchId) {
        Match match = getMatchById(matchId);
        if (match == null || match.getPlayerStats() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(match.getPlayerStats());
    }

    public List<PlayerMatchStats> getPlayerStatsByPlayer(int playerId) {
        return getAllMatches().stream()
                .filter(m -> m.getPlayerStats() != null)
                .flatMap(m -> m.getPlayerStats().stream())
                .filter(stats -> stats.getPlayerId() == playerId)
                .collect(Collectors.toList());
    }

    public boolean deleteMatch(int id) {
        try {
            RealtimeDatabaseService.delete(path(id));
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting match: " + e.getMessage());
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