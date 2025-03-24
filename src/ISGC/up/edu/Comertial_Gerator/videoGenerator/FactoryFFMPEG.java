package ISGC.up.edu.Comertial_Gerator.videoGenerator;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FactoryFFMPEG {
    private static FactoryFFMPEG instance;
    private List<String> createdFiles= new List<String>() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            return null;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return null;
        }

        @Override
        public boolean add(String s) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends String> c) {
            return false;
        }

        @Override
        public boolean addAll(int index, Collection<? extends String> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public String get(int index) {
            return "";
        }

        @Override
        public String set(int index, String element) {
            return "";
        }

        @Override
        public void add(int index, String element) {

        }

        @Override
        public String remove(int index) {
            return "";
        }

        @Override
        public int indexOf(Object o) {
            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {
            return 0;
        }

        @Override
        public ListIterator<String> listIterator() {
            return null;
        }

        @Override
        public ListIterator<String> listIterator(int index) {
            return null;
        }

        @Override
        public List<String> subList(int fromIndex, int toIndex) {
            return List.of();
        }
    };
    private int width;
    private int height;
    private List<String> tempFiles = new ArrayList<>();

    private FactoryFFMPEG() {
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
        createdFiles.add(outputPath);

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
        createdFiles.add(outputVideoPath);

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
        createdFiles.add(outputPath);

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
        createdFiles.add(outputPath);

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



    public String createVideoGrid(List<String> videoPaths, int rows, int cols) {
        // Validate inputs
        if (videoPaths == null || videoPaths.isEmpty() || rows <= 0 || cols <= 0) {
            System.err.println("Invalid input for video grid creation");
            return null;
        }

        // Ensure we don't exceed available videos
        int maxVideos = rows * cols;
        List<String> gridVideos = videoPaths.subList(0, Math.min(videoPaths.size(), maxVideos));

        // Pad with black videos if not enough videos
        while (gridVideos.size() < maxVideos) {
            gridVideos.add(createBlackVideo(width, height, 4)); // 4-second black video
        }

        String outputPath = "grid_" + UUID.randomUUID() + ".mp4";
        trackTempFile(outputPath);

        // Construct complex filter for grid layout
        StringBuilder filterComplex = new StringBuilder();

        // Input streams
        for (int i = 0; i < gridVideos.size(); i++) {
            filterComplex.append(String.format("[%d:v]scale=%d:%d:force_original_aspect_ratio=decrease,pad=%d:%d:(ow-iw)/2:(oh-ih)/2[v%d];",
                    i, width/cols, height/rows, width/cols, height/rows, i));
        }

        // Arrange grid
        filterComplex.append("  ");
        for (int i = 0; i < gridVideos.size(); i++) {
            filterComplex.append(String.format("[v%d]", i));
        }
        filterComplex.append(String.format("xstack=inputs=%d:layout=%dx%d[v]",
                gridVideos.size(), cols, rows));

        // Prepare FFmpeg command
        String[] command = {
                "ffmpeg",
                "-i", String.join(" -i ", gridVideos),
                "-filter_complex", filterComplex.toString(),
                "-map", "[v]",
                "-c:v", "libx264",
                "-preset", "medium",
                "-r", "30",
                "-pix_fmt", "yuv420p",
                outputPath
        };

        try {
            int exitCode = executeCommand(command);
            return exitCode == 0 ? outputPath : null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper method to create a black video
    private String createBlackVideo(int width, int height, int duration) {
        String outputPath = "black_" + UUID.randomUUID() + ".mp4";
        trackTempFile(outputPath);

        String[] command = {
                "ffmpeg",
                "-f", "lavfi",
                "-i", String.format("color=black:s=%dx%d:r=30", width, height),
                "-t", String.valueOf(duration),
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                outputPath
        };

        try {
            int exitCode = executeCommand(command);
            return exitCode == 0 ? outputPath : null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getCreatedFiles() {
        return createdFiles;
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