package ISGC.up.edu.Comertial_Gerator.videoGenerator;

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

/**
 * A factory class for handling video processing operations using FFMPEG.
 * Implements the Singleton pattern to ensure only one instance exists.
 */
public class FactoryFFMPEG {
    // Singleton instance
    private static FactoryFFMPEG instance;

    // Video dimensions
    private int width;
    private int height;

    // List to track temporary files for cleanup
    private List<String> tempFiles = new ArrayList<>();
    private List<String> gridFiles = new ArrayList<>();

    // Private constructor for Singleton pattern
    private FactoryFFMPEG() {
        this.width = 1024;
        this.height = 768;
    }

    /* ======================== */
    /* == Singleton Methods == */
    /* ======================== */

    /**
     * Gets the singleton instance of FactoryFFMPEG with specified dimensions.
     * If dimensions are invalid, keeps the default values.
     *
     * @param width  The desired width (positive value to update)
     * @param height The desired height (positive value to update)
     * @return The singleton instance
     */
    public static synchronized FactoryFFMPEG getInstance(int width, int height) {
        if (instance == null) {
            instance = new FactoryFFMPEG();
        }
        instance.width = (width > 0) ? width : instance.width;
        instance.height = (height > 0) ? height : instance.height;
        return instance;
    }

    /**********************************************************************************************/
    /**********************************************************************************************/
                /* ======================== */
                /* == Core FFMPEG Methods == */
                /* ======================== */

    /**
     * Converts an image to a video file with specified dimensions.
     *
     * @param path   Path to the input image
     * @param width  Output video width
     * @param height Output video height
     * @return Path to the generated video, or null if failed
     */
    public String imageToVideo(String path, int width, int height) {
        String outputPath = "img_" + UUID.randomUUID() + ".mp4";
        System.out.println("Generating video from: " + path);
        trackTempFile(outputPath);
        trackGridFiles(outputPath);

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


    /**
     * Concatenates two video files into one output file.
     *
     * @param firstVideoPath   Path to the first video
     * @param secondVideoPath  Path to the second video
     * @param outputVideoPath  Path for the output video
     * @return Path to the concatenated video, or null if failed
     */
    public String concatenateVideosPath(String firstVideoPath, String secondVideoPath, String outputVideoPath) {
        // Solo agregamos archivos que no son concatenados a filesForGrid

        String tempFirstVideo = "temp_first_" + UUID.randomUUID() + ".mp4";
        String tempSecondVideo = "temp_second_" + UUID.randomUUID() + ".mp4";
        String tempListPath = "concat_list_" + UUID.randomUUID() + ".txt";

        trackTempFile(tempFirstVideo);
        trackTempFile(tempSecondVideo);
        trackTempFile(tempListPath);

        System.out.println("Concatenating: " + firstVideoPath + " and " + secondVideoPath);

        try {
            // Re-encode both videos to ensure compatibility
            String[] command1 = {"ffmpeg", "-i", firstVideoPath, "-c:v", "libx264", "-c:a", "aac", tempFirstVideo};
            String[] command2 = {"ffmpeg", "-i", secondVideoPath, "-c:v", "libx264", "-c:a", "aac", tempSecondVideo};

            if (executeCommand(command1) != 0 || executeCommand(command2) != 0) {
                return null;
            }

            // Create concatenation list file
            Files.write(Paths.get(tempListPath),
                    Arrays.asList("file '" + tempFirstVideo + "'", "file '" + tempSecondVideo + "'"));

            // Execute concatenation command
            String[] concatCommand = {
                    "ffmpeg", "-f", "concat",
                    "-safe", "0",
                    "-i", tempListPath,
                    "-c", "copy",
                    outputVideoPath
            };

            return executeCommand(concatCommand) == 0 ? outputVideoPath : null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Re-encodes a video file to standard MP4 format.
     *
     * @param inputPath Path to the input video
     * @return Path to the re-encoded video, or null if failed
     */
    public String reEncoder(String inputPath) {
        String outputPath = "reencoded_" + UUID.randomUUID() + ".mp4";              //Generates a unique output path
        System.out.println("Re-encoding video: " + inputPath + " to MP4 format");
        trackTempFile(outputPath);
        trackGridFiles(outputPath);


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

    /* ======================== */
    /* == Utility Methods == */
    /* ======================== */

    /**
     * Executes a system command and returns the exit code.
     *
     * @param command The command and arguments to execute
     * @return Exit code of the process (0 for success)
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the process is interrupted
     */
    int executeCommand(String[] command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        // Read and discard output (uncomment to debug)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // Uncomment to see FFMPEG output: System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Error executing command: " + String.join(" ", command));
        }
        return exitCode;
    }

    /**
     * Tracks a temporary file for automatic cleanup.
     * this funtion adds the created files to a list of files made
     */
    private void trackTempFile(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            tempFiles.add(filePath);
        }
    }

    private void trackGridFiles(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            gridFiles.add(filePath);
        }
    }


    public String gridVideo(String outputPath) {
        if (gridFiles == null || gridFiles.isEmpty()) {
            System.err.println("No valid files in filesForGrid to create grid video.");
            return null;
        }

        int maxDuration = 8; // Maximum duration in seconds
        int fileCount = gridFiles.size();

        // Calculate optimal grid layout
        int columns = (int) Math.ceil(Math.sqrt(fileCount));
        int rows = (int) Math.ceil((double)fileCount / columns);

        // Ensure reasonable aspect ratio (at least 1:2)
        if ((double)width/height > 2.0) {
            columns = Math.min(columns * 2, fileCount);
            rows = (int) Math.ceil((double)fileCount / columns);
        }

        // Calculate cell dimensions
        int cellWidth = width / columns;
        int cellHeight = height / rows;

        StringBuilder command = new StringBuilder("ffmpeg ");
        StringBuilder filterComplex = new StringBuilder();
        int counter = 0;

        // Process input files
        for (String file : gridFiles) {
            File videoFile = new File(file);
            if (videoFile.exists()) {
                command.append("-i \"").append(file).append("\" ");
                filterComplex.append("[")
                        .append(counter)
                        .append(":v] trim=end=").append(maxDuration)
                        .append(",setpts=PTS-STARTPTS, scale=")
                        .append(cellWidth).append(":").append(cellHeight)
                        .append(":force_original_aspect_ratio=increase")
                        .append(",crop=").append(cellWidth).append(":").append(cellHeight)
                        .append(" [a").append(counter).append("]; ");
                counter++;
            }
        }

        if (counter == 0) {
            System.err.println("No valid files found in filesForGrid.");
            return null;
        }

        // Build grid layout positions
        StringBuilder layout = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                int index = r * columns + c;
                if (index < counter) {
                    layout.append(c * cellWidth).append("_").append(r * cellHeight);
                    if (index < counter - 1) {
                        layout.append("|");
                    }
                }
            }
        }

