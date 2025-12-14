package com.esports.arena.dao;

import com.esports.arena.database.DatabaseManager;
import com.esports.arena.model.Tournament;
import com.esports.arena.model.Team;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TournamentDAO {
    private final DatabaseManager dbManager;
    private final ExecutorService executor;
    private final TeamDAO teamDAO;

    public TournamentDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.executor = Executors.newFixedThreadPool(3);
        this.teamDAO = new TeamDAO();
    }

    public CompletableFuture<Integer> createTournamentAsync(Tournament tournament) {
        return CompletableFuture.supplyAsync(() -> createTournament(tournament), executor);
    }

    public int createTournament(Tournament tournament) {
        String sql = """
            INSERT INTO tournaments (name, game, format, start_date, end_date, 
                                    prize_pool, status, max_teams)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, tournament.getName());
            pstmt.setString(2, tournament.getGame());
            pstmt.setString(3, tournament.getFormat());
            pstmt.setString(4, tournament.getStartDate().toString());
            pstmt.setString(5, tournament.getEndDate().toString());
            pstmt.setDouble(6, tournament.getPrizePool());
            pstmt.setString(7, tournament.getStatus().toString());
            pstmt.setInt(8, tournament.getMaxTeams());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    tournament.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating tournament: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return -1;
    }

    public CompletableFuture<Tournament> getTournamentByIdAsync(int id) {
        return CompletableFuture.supplyAsync(() -> getTournamentById(id), executor);
    }

    public Tournament getTournamentById(int id) {
        String sql = "SELECT * FROM tournaments WHERE id = ?";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Tournament tournament = extractTournament(rs);
                loadRegisteredTeams(tournament);
                return tournament;
            }
        } catch (SQLException e) {
            System.err.println("Error getting tournament: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return null;
    }

    public CompletableFuture<List<Tournament>> getAllTournamentsAsync() {
        return CompletableFuture.supplyAsync(this::getAllTournaments, executor);
    }

    public List<Tournament> getAllTournaments() {
        List<Tournament> tournaments = new ArrayList<>();
        String sql = "SELECT * FROM tournaments ORDER BY start_date DESC";

        dbManager.getLock().readLock().lock();
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Tournament tournament = extractTournament(rs);
                loadRegisteredTeams(tournament);
                tournaments.add(tournament);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all tournaments: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return tournaments;
    }

    public CompletableFuture<Boolean> updateTournamentAsync(Tournament tournament) {
        return CompletableFuture.supplyAsync(() -> updateTournament(tournament), executor);
    }

    public boolean updateTournament(Tournament tournament) {
        String sql = """
            UPDATE tournaments 
            SET name = ?, game = ?, format = ?, start_date = ?, end_date = ?, 
                prize_pool = ?, status = ?, max_teams = ?
            WHERE id = ?
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, tournament.getName());
            pstmt.setString(2, tournament.getGame());
            pstmt.setString(3, tournament.getFormat());
            pstmt.setString(4, tournament.getStartDate().toString());
            pstmt.setString(5, tournament.getEndDate().toString());
            pstmt.setDouble(6, tournament.getPrizePool());
            pstmt.setString(7, tournament.getStatus().toString());
            pstmt.setInt(8, tournament.getMaxTeams());
            pstmt.setInt(9, tournament.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating tournament: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public CompletableFuture<Boolean> registerTeamAsync(int tournamentId, int teamId) {
        return CompletableFuture.supplyAsync(() ->
                registerTeam(tournamentId, teamId), executor);
    }

    public boolean registerTeam(int tournamentId, int teamId) {
        String sql = """
            INSERT INTO tournament_registrations (tournament_id, team_id, registration_date)
            VALUES (?, ?, ?)
        """;

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            pstmt.setInt(2, teamId);
            pstmt.setString(3, LocalDate.now().toString());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error registering team: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    public List<Team> getRegisteredTeams(int tournamentId) {
        List<Team> teams = new ArrayList<>();
        String sql = """
            SELECT t.* FROM teams t
            INNER JOIN tournament_registrations tr ON t.id = tr.team_id
            WHERE tr.tournament_id = ?
        """;

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, tournamentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                teams.add(extractTeamFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting registered teams: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return teams;
    }

    public List<Tournament> getTournamentsByStatus(Tournament.TournamentStatus status) {
        List<Tournament> tournaments = new ArrayList<>();
        String sql = "SELECT * FROM tournaments WHERE status = ? ORDER BY start_date";

        dbManager.getLock().readLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, status.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                tournaments.add(extractTournament(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting tournaments by status: " + e.getMessage());
        } finally {
            dbManager.getLock().readLock().unlock();
        }
        return tournaments;
    }

    public boolean deleteTournament(int id) {
        String sql = "DELETE FROM tournaments WHERE id = ?";

        dbManager.getLock().writeLock().lock();
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting tournament: " + e.getMessage());
        } finally {
            dbManager.getLock().writeLock().unlock();
        }
        return false;
    }

    private Tournament extractTournament(ResultSet rs) throws SQLException {
        Tournament tournament = new Tournament();
        tournament.setId(rs.getInt("id"));
        tournament.setName(rs.getString("name"));
        tournament.setGame(rs.getString("game"));
        tournament.setFormat(rs.getString("format"));
        tournament.setStartDate(LocalDate.parse(rs.getString("start_date")));
        tournament.setEndDate(LocalDate.parse(rs.getString("end_date")));
        tournament.setPrizePool(rs.getDouble("prize_pool"));
        tournament.setStatus(Tournament.TournamentStatus.valueOf(rs.getString("status")));
        tournament.setMaxTeams(rs.getInt("max_teams"));
        return tournament;
    }

    private Team extractTeamFromResultSet(ResultSet rs) throws SQLException {
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

    private void loadRegisteredTeams(Tournament tournament) {
        tournament.setRegisteredTeams(getRegisteredTeams(tournament.getId()));
    }

    public void shutdown() {
        executor.shutdown();
        teamDAO.shutdown();
    }
}