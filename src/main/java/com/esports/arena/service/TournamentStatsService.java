package com.esports.arena.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.model.Match;
import com.esports.arena.model.PlayerMatchStats;

/**
 * Service to calculate tournament-based statistics for players and teams
 */
public class TournamentStatsService {
    private final MatchDAO matchDAO;
    private final PlayerDAO playerDAO;

    public TournamentStatsService() {
        this.matchDAO = new MatchDAO();
        this.playerDAO = new PlayerDAO();
    }

    /**
     * Get tournament statistics for a player
     */
    public TournamentPlayerStats getPlayerTournamentStats(int playerId, int tournamentId) {
        TournamentPlayerStats stats = new TournamentPlayerStats();
        
        // Get all matches for this tournament
        List<Match> tournamentMatches = matchDAO.getMatchesByTournament(tournamentId);
        
        int kills = 0, deaths = 0, assists = 0, matchesPlayed = 0, matchesWon = 0;
        
        for (Match match : tournamentMatches) {
            // Get player stats for this match
            List<PlayerMatchStats> matchStats = matchDAO.getPlayerStatsByMatch(match.getId());
            
            for (PlayerMatchStats pms : matchStats) {
                if (pms.getPlayerId() == playerId) {
                    kills += pms.getKills();
                    deaths += pms.getDeaths();
                    assists += pms.getAssists();
                    matchesPlayed++;
                    
                    // Check if player's team won
                    if (match.getWinnerId() != null) {
                        com.esports.arena.model.Player player = playerDAO.getPlayerById(playerId);
                        if (player != null && player.getTeamId() != null && 
                            player.getTeamId().equals(match.getWinnerId())) {
                            matchesWon++;
                        }
                    }
                    break;
                }
            }
        }
        
        stats.kills = kills;
        stats.deaths = deaths;
        stats.assists = assists;
        stats.matchesPlayed = matchesPlayed;
        stats.matchesWon = matchesWon;
        
        return stats;
    }

    /**
     * Get tournament statistics for a team
     */
    public TournamentTeamStats getTeamTournamentStats(int teamId, int tournamentId) {
        TournamentTeamStats stats = new TournamentTeamStats();
        
        List<Match> tournamentMatches = matchDAO.getMatchesByTournament(tournamentId);
        
        int wins = 0, losses = 0, draws = 0;
        
        for (Match match : tournamentMatches) {
            if (match.getTeam1Id() == teamId || match.getTeam2Id() == teamId) {
                if (match.getStatus() == Match.MatchStatus.COMPLETED) {
                    if (match.getWinnerId() != null) {
                        if (match.getWinnerId() == teamId) {
                            wins++;
                        } else {
                            losses++;
                        }
                    } else {
                        draws++;
                    }
                }
            }
        }
        
        stats.wins = wins;
        stats.losses = losses;
        stats.draws = draws;
        stats.matchesPlayed = wins + losses + draws;
        
        return stats;
    }

    /**
     * Get all players' tournament stats
     */
    public Map<Integer, TournamentPlayerStats> getAllPlayersTournamentStats(int tournamentId) {
        Map<Integer, TournamentPlayerStats> statsMap = new HashMap<>();
        List<com.esports.arena.model.Player> players = playerDAO.getAllPlayers();
        
        for (com.esports.arena.model.Player player : players) {
            statsMap.put(player.getId(), getPlayerTournamentStats(player.getId(), tournamentId));
        }
        
        return statsMap;
    }

    public static class TournamentPlayerStats {
        public int kills;
        public int deaths;
        public int assists;
        public int matchesPlayed;
        public int matchesWon;

        public double getKdRatio() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }

        public double getWinRate() {
            return matchesPlayed == 0 ? 0 : (double) matchesWon / matchesPlayed * 100;
        }

        public double getKdaRatio() {
            return deaths == 0 ? (kills + assists) : (double) (kills + assists) / deaths;
        }
    }

    public static class TournamentTeamStats {
        public int wins;
        public int losses;
        public int draws;
        public int matchesPlayed;

        public double getWinRate() {
            return matchesPlayed == 0 ? 0 : (double) wins / matchesPlayed * 100;
        }
    }
}

