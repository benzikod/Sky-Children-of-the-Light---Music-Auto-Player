package SkyStudioApp;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static SkyStudioApp.Main.*;


public class PlaySong{
    private static final AtomicBoolean isPaused = new AtomicBoolean(false);
    private static final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private static long pauseStartTime = 0;
    private static long totalPausedTime = 0;

    public static void formatAndSaveJson(String filePath) {
        try {
            List<Song> songs = loadSongsFromJson(filePath);

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            String formattedJson = gson.toJson(songs);

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(filePath), StandardCharsets.UTF_16LE)) {
                writer.write("\uFEFF");
                writer.write(formattedJson);
            }

            System.out.println("JSON file has been formatted and saved successfully.");
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Error formatting and saving JSON: " + e.getMessage());
        }
    }

    public static List<Song> loadSongsFromJson(String filePath) throws IOException, JsonSyntaxException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        String content;

        if (bytes.length >= 2 && bytes[0] == (byte)0xFF && bytes[1] == (byte)0xFE) {
            content = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        } else {
            content = new String(bytes, StandardCharsets.UTF_16LE);
        }


        Gson gson = new GsonBuilder().create();
        JsonReader reader = new JsonReader(new StringReader(content));


        try {
            JsonElement jsonElement = JsonParser.parseReader(reader);

            if (jsonElement.isJsonArray()) {
                Type songListType = new TypeToken<List<Song>>() {}.getType();
                return gson.fromJson(jsonElement, songListType);
            } else {
                throw new JsonSyntaxException("Expected JSON array");
            }
        } catch (JsonSyntaxException e) {
            content = content.trim();
            if (!content.endsWith("]")) {
                content += "]";
            }
            try {
                Type songListType = new TypeToken<List<Song>>() {}.getType();
                return gson.fromJson(content, songListType);
            } catch (JsonSyntaxException e2) {
                throw new JsonSyntaxException("Error parsing JSON after attempted fix: " + e2.getMessage());
            }
        }
    }

    public static void playSong(Song song) throws AWTException {
        if (song == null || song.songNotes == null || song.songNotes.isEmpty()) {
            throw new IllegalArgumentException("Invalid song data");
        }
        Robot robot = new Robot();
        Random random = new Random();

        List<SongNote> sortedNotes = song.songNotes.stream()
                .sorted(Comparator.comparingLong(note -> note.time))
                .toList();

        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        Set<String> currentlyPressedKeys = new HashSet<>();
        isPlaying.set(true);
        isPaused.set(false);
        totalPausedTime = 0;

        Thread focusCheckThread = new Thread(() -> {
            while (isPlaying.get()) {
                if (!SkyWindowFocus.isSkyWindowFocused()) {
                    if (!isPaused.get()) {
                        isPaused.set(true);
                        pauseStartTime = System.currentTimeMillis();
                        System.out.println("失去焦點，暫停播放");
                    }
                } else if (isPaused.get()) {
                    isPaused.set(false);
                    totalPausedTime += System.currentTimeMillis() - pauseStartTime;
                    System.out.println("重新獲得焦點，繼續播放");
                    startTime.addAndGet(System.currentTimeMillis() - pauseStartTime);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            }
        });
        focusCheckThread.start();

        for (SongNote currentNote : sortedNotes) {
            while (isPaused.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            }

            long currentNoteTime = currentNote.time;
            long expectedElapsedTime = Math.round(currentNoteTime * (double) SONG_SPEED);
            long actualElapsedTime = System.currentTimeMillis() - startTime.get() - totalPausedTime;
            long delay = Math.max(0, expectedElapsedTime - actualElapsedTime);

            if (delay > 0) {
                robot.delay((int) delay);
            }

            if (USE_SUSTAIN) {
                currentlyPressedKeys.removeIf(key -> {
                    if (!key.equals(currentNote.key)) {
                        int keyCode = getKeyCode(key);
                        if (keyCode != -1) {
                            robot.keyRelease(keyCode);
                        }
                        return true;
                    }
                    return false;
                });
            } else {
                for (String key : currentlyPressedKeys) {
                    int keyCode = getKeyCode(key);
                    if (keyCode != -1) {
                        robot.keyRelease(keyCode);
                    }
                }
                currentlyPressedKeys.clear();
            }

            if (!currentlyPressedKeys.contains(currentNote.key)) {
                int keyCode = getKeyCode(currentNote.key);
                if (keyCode != -1) {
                    robot.keyPress(keyCode);
                    currentlyPressedKeys.add(currentNote.key);
                }
            }

            int minDelay = 30;
            int maxDelay = 50;
            int rd = random.nextInt(minDelay, maxDelay);
            robot.delay(rd);
        }

        isPlaying.set(false);

        for (String key : currentlyPressedKeys) {
            int keyCode = getKeyCode(key);
            if (keyCode != -1) {
                robot.keyRelease(keyCode);
            }
        }
    }

    private static int getKeyCode(String key) {
        Map<String, Integer> keyMap = new HashMap<>();
        keyMap.put("1Key0", KeyEvent.VK_Y);
        keyMap.put("2Key0", KeyEvent.VK_Y);
        keyMap.put("1Key1", KeyEvent.VK_U);
        keyMap.put("2Key1", KeyEvent.VK_U);
        keyMap.put("1Key2", KeyEvent.VK_I);
        keyMap.put("2Key2", KeyEvent.VK_I);
        keyMap.put("1Key3", KeyEvent.VK_O);
        keyMap.put("2Key3", KeyEvent.VK_O);
        keyMap.put("1Key4", KeyEvent.VK_P);
        keyMap.put("2Key4", KeyEvent.VK_P);
        keyMap.put("1Key5", KeyEvent.VK_H);
        keyMap.put("2Key5", KeyEvent.VK_H);
        keyMap.put("1Key6", KeyEvent.VK_J);
        keyMap.put("2Key6", KeyEvent.VK_J);
        keyMap.put("1Key7", KeyEvent.VK_K);
        keyMap.put("2Key7", KeyEvent.VK_K);

        keyMap.put("1Key8", KeyEvent.VK_L);
        keyMap.put("1Key9", KeyEvent.VK_SEMICOLON);
        keyMap.put("1Key10", KeyEvent.VK_N);
        keyMap.put("1Key11", KeyEvent.VK_M);
        keyMap.put("1Key12", KeyEvent.VK_COMMA);
        keyMap.put("1Key13", KeyEvent.VK_PERIOD);
        keyMap.put("1Key14", KeyEvent.VK_SLASH);

        return keyMap.getOrDefault(key, -1);
    }

    public static class SongNote {
        public String key;
        public long time;
    }

    public static class Song {
        public String name;
        public int bpm;
        public List<SongNote> songNotes;
    }
}