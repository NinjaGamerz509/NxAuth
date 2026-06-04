package dev.nxauth.backup;

import dev.nxauth.NxAuth;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;

public class BackupManager {

    private final NxAuth plugin;

    public BackupManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public boolean createBackup() {
        try {
            String backupPath = plugin.getConfigManager().getBackupPath();
            File backupDir = new File(backupPath);
            if (!backupDir.exists()) backupDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String fileName = "nxauth_backup_" + timestamp;

            if (plugin.getConfigManager().isBackupCompressed()) {
                fileName += ".zip";
                createZipBackup(backupDir, fileName);
            } else {
                fileName += ".db";
                createRawBackup(backupDir, fileName);
            }

            plugin.getLogger().info("Backup created: " + fileName);
            cleanOldBackups(backupDir);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Backup failed: " + e.getMessage());
            return false;
        }
    }

    private void createZipBackup(File backupDir, String fileName) throws IOException {
        File dbFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getSqliteFile());
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        File outputZip = new File(backupDir, fileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZip))) {
            // Add database
            if (dbFile.exists()) addFileToZip(zos, dbFile, "nxauth.db");
            // Add config
            if (configFile.exists()) addFileToZip(zos, configFile, "config.yml");
        }
    }

    private void createRawBackup(File backupDir, String fileName) throws IOException {
        File dbFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getSqliteFile());
        if (!dbFile.exists()) {
            plugin.getLogger().warning("Database file not found for backup.");
            return;
        }
        Files.copy(dbFile.toPath(), new File(backupDir, fileName).toPath(),
            StandardCopyOption.REPLACE_EXISTING);
    }

    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        zos.closeEntry();
    }

    private void cleanOldBackups(File backupDir) {
        int keepLast = plugin.getConfigManager().getBackupKeepLast();
        File[] backups = backupDir.listFiles(f -> f.getName().startsWith("nxauth_backup_"));
        if (backups == null || backups.length <= keepLast) return;

        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
        int toDelete = backups.length - keepLast;
        for (int i = 0; i < toDelete; i++) {
            if (backups[i].delete()) {
                plugin.getLogger().info("Deleted old backup: " + backups[i].getName());
            }
        }
    }

    public List<String> listBackups() {
        File backupDir = new File(plugin.getConfigManager().getBackupPath());
        List<String> result = new ArrayList<>();
        if (!backupDir.exists()) return result;
        File[] files = backupDir.listFiles(f -> f.getName().startsWith("nxauth_backup_"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
            for (File f : files) result.add(f.getName() + " (" + formatSize(f.length()) + ")");
        }
        return result;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
