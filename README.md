# BeatClick - Music Rhythm Game

**Final Project for Java Course**
By Suwen Wang (sw6359), Yifei Xu (yx3590), Ruqing Liu (rl5652)

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
13. **ScoreRecord**: Represents a single player's performance record including score details and timing information.
14. **ChartUtils**: Provides reusable methods to generate styled charts for visualizing player scores and rating distributions.



## Setup and Running

### Prerequisites

- Java 11 or higher
- Gradle (for building)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/yourusername/beatclick.git
cd beatclick

# Build with Gradle
./gradle build
or
gradle build
```

### Running the Game

```bash
./gradle run
or
gradle run
```

## Function
### Adding Custom Songs
BeatClick makes it easy for players to add their own songs and note patterns. Simply click the "+" button and import a .wav file — the system will automatically generate the notes for you. The process is fully dynamic, so there's no need to recompile or modify any Java files.

### Mode and difficulty level choice
You can choose from three difficulty levels — Easy, Medium, and Hard — in Normal Mode. There's also a Practice Mode, where you can play freely without affecting your score.

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
- Support for various audio formats beyond WAV
