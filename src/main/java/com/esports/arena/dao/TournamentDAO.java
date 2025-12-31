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
import com.esports.arena.model.Tournament;
import com.esports.arena.service.RealtimeDatabaseService;
import com.fasterxml.jackson.core.type.TypeReference;

public class TournamentDAO {
    private static final String COLLECTION = "tournaments";

    private final ExecutorService executor;
    private final TeamDAO teamDAO;

    public TournamentDAO() {
        this.executor = Executors.newFixedThreadPool(3);
        this.teamDAO = new TeamDAO();
    }

    public CompletableFuture<Integer> createTournamentAsync(Tournament tournament) {
        return CompletableFuture.supplyAsync(() -> createTournament(tournament), executor);
    }

    public int createTournament(Tournament tournament) {
        try {
            long nextId = RealtimeDatabaseService.nextId("counters/tournaments");
            int id = Math.toIntExact(nextId);
            tournament.setId(id);
            if (tournament.getRegisteredTeams() == null) {
                tournament.setRegisteredTeams(new ArrayList<>());
            }
            if (tournament.getMatches() == null) {
                tournament.setMatches(new ArrayList<>());
            }
            RealtimeDatabaseService.write(path(id), tournament);
            return id;
        } catch (Exception e) {
            System.err.println("Error creating tournament: " + e.getMessage());
            return -1;
        }
    }

    public CompletableFuture<Tournament> getTournamentByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getTournamentById(id), executor);
    }

    public Tournament getTournamentById(int id) {
        try {
            Tournament tournament = RealtimeDatabaseService.read(path(id), Tournament.class);
            if (tournament != null) {
                // Refresh registered teams from TeamDAO to include players
                List<Team> refreshed = tournament.getRegisteredTeams() == null ? new ArrayList<>() : tournament.getRegisteredTeams().stream()
                        .map(t -> teamDAO.getTeamById(t.getId()))
                        .filter(t -> t != null)
                        .collect(Collectors.toList());
                tournament.setRegisteredTeams(refreshed);
            }
            return tournament;
        } catch (Exception e) {
            System.err.println("Error getting tournament: " + e.getMessage());
            return null;
        }
    }

    public CompletableFuture<List<Tournament>> getAllTournamentsAsync() {
        return CompletableFuture.supplyAsync(this::getAllTournaments, executor);
    }

    public List<Tournament> getAllTournaments() {
        try {
            Map<String, Tournament> map = RealtimeDatabaseService.read(COLLECTION,
                    new TypeReference<Map<String, Tournament>>() {});
            if (map == null) {
                return new ArrayList<>();
            }
            return map.values().stream()
                    .peek(t -> {
                        if (t.getRegisteredTeams() != null) {
                            List<Team> refreshed = t.getRegisteredTeams().stream()
                                    .map(team -> teamDAO.getTeamById(team.getId()))
                                    .filter(team -> team != null)
                                    .collect(Collectors.toList());
                            t.setRegisteredTeams(refreshed);
                        }
                    })
                    .sorted(Comparator.comparing(Tournament::getStartDate, Comparator.nullsLast(LocalDate::compareTo)).reversed())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting all tournaments: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public CompletableFuture<Boolean> updateTournamentAsync(Tournament tournament) {
        return CompletableFuture.supplyAsync(() -> updateTournament(tournament), executor);
    }

    public boolean updateTournament(Tournament tournament) {
        try {
            RealtimeDatabaseService.write(path(tournament.getId()), tournament);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating tournament: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> registerTeamAsync(int tournamentId, int teamId) {
        return CompletableFuture.supplyAsync(() -> registerTeam(tournamentId, teamId), executor);
    }

    public boolean registerTeam(int tournamentId, int teamId) {
        Tournament tournament = getTournamentById(tournamentId);
        Team team = teamDAO.getTeamById(teamId);
        if (tournament == null || team == null) {
            return false;
        }
        if (tournament.getRegisteredTeams() == null) {
            tournament.setRegisteredTeams(new ArrayList<>());
        }
        boolean already = tournament.getRegisteredTeams().stream().anyMatch(t -> t.getId() == teamId);
        if (already || tournament.getRegisteredTeams().size() >= tournament.getMaxTeams()) {
            return false;
        }
        tournament.getRegisteredTeams().add(team);
        return updateTournament(tournament);
    }

    public List<Team> getRegisteredTeams(int tournamentId) {
        Tournament tournament = getTournamentById(tournamentId);
        if (tournament == null || tournament.getRegisteredTeams() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(tournament.getRegisteredTeams());
    }

    public List<Tournament> getTournamentsByStatus(Tournament.TournamentStatus status) {
        return getAllTournaments().stream()
                .filter(t -> t.getStatus() == status)
                .sorted(Comparator.comparing(Tournament::getStartDate, Comparator.nullsLast(LocalDate::compareTo)))
                .collect(Collectors.toList());
    }

    public boolean deleteTournament(int id) {
        try {
            RealtimeDatabaseService.delete(path(id));
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting tournament: " + e.getMessage());
            return false;
        }
    }

    private String path(int id) {
        return COLLECTION + "/" + id;
    }

    public void shutdown() {
        executor.shutdown();
        teamDAO.shutdown();
    }
}