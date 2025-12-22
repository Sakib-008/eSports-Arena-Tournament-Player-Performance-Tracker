package com.esports.arena.util;

import java.time.LocalDate;
import java.util.Random;

import com.esports.arena.dao.LeaderVoteDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.model.Player;
import com.esports.arena.model.Team;
import com.esports.arena.model.Tournament;

/**
 * Utility class to generate sample data for testing and demonstration
 */
public class SampleDataGenerator {

    private final TeamDAO teamDAO;
    private final PlayerDAO playerDAO;
    private final Random random;

    private static final String[] TEAM_NAMES = {
            "Cloud9", "Team Liquid", "FaZe Clan", "G2 Esports", "Fnatic",
            "Team SoloMid", "100 Thieves", "Evil Geniuses", "OpTic Gaming"
    };

    private static final String[] PLAYER_NAMES = {
            "Faker", "Impact", "Jensen", "Doublelift", "Bjergsen",
            "Rekkles", "Caps", "Perkz", "Uzi", "TheShy",
            "Rookie", "Knight", "Chovy", "ShowMaker", "Canyon"
    };

    private static final String[] ROLES = {
            "Top", "Jungle", "Mid", "ADC", "Support", "Tank", "DPS", "Healer"
    };

    private static final String[] REGIONS = {
            "North America", "Europe", "Korea", "China", "Southeast Asia"
    };

    public SampleDataGenerator() {
        this.teamDAO = new TeamDAO();
        this.playerDAO = new PlayerDAO();
        this.random = new Random();
    }

