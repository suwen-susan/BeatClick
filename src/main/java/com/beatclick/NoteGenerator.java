package com.beatclick;

import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * NoteGenerator - Generates notes based on the song's rhythm
 * Reads note patterns from a file or generates them based on audio analysis
 */
public class NoteGenerator implements Runnable {
    
    private static final int NUM_LANES = 4;
    private static final int NOTE_TRAVEL_TIME_MS = 2000; // Time for notes to travel from top to bottom
    private static final int INITIAL_DELAY_MS = 3000;    // Initial delay before first note
    
    private final String songId;
    private final GameManager gameManager;
    private final Random random;
    private final List<NoteData> noteData;
    
    private boolean isRunning;
    
    // Audio analysis parameters
    private static final float DEFAULT_BPM = 120.0f; // Default BPM if detection fails
    private static final float ENERGY_THRESHOLD_MULTIPLIER = 1.5f; // Multiplier for energy threshold
    private static final int MIN_BEAT_DISTANCE_MS = 250; // Minimum time between beats
    private static final int ANALYSIS_WINDOW_SIZE = 1024; // Size of analysis window
    
    private float bpm = DEFAULT_BPM;


    /**
     * Inner class to store note timing data
     */
    private static class NoteData {
        long hitTime;
        int laneIndex;
        
        NoteData(long hitTime, int laneIndex) {
            this.hitTime = hitTime;
            this.laneIndex = laneIndex;
        }
    }
    
    /**
     * Constructor
     * @param songId The ID of the song
     * @param gameManager The game manager
     */
    public NoteGenerator(String songId, GameManager gameManager) {
        this.songId = songId;
        this.gameManager = gameManager;
        this.random = new Random();
        this.noteData = new ArrayList<>();
        this.isRunning = false;
        this.bpm = DEFAULT_BPM;
        
        // Load note data from file or generate from audio
        loadNoteData();
        
        // Normalize note times to make them start at a reasonable time
        normalizeNoteTimes();
        
        // // Debug: Check if any notes were generated
        // System.out.println("DEBUG: Total notes loaded/generated: " + noteData.size());
        // if (noteData.size() > 0) {
        //     System.out.println("DEBUG: First note time: " + noteData.get(0).hitTime + ", Last note time: " + 
        //                        noteData.get(noteData.size() - 1).hitTime);
        // } else {
        //     System.out.println("DEBUG: *** NO NOTES GENERATED! ***");
        //     // Add a few emergency notes for testing if none were generated
        //     addEmergencyNotes();
        // }
    }
    
    /**
     * Normalizes note times to start at a reasonable delay after game start
     * This prevents long delays before the first note appears
     */
    private void normalizeNoteTimes() {
        if (noteData.isEmpty()) {
            return;
        }
        
        // Sort notes by hit time
        noteData.sort((a, b) -> Long.compare(a.hitTime, b.hitTime));
        
        // Get the first note's hit time
        long firstNoteTime = noteData.get(0).hitTime;
        
        // If the first note is already scheduled early enough, no need to adjust
        if (firstNoteTime <= INITIAL_DELAY_MS + NOTE_TRAVEL_TIME_MS) {
            return;
        }
        
        // Calculate time offset to make first note appear after INITIAL_DELAY_MS
        long timeOffset = firstNoteTime - (INITIAL_DELAY_MS + NOTE_TRAVEL_TIME_MS);
        System.out.println("DEBUG: Normalizing note times, subtracting offset: " + timeOffset + "ms");
        
        // Adjust all note times
        for (NoteData note : noteData) {
            note.hitTime -= timeOffset;
        }
    }
    
    /**
     * Add some basic notes as a fallback if no notes were generated
     * This ensures something appears on screen for debugging
     */
    private void addEmergencyNotes() {
        System.out.println("DEBUG: Adding emergency notes for testing");
        // Add a note every 1 second for 30 seconds
        for (int i = 0; i < 30; i++) {
            long hitTime = 3000 + (i * 1000); // Start at 3 seconds, then one per second
            int laneIndex = i % NUM_LANES;
            noteData.add(new NoteData(hitTime, laneIndex));
        }
    }
    
