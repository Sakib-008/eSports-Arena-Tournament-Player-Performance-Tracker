package com.esports.arena.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.esports.arena.model.LeaderVote;
import com.esports.arena.service.RealtimeDatabaseService;
import com.fasterxml.jackson.core.type.TypeReference;

public class LeaderVoteDAO {
    private static final String COLLECTION = "leader_votes";

    private final ExecutorService executor;

    public LeaderVoteDAO() {
        this.executor = Executors.newFixedThreadPool(2);
    }

    public CompletableFuture<Boolean> castVoteAsync(int teamId, int voterId, int candidateId) {
        return CompletableFuture.supplyAsync(() -> castVote(teamId, voterId, candidateId), executor);
    }

    public boolean castVote(int teamId, int voterId, int candidateId) {
        try {
            List<LeaderVote> votes = getAllVotes(teamId);
            // Deactivate previous active votes by this voter
            votes.forEach(v -> {
                if (v.getVoterId() == voterId) {
                    v.setActive(false);
                }
            });

            long nextId = RealtimeDatabaseService.nextId("counters/leaderVotes");
            LeaderVote vote = new LeaderVote(teamId, voterId, candidateId);
            vote.setId(Math.toIntExact(nextId));
            votes.add(vote);

            RealtimeDatabaseService.write(path(teamId), votes);
            return true;
        } catch (Exception e) {
            System.err.println("Error casting vote: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<List<LeaderVote>> getActiveVotesAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getActiveVotes(teamId), executor);
    }

    public List<LeaderVote> getActiveVotes(int teamId) {
        return getAllVotes(teamId).stream()
                .filter(LeaderVote::isActive)
                .collect(Collectors.toList());
    }

    public CompletableFuture<Map<Integer, Integer>> getVoteCountsAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getVoteCounts(teamId), executor);
    }

    public Map<Integer, Integer> getVoteCounts(int teamId) {
        Map<Integer, Integer> counts = new HashMap<>();
        getActiveVotes(teamId).forEach(v -> counts.merge(v.getCandidateId(), 1, Integer::sum));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
    }

    public CompletableFuture<Integer> getCurrentLeaderAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getCurrentLeader(teamId), executor);
    }

    public Integer getCurrentLeader(int teamId) {
        return getVoteCounts(teamId).entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public boolean hasVoted(int teamId, int voterId) {
        return getActiveVotes(teamId).stream().anyMatch(v -> v.getVoterId() == voterId);
    }

    public CompletableFuture<Boolean> resetVotesAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> resetVotes(teamId), executor);
    }

    public boolean resetVotes(int teamId) {
        try {
            RealtimeDatabaseService.delete(path(teamId));
            return true;
        } catch (Exception e) {
            System.err.println("Error resetting votes: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> getVotingStats(int teamId) {
        Map<String, Object> stats = new HashMap<>();
        List<LeaderVote> active = getActiveVotes(teamId);
        stats.put("totalVoters", (int) active.stream().map(LeaderVote::getVoterId).distinct().count());
        stats.put("totalVotes", active.size());
        stats.put("totalCandidates", (int) active.stream().map(LeaderVote::getCandidateId).distinct().count());
        return stats;
    }

    private List<LeaderVote> getAllVotes(int teamId) {
        try {
            List<LeaderVote> votes = RealtimeDatabaseService.read(path(teamId), new TypeReference<List<LeaderVote>>() {});
            if (votes == null) {
                return new ArrayList<>();
            }
            return votes.stream()
                    .sorted(Comparator.comparing(LeaderVote::getVoteTime))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error loading votes: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String path(int teamId) {
        return COLLECTION + "/" + teamId;
    }

    public void shutdown() {
        executor.shutdown();
    }
}