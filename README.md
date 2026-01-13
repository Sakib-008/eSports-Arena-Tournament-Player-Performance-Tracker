# eSports Arena

Desktop JavaFX application for managing eSports tournaments, teams, matches, and player performance. Organizers can curate competitions and export/import data; players can log in to track their stats, teams, and vote for team leaders. Data is persisted to Firebase Realtime Database.

## Features
- Organizer dashboard: manage players, teams, tournaments, matches, and leaderboards from a tabbed UI.
- Player dashboard: profile viewer, personal stats, team roster view, leader voting, and global leaderboard.
- Tournament management: team registration caps, match scheduling, live/completed status tracking, winners, and standings.
- Performance tracking: per-match player stats (K/D/A, damage, gold, MVP), cumulative player totals, team records, and computed win rates.
- Export/import: JSON export/import of players, teams, tournaments, matches, and embedded player stats from the organizer dashboard.
- Firebase-backed: REST-based CRUD with optimistic counter increments for IDs, plus optional .env-driven secrets handling.

## Tech Stack
- Java 25 (source/target in Maven)
- JavaFX 21 (controls, FXML, web, media, swing), ControlsFX, TilesFX, Ikonli
- Maven with `javafx-maven-plugin`
- Firebase Realtime Database (REST)
- Jackson (databind, jsr310) for JSON
- JUnit 5 for tests

## Project Structure
- `src/main/java/com/esports/arena/MainApp.java` — JavaFX entry point and scene navigation.
- `src/main/java/com/esports/arena/dao/*` — Data access for players, teams, tournaments, matches, votes (Firebase-backed).
- `src/main/java/com/esports/arena/model/*` — Domain models for users, teams, tournaments, matches, and stats.
- `src/main/java/com/esports/arena/tabs/*` — Tab controllers for organizer/player dashboards.
- `src/main/java/com/esports/arena/service/*` — Firebase client, JSON import/export, and stat aggregation.
- `src/main/resources/fxml/**` — FXML layouts for screens and tabs; `css/` for styling.

## Prerequisites
- JDK 25 (or newer JDK matching the Maven `source`/`target` values)
- Maven 3.9+ (or use the included Maven wrapper `mvnw`/`mvnw.cmd`)
- Internet access to reach Firebase Realtime Database

## Configuration
The Firebase database URL is set in `RealtimeDatabaseService` and expects an auth token.

Set the token via environment variable or `.env` file:

```bash
# .env (place next to pom.xml)
FIREBASE_DB_TOKEN=your_firebase_database_secret
```

At runtime the app loads `FIREBASE_DB_TOKEN` from the OS environment first, then from `.env` via `EnvLoader`.

## Run the App
```bash
# Windows (wrapper)
./mvnw.cmd clean javafx:run

# Or with system Maven
mvn clean javafx:run
```

The main window opens with the main menu. Choose Organizer (login), Player Login, or Player Signup to reach the dashboards.

## Running Tests
```bash
mvn test
```

## Data Export/Import (Organizer Dashboard)
- **Export**: click Export Data, choose a JSON file; all players, teams, tournaments, matches, and embedded player stats are written.
- **Import**: click Import Data and pick a JSON file; data is added to Firebase (existing IDs may collide).

## Notes
- IDs are generated with Firebase counters using optimistic ETag updates; avoid parallel imports that share the same counters.
- The legacy SQLite backend was removed; use the Firebase-backed DAOs only.
- If JavaFX fails to launch, verify JAVA_HOME points to a JDK (not JRE) that matches the project version and supports JavaFX.

## Contributing
This is a course project for CSE 2200 (Advanced Programming Laboratory) at Khulna University of Engineering and Technology (KUET). 

**Course**: Advanced Programming Laboratory (CSE 2200)  
**Project Type**: Android Application  
**Last Updated**: January 2026
