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

import com.esports.arena.model.Team;
import com.esports.arena.service.RealtimeDatabaseService;

public class TeamDAO {
    private static final String COLLECTION = "teams";

    private final ExecutorService executor;
    private final PlayerDAO playerDAO;

    public TeamDAO() {
        this.executor = Executors.newFixedThreadPool(4);
        this.playerDAO = new PlayerDAO();
    }

    public CompletableFuture<Integer> createTeamAsync(Team team) {
        return CompletableFuture.supplyAsync(() -> createTeam(team), executor);
    }

    public int createTeam(Team team) {
        try {
            long nextId = RealtimeDatabaseService.nextId("counters/teams");
            int id = Math.toIntExact(nextId);
            team.setId(id);
            if (team.getCreatedDate() == null) {
                team.setCreatedDate(LocalDate.now());
            }
            RealtimeDatabaseService.write(path(id), team);
            return id;
        } catch (Exception e) {
            System.err.println("Error creating team: " + e.getMessage());
            return -1;
        }
    }

    public CompletableFuture<Team> getTeamByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getTeamById(id), executor);
    }

    public Team getTeamById(int id) {
        try {
            Team team = RealtimeDatabaseService.read(path(id), Team.class);
            if (team != null) {
                team.setPlayers(playerDAO.getPlayersByTeam(id));
            }
            return team;
        } catch (Exception e) {
            System.err.println("Error getting team: " + e.getMessage());
            return null;
        }
    }

    public CompletableFuture<List<Team>> getAllTeamsAsync() {
        return CompletableFuture.supplyAsync(this::getAllTeams, executor);
    }

    public List<Team> getAllTeams() {
        try {
            Map<String, Team> map = RealtimeDatabaseService.readCollection(COLLECTION, Team.class);
            if (map == null) {
                return new ArrayList<>();
            }
            return map.values().stream()
                    .peek(t -> t.setPlayers(playerDAO.getPlayersByTeam(t.getId())))
                    .sorted(Comparator.comparing(Team::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting all teams: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public CompletableFuture<Boolean> updateTeamAsync(Team team) {
        return CompletableFuture.supplyAsync(() -> updateTeam(team), executor);
    }

    public boolean updateTeam(Team team) {
        try {
            RealtimeDatabaseService.write(path(team.getId()), team);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating team: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> updateTeamLeaderAsync(int teamId, int leaderId) {
        return CompletableFuture.supplyAsync(() -> updateTeamLeader(teamId, leaderId), executor);
    }

    public boolean updateTeamLeader(int teamId, int leaderId) {
        Team team = getTeamById(teamId);
        if (team == null) {
            return false;
        }
        team.setLeaderId(leaderId);
        return updateTeam(team);
    }

    public boolean updateTeamRecord(int teamId, boolean won, boolean draw) {
        Team team = getTeamById(teamId);
        if (team == null) {
            return false;
        }
        if (draw) {
            team.setDraws(team.getDraws() + 1);
        } else if (won) {
            team.setWins(team.getWins() + 1);
        } else {
            team.setLosses(team.getLosses() + 1);
        }
        return updateTeam(team);
    }

    public boolean deleteTeam(int id) {
        try {
            RealtimeDatabaseService.delete(path(id));
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting team: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<List<Team>> getLeaderboardAsync() {
        return CompletableFuture.supplyAsync(this::getLeaderboard, executor);
    }

    public List<Team> getLeaderboard() {
        return getAllTeams().stream()
                .sorted(Comparator
                        .comparingInt(Team::getWins).reversed()
                        .thenComparingInt(t -> t.getWins() + t.getLosses() + t.getDraws())
                        .thenComparing(Team::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    private String path(int id) {
        return COLLECTION + "/" + id;
    }

    public void shutdown() {
        executor.shutdown();
        playerDAO.shutdown();
    }
}