    /**
     * Generate complete sample dataset
     */
    public void generateSampleData() {
        System.out.println("🎮 Starting Sample Data Generation...\n");

        try {
            // Generate teams first
            System.out.println("Creating teams...");
            int[] teamIds = generateTeams(5);
            System.out.println("✓ Created " + teamIds.length + " teams\n");

            // Generate players for each team
            System.out.println("Creating players...");
            int totalPlayers = 0;
            for (int teamId : teamIds) {
                int playerCount = generatePlayersForTeam(teamId, 5);
                totalPlayers += playerCount;
            }
            System.out.println("✓ Created " + totalPlayers + " players\n");

            // Set team leaders through voting
            System.out.println("Setting up team leaders...");
            for (int teamId : teamIds) {
                setupTeamLeader(teamId);
            }
            System.out.println("✓ Team leaders established\n");

            System.out.println("✅ Sample Data Generation Complete!");
            System.out.println("You can now use the application with pre-populated data.\n");

        } catch (Exception e) {
            System.err.println("❌ Error generating sample data: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * Generate sample teams
     */
    private int[] generateTeams(int count) {
        int[] teamIds = new int[count];

        for (int i = 0; i < count && i < TEAM_NAMES.length; i++) {
            Team team = new Team(
                    TEAM_NAMES[i],
                    TEAM_NAMES[i].substring(0, Math.min(3, TEAM_NAMES[i].length())).toUpperCase(),
                    REGIONS[random.nextInt(REGIONS.length)]
            );

            // Add some match history
            team.setWins(random.nextInt(20));
            team.setLosses(random.nextInt(15));
            team.setDraws(random.nextInt(5));

            teamIds[i] = teamDAO.createTeam(team);
            System.out.println("  • Created team: " + team.getName() + " [" + team.getTag() + "]");
        }

        return teamIds;
    }

    /**
     * Generate players for a specific team
     */
    private int generatePlayersForTeam(int teamId, int playerCount) {
        int created = 0;

        for (int i = 0; i < playerCount && i < PLAYER_NAMES.length; i++) {
            String username = PLAYER_NAMES[i] + random.nextInt(1000);
            String realName = generateRealName();
            String email = username.toLowerCase() + "@esports.com";
            String role = ROLES[random.nextInt(ROLES.length)];

            Player player = new Player(username, realName, email, role);
            // Set default password for sample players
            player.setPassword("password123");
            player.setTeamId(teamId);

            // Add some statistics
            player.setTotalKills(random.nextInt(500) + 100);
            player.setTotalDeaths(random.nextInt(400) + 50);
            player.setTotalAssists(random.nextInt(600) + 150);
            player.setMatchesPlayed(random.nextInt(100) + 20);
            player.setMatchesWon(random.nextInt(player.getMatchesPlayed()));

            // Random availability
            player.setAvailable(random.nextDouble() > 0.2); // 80% available
            if (!player.isAvailable()) {
                String[] reasons = {"Vacation", "Injury", "Personal matters", "Scheduled break"};
                player.setAvailabilityReason(reasons[random.nextInt(reasons.length)]);
            }

            int playerId = playerDAO.createPlayer(player);
            if (playerId > 0) {
                created++;
                System.out.println("  • Created player: " + player.getUsername() +
                        " (" + role + ") - Team ID: " + teamId);
            }
        }

        return created;
    }

    /**
     * Setup team leader through simulated voting
     */
    private void setupTeamLeader(int teamId) {
        try {
            LeaderVoteDAO voteDAO = new LeaderVoteDAO();
            var players = playerDAO.getPlayersByTeam(teamId);

            if (players.isEmpty()) return;

            // Pick a random player as the popular choice
            Player leader = players.get(random.nextInt(players.size()));

            // Simulate voting - most votes for the leader
            for (Player voter : players) {
                if (voter.getId() == leader.getId()) continue; // Don't vote for self

                // 70% vote for leader, 30% vote for someone else
                Player candidate = random.nextDouble() < 0.7 ?
                        leader : players.get(random.nextInt(players.size()));

                voteDAO.castVote(teamId, voter.getId(), candidate.getId());
            }

            // Update team with elected leader
            Integer electedLeader = voteDAO.getCurrentLeader(teamId);
            if (electedLeader != null) {
                teamDAO.updateTeamLeader(teamId, electedLeader);
                System.out.println("  • Team " + teamId + " leader: " +
                        playerDAO.getPlayerById(electedLeader).getUsername());
            }

            voteDAO.shutdown();

        } catch (Exception e) {
            System.err.println("Error setting up leader for team " + teamId + ": " + e.getMessage());
        }
    }

    /**
     * Generate random real name
     */
    private String generateRealName() {
        String[] firstNames = {"John", "Jane", "Michael", "Emily", "David", "Sarah",
                "James", "Lisa", "Robert", "Maria"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones",
                "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};

        return firstNames[random.nextInt(firstNames.length)] + " " +
                lastNames[random.nextInt(lastNames.length)];
    }

    /**
     * Generate sample tournaments
     */
    public void generateTournaments() {
        System.out.println("\n🏆 Generating sample tournaments...");

        String[] games = {"League of Legends", "CS:GO", "Dota 2", "Valorant", "Overwatch"};
        String[] formats = {"Single Elimination", "Double Elimination", "Round Robin"};

        for (int i = 0; i < 3; i++) {
            Tournament tournament = new Tournament(
                    games[i] + " Championship 2025",
                    games[i],
                    formats[random.nextInt(formats.length)],
                    LocalDate.now().plusDays(random.nextInt(30)),
                    LocalDate.now().plusDays(random.nextInt(30) + 30),
                    random.nextDouble() * 100000 + 50000, // $50k-$150k prize pool
                    16
            );

            tournament.setStatus(Tournament.TournamentStatus.REGISTRATION_OPEN);
            System.out.println("  • Created tournament: " + tournament.getName());
        }
    }

    /**
     * Clean shutdown
     */
    private void shutdown() {
        teamDAO.shutdown();
        playerDAO.shutdown();
    }

    /**
     * Main method to run data generation
     */
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   eSports Arena - Sample Data Generator  ║");
        System.out.println("╚═══════════════════════════════════════════╝\n");

        SampleDataGenerator generator = new SampleDataGenerator();
        generator.generateSampleData();

        // Optionally generate tournaments
        if (args.length > 0 && args[0].equals("--with-tournaments")) {
            generator.generateTournaments();
        }

        System.out.println("\n💡 Tip: Run the application to see the generated data!");
        System.out.println("Command: mvn javafx:run\n");
    }
}