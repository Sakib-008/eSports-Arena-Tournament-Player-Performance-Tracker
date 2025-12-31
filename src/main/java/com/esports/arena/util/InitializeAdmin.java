package com.esports.arena.util;

import com.esports.arena.dao.OrganizerDAO;
import com.esports.arena.model.Organizer;

/**
 * Utility to create the first admin organizer.
 * Run this once to initialize your admin account in Firebase.
 */
public class InitializeAdmin {
    public static void main(String[] args) {
        System.out.println("Creating default admin organizer...");
        
        OrganizerDAO organizerDAO = new OrganizerDAO();
        
        // Check if admin already exists
        Organizer existing = organizerDAO.getOrganizerByUsername("admin");
        if (existing != null) {
            System.out.println("Admin already exists with ID: " + existing.getId());
            System.out.println("Username: " + existing.getUsername());
            System.out.println("Email: " + existing.getEmail());
            return;
        }
        
        // Create default admin
        Organizer admin = new Organizer();
        admin.setUsername("admin");
        admin.setPassword("admin123"); // Change this password after first login!
        admin.setEmail("admin@esports-arena.com");
        admin.setFullName("System Administrator");
        
        boolean success = organizerDAO.createOrganizer(admin);
        
        if (success) {
            System.out.println("✓ Admin organizer created successfully!");
            System.out.println("Username: admin");
            System.out.println("Password: admin123");
            System.out.println("⚠ IMPORTANT: Change this password after first login!");
        } else {
            System.err.println("✗ Failed to create admin organizer.");
        }
    }
}
