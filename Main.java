package SkyStudioApp;

import com.google.gson.JsonSyntaxException;
import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static SkyStudioApp.PlaySong.*;

public class Main {
    public static final float SONG_SPEED = 1.03f; //播歌速度
    public static final boolean Loop = false; // true: 循環播放所有歌曲, false: 只播放指定歌曲
    public static String userDir = System.getProperty("user.dir");
    public static String SongName = "紅蓮華.json"; //音樂名稱+.json
    public static boolean USE_SUSTAIN = true; //是否有延音

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(2000);
        String musicFolder = Paths.get(userDir, "SkyStudioApp", "Music").toString();
        String filePath = Paths.get(musicFolder, SongName).toString();

        try {
            if (Loop) {
                playAllSongsInLoop(musicFolder);
            } else {
                playSingleSong(filePath);
            }
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
        } catch (AWTException e) {
            System.err.println("Error playing song: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
    }

    private static void playSingleSong(String filePath) throws IOException, AWTException, InterruptedException {
        formatAndSaveJson(filePath);
        List<PlaySong.Song> songs = loadSongsFromJson(filePath);
        for (PlaySong.Song song : songs) {
            System.out.println("Now playing: " + new File(filePath).getName() + " BPM:" + song.bpm);
            playSong(song);
        }
    }

    private static void playAllSongsInLoop(String folderPath) throws IOException, AWTException, InterruptedException {
        List<String> songPaths = getSongPaths(folderPath);
        while (true) {
            for (String filePath : songPaths) {
                formatAndSaveJson(filePath);
                List<PlaySong.Song> songs = loadSongsFromJson(filePath);
                for (PlaySong.Song song : songs) {
                    System.out.println("Now playing: " + new File(filePath).getName() + " BPM:" + song.bpm);
                    playSong(song);
                    Thread.sleep(1000);
                }
            }
            System.out.println("Finished playing all songs. Restarting playlist.");
        }
    }

    public static List<String> getSongPaths(String folderPath) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        List<String> songPaths = new ArrayList<>();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    songPaths.add(file.getAbsolutePath());
                }
            }
        }

        return songPaths;
    }
}