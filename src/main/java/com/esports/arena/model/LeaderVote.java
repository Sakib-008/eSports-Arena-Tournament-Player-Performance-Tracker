package com.esports.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;

public class LeaderVote {
    @JsonProperty("id")
    private int id;

    @JsonProperty("teamId")
    private int teamId;

    @JsonProperty("voterId")
    private int voterId;

    @JsonProperty("candidateId")
    private int candidateId;

    @JsonProperty("voteTime")
    private LocalDateTime voteTime;

    @JsonProperty("active")
    private boolean active;

    public LeaderVote() {
        this.voteTime = LocalDateTime.now();
        this.active = true;
    }

    public LeaderVote(int teamId, int voterId, int candidateId) {
        this();
        this.teamId = teamId;
        this.voterId = voterId;
        this.candidateId = candidateId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTeamId() { return teamId; }
    public void setTeamId(int teamId) { this.teamId = teamId; }

    public int getVoterId() { return voterId; }
    public void setVoterId(int voterId) { this.voterId = voterId; }

    public int getCandidateId() { return candidateId; }
    public void setCandidateId(int candidateId) { this.candidateId = candidateId; }

    public LocalDateTime getVoteTime() { return voteTime; }
    public void setVoteTime(LocalDateTime voteTime) { this.voteTime = voteTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaderVote that = (LeaderVote) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Vote for Candidate #" + candidateId + " by Voter #" + voterId;
    }
}

class VoteResult {
    private int candidateId;
    private String candidateName;
    private int voteCount;

    public VoteResult(int candidateId, String candidateName, int voteCount) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.voteCount = voteCount;
    }

    public int getCandidateId() { return candidateId; }
    public void setCandidateId(int candidateId) { this.candidateId = candidateId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }

    @Override
    public String toString() {
        return candidateName + ": " + voteCount + " votes";
    }
}