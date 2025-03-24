package ISGC.up.edu.Comertial_Gerator.videoGenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VideoWrapper {
    private static VideoWrapper instance;
    private final FactoryFFMPEG factoryFFMPEG;
    private final int width;
    private final int height;
    private List<String> tempFilesPaths = new ArrayList<>();
    private List<String> generatedMp4Files = new ArrayList<>(); // Track only generated MP4 files

    private VideoWrapper(int width, int height) {
        this.width = width;
        this.height = height;
        this.factoryFFMPEG = FactoryFFMPEG.getInstance(width, height);
    }

    public static synchronized VideoWrapper getInstance(int width, int height) {
        if (instance == null) {
            instance = new VideoWrapper(width, height);
        }
        return instance;
    }

    public String createVideoFromMediaArray(String[][] mediaDataList, String outputPath) {
        if (mediaDataList == null || mediaDataList.length == 0) {
            System.out.println("No media data provided.");
            return null;
        }

        List<String> processedFiles = new ArrayList<>();
        String finalOutput = null;

        try {
            for (String[] mediaItem : mediaDataList) {
                if (mediaItem == null || mediaItem.length < 2 || mediaItem[1] == null) {
                    System.out.println("Invalid media item: " + (mediaItem != null ? String.join(",", mediaItem) : "null"));
                    continue;
                }

                String filePath = mediaItem[1];
                int rotation = 0;
                if (mediaItem.length > 3 && mediaItem[3] != null && !mediaItem[3].isEmpty()) {
                    try {
                        rotation = Integer.parseInt(mediaItem[3]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid rotation value: " + mediaItem[3]);
                    }
                }

                String processedFilePath = processMediaFile(filePath, rotation);
                if (processedFilePath != null) {
                    processedFiles.add(processedFilePath);
                    tempFilesPaths.add(processedFilePath);
                    // Track only MP4 files generated by the code
                    if (processedFilePath.toLowerCase().endsWith(".mp4")) {
                        generatedMp4Files.add(processedFilePath);
                    }
                }
            }

            if (processedFiles.isEmpty()) {
                System.out.println("No files were successfully processed.");
                return null;
            }

            finalOutput = concatenateAllVideos(processedFiles, outputPath);
            return finalOutput;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // Always clean up temporary files regardless of success or failure
            // Don't clean up the final output file
            if (finalOutput != null) {
                tempFilesPaths.remove(finalOutput);
                generatedMp4Files.remove(finalOutput);
            }
            // Create grid only from generated MP4 files
            if (!generatedMp4Files.isEmpty()) {
                String grid = Grid();
                if (grid != null) {
                    factoryFFMPEG.concatenateVideosPath(outputPath, grid, outputPath);
                    tempFilesPaths.add(grid); // Ensure grid is cleaned up later
                }
            }
            cleanup();
        }
    }

    private String processMediaFile(String filePath, int rotation) {
        if (filePath == null || !new File(filePath).exists()) {
            System.out.println("File does not exist: " + filePath);
            return null;
        }

        try {
            String fileExtension = filePath.substring(filePath.lastIndexOf(".")).toLowerCase();
            String processedFile = null;

            if (fileExtension.matches("\\.(jpg|jpeg|png|bmp|gif)$")) {
                processedFile = factoryFFMPEG.imageToVideo(filePath, width, height);
                if (processedFile != null) {
                    tempFilesPaths.add(processedFile);
                }

                if (processedFile != null && rotation != 0) {
                    String rotatedFile = rotateVideo(processedFile, rotation);
                    return rotatedFile != null ? rotatedFile : processedFile;
                }
                return processedFile;
            } else if (fileExtension.matches("\\.(mp4|avi|mov|wmv|flv|mkv)$")) {
                // Re-encode the video first
                processedFile = factoryFFMPEG.reEncoder(filePath);
                if (processedFile != null) {
                    tempFilesPaths.add(processedFile);
                }

                // Get current rotation metadata from the video
                int currentRotation = getVideoRotation(processedFile);

                // Only rotate if the current rotation is different from the desired rotation
                if (processedFile != null && rotation != 0 && currentRotation != rotation) {
                    String rotatedFile = rotateVideo(processedFile, rotation);
                    return rotatedFile != null ? rotatedFile : processedFile;
                }
                return processedFile;
            } else {
                System.out.println("Unsupported file format: " + fileExtension);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getVideoRotation(String videoPath) {
        try {
            String[][] probeCommands = {
                    {"ffprobe", "-i", videoPath, "-show_entries", "stream_tags=rotate", "-v", "quiet", "-of", "csv=p=0"},
                    {"exiftool", "-Rotation", "-s3", videoPath}
            };

            for (String[] command : probeCommands) {
                Process process = new ProcessBuilder(command).start();
                process.waitFor();

                String result = new String(process.getInputStream().readAllBytes()).trim();
                if (!result.isEmpty()) {
                    try {
                        return Integer.parseInt(result);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }

            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String rotateVideo(String videoPath, int rotation) {
        String tempOutputPath = "rotated_" + UUID.randomUUID() + ".mp4";
        tempFilesPaths.add(tempOutputPath);

        String transposeValue = "0";
        switch (rotation) {
            case 90 -> transposeValue = "1";
            case 180 -> transposeValue = "2,2";
            case 270 -> transposeValue = "3";
        }

        String[] command = {
                "ffmpeg", "-i", videoPath,
                "-vf", "transpose=" + transposeValue,
                "-c:v", "libx264",
                "-c:a", "aac",
                "-preset", "medium",
                "-s", width + "x" + height,
                tempOutputPath
        };

        try {
            return (factoryFFMPEG.executeCommand(command) == 0) ? tempOutputPath : null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String Grid(){
        return factoryFFMPEG.createVideoGrid(factoryFFMPEG.getCreatedFiles(), 4, 4);
    }

    private String concatenateAllVideos(List<String> videoPaths, String outputPath) {
        String listFilePath = "concat_list_" + UUID.randomUUID() + ".txt";
        tempFilesPaths.add(listFilePath);

        try {
            File listFile = new File(listFilePath);
            FileWriter writer = new FileWriter(listFile);
            for (String videoPath : videoPaths) {
                writer.write("file '" + videoPath + "'\n");
            }
            writer.close();

            String[] command = {
                    "ffmpeg", "-f", "concat",
                    "-safe", "0",
                    "-i", listFilePath,
                    "-c", "copy",
                    outputPath
            };

            if (factoryFFMPEG.executeCommand(command) == 0) {
                return outputPath;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void cleanup() {
        cleanup(tempFilesPaths);
        tempFilesPaths.clear();
        generatedMp4Files.clear();
    }

    private void cleanup(List<String> filePaths) {
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
            }
        }
    }
}