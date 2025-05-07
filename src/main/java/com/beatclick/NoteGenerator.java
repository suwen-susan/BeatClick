package com.beatclick;

import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
                if (isRunning && gameManager.isGameRunning()) {
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
            while ((line = reader.readLine()) != null) {
                // Parse each line in format: hitTime,laneIndex
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    long hitTime = Long.parseLong(parts[0].trim());
                    int laneIndex = Integer.parseInt(parts[1].trim());
                    
                    // Add to note data
                    noteData.add(new NoteData(hitTime, laneIndex));
                }
            }
            System.out.println("Loaded " + noteData.size() + " notes from file: " + notesFilePath);
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
        int analysisWindowSize = Math.min(samplesPerBeat / 4, ANALYSIS_WINDOW_SIZE);

        // --- 2. Compute grid parameters ---
        float beatMs = 60000f / bpm;           // ms per beat
        int resolution = 4;                    // 4 subdivisions per beat
        float gridMs = beatMs / resolution;    // ms per grid step
        // int totalMs = allBytes.length / frameSize * 1000 / (int)sampleRate;
        int totalMs = (int)audioLengthMs;
        int totalSteps = totalMs / (int)gridMs;

        // --- 3. First pass: accumulate mean energy per band ---
        double[] sumEnergy = new double[NUM_LANES];
        for (int i = 0; i < totalSteps; i++) {
            int startMs = (int)(i * gridMs);
            int startSample = (int)(startMs * sampleRate / 1000);
            int startByte = startSample * frameSize;

            int length = Math.min(analysisWindowSize * frameSize, allBytes.length - startByte);
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
        final double thresholdFactor = 1.5;  // you can tweak this
        for (int i = 0; i < totalSteps; i++) {
            int startMs = (int)(i * gridMs);
            int startSample = (int)(startMs * sampleRate / 1000);
            int startByte = startSample * frameSize;
            int length = Math.min(analysisWindowSize * frameSize, allBytes.length - startByte);
            double[] energies = calculateBandEnergies(allBytes, startByte, length, format);

            for (int lane = 0; lane < NUM_LANES; lane++) {
                // if current band energy significantly above its mean -> note
                if (energies[lane] > meanEnergy[lane] * thresholdFactor) {
                    noteData.add(new NoteData(startMs, lane));
                }
            }
        }

        // --- 5. Sort by hit time to produce final sequence ---
        noteData.sort(Comparator.comparingLong(n -> n.hitTime));
    }

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
            if (freq < 250) {
                lane = 0;
            } else if (freq < 1000) {
                lane = 1;
            } else if (freq < 5000) {
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

