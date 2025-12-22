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
import com.esports.arena.model.Player;

public class PlayerDAO {
    private final DatabaseManager dbManager;
    private final ExecutorService executor;

    public PlayerDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.executor = Executors.newFixedThreadPool(4);
    }

    public CompletableFuture<Integer> createPlayerAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> createPlayer(player), executor);
    }

    public int createPlayer(Player player) {
        String sql = """
            INSERT INTO players (username, password, real_name, email, team_id, role, join_date, 
                                available, availability_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, player.getUsername());
            pstmt.setString(2, player.getPassword());
            pstmt.setString(3, player.getRealName());
            pstmt.setString(4, player.getEmail());
            if (player.getTeamId() != null) {
                pstmt.setInt(5, player.getTeamId());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }
            pstmt.setString(6, player.getRole());
            pstmt.setString(7, player.getJoinDate().toString());
            pstmt.setInt(8, player.isAvailable() ? 1 : 0);
            pstmt.setString(9, player.getAvailabilityReason());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    player.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating player: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return -1;
    }

    public CompletableFuture<Player> getPlayerByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getPlayerById(id), executor);
    }

    public Player getPlayerById(int id) {
        String sql = "SELECT * FROM players WHERE id = ?";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractPlayer(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting player: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return null;
    }

    public Player getPlayerByUsername(String username) {
        String sql = "SELECT * FROM players WHERE username = ?";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractPlayer(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting player by username: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return null;
    }

    public CompletableFuture<List<Player>> getAllPlayersAsync() {
        return CompletableFuture.supplyAsync(this::getAllPlayers, executor);
    }

    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM players ORDER BY username";

        dbManager.getLock().readLock().lock();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                players.add(extractPlayer(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all players: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return players;
    }

    public CompletableFuture<List<Player>> getPlayersByTeamAsync(int teamId) {
        return CompletableFuture.supplyAsync(() -> getPlayersByTeam(teamId), executor);
    }

    public List<Player> getPlayersByTeam(int teamId) {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM players WHERE team_id = ? ORDER BY username";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                players.add(extractPlayer(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting players by team: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return players;
    }

    public List<Player> getAvailablePlayersByTeam(int teamId) {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM players WHERE team_id = ? AND available = 1 ORDER BY username";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                players.add(extractPlayer(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting available players: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return players;
    }

    public CompletableFuture<Boolean> updatePlayerAsync(Player player) {
        return CompletableFuture.supplyAsync(() -> updatePlayer(player), executor);
    }

    public boolean updatePlayer(Player player) {
        String sql = """
            UPDATE players SET username = ?, real_name = ?, email = ?, team_id = ?, 
                              role = ?, available = ?, availability_reason = ?,
                              total_kills = ?, total_deaths = ?, total_assists = ?,
                              matches_played = ?, matches_won = ?
            WHERE id = ?
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, player.getUsername());
            pstmt.setString(2, player.getRealName());
            pstmt.setString(3, player.getEmail());
            if (player.getTeamId() != null) {
                pstmt.setInt(4, player.getTeamId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setString(5, player.getRole());
            pstmt.setInt(6, player.isAvailable() ? 1 : 0);
            pstmt.setString(7, player.getAvailabilityReason());
            pstmt.setInt(8, player.getTotalKills());
            pstmt.setInt(9, player.getTotalDeaths());
            pstmt.setInt(10, player.getTotalAssists());
            pstmt.setInt(11, player.getMatchesPlayed());
            pstmt.setInt(12, player.getMatchesWon());
            pstmt.setInt(13, player.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating player: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public boolean updatePlayerStats(int playerId, int kills, int deaths, int assists,
                                     boolean won) {
        String sql = """
            UPDATE players 
            SET total_kills = total_kills + ?, 
                total_deaths = total_deaths + ?,
                total_assists = total_assists + ?,
                matches_played = matches_played + 1,
                matches_won = matches_won + ?
            WHERE id = ?
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, kills);
            pstmt.setInt(2, deaths);
            pstmt.setInt(3, assists);
            pstmt.setInt(4, won ? 1 : 0);
            pstmt.setInt(5, playerId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating player stats: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public CompletableFuture<Boolean> updateAvailabilityAsync(int playerId, boolean available,
                                                              String reason) {
        return CompletableFuture.supplyAsync(() ->
                updateAvailability(playerId, available, reason), executor);
    }

    public boolean updateAvailability(int playerId, boolean available, String reason) {
        String sql = "UPDATE players SET available = ?, availability_reason = ? WHERE id = ?";

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, available ? 1 : 0);
            pstmt.setString(2, reason);
            pstmt.setInt(3, playerId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating availability: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public boolean deletePlayer(int id) {
        String sql = "DELETE FROM players WHERE id = ?";

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting player: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    private Player extractPlayer(ResultSet rs) throws SQLException {
        Player player = new Player();
        player.setId(rs.getInt("id"));
        player.setUsername(rs.getString("username"));
        player.setPassword(rs.getString("password"));
        player.setRealName(rs.getString("real_name"));
        player.setEmail(rs.getString("email"));

        int teamId = rs.getInt("team_id");
        player.setTeamId(rs.wasNull() ? null : teamId);

        player.setRole(rs.getString("role"));
        player.setJoinDate(LocalDate.parse(rs.getString("join_date")));
        player.setTotalKills(rs.getInt("total_kills"));
        player.setTotalDeaths(rs.getInt("total_deaths"));
        player.setTotalAssists(rs.getInt("total_assists"));
        player.setMatchesPlayed(rs.getInt("matches_played"));
        player.setMatchesWon(rs.getInt("matches_won"));
        player.setAvailable(rs.getInt("available") == 1);
        player.setAvailabilityReason(rs.getString("availability_reason"));

        return player;
    }

    public void shutdown() {
        executor.shutdown();
    }
}