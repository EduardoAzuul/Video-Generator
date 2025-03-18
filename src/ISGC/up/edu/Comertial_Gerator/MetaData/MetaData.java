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

    public static  String[][] metaData(List<File> files){
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

        // Sort the array based on the date in the second column (index 1)
        Arrays.sort(sortedMetaData, (row1, row2) -> {
            try {
                // Parse ISO 8601 format dates using Instant
                Instant date1 = Instant.parse(row1[1]);
                Instant date2 = Instant.parse(row2[1]);

                // Compare the dates
                return date1.compareTo(date2);
            } catch (DateTimeParseException e) {
                // If parsing fails, compare as strings (fallback)
                return row1[1].compareTo(row2[1]);
            }
        });

        return sortedMetaData;
    }



    //This funtion retrives the rotation of the file metadata
    private static String getRotation(File file) {
        try {
            // Execute ExifTool command to get the Orientation or Rotate metadata
            ProcessBuilder pb = new ProcessBuilder("exiftool", "-Orientation", "-Rotate", file.getAbsolutePath());
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Orientation")) {
                    if (line.contains("180")) return "180";
                    if (line.contains("90 CW")) return "90";
                    if (line.contains("90 CCW")) return "270";
                    if (line.contains("0") || line.contains("Normal")) return "0";
                } else if (line.contains("Rotate")) {
                    // Some files use "Rotate" instead of "Orientation"
                    return line.split(":")[1].trim() + "°";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "0";
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
