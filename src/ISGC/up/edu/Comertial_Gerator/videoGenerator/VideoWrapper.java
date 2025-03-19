package ISGC.up.edu.Comertial_Gerator.videoGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A wrapper class for VideoModifier that handles batch processing of media files
 * with support for videos and images, including rotation.
 */
public class VideoWrapper {
    private static VideoWrapper instance;
    private final VideoModifier videoModifier;
    private final int width;
    private final int height;

    /**
     * Private constructor to enforce singleton pattern
     *
     * @param width  The width for output videos
     * @param height The height for output videos
     */
    private VideoWrapper(int width, int height) {
        this.width = width;
        this.height = height;
        this.videoModifier = VideoModifier.getInstance(width, height);
    }

    /**
     * Provides a single instance of VideoWrapper
     *
     * @param width  The width for output videos
     * @param height The height for output videos
     * @return The single instance of VideoWrapper
     */
    public static synchronized VideoWrapper getInstance(int width, int height) {
        if (instance == null) {
            instance = new VideoWrapper(width, height);
        }
        return instance;
    }

    /**
     * Processes a String array of media files and combines them into a single video
     *
     * @param mediaData A 2D array where:
     *                  - Second column [x][1] contains the file path
     *                  - Fourth column [x][3] contains rotation in degrees (0, 90, 180, or 270)
     * @param outputPath The path for the final output video
     * @return The File object of the created video or null if processing failed
     */
    public File createVideoFromMediaArray(String[][] mediaData, String outputPath) {
        if (mediaData == null || mediaData.length == 0) {
            System.out.println("No media data provided");
            return null;
        }

        List<String> processedFiles = new ArrayList<>();

        try {
            // Process each media file
            for (String[] mediaItem : mediaData) {
                String filePath = mediaItem[1];
                int rotation = 0;

                // Check if rotation value exists and parse it
                if (mediaItem.length > 3 && mediaItem[3] != null && !mediaItem[3].isEmpty()) {
                    try {
                        rotation = Integer.parseInt(mediaItem[3]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid rotation value: " + mediaItem[3] + ". Using default (0)");
                    }
                }

                // Process file based on type
                String processedFilePath = processMediaFile(filePath, rotation);
                if (processedFilePath != null) {
                    processedFiles.add(processedFilePath);
                }
            }

            // Concatenate all processed files
            if (processedFiles.size() > 0) {
                return concatenateAllVideos(processedFiles, outputPath);
            } else {
                System.out.println("No files were successfully processed");
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            cleanup(processedFiles);
            return null;
        }
    }

    /**
     * Processes an individual media file based on its type and rotation
     *
     * @param filePath Path to the media file
     * @param rotation Rotation in degrees (0, 90, 180, or 270)
     * @return Path to the processed video file
     */
    private String processMediaFile(String filePath, int rotation) {
        if (filePath == null || !new File(filePath).exists()) {
            System.out.println("File does not exist: " + filePath);
            return null;
        }

        try {
            String fileExtension = filePath.substring(filePath.lastIndexOf(".")).toLowerCase();
            String tempOutputPath = "temp_" + UUID.randomUUID().toString() + ".mp4";

            // Handle image files
            if (fileExtension.matches("\\.(jpg|jpeg|png|bmp|gif)$")) {
                // Convert image to video
                File videoFile = videoModifier.imageToVideo(filePath, width, height);

                if (videoFile != null && rotation != 0) {
                    // Rotate the video if needed
                    return rotateVideo(videoFile.getPath(), rotation, tempOutputPath);
                } else if (videoFile != null) {
                    return videoFile.getPath();
                }
            }
            // Handle video files
            else if (fileExtension.matches("\\.(mp4|avi|mov|wmv|flv|mkv)$")) {
                if (rotation != 0) {
                    // Rotate the video if needed
                    return rotateVideo(filePath, rotation, tempOutputPath);
                } else {
                    // Re-encode the video to ensure compatibility
                    return reEncodeVideo(filePath, tempOutputPath);
                }
            } else {
                System.out.println("Unsupported file format: " + fileExtension);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Rotates a video by the specified angle
     *
     * @param videoPath Path to the input video
     * @param rotation Rotation angle in degrees
     * @param outputPath Path for the rotated video
     * @return Path to the rotated video file
     */
    private String rotateVideo(String videoPath, int rotation, String outputPath) {
        try {
            // Map rotation angle to ffmpeg transpose filter values
            String transposeValue;
            switch (rotation) {
                case 90:
                    transposeValue = "1"; // 90 degrees clockwise
                    break;
                case 180:
                    transposeValue = "2,2"; // 180 degrees (2 times 90 degrees)
                    break;
                case 270:
                    transposeValue = "2"; // 90 degrees counterclockwise
                    break;
                default:
                    // No rotation
                    return reEncodeVideo(videoPath, outputPath);
            }

            // Construct FFmpeg command with rotation
            String[] command = {
                    "ffmpeg", "-i", videoPath,
                    "-vf", "transpose=" + transposeValue,
                    "-c:v", "libx264", "-c:a", "aac",
                    "-preset", "medium",
                    "-s", width + "x" + height,
                    outputPath
            };

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Log the output
            BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Video rotated successfully: " + outputPath);
                return outputPath;
            } else {
                System.out.println("Error rotating video");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Re-encodes a video to ensure compatibility for concatenation
     *
     * @param videoPath Path to the input video
     * @param outputPath Path for the re-encoded video
     * @return Path to the re-encoded video file
     */
    private String reEncodeVideo(String videoPath, String outputPath) {
        try {
            // Re-encode the video with consistent settings
            String[] command = {
                    "ffmpeg", "-i", videoPath,
                    "-c:v", "libx264", "-c:a", "aac",
                    "-preset", "medium",
                    "-s", width + "x" + height,
                    outputPath
            };

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Log the output
            BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Video re-encoded successfully: " + outputPath);
                return outputPath;
            } else {
                System.out.println("Error re-encoding video");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Concatenates all processed videos into a single output file
     *
     * @param videoPaths List of paths to videos to concatenate
     * @param outputPath Path for the final video
     * @return The File object of the created video or null if concatenation failed
     */
    private File concatenateAllVideos(List<String> videoPaths, String outputPath) {
        if (videoPaths.size() == 1) {
            // If there's only one video, just copy it to the output path
            try {
                Files.copy(Paths.get(videoPaths.get(0)), Paths.get(outputPath));
                return new File(outputPath);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        // For multiple videos, concatenate them sequentially
        String tempOutput = outputPath;
        File result = null;

        for (int i = 0; i < videoPaths.size() - 1; i++) {
            String firstVideo = videoPaths.get(i);
            String secondVideo = videoPaths.get(i + 1);

            // For intermediate steps, use temporary files
            String intermediateOutput = (i < videoPaths.size() - 2) ?
                    "temp_concat_" + UUID.randomUUID().toString() + ".mp4" : outputPath;

            result = videoModifier.concatenateVideos(firstVideo, secondVideo, intermediateOutput);

            // Clean up intermediate files
            if (i > 0) {
                new File(tempOutput).delete();
            }

            if (result == null) {
                System.out.println("Failed to concatenate videos at step " + i);
                cleanup(videoPaths);
                return null;
            }

            tempOutput = intermediateOutput;
        }

        // Clean up all processed files except the final output
        for (String path : videoPaths) {
            new File(path).delete();
        }

        return result;
    }

    /**
     * Cleans up temporary files
     *
     * @param filePaths List of file paths to clean up
     */
    private void cleanup(List<String> filePaths) {
        for (String path : filePaths) {
            try {
                Files.deleteIfExists(Paths.get(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}