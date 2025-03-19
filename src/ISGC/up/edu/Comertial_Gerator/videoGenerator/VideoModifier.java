package ISGC.up.edu.Comertial_Gerator.videoGenerator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class VideoModifier {
    private static VideoModifier instance;
    private Robot robot;
    private int width;
    private int height;

    private VideoModifier() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        this.width = 1024;  // Default values
        this.height = 768;
    }

    //This method ensures that multiple instances of videomodifier are not being created to save memory
    public static synchronized VideoModifier getInstance(int width, int height) {
        if (instance == null) {
            instance = new VideoModifier();
        }

        if (width > 0) {
            instance.width = width;
        }
        if (height > 0) {
            instance.height = height;
        }

        return instance;
    }

    public File fillSides(String path) {
        // Implementation needed
        return null;
    }

    public File imageToVideo(String path, int width, int height) {
        String outputPath = path.replaceFirst("\\.\\w+$", ".mp4"); // Replaces the file extension with mp4
        String tempImagePath = path.replaceFirst("\\.\\w+$", "_processed.png"); // Temporary path for processed image

        try {
            // Load the original image
            File imageFile = new File(path);
            BufferedImage originalImage = ImageIO.read(imageFile);

            // Create a Robot instance for screen capture/manipulation
            // No need to create a new Robot instance here since we already have one as a class member
            robotWake(); // Ensure robot is initialized

            // Create a new BufferedImage with the desired dimensions
            BufferedImage processedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            processedImage.getGraphics().drawImage(originalImage, 0, 0, width, height, null);

            // Save the processed image
            ImageIO.write(processedImage, "png", new File(tempImagePath));

            // Create video from the processed image using FFMPEG
            String[] command = {
                    "ffmpeg", "-loop", "1", "-t", "4", "-i", tempImagePath,
                    "-vf", "scale=" + width + ":" + height + ",format=yuv420p",
                    "-r", "30", outputPath
            };

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();

            // Clean up the temporary file
            new File(tempImagePath).delete();

            if (exitCode == 0) {
                System.out.println("Video created successfully: " + outputPath);
                return new File(outputPath);
            } else {
                System.out.println("Error creating video.");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();

            // Clean up the temporary file if it exists
            File tempFile = new File(tempImagePath);
            if (tempFile.exists()) {
                tempFile.delete();
            }

            return null;
        }
    }

    public File concatenateVideos(String firstVideoPath, String secondVideoPath, String outputVideoPath) {
        try {
            // First, get information about the first video to match formats
            ProcessBuilder probeBuilder = new ProcessBuilder(
                    "ffprobe", "-v", "error", "-select_streams", "v:0",
                    "-show_entries", "stream=width,height,r_frame_rate",
                    "-of", "csv=p=0", firstVideoPath
            );
            probeBuilder.redirectErrorStream(true);
            Process probeProcess = probeBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()));
            String info = reader.readLine();
            probeProcess.waitFor();

            // Parse the video info
            String[] videoInfo = info.split(",");
            String width = videoInfo[0];
            String height = videoInfo[1];
            String frameRate = videoInfo[2];

            // Generate temporary files with consistent encoding
            String tempFirstVideo = "temp_first.mp4";
            String tempSecondVideo = "temp_second.mp4";
            String tempListPath = "concat_list.txt";

            // Re-encode the first video
            String[] command1 = {
                    "ffmpeg", "-i", firstVideoPath,
                    "-c:v", "libx264", "-c:a", "aac",
                    "-s", width + "x" + height,
                    "-r", frameRate,
                    tempFirstVideo
            };

            // Re-encode the second video to match the first
            String[] command2 = {
                    "ffmpeg", "-i", secondVideoPath,
                    "-c:v", "libx264", "-c:a", "aac",
                    "-s", width + "x" + height,
                    "-r", frameRate,
                    tempSecondVideo
            };

            // Execute first re-encoding
            ProcessBuilder builder1 = new ProcessBuilder(command1);
            builder1.redirectErrorStream(true);
            Process process1 = builder1.start();
            logProcess(process1);
            int exitCode1 = process1.waitFor();

            if (exitCode1 != 0) {
                System.out.println("Error re-encoding first video.");
                cleanup(tempFirstVideo);
                return null;
            }

            // Execute second re-encoding
            ProcessBuilder builder2 = new ProcessBuilder(command2);
            builder2.redirectErrorStream(true);
            Process process2 = builder2.start();
            logProcess(process2);
            int exitCode2 = process2.waitFor();

            if (exitCode2 != 0) {
                System.out.println("Error re-encoding second video.");
                cleanup(tempFirstVideo, tempSecondVideo);
                return null;
            }

            // Create a text file listing the videos to concatenate
            List<String> fileList = Arrays.asList(
                    "file '" + new File(tempFirstVideo).getAbsolutePath() + "'",
                    "file '" + new File(tempSecondVideo).getAbsolutePath() + "'"
            );

            // Write the file list to the temporary file
            Path path = Paths.get(tempListPath);
            Files.write(path, fileList);

            // Concatenate the re-encoded videos
            String[] concatCommand = {
                    "ffmpeg",
                    "-f", "concat",
                    "-safe", "0",
                    "-i", tempListPath,
                    "-c", "copy",
                    outputVideoPath
            };

            ProcessBuilder concatBuilder = new ProcessBuilder(concatCommand);
            concatBuilder.redirectErrorStream(true);
            Process concatProcess = concatBuilder.start();
            logProcess(concatProcess);
            int concatExitCode = concatProcess.waitFor();

            // Clean up
            cleanup(tempListPath, tempFirstVideo, tempSecondVideo);

            if (concatExitCode == 0) {
                System.out.println("Videos concatenated successfully with re-encoding: " + outputVideoPath);
                return new File(outputVideoPath);
            } else {
                System.out.println("Error concatenating videos with re-encoding.");
                return null;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            cleanup("concat_list.txt", "temp_first.mp4", "temp_second.mp4");
            return null;
        }
    }

    private void logProcess(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }

    private void robotWake() {   // Creates the bot if it doesn't exist
        if (robot == null) {
            try {
                this.robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
    }

    private void cleanup(String... filePaths) {
        for (String path : filePaths) {
            if (path != null) {
                try {
                    Files.deleteIfExists(Paths.get(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cleanup() {    // Destroys the robot
        if (robot != null) {
            robot = null;
            //System.out.println("Robot instance cleaned up.");
        }
    }
}