        // Build xstack inputs
        StringBuilder xstackInputs = new StringBuilder();
        for (int i = 0; i < counter; i++) {
            xstackInputs.append("[a").append(i).append("]");
        }

        // Construct filter chain
        filterComplex.append(xstackInputs.toString())
                .append("xstack=inputs=").append(counter)
                .append(":layout=").append(layout.toString())
                .append("[grid]; ")
                .append("[grid]scale=").append(width).append(":").append(height)
                .append("[final]");

        command.append("-filter_complex \"")
                .append(filterComplex)
                .append("\" -map \"[final]\" ")
                .append("-c:v libx264 -preset fast -t ").append(maxDuration)
                .append(" -f mp4 \"")
                .append(outputPath).append("\"");

        System.out.println("Executing FFmpeg command: " + command);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.toString().split(" "));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Video grid created successfully at " + outputPath);
                return outputPath;
            } else {
                System.err.println("FFmpeg process failed with exit code: " + exitCode);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* ======================== */
    /* == Cleanup Methods == */
    /* ======================== */

    /**
     * Cleans up all tracked temporary files.
     */
    public void cleanupAllTempFiles() {
        cleanup(tempFiles.toArray(new String[0]));
        tempFiles.clear();
    }

    /**
     * Deletes specified files and removes them from tracking.
     *
     * @param filePaths Varargs of file paths to delete
     */
    public void cleanup(String... filePaths) {
        for (String path : filePaths) {
            if (path != null) {
                try {
                    File file = new File(path);
                    if (file.exists()) {
                        boolean deleted = file.delete();
                        if (!deleted) {
                            file.deleteOnExit();
                            System.out.println("Could not delete temp file immediately, marked for deletion on exit: " + path);
                        } else {
                            System.out.println("Successfully deleted temp file: " + path);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error deleting " + path + ": " + e.getMessage());
                    e.printStackTrace();
                }
                tempFiles.remove(path);
            }
        }
    }
}