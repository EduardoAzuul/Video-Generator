package ISGC.up.edu.Comertial_Gerator.MetaData;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
            String fileCreationDate = getFileCreationDate(filePath);
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
            // Handle null or incomplete rows
            if (row1 == null || row1.length < 3 || row2 == null || row2.length < 3) {
                return 0;
            }

            // Check if either date is "Unknown"
            if ("Unknown".equals(row1[2]) && "Unknown".equals(row2[2])) {
                return 0;
            }
            if ("Unknown".equals(row1[2])) {
                return 1;  // Unknown dates go to the end
            }
            if ("Unknown".equals(row2[2])) {
                return -1;  // Unknown dates go to the end
            }

            try {
                // Try parsing different date formats
                SimpleDateFormat[] formats = {
                        new SimpleDateFormat("yyyy:MM:dd HH:mm:ss"),  // ExifTool default format
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"),
                        new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
                };

                Date date1 = null;
                Date date2 = null;

                // Try parsing with multiple formats
                for (SimpleDateFormat format : formats) {
                    try {
                        date1 = format.parse(row1[2]);
                        date2 = format.parse(row2[2]);
                        break;  // If parsing succeeds, exit the loop
                    } catch (ParseException e) {
                        // Continue to next format
                    }
                }

                // If no format worked, return 0 to maintain original order
                if (date1 == null || date2 == null) {
                    System.err.println("Could not parse dates: " + row1[2] + " or " + row2[2]);
                    return 0;
                }

                // Compare the dates
                return date1.compareTo(date2);

            } catch (Exception e) {
                // Log any unexpected parsing errors
                System.err.println("Unexpected error parsing dates: " +
                        row1[2] + " or " + row2[2] +
                        " - Error: " + e.getMessage());
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




    public static String getFileCreationDate(String path) {
        try {
            // Array of possible date-related ExifTool tags to try
            if (path == null){
                return null;
            }

            String[] dateTags = {
                    "-CreateDate",
                    "-DateTimeOriginal",
                    "-ModifyDate",
                    "-FileModifyDate"
            };

            for (String tag : dateTags) {       //Tries all the posible names for the creation date
                String[] command = {
                        "exiftool",
                        tag,
                        "-s3",
                        path
                };

                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String dateValue = reader.readLine();

                int exitCode = process.waitFor();

                // Check if the date is valid (not empty, not 0000:00:00, not "Unknown")
                if (exitCode == 0 &&
                        dateValue != null &&
                        !dateValue.trim().isEmpty() &&
                        !dateValue.trim().equals("0000:00:00 00:00:00") &&
                        !dateValue.trim().equalsIgnoreCase("Unknown")) {
                    return dateValue.trim();
                }
            }

            return "Unknown";
        } catch (IOException | InterruptedException e) {
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
