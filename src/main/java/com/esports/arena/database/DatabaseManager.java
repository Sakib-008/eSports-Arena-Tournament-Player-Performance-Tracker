package com.esports.arena.database;

import java.sql.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final String DB_URL = "jdbc:sqlite:esports_arena.db";

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        lock.writeLock().lock();
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS teams (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    tag TEXT NOT NULL UNIQUE,
                    region TEXT,
                    created_date TEXT,
                    wins INTEGER DEFAULT 0,
                    losses INTEGER DEFAULT 0,
                    draws INTEGER DEFAULT 0,
                    leader_id INTEGER,
                    FOREIGN KEY (leader_id) REFERENCES players(id) ON DELETE SET NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    real_name TEXT,
                    email TEXT UNIQUE,
                    team_id INTEGER,
                    role TEXT,
                    join_date TEXT,
                    total_kills INTEGER DEFAULT 0,
                    total_deaths INTEGER DEFAULT 0,
                    total_assists INTEGER DEFAULT 0,
                    matches_played INTEGER DEFAULT 0,
                    matches_won INTEGER DEFAULT 0,
                    available INTEGER DEFAULT 1,
                    availability_reason TEXT,
                    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS leader_votes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    team_id INTEGER NOT NULL,
                    voter_id INTEGER NOT NULL,
                    candidate_id INTEGER NOT NULL,
                    vote_time TEXT NOT NULL,
                    active INTEGER DEFAULT 1,
                    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
                    FOREIGN KEY (voter_id) REFERENCES players(id) ON DELETE CASCADE,
                    FOREIGN KEY (candidate_id) REFERENCES players(id) ON DELETE CASCADE,
                    UNIQUE(team_id, voter_id, active)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tournaments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    game TEXT NOT NULL,
                    format TEXT,
                    start_date TEXT,
                    end_date TEXT,
                    prize_pool REAL,
                    status TEXT,
                    max_teams INTEGER
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tournament_registrations (
                    tournament_id INTEGER,
                    team_id INTEGER,
                    registration_date TEXT,
                    PRIMARY KEY (tournament_id, team_id),
                    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
                    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS matches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tournament_id INTEGER NOT NULL,
                    team1_id INTEGER NOT NULL,
                    team2_id INTEGER NOT NULL,
                    team1_score INTEGER DEFAULT 0,
                    team2_score INTEGER DEFAULT 0,
                    scheduled_time TEXT,
                    actual_start_time TEXT,
                    actual_end_time TEXT,
                    status TEXT,
                    round TEXT,
                    winner_id INTEGER,
                    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
                    FOREIGN KEY (team1_id) REFERENCES teams(id),
                    FOREIGN KEY (team2_id) REFERENCES teams(id),
                    FOREIGN KEY (winner_id) REFERENCES teams(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_match_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    match_id INTEGER NOT NULL,
                    player_id INTEGER NOT NULL,
                    kills INTEGER DEFAULT 0,
                    deaths INTEGER DEFAULT 0,
                    assists INTEGER DEFAULT 0,
                    damage_dealt INTEGER DEFAULT 0,
                    damage_taken INTEGER DEFAULT 0,
                    gold_earned INTEGER DEFAULT 0,
                    mvp INTEGER DEFAULT 0,
                    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_team ON players(team_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_players_available ON players(available)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_votes_team ON leader_votes(team_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_votes_active ON leader_votes(active)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_matches_tournament ON matches(tournament_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_match ON player_match_stats(match_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_player ON player_match_stats(player_id)");

        } finally {
            lock.writeLock().unlock();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }
}