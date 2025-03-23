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
                    if (rotatedFile != null) {
                        return rotatedFile;
                    }
                    return processedFile;
                }
                return processedFile;
            } else if (fileExtension.matches("\\.(mp4|avi|mov|wmv|flv|mkv)$")) {
                // Re-encode the video first
                processedFile = factoryFFMPEG.reEncoder(filePath);
                if (processedFile != null) {
                    tempFilesPaths.add(processedFile);
                }

                if (processedFile != null && rotation != 0) {
                    String rotatedFile = rotateVideo(processedFile, rotation);
                    if (rotatedFile != null) {
                        return rotatedFile;
                    }
                    return processedFile;
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

    private String rotateVideo(String videoPath, int rotation) {
        String tempOutputPath = "rotated_" + UUID.randomUUID() + ".mp4";
        tempFilesPaths.add(tempOutputPath);

        String transposeValue = switch (rotation) {
            case 90 -> "1";
            case 180 -> "2,2";
            case 270 -> "2";
            default -> "0";
        };

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
    }

    private void cleanup(List<String> filePaths) {
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
            }
        }
    }
}