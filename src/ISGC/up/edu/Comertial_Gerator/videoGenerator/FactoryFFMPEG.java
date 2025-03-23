package ISGC.up.edu.Comertial_Gerator.videoGenerator;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FactoryFFMPEG {
    private static FactoryFFMPEG instance;
    private Robot robot;
    private int width;
    private int height;
    private List<String> tempFiles = new ArrayList<>();

    private FactoryFFMPEG() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        this.width = 1024;
        this.height = 768;
    }

    public static synchronized FactoryFFMPEG getInstance(int width, int height) {
        if (instance == null) {
            instance = new FactoryFFMPEG();
        }
        instance.width = (width > 0) ? width : instance.width;
        instance.height = (height > 0) ? height : instance.height;
        return instance;
    }

    public String imageToVideo(String path, int width, int height) {
        String outputPath = "img_" + UUID.randomUUID() + ".mp4";
        System.out.println("Generating video from: " + path);
        trackTempFile(outputPath);

        try {
            String[] command = {
                    "ffmpeg", "-loop", "1", "-t", "4", "-i", path,
                    "-vf", "scale=" + width + ":" + height + ",format=yuv420p",
                    "-c:v", "libx264",
                    "-preset", "medium",
                    "-profile:v", "main",
                    "-pix_fmt", "yuv420p",
                    "-r", "30",
                    "-movflags", "+faststart",
                    outputPath
            };
            return executeCommand(command) == 0 ? outputPath : null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String concatenateVideosPath(String firstVideoPath, String secondVideoPath, String outputVideoPath) {
        String tempFirstVideo = "temp_first_" + UUID.randomUUID() + ".mp4";
        String tempSecondVideo = "temp_second_" + UUID.randomUUID() + ".mp4";
        String tempListPath = "concat_list_" + UUID.randomUUID() + ".txt";

        trackTempFile(tempFirstVideo);
        trackTempFile(tempSecondVideo);
        trackTempFile(tempListPath);

        System.out.println("Concatenating: " + firstVideoPath + " and " + secondVideoPath);
        try {
            String[] command1 = {"ffmpeg", "-i", firstVideoPath, "-c:v", "libx264", "-c:a", "aac", tempFirstVideo};
            String[] command2 = {"ffmpeg", "-i", secondVideoPath, "-c:v", "libx264", "-c:a", "aac", tempSecondVideo};

            if (executeCommand(command1) != 0 || executeCommand(command2) != 0) {
                return null;
            }

            Files.write(Paths.get(tempListPath), Arrays.asList("file '" + tempFirstVideo + "'", "file '" + tempSecondVideo + "'"));
            String[] concatCommand = {"ffmpeg", "-f", "concat", "-safe", "0", "-i", tempListPath, "-c", "copy", outputVideoPath};
            return executeCommand(concatCommand) == 0 ? outputVideoPath : null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String reEncoder(String inputPath) {
        String outputPath = "reencoded_" + UUID.randomUUID() + ".mp4";
        System.out.println("Re-encoding video: " + inputPath + " to MP4 format");
        trackTempFile(outputPath);

        try {
            String[] command = {
                    "ffmpeg", "-i", inputPath,
                    "-c:v", "libx264",
                    "-preset", "medium",
                    "-profile:v", "main",
                    "-pix_fmt", "yuv420p",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-movflags", "+faststart",
                    outputPath
            };

            int exitCode = executeCommand(command);
            if (exitCode == 0) {
                System.out.println("Successfully re-encoded video to: " + outputPath);
                return outputPath;
            } else {
                System.err.println("Failed to re-encode video");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("Exception during video re-encoding: " + e.getMessage());
            return null;
        }
    }

    public File fillSides(String path) {
        String outputPath = "filled_" + UUID.randomUUID() + ".mp4";
        trackTempFile(outputPath);

        String[] command = {
                "ffmpeg", "-i", path,
                "-vf", "pad=width=1280:height=720:x=(ow-iw)/2:y=(oh-ih)/2:color=black",
                "-c:v", "libx264",
                outputPath
        };
        try {
            return executeCommand(command) == 0 ? new File(outputPath) : null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    int executeCommand(String[] command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //System.out.println(line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Error executing command: " + String.join(" ", command));
        }
        return exitCode;
    }

    // Track temporary files
    private void trackTempFile(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            tempFiles.add(filePath);
        }
    }

    // Public method to clean up all temporary files
    public void cleanupAllTempFiles() {
        cleanup(tempFiles.toArray(new String[0]));
        tempFiles.clear();
    }

    public void cleanup(String... filePaths) {
        for (String path : filePaths) {
            if (path != null) {
                try {
                    File file = new File(path);
                    if (file.exists()) {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            file.deleteOnExit(); // Fallback to delete on JVM exit
                            System.out.println("Could not delete temp file immediately, marked for deletion on exit: " + path);
                        } else {
                            System.out.println("Successfully deleted temp file: " + path);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting " + path + ": " + e.getMessage());
                    e.printStackTrace();
                }

                // Remove from tracking list if present
                tempFiles.remove(path);
            }
        }
    }
}