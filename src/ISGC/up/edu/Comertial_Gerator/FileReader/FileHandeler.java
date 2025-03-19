package ISGC.up.edu.Comertial_Gerator.FileReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//This class is designed into a singleton pattern
// This class is responsible for selecting files from a folder for usage.
public class FileHandeler implements Reader {
    private static FileHandeler instance;
    private List<String> filter = new ArrayList<>();
    private String folderDir;
    private final List<BufferedReader> bufferedReaders = new ArrayList<>();
    private final List<File> files = new ArrayList<>(); // To store the File objects

    // Private constructor to prevent direct instantiation
    private FileHandeler() {
    }

    // Static method to get the singleton instance
    public static synchronized FileHandeler getInstance() {
        if (instance == null) {
            instance = new FileHandeler();
        }
        return instance;
    }

    /****************** ETERS AND SETERS ************************/
    // Setter for folder directory
    public void setFolderDir(String folderDir) {
        this.folderDir = folderDir;
        System.out.println("Folder: " + folderDir);
    }

    // Setter for filter (file extension)
    private void setFilter(List<String> filters) {
        this.filter = filters;
    }

    /*********************** STRUCTURE************************************/

    // Class to read the user files
    public void structure(String path) {
        // User entered file reader
        System.out.println("User entered file structure");
        setFilter(Arrays.asList(".png", ".jpg", ".mp4", ".mkv"));   // sets the filter
        setFolderDir(path);
        setFolderDir("C:/Users/josem/OneDrive/Imágenes/ComertialGenerator");    // REMOVE; TEST ONLY
        TakeFiles();    // Puts the valid files of the folder in a list of buffer readers accessible in the class
        printFiles();   // Prints the picked files in console
    }

    public void addReader(String fileName) {
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            bufferedReaders.add(new BufferedReader(fileReader));
            files.add(file); // Store the corresponding file
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName);
        }
    }

    // This function reads and selects the files in the folder based on the filter.
    public void TakeFiles() {
        if (folderDir == null || filter.isEmpty()) {  // Check if filters are set and there is a folder
            System.out.println("Folder directory or filters not set.");
            return;
        }

        // Reading and creation of the folder in the program
        File folder = new File(folderDir);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid directory path: " + folderDir);
            return;
        }

        System.out.println("The path is valid: " + folderDir);

        // Process the files
        File[] listOfFiles = folder.listFiles((dir, name) -> {
            for (String ext : filter) {
                if (name.toLowerCase().endsWith(ext)) {
                    return true;
                }
            }
            return false;
        });

        if (listOfFiles != null && listOfFiles.length > 0) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    addReader(file.getAbsolutePath()); // Add reader for each valid file
                }
            }
        } else {
            System.out.println("No files found with the filters: " + filter);
        }
    }

    // Prints the names of the selected files
    public void printFiles() {
        if (files.isEmpty()) {
            System.out.println("No files selected.");
            return;
        }

        System.out.println("Selected Files:");
        for (File file : files) {
            System.out.println(file.getAbsolutePath());
        }
    }

    @Override
    public List<BufferedReader> getFiles() {
        return List.of();
    }

    // Function to get all the files stored
    public List<File> getFilesList() {
        return files; // Return the list of File objects
    }
}