    /**
     * Thread run method
     */
    @Override
    public void run() {
        isRunning = true;
        
        try {
            // Wait for a short delay before starting to generate notes
            Thread.sleep(100);
            
            // Process all note data
            for (NoteData data : noteData) {
                if (!isRunning || Thread.currentThread().isInterrupted()) {
                    break;
                }
                
                synchronized (gameManager.getSyncLock()) {
                    // Check if the game is paused
                    while (gameManager.getGameState().isPaused()) {
                        try {
                            gameManager.getSyncLock().wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                
                // Calculate spawn time based on hit time and travel duration
                long hitTime = data.hitTime;
                long spawnTime = hitTime - NOTE_TRAVEL_TIME_MS;
                int laneIndex = data.laneIndex;
                
                // Skip notes that should have already spawned
                long currentTime = gameManager.getGameState().getCurrentGameTime();
                if (spawnTime < currentTime) {
                    // System.out.println("DEBUG: Skipping note, spawnTime=" + spawnTime + ", currentTime=" + currentTime);
                    continue;
                }
                
                // Wait until it's time to spawn this note
                long waitTime = spawnTime - currentTime;
                if (waitTime > 0) {
                    // System.out.println("DEBUG: Waiting " + waitTime + "ms to spawn note at lane " + laneIndex);
                    Thread.sleep(waitTime);
                }
                
                // Add the note to the game
                if (isRunning && gameManager.isGameRunning() && !gameManager.getGameState().isPaused()) {
                    // System.out.println("DEBUG: Adding note at lane " + laneIndex + ", spawnTime=" + spawnTime + ", hitTime=" + hitTime);
                    gameManager.addNote(laneIndex, spawnTime, hitTime);
                }
            }
            
            // Debug output when all notes have been processed
            // System.out.println("DEBUG: All notes have been processed");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Loads note data from a file or generates it from audio analysis
     */
    private void loadNoteData() {
        String notesFilePath = "assets/songs/" + songId + ".notes";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(notesFilePath))) {
            String line;
            boolean isFirstLine = true;
            noteData.clear(); // Clear any existing note data
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                if (isFirstLine && line.startsWith("#")) {
                    isFirstLine = false;
                    // Try to extract BPM from metadata
                    if (line.contains("BPM:")) {
                        try {
                            int bpmStart = line.indexOf("BPM:") + 4;
                            int bpmEnd = line.indexOf(";", bpmStart);
                            if (bpmEnd == -1) bpmEnd = line.length();
                            String bpmStr = line.substring(bpmStart, bpmEnd);
                            this.bpm = Float.parseFloat(bpmStr);
                            // this.beatMs = 60000f / this.bpm;
                        } catch (Exception e) {
                            System.err.println("Error parsing BPM metadata: " + e.getMessage());
                            // Fall back to default BPM or detection
                            this.bpm = DEFAULT_BPM;
                        }
                    }
                    continue;
                }
                // Parse each line in format: hitTime,laneIndex
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    try{
                        long hitTime = Long.parseLong(parts[0].trim());
                        int laneIndex = Integer.parseInt(parts[1].trim());
                        
                        // Validate lane index
                        if (laneIndex >= 0 && laneIndex < NUM_LANES) {
                            // Add to note data
                            noteData.add(new NoteData(hitTime, laneIndex));
                        }
                    }catch (NumberFormatException e) {
                        System.err.println("Error parsing note data line: " + line);
                    }
                    
                }
            }
            if (!noteData.isEmpty()) {
                // Sort by hit time
                noteData.sort(Comparator.comparingLong(n -> n.hitTime));
                
                // Ensure reasonable spacing
                // ensureReasonableNoteSpacing();
                
                System.out.println("Loaded and optimized " + noteData.size() + " notes from file: " + notesFilePath);
            }
        } catch (IOException e) {
            // File not found, try to generate from audio analysis
            System.out.println("Note data file not found, attempting to generate from audio analysis.");
            boolean audioAnalysisSuccessful = generateNotesFromAudio();
            
            // If audio analysis failed, fall back to algorithmic generation
            if (!audioAnalysisSuccessful) {
                System.out.println("Audio analysis failed, generating notes algorithmically.");
                generateNotesAlgorithmically();
            }
        }
    }
    
    /**
     * Generates notes from audio file analysis
     * @return true if generation was successful, false otherwise
     */
    private boolean generateNotesFromAudio() {
        String audioFilePath = "assets/songs/" + songId + ".wav";
        File audioFile = new File(audioFilePath);
        
        if (!audioFile.exists()) {
            System.out.println("Audio file not found: " + audioFilePath);
            return false;
        }
        
        try {
            // Open the audio file
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            
            System.out.println("Audio format: " + format.toString());
            
            // Ensure we have PCM format for analysis
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                audioStream = AudioSystem.getAudioInputStream(
                    AudioFormat.Encoding.PCM_SIGNED, audioStream);
                format = audioStream.getFormat();
            }
            
            // Detect BPM (this is a simplified method)
            float bpm = detectBPM(audioFile);
            if (bpm <= 0) {
                bpm = DEFAULT_BPM;
                System.out.println("Failed to detect BPM, using default: " + bpm);
            } else {
                System.out.println("Detected BPM: " + bpm);
            }
            
            // Generate notes through onset detection
            // generateNotesFromOnsets(audioStream, format, bpm);
            generateNotesByGrid(audioStream, format, bpm);


            if (noteData.size() > 0) {
                boolean saveSuccess = saveNotesToFile();
                if (saveSuccess) {
                    System.out.println("Successfully saved " + noteData.size() + " notes to file.");
                } else {
                    System.out.println("Failed to save notes to file.");
                }
            }
            
            return noteData.size() > 0;
            
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Unsupported audio file format: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("Error reading audio file: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error generating notes from audio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Saves generated note data to a file for future use
     * @return true if save was successful, false otherwise
     */
    private boolean saveNotesToFile() {
        String notesFilePath = "assets/songs/" + songId + ".notes";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(notesFilePath))) {
            // Sort notes by hit time before saving
            List<NoteData> sortedNotes = new ArrayList<>(noteData);
            sortedNotes.sort(Comparator.comparingLong(n -> n.hitTime));
            
            writer.write("# BPM:" + bpm + ";VERSION:1.0;TIMESTAMP:" + System.currentTimeMillis());
            writer.newLine();

            // Write each note data in format: hitTime,laneIndex
            for (NoteData note : sortedNotes) {
                writer.write(note.hitTime + "," + note.laneIndex);
                writer.newLine();
            }
            
            System.out.println("Saved " + sortedNotes.size() + " notes to file: " + notesFilePath);
            return true;
            
        } catch (IOException e) {
            System.err.println("Error saving notes to file: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Detects the BPM (Beats Per Minute) of the audio file
     * This is a simplified implementation using energy-based beat detection
     * 
     * @param audioFile The audio file to analyze
     * @return The estimated BPM, or -1 if detection failed
     */
    private float detectBPM(File audioFile) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            
            // Convert to PCM signed if needed
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                audioStream = AudioSystem.getAudioInputStream(
                    AudioFormat.Encoding.PCM_SIGNED, audioStream);
                format = audioStream.getFormat();
            }
            
            int sampleRate = (int) format.getSampleRate();
            int frameSize = format.getFrameSize();
            
            // Buffer for reading audio data
            byte[] buffer = new byte[ANALYSIS_WINDOW_SIZE * frameSize];
            
            // Lists for energy and beat times
            List<Float> energyHistory = new ArrayList<>();
            List<Long> beatTimes = new ArrayList<>();
            
            // Read and analyze audio data
            long sampleCount = 0;
            int bytesRead;
            
            while ((bytesRead = audioStream.read(buffer)) > 0) {
                // Calculate energy for this window
                float energy = calculateEnergy(buffer, bytesRead, format);
                energyHistory.add(energy);
                
                // Number of samples in this window
                int samplesInWindow = bytesRead / frameSize;
                sampleCount += samplesInWindow;
            }
            
            // Calculate average energy
            float avgEnergy = 0;
            for (float e : energyHistory) {
                avgEnergy += e;
            }
            avgEnergy /= energyHistory.size();
            
            // Set threshold at X times the average energy
            float threshold = avgEnergy * ENERGY_THRESHOLD_MULTIPLIER;
            
            // Reset and re-analyze for beats
            audioStream.close();
            audioStream = AudioSystem.getAudioInputStream(audioFile);
            
            sampleCount = 0;
            long lastBeatTime = -MIN_BEAT_DISTANCE_MS; // Ensure first beat can be detected
            
            while ((bytesRead = audioStream.read(buffer)) > 0) {
                float energy = calculateEnergy(buffer, bytesRead, format);
                
                // Current time in milliseconds
                long currentTimeMs = (long) (sampleCount * 1000.0 / sampleRate);
                
                // Check if this is a beat (energy peak above threshold)
                if (energy > threshold && (currentTimeMs - lastBeatTime) >= MIN_BEAT_DISTANCE_MS) {
                    beatTimes.add(currentTimeMs);
                    lastBeatTime = currentTimeMs;
                }
                
                // Update sample count
                sampleCount += bytesRead / frameSize;
            }
            
            // Calculate BPM from beat intervals
            if (beatTimes.size() >= 2) {
                float totalIntervals = 0;
                int intervalCount = 0;
                
                for (int i = 1; i < beatTimes.size(); i++) {
                    long interval = beatTimes.get(i) - beatTimes.get(i-1);
                    totalIntervals += interval;
                    intervalCount++;
                }
                
                float avgIntervalMs = totalIntervals / intervalCount;
                float bpm = 60000.0f / avgIntervalMs;
                
                System.out.println("DEBUG: Detected " + beatTimes.size() + " beats, average interval: " + avgIntervalMs + "ms, BPM: " + bpm);
                
                this.bpm = bpm;
                return bpm;
            }
            
        } catch (UnsupportedAudioFileException e) {
            System.err.println("BPM detection failed - Unsupported audio format: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("BPM detection failed - IO error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("BPM detection failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1; // Failed to detect BPM
    }
    
    /**
     * Calculates the energy of an audio buffer
     * 
     * @param buffer The audio buffer
     * @param length The valid length of the buffer
     * @param format The audio format
     * @return The energy value
     */
    private float calculateEnergy(byte[] buffer, int length, AudioFormat format) {
        int frameSize = format.getFrameSize();
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        boolean isBigEndian = format.isBigEndian();
        
        float energy = 0;
        int numSamples = length / frameSize;
        
        for (int i = 0; i < numSamples; i++) {
            int sampleIndex = i * frameSize;
            short sample = 0;
            
            // Convert bytes to sample value based on format
            if (bytesPerSample == 2) { // 16-bit audio
                if (isBigEndian) {
                    sample = (short)(((buffer[sampleIndex] & 0xFF) << 8) | (buffer[sampleIndex+1] & 0xFF));
                } else {
                    sample = (short)(((buffer[sampleIndex+1] & 0xFF) << 8) | (buffer[sampleIndex] & 0xFF));
                }
            } else if (bytesPerSample == 1) { // 8-bit audio
                sample = (short)((buffer[sampleIndex] & 0xFF) - 128);
            }
            
            // Square the sample value to get energy
            energy += sample * sample;
        }
        
        // Return average energy per sample
        return numSamples > 0 ? energy / numSamples : 0;
    }
    
    /**
     * Generates notes algorithmically based on a simple rhythm pattern
     * This is a fallback method if audio analysis fails
     */
    private void generateNotesAlgorithmically() {
        // Assume a standard 4/4 time signature with 120 BPM
        // Quarter note = 500ms
        int bpm = 120;
        long quarterNoteMs = 60000 / bpm;
        
        // Start with a delay before the first note
        long currentTime = 2000;
        
        // Generate about 2 minutes worth of notes
        long songDuration = 120000; // 2 minutes
        
        while (currentTime < songDuration) {
            // Choose a lane
            int laneIndex = random.nextInt(NUM_LANES);
            
            // Add the note
            noteData.add(new NoteData(currentTime, laneIndex));
            
            // Move to the next beat (with slight variations)
            int pattern = random.nextInt(10);
            
            if (pattern < 5) {
                // Quarter note
                currentTime += quarterNoteMs;
            } else if (pattern < 8) {
                // Eighth note
                currentTime += quarterNoteMs / 2;
            } else {
                // Half note
                currentTime += quarterNoteMs * 2;
            }
            
            // Occasionally add a rest
            if (random.nextInt(10) < 2) {
                currentTime += quarterNoteMs;
            }
        }
        
        System.out.println("Generated " + noteData.size() + " notes algorithmically.");
    }

    /**
     * Generate notes by fixed beat grid & multi-band energy detection.
     *
     * @param audioStream The PCM audio input stream (must be PCM_SIGNED).
     * @param format      The audio format info.
     * @param bpm         Estimated beats per minute.
     * @throws IOException On I/O error.
     */
    private void generateNotesByGrid(AudioInputStream audioStream, AudioFormat format, float bpm) throws IOException {
        // --- 1. Load entire audio into memory ---
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read;
        while ((read = audioStream.read(buf)) > 0) {
            baos.write(buf, 0, read);
        }
        byte[] allBytes = baos.toByteArray();
        int frameSize = format.getFrameSize();
        float sampleRate = format.getSampleRate();
        long frameLength = audioStream.getFrameLength();
        long audioLengthMs = (long)(frameLength * 1000 / sampleRate);

        // Calculate buffer sizes based on BPM for analysis windows
        int samplesPerBeat = (int) (60.0f / bpm * sampleRate);
        int analysisWindowSize = Math.max(1, Math.min(samplesPerBeat / 4, ANALYSIS_WINDOW_SIZE));

        // --- 2. Compute grid parameters ---
        float beatMs = 60000f / bpm;           // ms per beat
        int resolution = 8;                    // 4 subdivisions per beat
        float gridMs = beatMs / resolution;    // ms per grid step
        // int totalMs = allBytes.length / frameSize * 1000 / (int)sampleRate;
        int totalMs = (int)audioLengthMs;
        int totalSteps = Math.max(1, totalMs / (int)gridMs);

        // --- 3. First pass: accumulate mean energy per band ---
        double[] sumEnergy = new double[NUM_LANES];
        for (int i = 0; i < totalSteps; i++) {
            int startMs = (int)(i * gridMs);
            int startSample = (int)(startMs * sampleRate / 1000);
            int startByte = startSample * frameSize;

            if (startByte >= allBytes.length) {
                continue;
            }

            int length = Math.min(analysisWindowSize * frameSize, allBytes.length - startByte);
            
            if (length <= 0) {
                continue;
            }

            double[] energies = calculateBandEnergies(allBytes, startByte, length, format);
            for (int lane = 0; lane < NUM_LANES; lane++) {
                sumEnergy[lane] += energies[lane];
            }
        }
        double[] meanEnergy = new double[NUM_LANES];
        for (int lane = 0; lane < NUM_LANES; lane++) {
            meanEnergy[lane] = sumEnergy[lane] / totalSteps;
        }

        // --- 4. Second pass: threshold check & note generation ---
        final double thresholdFactor = 1.05;  // you can tweak this
        for (int i = 0; i < totalSteps; i++) {
            int startMs = (int)(i * gridMs);
            int startSample = (int)(startMs * sampleRate / 1000);
            int startByte = startSample * frameSize;

            if (startByte >= allBytes.length) {
                continue;
            }

            int length = Math.min(analysisWindowSize * frameSize, allBytes.length - startByte);
            
            if (length <= 0) {
                continue;
            }
           
            double[] energies = calculateBandEnergies(allBytes, startByte, length, format);

            for (int lane = 0; lane < NUM_LANES; lane++) {
                // if current band energy significantly above its mean -> note
                if (energies[lane] > meanEnergy[lane] * thresholdFactor) {
                    noteData.add(new NoteData(startMs, lane));
                }
            }
        }

        // --- 5. optimize global note density ---
        optimizeGlobalNoteDensity();

        // --- 6. Sort by hit time to produce final sequence ---
        noteData.sort(Comparator.comparingLong(n -> n.hitTime));
    }

  

    /**
     * Optimizes the global note density by applying a series of filters and adjustments.
     * This includes applying a global minimum spacing, density control using a sliding window,
     */
    private void optimizeGlobalNoteDensity() {
        if (noteData.isEmpty()) return;
        long beatMs = (long)(60000f / bpm); // 每拍的毫秒数
        
        // ========== Step 0: Sort notes by hit time ==========
        List<NoteData> sortedNotes = new ArrayList<>(noteData);
        sortedNotes.sort(Comparator.comparingLong(n -> n.hitTime));
        
        System.out.println("Starting global density optimization with " + sortedNotes.size() + " notes");
        
        // ========== Step 1: Apply global minimum spacing ==========
        
        // Set a global minimum spacing of 0.5 beats (500ms)
        long absoluteMinSpacing = (long)(beatMs * 0.5); 
        
        List<NoteData> spacedNotes = new ArrayList<>();
        
        // keep the first note
        if (!sortedNotes.isEmpty()) {
            spacedNotes.add(sortedNotes.get(0));
        }
        
        // apply global minimum spacing
        for (int i = 1; i < sortedNotes.size(); i++) {
            NoteData currentNote = sortedNotes.get(i);
            NoteData lastAddedNote = spacedNotes.get(spacedNotes.size() - 1);
            
            long spacing = currentNote.hitTime - lastAddedNote.hitTime;
            
            if (spacing >= absoluteMinSpacing) {
                spacedNotes.add(currentNote);
            } else {
                System.out.println("Removing note at time " + currentNote.hitTime + 
                                " (too close to previous note, spacing: " + spacing + "ms)");
            }
        }
        
        System.out.println("After global minimum spacing: " + spacedNotes.size() + " notes");
        
        // ========== Step 2: Density control using a sliding window ==========
        
        // Apply a sliding window to control the density of notes
        long windowSize = (long)(beatMs * 1); 
        int maxNotesPerWindow = 3; 
        
        List<NoteData> densityControlledNotes = new ArrayList<>();
        
        for (NoteData note : spacedNotes) {
            // calculate the start of the window
            long windowStart = note.hitTime - windowSize;
            
            // calculate the number of notes in the window
            int notesInWindow = 0;
            for (NoteData addedNote : densityControlledNotes) {
                if (addedNote.hitTime >= windowStart && addedNote.hitTime <= note.hitTime) {
                    notesInWindow++;
                }
            }
            
            // if the number of notes in the window is less than the max, add the note
            if (notesInWindow < maxNotesPerWindow) {
                densityControlledNotes.add(note);
            } else {
                System.out.println("Removing note at time " + note.hitTime + 
                                " (window already has " + notesInWindow + " notes)");
            }
        }
        
        System.out.println("After density window control: " + densityControlledNotes.size() + " notes");
        
        // ========== Step 3: Filter same-lane notes ==========
        
        List<NoteData> finalNotes = new ArrayList<>();
        
        // keep the first note (if any)
        if (!densityControlledNotes.isEmpty()) {
            finalNotes.add(densityControlledNotes.get(0));
        }
        
        // filter out same-lane notes that are too close together
        for (int i = 1; i < densityControlledNotes.size(); i++) {
            NoteData currentNote = densityControlledNotes.get(i);
            NoteData previousNote = finalNotes.get(finalNotes.size() - 1);
            
            // calculate the time difference between current and previous note
            if (currentNote.laneIndex == previousNote.laneIndex && 
                (currentNote.hitTime - previousNote.hitTime) < beatMs) {
                System.out.println("Removing note at time " + currentNote.hitTime + 
                                " (same lane as previous note and too close)");
            } else {
                finalNotes.add(currentNote);
            }
        }
        
        System.out.println("After same-lane filtering: " + finalNotes.size() + " notes");
        
        // ========== Step 4: Reassign lanes to avoid consecutive notes on the same lane ==========
        
        // reassign lanes to avoid consecutive notes on the same lane
        for (int i = 1; i < finalNotes.size(); i++) {
            NoteData currentNote = finalNotes.get(i);
            NoteData previousNote = finalNotes.get(i - 1);
            
            // calculate the time difference between current and previous note
            long timeDiff = currentNote.hitTime - previousNote.hitTime;
            
            if (timeDiff < beatMs) { 
                // If the current note is on the same lane as the previous note, change its lane
                if (currentNote.laneIndex == previousNote.laneIndex) {
                    // Find a new lane that is not the same as the previous note's lane
                    int newLane = (previousNote.laneIndex + 2) % NUM_LANES; // skip neighboring lane
                    System.out.println("Moving note at time " + currentNote.hitTime + 
                                    " from lane " + currentNote.laneIndex + " to lane " + newLane);
                    currentNote.laneIndex = newLane;
                }
            }
        }
        
        // ========== Step 5: Filter out very tight triple notes ==========
        
        List<NoteData> tripleFilteredNotes = new ArrayList<>();
        
        // keep the first two notes (if any)
        if (finalNotes.size() >= 1) tripleFilteredNotes.add(finalNotes.get(0));
        if (finalNotes.size() >= 2) tripleFilteredNotes.add(finalNotes.get(1));
        
        // filter out very tight triple notes
        for (int i = 2; i < finalNotes.size(); i++) {
            NoteData note1 = tripleFilteredNotes.get(tripleFilteredNotes.size() - 2);
            NoteData note2 = tripleFilteredNotes.get(tripleFilteredNotes.size() - 1);
            NoteData note3 = finalNotes.get(i);
            
            // calculate the spacing between the notes
            long spacing1 = note2.hitTime - note1.hitTime;
            long spacing2 = note3.hitTime - note2.hitTime;
            
            // check if the spacing is too tight
            if (spacing1 < beatMs * 0.4 && spacing2 < beatMs * 0.4) { 
                // remove the middle note (note2) to break the pattern
                tripleFilteredNotes.remove(tripleFilteredNotes.size() - 1);
                System.out.println("Breaking very tight triple note pattern by removing middle note at " + note2.hitTime);
                
                // check if the third note (note3) is also too close to the first note (note1)
                long newSpacing = note3.hitTime - note1.hitTime;
                
                // add the third note (note3) only if it is not too close to the first note (note1)
                if (newSpacing >= beatMs * 0.5) {
                    tripleFilteredNotes.add(note3);
                } else {
                    System.out.println("Also skipping third note in triple pattern");
                }
            } else {
                // If the spacing is acceptable, add the note3 to the filtered list
                tripleFilteredNotes.add(note3);
            }
        }
        
        System.out.println("After triple pattern filtering: " + tripleFilteredNotes.size() + " notes");
        
        // ========== Step 6: Normalize note times to start after a delay ==========
        
        // Set a delay for the first note to appear after game start
        long gameStartDelay = 3000; // 3 seconds for preparation
        
        if (!tripleFilteredNotes.isEmpty() && tripleFilteredNotes.get(0).hitTime < gameStartDelay) {
            tripleFilteredNotes.get(0).hitTime = gameStartDelay;
        }
        
        // ========== Step 7: Add filler notes to fill gaps ==========
        
        // Set a maximum spacing for filler notes (4 beats)
        long maxSpacing = (long)(beatMs * 4); 
        
        List<NoteData> finalNotesWithFiller = new ArrayList<>(tripleFilteredNotes);
        List<NoteData> notesToAdd = new ArrayList<>();
        
        for (int i = 1; i < finalNotesWithFiller.size(); i++) {
            long spacing = finalNotesWithFiller.get(i).hitTime - finalNotesWithFiller.get(i-1).hitTime;
            
            // If the spacing is larger than the maximum allowed, add filler notes
            if (spacing > maxSpacing) {
                // Calculate the number of filler notes to add
                int fillerCount = spacing > maxSpacing * 1.5 ? 2 : 1;
                
                for (int j = 0; j < fillerCount; j++) {
                    // calculate the time for the new note
                    long newTime;
                    if (fillerCount == 1) {
                        // If adding one note, place it in the middle of the gap
                        newTime = finalNotesWithFiller.get(i-1).hitTime + spacing / 2;
                    } else {
                        // If adding two notes, place them evenly spaced in the gap
                        newTime = finalNotesWithFiller.get(i-1).hitTime + spacing * (j + 1) / (fillerCount + 1);
                    }
                    
                    // choose a lane for the new note
                    int prevLane = finalNotesWithFiller.get(i-1).laneIndex;
                    int nextLane = finalNotesWithFiller.get(i).laneIndex;
                    
                    // choose a new lane that is not the same as the previous or next note
                    int newLane = 0;
                    for (int lane = 0; lane < NUM_LANES; lane++) {
                        if (lane != prevLane && lane != nextLane) {
                            newLane = lane;
                            break;
                        }
                    }
                    
                    //Add the new note to the list of notes to add
                    notesToAdd.add(new NoteData(newTime, newLane));
                    System.out.println("Adding filler note at time " + newTime + " in lane " + newLane);
                }
            }
        }
        
        // add the filler notes to the final notes list
        finalNotesWithFiller.addAll(notesToAdd);
        finalNotesWithFiller.sort(Comparator.comparingLong(n -> n.hitTime));
        
        System.out.println("After adding filler notes: " + finalNotesWithFiller.size() + " notes");
        
        // ========== Extra Step: Add beat-based filler notes ==========
        
        // create a list to hold beat-based filler notes
        List<NoteData> beatFillerNotes = new ArrayList<>();
        
        // Get the song end time
        long songEndTime = finalNotesWithFiller.isEmpty() ? 0 : 
                        finalNotesWithFiller.get(finalNotesWithFiller.size() - 1).hitTime;
        
        // calculate the main beats based on the song length
        List<Long> mainBeats = new ArrayList<>();
        for (long time = gameStartDelay; time < songEndTime; time += beatMs) {
            mainBeats.add(time);
        }
        
        // check if there are any main beats
        Set<Long> existingNoteTimes = new HashSet<>();
        for (NoteData note : finalNotesWithFiller) {
            for (long t = note.hitTime - 30; t <= note.hitTime + 30; t++) {
                existingNoteTimes.add(t);
            }
        }
        
        Random random = new Random();
        

        for (long beatTime : mainBeats) {
            // check if there are any existing notes within 100ms of the beat time
            boolean hasNearbyNote = false;
            for (long t = beatTime - 100; t <= beatTime + 100; t++) {
                if (existingNoteTimes.contains(t)) {
                    hasNearbyNote = true;
                    break;
                }
            }
            
            // if there are no nearby notes, add a beat-based filler note
            if (!hasNearbyNote && random.nextInt(4) == 0) { // only add 25% of the time
                int beatLane = random.nextInt(NUM_LANES);
                
                beatFillerNotes.add(new NoteData(beatTime, beatLane));
                
                for (long t = beatTime - 30; t <= beatTime + 30; t++) {
                    existingNoteTimes.add(t);
                }
            }
        }
        

        if (!beatFillerNotes.isEmpty()) {
            System.out.println("Adding " + beatFillerNotes.size() + " beat-based filler notes");
            finalNotesWithFiller.addAll(beatFillerNotes);
            finalNotesWithFiller.sort(Comparator.comparingLong(n -> n.hitTime));
        }
        
        // ========== Last Step: Remove duplicates ==========
        
        // remove duplicates from the final notes list
        noteData.clear();
        noteData.addAll(finalNotesWithFiller);
        
        System.out.println("Final optimized note count: " + noteData.size());
    }

    // /**
    //  * 检查时间点是否位于拍子上
    //  */
    // private boolean isOnBeat(long timeMs) {
    //     float beatMs = 60000f / bpm;
    //     return Math.abs(timeMs % (long)beatMs) < beatMs / 8;
    // }




    /**
     * Compute energy in 4 frequency bands (bass, low-mid, mid-high, treble)
     * using naive DFT on the given byte window.
     *
     * @param allBytes Full PCM byte array.
     * @param offset   Byte offset to start.
     * @param length   Number of bytes in window.
     * @param format   AudioFormat info.
     * @return Array of length NUM_LANES with band energies.
     */
    private double[] calculateBandEnergies(byte[] allBytes, int offset, int length, AudioFormat format) {
        // 防御性代码：检查参数
        if (length <= 0) {
            System.out.println("Warning: Attempted to calculate band energies with non-positive length: " + length);
            // 返回空能量数组避免异常
            return new double[NUM_LANES];
        }
        
        if (offset < 0 || offset >= allBytes.length) {
            System.out.println("Warning: Invalid offset for band energy calculation: " + offset);
            return new double[NUM_LANES];
        }
        
        // 确保不会越界
        length = Math.min(length, allBytes.length - offset);
        
        
        int frameSize = format.getFrameSize();
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        boolean bigEndian = format.isBigEndian();
        float sampleRate = format.getSampleRate();
        int sampleCount = length / frameSize;

        // 1) decode PCM to normalized [-1,1] samples
        double[] samples = new double[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            int idx = offset + i * frameSize;
            short val = 0;
            if (bytesPerSample == 2) {
                if (bigEndian) {
                    val = (short)(((allBytes[idx] & 0xFF) << 8) | (allBytes[idx+1] & 0xFF));
                } else {
                    val = (short)(((allBytes[idx+1] & 0xFF) << 8) | (allBytes[idx] & 0xFF));
                }
            } else if (bytesPerSample == 1) {
                val = (short)((allBytes[idx] & 0xFF) - 128);
            }
            samples[i] = val / 32768.0;
        }

        int N = samples.length;
        // we'll accumulate magnitude-squared per band
        double[] bandEnergy = new double[NUM_LANES];

        // 2) naive DFT and bucket into bands
        for (int k = 0; k < N/2; k++) {
            double re = 0, im = 0;
            double angleFactor = 2 * Math.PI * k / N;
            for (int n = 0; n < N; n++) {
                double angle = angleFactor * n;
                re += samples[n] * Math.cos(angle);
                im -= samples[n] * Math.sin(angle);
            }
            double mag2 = re*re + im*im;  // energy ~ magnitude squared
            double freq = k * sampleRate / N;

            // map freq to band index
            int lane;
            if (freq < 150) {
                lane = 0;
            } else if (freq < 500) {
                lane = 1;
            } else if (freq < 1500) {
                lane = 2;
            } else {
                lane = 3;
            }
            bandEnergy[lane] += mag2;
        }
        return bandEnergy;
    }
    
    /**
     * Stops the note generator
     */
    public void stop() {
        isRunning = false;
    }
}