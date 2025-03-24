package ISGC.up.edu.Comertial_Gerator.MetaData;

import java.io.*;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;


public class MetaData implements Meta{

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

    public static  String[][] getMetaData(List<File> files){
        System.out.println("=== EXTRACTING METADATA FROM " + files.size() + " FILES ===");

        String[][] metaData = new String[files.size()][4];
        //Name, path, creation date, rotation
        int counter = 0;
        for (File file : files) {   //For all the given files
            //Extraction of name and path
            String filePath = file.getAbsolutePath();
            String fileName = file.getName();
            String fileCreationDate = getFileCreationDate(file);
            String fileRotation = getRotation(file);


            String[] metaDataRow = new String[]{fileName,filePath,fileCreationDate,fileRotation};
            metaData[counter] = metaDataRow;

            counter++;

            System.out.println("\nMetadata for: " + fileName);
            System.out.println("----------------------------------------");
            System.out.println("Path: " + filePath);
            System.out.println("Creation Date: " + fileCreationDate);
            System.out.println("Rotation: " + fileRotation);
            System.out.println("----------------------------------------");

        }
        metaData = order(metaData);
        return metaData;
    }


    private static String[][] order(String[][] metaData) {
        // Create a copy of the input array to avoid modifying the original
        String[][] sortedMetaData = Arrays.copyOf(metaData, metaData.length);

        // Sort the array based on the creation date (index 2)
        Arrays.sort(sortedMetaData, (row1, row2) -> {
            try {
                // Ensure both rows are not null and have enough elements
                if (row1 == null || row1.length < 3 || row2 == null || row2.length < 3) {
                    return 0;
                }

                // Parse ISO 8601 format dates using Instant
                Instant date1 = Instant.parse(row1[2]);
                Instant date2 = Instant.parse(row2[2]);

                // Compare the dates
                return date1.compareTo(date2);
            } catch (DateTimeParseException | NullPointerException e) {
                // If parsing fails, log the error and maintain original order
                System.err.println("Error parsing date: " +
                        (row1 != null ? row1[2] : "null") +
                        " or " +
                        (row2 != null ? row2[2] : "null"));
                return 0;
            }
        });

        return sortedMetaData;
    }



    //This funtion retrives the rotation of the file metadata
    private static String getRotation(File file) {
        try {
            // Execute ExifTool command to get rotation-related metadata
            ProcessBuilder pb = new ProcessBuilder(EXIFTOOL_COMMAND, "-Rotation", file.getAbsolutePath());
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Look specifically for the Rotation line
                if (line.contains("Rotation")) {
                    // Extract the numeric value
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String rotationValue = parts[1].trim();
                        // Ensure we return a clean rotation value
                        return rotationValue.replaceAll("[^0-9]", "");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "0";  // Default if no rotation information is found
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

    public static void printMetaDataList(String[][] metaData) {
        if (metaData == null || metaData.length == 0) {
            System.out.println("No metadata to display.");
            return;
        }

        System.out.println("\n===== METADATA LIST =====");
        System.out.println("----------------------------------------");
        System.out.println("| # | File Name | Path | Creation Date | Rotation |");
        System.out.println("----------------------------------------");

        for (int i = 0; i < metaData.length; i++) {
            String[] row = metaData[i];
            if (row != null && row.length >= 4) {
                System.out.printf("| %d | %s | %s | %s | %s |\n",
                        i + 1,
                        truncateString(row[0], 20),
                        truncateString(row[1], 30),
                        row[2],
                        row[3]);
            }
        }

        System.out.println("----------------------------------------");
        System.out.println("Total files: " + metaData.length);
    }

    private static String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }



    public static void executeProcess() {
        String[] command;
        //Determines the operating sistem
        if (System.getProperty("os.name").toLowerCase().contains("win")) {  //For windows
            command = new String[]{"cmd.exe", "/c", "echo Procesando imagenes... && timeout /t 1 && echo Generando video && echo Proceso finalizado"};
        } else {    //For Unix
            command = new String[]{"/bin/bash", "-c", "echo 'Procesando imágenes...' && sleep 1 && echo 'Generando video' && echo 'Proceso finalizado'"};
        }

        Process process = null;
        BufferedReader reader = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);   //Creates a class proces bulder and give it the command
            process = builder.start();  //Runs the command
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Proceso finalizado con código: " + process.waitFor());
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        finally { //In every case do
            try {
                if (reader != null) reader.close();
                if (process != null) process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
