# TiRania Bot

A general-purpose Discord bot built with **Java** and **JDA**.  
Currently includes a **music module**🎶, with more features planned in the future.

---

## Current Features

### Music
- Play or search tracks (`!play <url|search>`)
- Interactive search with button-based track selection (up to 5 results)
- Skip to next track (`!next`)
- Pause and resume (`!pause`, `!resume`)
- Stop and clear queue (`!stop`)
- Show current track (`!nowplaying`)
- Display queue (`!queue`)
- Auto-advance when a track ends

### Future Modules
- ⚔️ Moderation commands
- ⚡ Utility commands
- 🎉 Fun/social commands
- 🌐 Integration with external APIs

---

## 📜 Commands

| Command              | Description                       |
|----------------------|-----------------------------------|
| `!roll XdY`          | Roll one or multiple dice         |
| `!play <url\search>` | Play or search a track            |
| `!next`              | Skip to the next track            |
| `!queue`             | Display the current queue         |
| `!pause`             | Pause the currently playing track |
| `!resume`            | Resume playback                   |
| `!stop`              | Stop playback and clear the queue |
| `!nowplaying`        | Show the track currently playing  |
| `!help`              | Show the available commands       |

> More commands will be added as the bot grows.

---

## Setup

1. **Clone the repository:**

```bash
git clone git@github.com:niaou/tirania.git
cd tirania
```
2. **Set environment variable for bot token**  

> The bot reads the token from the `TIRANIA_TOKEN` environment variable.

```properties
tirania.token=${TIRANIA_TOKEN:}
```

3. **Build the project**:

```bash
./gradlew clean build
```

4. **Run the bot**:

```bash
java -jar build/libs/tirania-bot.jar
```

---

## Requirements

- Java 24+
- Gradle
- Discord bot token
