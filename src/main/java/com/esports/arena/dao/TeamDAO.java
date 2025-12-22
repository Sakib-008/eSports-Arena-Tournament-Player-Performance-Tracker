package com.esports.arena.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.esports.arena.database.DatabaseManager;
import com.esports.arena.model.Team;

public class TeamDAO {
    private final DatabaseManager dbManager;
    private final ExecutorService executor;
    private final PlayerDAO playerDAO;

    public TeamDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.executor = Executors.newFixedThreadPool(4);
        this.playerDAO = new PlayerDAO();
    }

    public CompletableFuture<Integer> createTeamAsync(Team team) {
        return CompletableFuture.supplyAsync(() -> createTeam(team), executor);
    }

    public int createTeam(Team team) {
        String sql = """
            INSERT INTO teams (name, tag, region, created_date, wins, losses, draws, leader_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, team.getName());
            pstmt.setString(2, team.getTag());
            pstmt.setString(3, team.getRegion());
            pstmt.setString(4, team.getCreatedDate().toString());
            pstmt.setInt(5, team.getWins());
            pstmt.setInt(6, team.getLosses());
            pstmt.setInt(7, team.getDraws());
            if (team.getLeaderId() != null) {
                pstmt.setInt(8, team.getLeaderId());
            } else {
                pstmt.setNull(8, Types.INTEGER);
            }

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    team.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating team: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return -1;
    }

    public CompletableFuture<Team> getTeamByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getTeamById(id), executor);
    }

    public Team getTeamById(int id) {
        String sql = "SELECT * FROM teams WHERE id = ?";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Team team = extractTeam(rs);
                // Load players for this team
                team.setPlayers(playerDAO.getPlayersByTeam(id));
                return team;
            }
        } catch (SQLException e) {
            System.err.println("Error getting team: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return null;
    }

    public CompletableFuture<List<Team>> getAllTeamsAsync() {
        return CompletableFuture.supplyAsync(this::getAllTeams, executor);
    }

    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams ORDER BY name";

        dbManager.getLock().readLock().lock();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Team team = extractTeam(rs);
                // Load players for each team
                team.setPlayers(playerDAO.getPlayersByTeam(team.getId()));
                teams.add(team);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all teams: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return teams;
    }

    public CompletableFuture<Boolean> updateTeamAsync(Team team) {
        return CompletableFuture.supplyAsync(() -> updateTeam(team), executor);
    }

    public boolean updateTeam(Team team) {
        String sql = """
            UPDATE teams SET name = ?, tag = ?, region = ?, leader_id = ?
            WHERE id = ?
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, team.getName());
            pstmt.setString(2, team.getTag());
            pstmt.setString(3, team.getRegion());
            if (team.getLeaderId() != null) {
                pstmt.setInt(4, team.getLeaderId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setInt(5, team.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating team: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public CompletableFuture<Boolean> updateTeamLeaderAsync(int teamId, int leaderId) {
        return CompletableFuture.supplyAsync(() ->
                updateTeamLeader(teamId, leaderId), executor);
    }

    public boolean updateTeamLeader(int teamId, int leaderId) {
        String sql = "UPDATE teams SET leader_id = ? WHERE id = ?";

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, leaderId);
            pstmt.setInt(2, teamId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating team leader: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public boolean updateTeamRecord(int teamId, boolean won, boolean draw) {
        String sql;
        if (draw) {
            sql = "UPDATE teams SET draws = draws + 1 WHERE id = ?";
        } else if (won) {
            sql = "UPDATE teams SET wins = wins + 1 WHERE id = ?";
        } else {
            sql = "UPDATE teams SET losses = losses + 1 WHERE id = ?";
        }

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating team record: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public boolean deleteTeam(int id) {
        String sql = "DELETE FROM teams WHERE id = ?";

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting team: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public CompletableFuture<List<Team>> getLeaderboardAsync() {
        return CompletableFuture.supplyAsync(this::getLeaderboard, executor);
    }

    public List<Team> getLeaderboard() {
        List<Team> teams = new ArrayList<>();
        String sql = """
            SELECT * FROM teams 
            ORDER BY wins DESC, (wins + losses + draws) DESC, name ASC
        """;

        dbManager.getLock().readLock().lock();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                teams.add(extractTeam(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting leaderboard: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return teams;
    }

    private Team extractTeam(ResultSet rs) throws SQLException {
        Team team = new Team();
        team.setId(rs.getInt("id"));
        team.setName(rs.getString("name"));
        team.setTag(rs.getString("tag"));
        team.setRegion(rs.getString("region"));
        team.setCreatedDate(LocalDate.parse(rs.getString("created_date")));
        team.setWins(rs.getInt("wins"));
        team.setLosses(rs.getInt("losses"));
        team.setDraws(rs.getInt("draws"));

        int leaderId = rs.getInt("leader_id");
        team.setLeaderId(rs.wasNull() ? null : leaderId);

        return team;
    }

    public void shutdown() {
        executor.shutdown();
        playerDAO.shutdown();
    }
}