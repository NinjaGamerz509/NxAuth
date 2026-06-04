package dev.nxauth.security;

import dev.nxauth.NxAuth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BruteForceProtection {

    private final NxAuth plugin;
    // IP -> fail count
    private final Map<String, Integer> failCounts = new ConcurrentHashMap<>();
    // IP -> blocked until timestamp
    private final Map<String, Long> blockedIps = new ConcurrentHashMap<>();

    public BruteForceProtection(NxAuth plugin) {
        this.plugin = plugin;
    }

    public void recordFailedAttempt(String ip) {
        int count = failCounts.getOrDefault(ip, 0) + 1;
        failCounts.put(ip, count);
    }

    public boolean isBlocked(String ip) {
        Long blockedUntil = blockedIps.get(ip);
        if (blockedUntil == null) return false;
        if (System.currentTimeMillis() < blockedUntil) return true;
        // Expired
        blockedIps.remove(ip);
        failCounts.remove(ip);
        return false;
    }

    public void tempban(String ip, int minutes) {
        long expires = System.currentTimeMillis() + (minutes * 60000L);
        blockedIps.put(ip, expires);
        failCounts.remove(ip);
        // Also persist in DB
        plugin.getDatabaseManager().addIpBan(ip, "Brute force protection", "NxAuth", expires);
    }

    public void clearAttempts(String ip) {
        failCounts.remove(ip);
    }
}
