package com.esports.arena.dao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.esports.arena.model.Organizer;
import com.esports.arena.service.RealtimeDatabaseService;

public class OrganizerDAO {
    private static final String COLLECTION = "organizers";

    private final ExecutorService executor;

    public OrganizerDAO() {
        this.executor = Executors.newFixedThreadPool(2);
    }

    public Organizer getOrganizerByUsername(String username) {
        return getAllOrganizers().stream()
                .filter(o -> o.getUsername() != null && o.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);
    }

    public Organizer getOrganizerById(int id) {
        try {
            return RealtimeDatabaseService.read(path(id), Organizer.class);
        } catch (Exception e) {
            System.err.println("Error fetching organizer by id: " + e.getMessage());
            return null;
        }
    }

    public List<Organizer> getAllOrganizers() {
        try {
            Map<String, Organizer> map = RealtimeDatabaseService.readCollection(COLLECTION, Organizer.class);
            if (map == null) {
                return new ArrayList<>();
            }
            return map.values().stream()
                    .sorted(Comparator.comparing(Organizer::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error fetching all organizers: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean createOrganizer(Organizer organizer) {
        try {
            long nextId = RealtimeDatabaseService.nextId("counters/organizers");
            int id = Math.toIntExact(nextId);
            organizer.setId(id);
            organizer.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            RealtimeDatabaseService.write(path(id), organizer);
            return true;
        } catch (Exception e) {
            System.err.println("Error creating organizer: " + e.getMessage());
            return false;
        }
    }

    public boolean updateOrganizer(Organizer organizer) {
        try {
            RealtimeDatabaseService.write(path(organizer.getId()), organizer);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating organizer: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteOrganizer(int id) {
        try {
            RealtimeDatabaseService.delete(path(id));
            return true;
        } catch (Exception e) {
            System.err.println("Error deleting organizer: " + e.getMessage());
            return false;
        }
    }

    public Organizer authenticateOrganizer(String username, String password) {
        Organizer organizer = getOrganizerByUsername(username);
        if (organizer != null && organizer.getPassword() != null && organizer.getPassword().equals(password)) {
            return organizer;
        }
        return null;
    }

    public CompletableFuture<Organizer> authenticateOrganizerAsync(String username, String password) {
        return CompletableFuture.supplyAsync(() -> authenticateOrganizer(username, password), executor);
    }

    private String path(int id) {
        return COLLECTION + "/" + id;
    }
}
