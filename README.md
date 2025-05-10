# BeatClick - Music Rhythm Game

**Final Project for Java Course**
By Suwen Wang (sw6359), Yifei Xu (yx3590), Ruqing Liu (rl5256)

## Project Overview

"BeatClick" is a music-based timing game inspired by titles like Piano Tiles. Players tap or click corresponding notes in sync with the background music. The game uses multithreading to manage music playback, user input handling, and real-time animations, ensuring each component stays perfectly in sync.

## Key Features

1. **Multi-Threaded Core**

   * Separate threads for music playback, note generation, input detection, and animations
   * Time synchronization mechanism to keep each subsystem in perfect rhythm
2. **Graphical User Interface**

   * Eye-catching visual effects that respond to successful hits or misses
   * Smooth animations that align with song tempo and note patterns
3. **Score Tracking & Database**

   * Local high score management stored in an SQLite database

## Technical Implementation

### Multi-Threading Architecture

The game utilizes Java's concurrency framework to manage several threads:

- **Music Player Thread**: Handles audio playback and synchronization
- **Note Generator Thread**: Creates and positions notes based on the song's rhythm
- **Input Processor Thread**: Processes user input events (keyboard and mouse)
- **Animation Controller Thread**: Manages all visual animations and updates

### Classes Overview

1. **BeatClick.java**: Main application class and entry point
2. **GameManager.java**: Coordinates all game components and thread management
3. **GameState.java**: Manages the current state of the game (score, notes, etc.)
4. **Note.java**: Represents a single note in the game
5. **MusicPlayer.java**: Handles music playback in a separate thread
6. **NoteGenerator.java**: Generates notes based on the song's rhythm
7. **InputProcessor.java**: Processes player input events
8. **InputHandler.java**: Captures keyboard and mouse input
9. **AnimationController.java**: Manages game animations
10. **GamePanel.java**: Renders the game graphics
11. **MenuPanel.java**: Implements the main menu
12. **DatabaseManager.java**: Manages score persistence using SQLite

## Custom Song Import Support

BeatClick allows players to easily add their own songs and note patterns:

1. **WAV File Import**
   Click the "+" button and import the .wav file. The system will generate notes for you.
3. **No Code Changes Needed**
   The system is fully dynamic —— no need to recompile or edit Java files to add songs.

## Setup and Running

### Prerequisites

- Java 11 or higher
- Gradle (for building)

### Building the Project

```bash
# Build with Gradle
./gradle build
```
or
```bash
gradle build
```

### Running the Game

```bash
./gradle run
or
gradle run
```

### Adding Custom Songs

1. Place your WAV audio files in the `assets/songs` directory
2. The game will automatically detect and list them in the menu
3. For custom note patterns, create a matching `.notes` file with the same name as your WAV file

## Controls

- **D, F, J, K**: Hit notes in lanes 1, 2, 3, and 4 respectively
- **Mouse Click**: Click on the lane to hit notes. Use Pause button to Pause/Resume the game, the note and track appearance can be changed in the pause page

## Technical Challenges and Solutions

### Challenge 1: Timing Precision & Concurrency Management

- **Solution**: Implemented a central timing system in GameManager, with all threads synchronized to a common game clock.

### Challenge 2: Performance Constraints

- **Solution**: Optimized rendering using double buffering and minimized object creation during gameplay.

### Challenge 3: Player Engagement

- **Solution**: Added visual effects, combo system, and high score tracking to enhance the gameplay experience.

## Future Enhancements

- Online leaderboards using a REST API
- More visual effects and themes
- Custom note pattern editor
- Support for various audio formats beyond WAV
