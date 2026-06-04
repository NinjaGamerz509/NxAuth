package dev.nxauth.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.nxauth.NxAuth;
import dev.nxauth.config.ConfigManager;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final NxAuth plugin;
    private HikariDataSource dataSource;
    private String dbType;

    public DatabaseManager(NxAuth plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        ConfigManager cfg = plugin.getConfigManager();
        dbType = cfg.getDbType().toLowerCase();

        HikariConfig config = new HikariConfig();

        if (dbType.equals("mysql")) {
            config.setJdbcUrl("jdbc:mysql://" + cfg.getMysqlHost() + ":" + cfg.getMysqlPort()
                + "/" + cfg.getMysqlDatabase() + "?useSSL=false&autoReconnect=true&characterEncoding=UTF-8");
            config.setUsername(cfg.getMysqlUsername());
            config.setPassword(cfg.getMysqlPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(cfg.getMysqlPoolSize());
            config.setConnectionTimeout(30000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
        } else {
            // SQLite
            File dbFile = new File(plugin.getDataFolder(), cfg.getSqliteFile());
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1); // SQLite is single-threaded
            config.setConnectionTimeout(10000);
        }

        config.setPoolName("NxAuth-Pool");
        config.setLeakDetectionThreshold(60000);

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            plugin.getLogger().info("Database connected! Type: " + dbType.toUpperCase());
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection failed!", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Players table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nxauth_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    email VARCHAR(255),
                    ip_address VARCHAR(45),
                    last_login BIGINT DEFAULT 0,
                    last_location TEXT,
                    registered_at BIGINT DEFAULT 0,
                    register_ip VARCHAR(45),
                    is_premium BOOLEAN DEFAULT FALSE,
                    is_bedrock BOOLEAN DEFAULT FALSE,
                    language VARCHAR(5) DEFAULT 'en',
                    totp_secret VARCHAR(255),
                    totp_enabled BOOLEAN DEFAULT FALSE,
                    backup_codes TEXT,
                    security_score INT DEFAULT 0,
                    is_banned BOOLEAN DEFAULT FALSE,
                    ban_expires BIGINT DEFAULT 0,
                    ban_reason VARCHAR(255)
                )
            """);

            // Sessions table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nxauth_sessions (
                    uuid VARCHAR(36) PRIMARY KEY,
                    ip_address VARCHAR(45) NOT NULL,
                    session_token VARCHAR(255),
                    created_at BIGINT DEFAULT 0,
                    expires_at BIGINT DEFAULT 0
                )
            """);

            // Login logs table
            String autoInc = dbType.equals("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT";
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS nxauth_logs (" +
                "    id INTEGER PRIMARY KEY " + autoInc + "," +
                "    uuid VARCHAR(36)," +
                "    username VARCHAR(16)," +
                "    event_type VARCHAR(50)," +
                "    ip_address VARCHAR(45)," +
                "    success BOOLEAN," +
                "    details TEXT," +
                "    timestamp BIGINT" +
                ")"
            );

            // IP bans table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nxauth_ipbans (
                    ip_address VARCHAR(45) PRIMARY KEY,
                    reason VARCHAR(255),
                    banned_by VARCHAR(36),
                    banned_at BIGINT,
                    expires_at BIGINT DEFAULT 0
                )
            """);

            // Failed attempts table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nxauth_attempts (
                    ip_address VARCHAR(45) PRIMARY KEY,
                    count INT DEFAULT 0,
                    last_attempt BIGINT DEFAULT 0,
                    blocked_until BIGINT DEFAULT 0
                )
            """);
        }
    }

    // =================== PLAYER CRUD ===================

    public boolean isRegistered(String uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM nxauth_players WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "isRegistered error", e);
            return false;
        }
    }

    public boolean registerPlayer(String uuid, String username, String passwordHash, String ip) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO nxauth_players (uuid, username, password_hash, ip_address, registered_at, register_ip, last_login, security_score) VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, uuid);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.setString(4, ip);
            ps.setLong(5, System.currentTimeMillis());
            ps.setString(6, ip);
            ps.setLong(7, System.currentTimeMillis());
            ps.setInt(8, 30); // Base security score
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "registerPlayer error", e);
            return false;
        }
    }

    public String getPasswordHash(String uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT password_hash FROM nxauth_players WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("password_hash");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "getPasswordHash error", e);
        }
        return null;
    }

    public void updateLastLogin(String uuid, String ip) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE nxauth_players SET last_login=?, ip_address=? WHERE uuid=?")) {
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, ip);
            ps.setString(3, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "updateLastLogin error", e);
        }
    }

    public void updatePassword(String uuid, String newHash) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE nxauth_players SET password_hash=? WHERE uuid=?")) {
            ps.setString(1, newHash);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "updatePassword error", e);
        }
    }

    public boolean unregisterPlayer(String uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM nxauth_players WHERE uuid=?")) {
            ps.setString(1, uuid);
            ps.executeUpdate();
            // Also delete session
            try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM nxauth_sessions WHERE uuid=?")) {
                ps2.setString(1, uuid);
                ps2.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "unregisterPlayer error", e);
            return false;
        }
    }

    public String getLastIp(String uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ip_address FROM nxauth_players WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("ip_address");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "getLastIp error", e);
        }
        return null;
    }

    public int countAccountsByIp(String ip) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM nxauth_players WHERE register_ip=?")) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "countAccountsByIp error", e);
        }
        return 0;
    }

    public long getTotalPlayers() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM nxauth_players")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "getTotalPlayers error", e);
        }
        return 0;
    }

    // =================== 2FA ===================

    public void setTotpSecret(String uuid, String secret) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE nxauth_players SET totp_secret=?, totp_enabled=TRUE WHERE uuid=?")) {
            ps.setString(1, secret);
            ps.setString(2, uuid);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "setTotpSecret error", e);
        }
    }

    public String getTotpSecret(String uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT totp_secret FROM nxauth_players WHERE uuid=? AND totp_enabled=TRUE")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("totp_secret");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "getTotpSecret error", e);
        }
        return null;
    }

    public boolean is2faEnabled(String uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT totp_enabled FROM nxauth_players WHERE uuid=?")) {
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("totp_enabled");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "is2faEnabled error", e);
        }
        return false;
    }

    // =================== LOGGING ===================

    public void logEvent(String uuid, String username, String eventType, String ip, boolean success, String details) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO nxauth_logs (uuid, username, event_type, ip_address, success, details, timestamp) VALUES (?,?,?,?,?,?,?)")) {
            ps.setString(1, uuid);
            ps.setString(2, username);
            ps.setString(3, eventType);
            ps.setString(4, ip);
            ps.setBoolean(5, success);
            ps.setString(6, details);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "logEvent error", e);
        }
    }

    // =================== IP BANS ===================

    public boolean isIpBanned(String ip) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT expires_at FROM nxauth_ipbans WHERE ip_address=?")) {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long expires = rs.getLong("expires_at");
                if (expires == 0) return true; // Permanent
                if (expires > System.currentTimeMillis()) return true;
                // Expired, remove
                removeIpBan(ip);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "isIpBanned error", e);
        }
        return false;
    }

    public void addIpBan(String ip, String reason, String bannedBy, long expiresAt) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO nxauth_ipbans (ip_address, reason, banned_by, banned_at, expires_at) VALUES (?,?,?,?,?)")) {
            ps.setString(1, ip);
            ps.setString(2, reason);
            ps.setString(3, bannedBy);
            ps.setLong(4, System.currentTimeMillis());
            ps.setLong(5, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "addIpBan error", e);
        }
    }

    public void removeIpBan(String ip) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM nxauth_ipbans WHERE ip_address=?")) {
            ps.setString(1, ip);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "removeIpBan error", e);
        }
    }

    // =================== UTILITY ===================

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public String getDbType() {
        return dbType;
    }
}
