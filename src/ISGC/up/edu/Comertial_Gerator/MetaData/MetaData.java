package ISGC.up.edu.Comertial_Gerator.MetaData;

import java.io.*;
import java.util.*;

public class MetaData {

    private static final String EXIFTOOL_COMMAND = "exiftool";
    private static MetaData instance; // Singleton instance

    private MetaData() {}

    public static MetaData getInstance() {
        if (instance == null) {
            synchronized (MetaData.class) {
                if (instance == null) {
                    instance = new MetaData();
                }
            }
        }
        return instance;
    }

    /***************************************************/

    public static Map<String, Map<String, String>> extractMetadataFromFiles(List<File> files) {
        Map<String, Map<String, String>> allMetadata = new HashMap<>();

        System.out.println("=== EXTRACTING METADATA FROM " + files.size() + " FILES ===");

        for (File file : files) {
            Map<String, String> fileMetadata = new HashMap<>();
            String filePath = file.getAbsolutePath();
            String fileName = file.getName();

            // Add basic file information
            fileMetadata.put("Name", fileName);
            fileMetadata.put("Path", filePath);
            fileMetadata.put("Creation Date", getFileCreationDate(file));
            fileMetadata.put("Modification Date", new Date(file.lastModified()).toString());

            // Get additional metadata using ExifTool
            Map<String, String> exifMetadata = ExtraerMetadata(file);
            // Merge the ExifTool metadata with our basic metadata
            fileMetadata.putAll(exifMetadata);

            // Store the metadata in the map
            allMetadata.put(filePath, fileMetadata);

            // Print metadata to console
            System.out.println("\nMetadata for: " + fileName);
            System.out.println("----------------------------------------");
            System.out.println("Path: " + filePath);
            System.out.println("Creation Date: " + fileMetadata.get("Creation Date"));
            System.out.println("Modification Date: " + fileMetadata.get("Modification Date"));
            System.out.println("Rotation: " + fileMetadata.getOrDefault("Rotation", "Not available"));
            System.out.println("----------------------------------------");
        }

        System.out.println("\n=== METADATA EXTRACTION COMPLETE ===\n");

        return allMetadata;
    }

    public static Map<String, String> ExtraerMetadata(File archivo) {
        Map<String, String> metadata = new HashMap<>();
        String[] command = new String[]{"exiftool", archivo.getAbsolutePath()};

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("[EXIF] " + line); // Debug: muestra la salida de exiftool
                if (line.contains("Rotation")) {
                    metadata.put("Rotation", line.substring(line.indexOf(":") + 1).trim());
                } else if (line.contains("Create Date")) {
                    metadata.put("Create Date", line.substring(line.indexOf(":") + 1).trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return metadata;
    }

    private static String getFileCreationDate(File file) {
        try {
            java.nio.file.Path path = file.toPath();
            java.nio.file.attribute.BasicFileAttributes attrs = java.nio.file.Files.readAttributes(
                    path, java.nio.file.attribute.BasicFileAttributes.class);

            return attrs.creationTime().toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    public static void ejecutarProceso() {
        String[] command = new String[]{"/bin/bash", "-c", "echo 'Procesando imágenes...' && sleep 1 && echo 'Generando video' && echo 'Proceso finalizado'"};
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            command = new String[]{"cmd.exe", "/c", "echo Procesando imágenes... && timeout /t 1 && echo Generando video && echo Proceso finalizado"};
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Proceso finalizado con código: " + process.waitFor());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
