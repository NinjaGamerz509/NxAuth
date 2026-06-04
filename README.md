# 🔐 NxAuth — Ultimate Minecraft Auth Plugin

<div align="center">

![NxAuth Banner](https://img.shields.io/badge/NxAuth-Ultimate%20Auth-blue?style=for-the-badge&logo=minecraft)
![Version](https://img.shields.io/badge/version-1.0.0-green?style=for-the-badge)
![Paper](https://img.shields.io/badge/Paper-1.21+-orange?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21+-red?style=for-the-badge)
![Build](https://img.shields.io/github/actions/workflow/status/yourusername/NxAuth/build.yml?style=for-the-badge)

**The most advanced authentication plugin for Paper Minecraft servers.**

</div>

---

## ✨ Features

### 🔐 Core Authentication
- `/register <password> <confirm>` — Secure registration with BCrypt hashing
- `/login <password>` — Fast login with session memory
- `/logout` — Manual logout
- `/changepassword <old> <new>` — Self-service password change
- IP session memory — same IP = auto re-login (configurable)
- Login timeout → auto kick with countdown

### 🎨 Custom Join Experience
- Player **freeze** until logged in (no movement possible)
- **Blindness effect** while not authenticated
- **Inventory hidden** until login
- **Hidden from other players** until logged in
- Teleport to custom auth location on join
- Custom join titles and prompts

### 🌈 Animated BossBar
- Countdown timer with color transitions (Green → Yellow → Red)
- Pulsing animation
- Remaining seconds shown in real-time
- Warning sound at 10 seconds remaining

### 🔊 Sound Effects
- Login success / fail
- Register success + first-time firework 🎆
- Timeout warning bell
- Wrong password sound
- Captcha success / fail

### 🎭 Login Animations
- **Particle burst** on login success (configurable particle type)
- **Firework display** on first-time registration
- **Cinematic camera path** on join (optional)

### 🔐 Security
- **BCrypt** password hashing (industry standard)
- **Brute force protection** — IP tempban after X failed attempts
- **Anti-bot** — join rate limiter with auto-lockdown
- **CAPTCHA** — math questions before register/login
- **2FA (TOTP)** — Google Authenticator support
- **IP ban / unban** system
- **Multiple accounts per IP** control
- Weak password blocking

### 📱 Premium & Bedrock Support
- **Premium auto-login** — paid Minecraft accounts skip auth
- **Bedrock auto-login** — Geyser/Floodgate players auto-authenticated
- Configurable via `config.yml` — enable/disable separately

### 🌍 Per-World Authentication
- Define which worlds require authentication
- Bypass worlds (creative, admin)
- Optional re-auth when switching worlds

### 🚧 Maintenance Mode
- Toggle via command or web dashboard
- Auto-kick non-admin players
- Custom maintenance MOTD
- Admin bypass permission

### 📺 Dynamic MOTD
- Different MOTD for registered / unregistered / maintenance states
- Live placeholder support: `{online}`, `{max}`, `{registered}`
- Animated MOTD support

### 🔔 Discord Webhooks
- New registration alerts
- Failed login notifications
- Max attempts / tempban alerts
- Maintenance toggle alerts
- Daily stats summary
- Beautiful embed format with color coding

### 🎮 In-Game Admin Alerts
- ActionBar / Chat / Title format (configurable)
- Failed logins, bot attacks, suspicious activity
- Real-time notifications to all admins

### 🌐 Web Dashboard
- Runs directly on your server IP + port
- Player manager (online/offline, force login, unregister)
- Config editor — change ANY setting from browser
- Login logs with filtering
- IP ban management
- Backup management
- Export players to CSV
- Maintenance toggle
- Beautiful dark theme UI

### 💾 Backup & Recovery
- Automatic scheduled backups (every X hours)
- Compressed `.zip` backups (includes config + DB)
- Auto-cleanup of old backups (keep last N)
- Web dashboard restore
- CSV export of all player data

### 🗣️ Multi-Language
- Built-in: English 🇬🇧, Hindi 🇮🇳, Spanish 🇪🇸
- Add custom languages via `languages/messages_XX.yml`
- Per-player language with `/language <code>`
- Auto-detection option

---

## 📥 Installation

1. Download `NxAuth-X.X.X.jar` from [Releases](../../releases)
2. Place in `plugins/` folder
3. Restart server
4. Configure `plugins/NxAuth/config.yml`
5. Access dashboard at `http://YOUR_SERVER_IP:8080`

---

## ⚙️ Requirements

| Requirement | Version |
|---|---|
| Paper / Purpur | 1.21.x+ |
| Java | 21+ |
| Database | SQLite (built-in) or MySQL |

### Optional
- **Geyser + Floodgate** — Bedrock auto-login
- **MaxMind GeoLite2** — GeoIP country blocking

---

## 📋 Commands

| Command | Description | Permission |
|---|---|---|
| `/login <password>` | Login | - |
| `/register <pass> <confirm>` | Register | - |
| `/logout` | Logout | - |
| `/changepassword <old> <new>` | Change password | - |
| `/language <code>` | Change language | - |
| `/2fa <enable\|disable>` | Manage 2FA | - |
| `/nxauth reload` | Reload config | `nxauth.admin` |
| `/nxauth forcelogin <player>` | Force login | `nxauth.admin` |
| `/nxauth unregister <player>` | Delete account | `nxauth.admin` |
| `/nxauth tempban <player> <min>` | Temp ban | `nxauth.admin` |
| `/nxauth maintenance <on\|off>` | Maintenance | `nxauth.admin` |
| `/nxauth stats` | View stats | `nxauth.admin` |
| `/nxauth backup` | Create backup | `nxauth.admin` |

---

## 🏗️ Build from Source

```bash
git clone https://github.com/yourusername/NxAuth.git
cd NxAuth
mvn clean package
# JAR will be in target/
```

Or push a tag to trigger GitHub Actions auto-build:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 📁 File Structure (after first run)

```
plugins/NxAuth/
├── config.yml          # Main configuration
├── nxauth.db           # SQLite database (default)
├── languages/
│   ├── messages_en.yml
│   ├── messages_hi.yml
│   └── messages_es.yml
├── backups/            # Auto backups stored here
│   └── nxauth_backup_2024-01-01_12-00-00.zip
├── logs/
│   └── auth.log
└── icons/              # Custom server icons per state
```

---

## 🤝 Contributing

PRs welcome! Please open an issue first for major changes.

---

## 📜 License

MIT License — Free to use and modify.

---

<div align="center">
Made with ❤️ by the NxAuth Team
</div>
