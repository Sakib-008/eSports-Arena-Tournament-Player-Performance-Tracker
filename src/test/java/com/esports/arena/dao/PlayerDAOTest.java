package com.esports.arena.dao;

import com.esports.arena.model.Player;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlayerDAOTest {

    private static PlayerDAO playerDAO;
    private static int testPlayerId;

    @BeforeAll
    static void setup() {
        playerDAO = new PlayerDAO();
        System.out.println("PlayerDAO Test Suite Started");
    }

    @AfterAll
    static void tearDown() {
        playerDAO.shutdown();
        System.out.println("PlayerDAO Test Suite Completed");
    }

    @Test
    @Order(1)
    @DisplayName("Test Create Player")
    void testCreatePlayer() {
        // Arrange
        Player player = new Player("TestPlayer123", "John Doe", "john@test.com", "DPS");
        player.setAvailable(true);

        // Act
        testPlayerId = playerDAO.createPlayer(player);

        // Assert
        assertTrue(testPlayerId > 0, "Player ID should be greater than 0");
        System.out.println("Created player with ID: " + testPlayerId);
    }

    @Test
    @Order(2)
    @DisplayName("Test Get Player By ID")
    void testGetPlayerById() {
        // Act
        Player player = playerDAO.getPlayerById(testPlayerId);

        // Assert
        assertNotNull(player, "Player should not be null");
        assertEquals("TestPlayer123", player.getUsername());
        assertEquals("John Doe", player.getRealName());
        assertEquals("john@test.com", player.getEmail());
        assertTrue(player.isAvailable());
        System.out.println("Retrieved player: " + player.getUsername());
    }

    @Test
    @Order(3)
    @DisplayName("Test Update Player Async")
    void testUpdatePlayerAsync() throws Exception {
        // Arrange
        Player player = playerDAO.getPlayerById(testPlayerId);
        player.setUsername("UpdatedPlayer123");
        player.setRole("Tank");

        // Act
        CompletableFuture<Boolean> future = playerDAO.updatePlayerAsync(player);
        Boolean result = future.get(); // Wait for completion

        // Assert
        assertTrue(result, "Update should succeed");

        // Verify update
        Player updated = playerDAO.getPlayerById(testPlayerId);
        assertEquals("UpdatedPlayer123", updated.getUsername());
        assertEquals("Tank", updated.getRole());
        System.out.println("Updated player to: " + updated.getUsername());
    }

    @Test
    @Order(4)
    @DisplayName("Test Update Availability")
    void testUpdateAvailability() throws Exception {
        // Arrange
        String reason = "On vacation";

        // Act
        CompletableFuture<Boolean> future = playerDAO.updateAvailabilityAsync(
                testPlayerId, false, reason);
        Boolean result = future.get();

        // Assert
        assertTrue(result, "Availability update should succeed");

        // Verify
        Player player = playerDAO.getPlayerById(testPlayerId);
        assertFalse(player.isAvailable());
        assertEquals(reason, player.getAvailabilityReason());
        System.out.println("Updated availability: " + player.getAvailabilityStatus());
    }

    @Test
    @Order(5)
    @DisplayName("Test Update Player Stats")
    void testUpdatePlayerStats() {
        // Act
        boolean result = playerDAO.updatePlayerStats(testPlayerId, 10, 5, 8, true);

        // Assert
        assertTrue(result, "Stats update should succeed");

        // Verify
        Player player = playerDAO.getPlayerById(testPlayerId);
        assertEquals(10, player.getTotalKills());
        assertEquals(5, player.getTotalDeaths());
        assertEquals(8, player.getTotalAssists());
        assertEquals(1, player.getMatchesPlayed());
        assertEquals(1, player.getMatchesWon());
        assertEquals(2.0, player.getKdRatio(), 0.01);
        System.out.println("Updated stats - K/D: " + player.getKdRatio());
    }

    @Test
    @Order(6)
    @DisplayName("Test Get All Players Async")
    void testGetAllPlayersAsync() throws Exception {
        // Act
        CompletableFuture<List<Player>> future = playerDAO.getAllPlayersAsync();
        List<Player> players = future.get();

        // Assert
        assertNotNull(players, "Players list should not be null");
        assertFalse(players.isEmpty(), "Players list should not be empty");

        boolean found = players.stream()
                .anyMatch(p -> p.getId() == testPlayerId);
        assertTrue(found, "Created player should be in the list");
        System.out.println("Found " + players.size() + " players in database");
    }

    @Test
    @Order(7)
    @DisplayName("Test Concurrent Operations")
    void testConcurrentOperations() throws Exception {
        // Arrange - Create multiple players concurrently
        CompletableFuture<Integer> future1 = playerDAO.createPlayerAsync(
                new Player("Concurrent1", "Player One", "p1@test.com", "Support"));
        CompletableFuture<Integer> future2 = playerDAO.createPlayerAsync(
                new Player("Concurrent2", "Player Two", "p2@test.com", "Mid"));
        CompletableFuture<Integer> future3 = playerDAO.createPlayerAsync(
                new Player("Concurrent3", "Player Three", "p3@test.com", "Top"));

        // Act - Wait for all to complete
        CompletableFuture.allOf(future1, future2, future3).get();

        // Assert
        assertTrue(future1.get() > 0, "Player 1 should be created");
        assertTrue(future2.get() > 0, "Player 2 should be created");
        assertTrue(future3.get() > 0, "Player 3 should be created");
        System.out.println("Created 3 players concurrently");
    }

    @Test
    @Order(8)
    @DisplayName("Test Delete Player")
    void testDeletePlayer() {
        // Act
        boolean result = playerDAO.deletePlayer(testPlayerId);

        // Assert
        assertTrue(result, "Delete should succeed");

        // Verify deletion
        Player deleted = playerDAO.getPlayerById(testPlayerId);
        assertNull(deleted, "Deleted player should not be found");
        System.out.println("Successfully deleted player with ID: " + testPlayerId);
    }

    @Test
    @DisplayName("Test Player K/D Calculation")
    void testKDCalculation() {
        // Arrange
        Player player = new Player();

        // Test normal K/D
        player.setTotalKills(20);
        player.setTotalDeaths(10);
        assertEquals(2.0, player.getKdRatio(), 0.01);

        // Test K/D with zero deaths
        player.setTotalDeaths(0);
        assertEquals(20.0, player.getKdRatio(), 0.01);

        System.out.println("K/D calculations verified");
    }

    @Test
    @DisplayName("Test Win Rate Calculation")
    void testWinRateCalculation() {
        // Arrange
        Player player = new Player();

        // Test normal win rate
        player.setMatchesPlayed(10);
        player.setMatchesWon(7);
        assertEquals(70.0, player.getWinRate(), 0.01);

        // Test with no matches played
        player.setMatchesPlayed(0);
        assertEquals(0.0, player.getWinRate(), 0.01);

        System.out.println("Win rate calculations verified");
    }
}