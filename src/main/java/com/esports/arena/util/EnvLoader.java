package com.esports.arena.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {
    private static final Map<String, String> envVars = new HashMap<>();
    private static boolean loaded = false;

    public static String get(String key) {
        if (!loaded) {
            loadEnv();
        }
        // First check system environment
        String systemValue = System.getenv(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        // Then check .env file
        return envVars.get(key);
    }

    private static synchronized void loadEnv() {
        if (loaded) {
            return;
        }
        
        // Try to find .env file in project root
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            // Try parent directory (for when running from different working directories)
            envPath = Paths.get("../.env");
            if (!Files.exists(envPath)) {
                System.out.println("Note: .env file not found, using system environment variables only");
                loaded = true;
                return;
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    envVars.put(key, value);
                }
            }
            System.out.println("Loaded " + envVars.size() + " environment variables from .env file");
        } catch (IOException e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
        }
        
        loaded = true;
    }
}
