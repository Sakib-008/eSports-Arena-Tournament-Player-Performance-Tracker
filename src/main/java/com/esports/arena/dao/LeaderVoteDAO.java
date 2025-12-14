package com.esports.arena.dao;

import com.esports.arena.database.DatabaseManager;
import com.esports.arena.model.LeaderVote;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaderVoteDAO {
    private final DatabaseManager dbManager;
    private final ExecutorService executor;

    public LeaderVoteDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.executor = Executors.newFixedThreadPool(2);
    }

    public CompletableFuture<Boolean> castVoteAsync(int teamId, int voterId, int candidateId) {
        return CompletableFuture.supplyAsync(() ->
                castVote(teamId, voterId, candidateId), executor);
    }

    public boolean castVote(int teamId, int voterId, int candidateId) {
        dbManager.getLock().writeLock().lock();
        Connection conn = dbManager.getConnection();

        try {
            conn.setAutoCommit(false);

            String deactivateSql = """
                UPDATE leader_votes 
                SET active = 0 
                WHERE team_id = ? AND voter_id = ? AND active = 1
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(deactivateSql)) {
                pstmt.setInt(1, teamId);
                pstmt.setInt(2, voterId);
                pstmt.executeUpdate();
            }

            String insertSql = """
                INSERT INTO leader_votes (team_id, voter_id, candidate_id, vote_time, active)
                VALUES (?, ?, ?, ?, 1)
            """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, teamId);
                pstmt.setInt(2, voterId);
                pstmt.setInt(3, candidateId);
                pstmt.setString(4, LocalDateTime.now().toString());
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("Error casting vote: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public CompletableFuture<List<LeaderVote>> getActiveVotesAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getActiveVotes(teamId), executor);
    }

    public List<LeaderVote> getActiveVotes(int teamId) {
        List<LeaderVote> votes = new ArrayList<>();
        String sql = "SELECT * FROM leader_votes WHERE team_id = ? AND active = 1";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                votes.add(extractVote(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting active votes: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return votes;
    }

    public CompletableFuture<Map<Integer, Integer>> getVoteCountsAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getVoteCounts(teamId), executor);
    }

    public Map<Integer, Integer> getVoteCounts(int teamId) {
        Map<Integer, Integer> voteCounts = new HashMap<>();
        String sql = """
            SELECT candidate_id, COUNT(*) as vote_count 
            FROM leader_votes 
            WHERE team_id = ? AND active = 1 
            GROUP BY candidate_id 
            ORDER BY vote_count DESC
        """;

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                voteCounts.put(rs.getInt("candidate_id"), rs.getInt("vote_count"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting vote counts: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return voteCounts;
    }

    public CompletableFuture<Integer> getCurrentLeaderAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getCurrentLeader(teamId), executor);
    }

    public Integer getCurrentLeader(int teamId) {
        String sql = """
            SELECT candidate_id, COUNT(*) as vote_count 
            FROM leader_votes 
            WHERE team_id = ? AND active = 1 
            GROUP BY candidate_id 
            ORDER BY vote_count DESC 
            LIMIT 1
        """;

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("candidate_id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting current leader: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return null;
    }

    public boolean hasVoted(int teamId, int voterId) {
        String sql = """
            SELECT COUNT(*) as count 
            FROM leader_votes 
            WHERE team_id = ? AND voter_id = ? AND active = 1
        """;

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            pstmt.setInt(2, voterId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking vote status: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return false;
    }

    public CompletableFuture<Boolean> resetVotesAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> resetVotes(teamId), executor);
    }

    public boolean resetVotes(int teamId) {
        String sql = "UPDATE leader_votes SET active = 0 WHERE team_id = ?";

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            return pstmt.executeUpdate() >= 0; // Can be 0 if no votes exist
        } catch (SQLException e) {
            System.err.println("Error resetting votes: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public Map<String, Object> getVotingStats(int teamId) {
        Map<String, Object> stats = new HashMap<>();

        String sql = """
            SELECT 
                COUNT(DISTINCT voter_id) as total_voters,
                COUNT(*) as total_votes,
                COUNT(DISTINCT candidate_id) as total_candidates
            FROM leader_votes 
            WHERE team_id = ? AND active = 1
        """;

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                stats.put("totalVoters", rs.getInt("total_voters"));
                stats.put("totalVotes", rs.getInt("total_votes"));
                stats.put("totalCandidates", rs.getInt("total_candidates"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting voting stats: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return stats;
    }

    private LeaderVote extractVote(ResultSet rs) throws SQLException {
        LeaderVote vote = new LeaderVote();
        vote.setId(rs.getInt("id"));
        vote.setTeamId(rs.getInt("team_id"));
        vote.setVoterId(rs.getInt("voter_id"));
        vote.setCandidateId(rs.getInt("candidate_id"));
        vote.setVoteTime(LocalDateTime.parse(rs.getString("vote_time")));
        vote.setActive(rs.getInt("active") == 1);
        return vote;
    }

    public void shutdown() {
        executor.shutdown();
    }
}