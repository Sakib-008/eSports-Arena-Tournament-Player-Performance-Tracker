package com.esports.arena.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.esports.arena.database.DatabaseManager;
import com.esports.arena.model.Organizer;

public class OrganizerDAO {
    private final DatabaseManager dbManager;
    private final ExecutorService executor;

    public OrganizerDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.executor = Executors.newFixedThreadPool(2);
    }

    // Get organizer by username
    public Organizer getOrganizerByUsername(String username) {
        String query = "SELECT * FROM organizers WHERE username = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToOrganizer(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching organizer by username: " + e.getMessage());
        }
        return null;
    }

    // Get organizer by ID
    public Organizer getOrganizerById(int id) {
        String query = "SELECT * FROM organizers WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToOrganizer(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching organizer by id: " + e.getMessage());
        }
        return null;
    }

    // Get all organizers
    public List<Organizer> getAllOrganizers() {
        List<Organizer> organizers = new ArrayList<>();
        String query = "SELECT * FROM organizers";
        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                organizers.add(mapResultSetToOrganizer(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all organizers: " + e.getMessage());
        }
        return organizers;
    }

    // Create organizer
    public boolean createOrganizer(Organizer organizer) {
        String query = "INSERT INTO organizers (username, password, email, full_name, created_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, organizer.getUsername());
            pstmt.setString(2, organizer.getPassword());
            pstmt.setString(3, organizer.getEmail());
            pstmt.setString(4, organizer.getFullName());
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        organizer.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating organizer: " + e.getMessage());
        }
        return false;
    }

    // Update organizer
    public boolean updateOrganizer(Organizer organizer) {
        String query = "UPDATE organizers SET username = ?, password = ?, email = ?, full_name = ? WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(query)) {
            pstmt.setString(1, organizer.getUsername());
            pstmt.setString(2, organizer.getPassword());
            pstmt.setString(3, organizer.getEmail());
            pstmt.setString(4, organizer.getFullName());
            pstmt.setInt(5, organizer.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating organizer: " + e.getMessage());
        }
        return false;
    }

    // Delete organizer
    public boolean deleteOrganizer(int id) {
        String query = "DELETE FROM organizers WHERE id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting organizer: " + e.getMessage());
        }
        return false;
    }

    // Authenticate organizer (username and password check)
    public Organizer authenticateOrganizer(String username, String password) {
        Organizer organizer = getOrganizerByUsername(username);
        if (organizer != null && organizer.getPassword().equals(password)) {
            return organizer;
        }
        return null;
    }

    // Async method for authentication
    public CompletableFuture<Organizer> authenticateOrganizerAsync(String username, String password) {
        return CompletableFuture.supplyAsync(() -> authenticateOrganizer(username, password), executor);
    }

    // Helper method to map ResultSet to Organizer
    private Organizer mapResultSetToOrganizer(ResultSet rs) throws SQLException {
        Organizer organizer = new Organizer();
        organizer.setId(rs.getInt("id"));
        organizer.setUsername(rs.getString("username"));
        organizer.setPassword(rs.getString("password"));
        organizer.setEmail(rs.getString("email"));
        organizer.setFullName(rs.getString("full_name"));
        organizer.setCreatedDate(rs.getString("created_date"));
        return organizer;
    }
}
