package com.esports.arena.dao;

import com.esports.arena.database.DatabaseManager;
import com.esports.arena.model.Match;
import com.esports.arena.model.PlayerMatchStats;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MatchDAO {
    private final DatabaseManager dbManager;
    private final ExecutorService executor;

    public MatchDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.executor = Executors.newFixedThreadPool(3);
    }

    public CompletableFuture<Integer> createMatchAsync(Match match) {
        return CompletableFuture.supplyAsync(() -> createMatch(match), executor);
    }

    public int createMatch(Match match) {
        String sql = """
            INSERT INTO matches (tournament_id, team1_id, team2_id, team1_score, team2_score,
                                scheduled_time, status, round)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, match.getTournamentId());
            pstmt.setInt(2, match.getTeam1Id());
            pstmt.setInt(3, match.getTeam2Id());
            pstmt.setInt(4, match.getTeam1Score());
            pstmt.setInt(5, match.getTeam2Score());
            pstmt.setString(6, match.getScheduledTime().toString());
            pstmt.setString(7, match.getStatus().toString());
            pstmt.setString(8, match.getRound());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    match.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating match: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return -1;
    }

    public CompletableFuture<Match> getMatchByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getMatchById(id), executor);
    }

    public Match getMatchById(int id) {
        String sql = "SELECT * FROM matches WHERE id = ?";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Match match = extractMatch(rs);
                loadPlayerStats(match);
                return match;
            }
        } catch (SQLException e) {
            System.err.println("Error getting match: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return null;
    }

    public CompletableFuture<List<Match>> getMatchesByTournamentAsync(int tournamentId) {
        return CompletableFuture.supplyAsync(() ->
                getMatchesByTournament(tournamentId), executor);
    }

    public List<Match> getMatchesByTournament(int tournamentId) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE tournament_id = ? ORDER BY scheduled_time";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Match match = extractMatch(rs);
                loadPlayerStats(match);
                matches.add(match);
            }
        } catch (SQLException e) {
            System.err.println("Error getting matches by tournament: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return matches;
    }

    public List<Match> getMatchesByStatus(Match.MatchStatus status) {
        List<Match> matches = new ArrayList<>();
        String sql = "SELECT * FROM matches WHERE status = ? ORDER BY scheduled_time";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                matches.add(extractMatch(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting matches by status: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return matches;
    }

    public CompletableFuture<Boolean> updateMatchAsync(Match match) {
        return CompletableFuture.supplyAsync(() -> updateMatch(match), executor);
    }

    public boolean updateMatch(Match match) {
        String sql = """
            UPDATE matches 
            SET team1_score = ?, team2_score = ?, actual_start_time = ?, 
                actual_end_time = ?, status = ?, winner_id = ?
            WHERE id = ?
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, match.getTeam1Score());
            pstmt.setInt(2, match.getTeam2Score());

            if (match.getActualStartTime() != null) {
                pstmt.setString(3, match.getActualStartTime().toString());
            } else {
                pstmt.setNull(3, Types.VARCHAR);
            }

            if (match.getActualEndTime() != null) {
                pstmt.setString(4, match.getActualEndTime().toString());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }

            pstmt.setString(5, match.getStatus().toString());

            if (match.getWinnerId() != null) {
                pstmt.setInt(6, match.getWinnerId());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }

            pstmt.setInt(7, match.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating match: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public boolean startMatch(int matchId) {
        Match match = getMatchById(matchId);
        if (match != null) {
            match.startMatch();
            return updateMatch(match);
        }
        return false;
    }

    public boolean endMatch(int matchId, int winnerId) {
        Match match = getMatchById(matchId);
        if (match != null) {
            match.endMatch(winnerId);
            return updateMatch(match);
        }
        return false;
    }

    public CompletableFuture<Boolean> addPlayerStatsAsync(PlayerMatchStats stats) {
        return CompletableFuture.supplyAsync(() -> addPlayerStats(stats), executor);
    }

    public boolean addPlayerStats(PlayerMatchStats stats) {
        String sql = """
            INSERT INTO player_match_stats (match_id, player_id, kills, deaths, assists,
                                           damage_dealt, damage_taken, gold_earned, mvp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, stats.getMatchId());
            pstmt.setInt(2, stats.getPlayerId());
            pstmt.setInt(3, stats.getKills());
            pstmt.setInt(4, stats.getDeaths());
            pstmt.setInt(5, stats.getAssists());
            pstmt.setInt(6, stats.getDamageDealt());
            pstmt.setInt(7, stats.getDamageTaken());
            pstmt.setInt(8, stats.getGoldEarned());
            pstmt.setInt(9, stats.isMvp() ? 1 : 0);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    stats.setId(rs.getInt(1));
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding player stats: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public List<PlayerMatchStats> getPlayerStatsByMatch(int matchId) {
        List<PlayerMatchStats> statsList = new ArrayList<>();
        String sql = "SELECT * FROM player_match_stats WHERE match_id = ?";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, matchId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                statsList.add(extractPlayerStats(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting player stats: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return statsList;
    }

    public List<PlayerMatchStats> getPlayerStatsByPlayer(int playerId) {
        List<PlayerMatchStats> statsList = new ArrayList<>();
        String sql = "SELECT * FROM player_match_stats WHERE player_id = ?";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                statsList.add(extractPlayerStats(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting player stats: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return statsList;
    }

    public boolean deleteMatch(int id) {
        String sql = "DELETE FROM matches WHERE id = ?";

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting match: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    private Match extractMatch(ResultSet rs) throws SQLException {
        Match match = new Match();
        match.setId(rs.getInt("id"));
        match.setTournamentId(rs.getInt("tournament_id"));
        match.setTeam1Id(rs.getInt("team1_id"));
        match.setTeam2Id(rs.getInt("team2_id"));
        match.setTeam1Score(rs.getInt("team1_score"));
        match.setTeam2Score(rs.getInt("team2_score"));
        match.setScheduledTime(LocalDateTime.parse(rs.getString("scheduled_time")));

        String startTime = rs.getString("actual_start_time");
        if (startTime != null) {
            match.setActualStartTime(LocalDateTime.parse(startTime));
        }

        String endTime = rs.getString("actual_end_time");
        if (endTime != null) {
            match.setActualEndTime(LocalDateTime.parse(endTime));
        }

        match.setStatus(Match.MatchStatus.valueOf(rs.getString("status")));
        match.setRound(rs.getString("round"));

        int winnerId = rs.getInt("winner_id");
        match.setWinnerId(rs.wasNull() ? null : winnerId);

        return match;
    }

    private PlayerMatchStats extractPlayerStats(ResultSet rs) throws SQLException {
        PlayerMatchStats stats = new PlayerMatchStats();
        stats.setId(rs.getInt("id"));
        stats.setMatchId(rs.getInt("match_id"));
        stats.setPlayerId(rs.getInt("player_id"));
        stats.setKills(rs.getInt("kills"));
        stats.setDeaths(rs.getInt("deaths"));
        stats.setAssists(rs.getInt("assists"));
        stats.setDamageDealt(rs.getInt("damage_dealt"));
        stats.setDamageTaken(rs.getInt("damage_taken"));
        stats.setGoldEarned(rs.getInt("gold_earned"));
        stats.setMvp(rs.getInt("mvp") == 1);
        return stats;
    }

    private void loadPlayerStats(Match match) {
        match.setPlayerStats(getPlayerStatsByMatch(match.getId()));
    }

    public void shutdown() {
        executor.shutdown();
    }
}