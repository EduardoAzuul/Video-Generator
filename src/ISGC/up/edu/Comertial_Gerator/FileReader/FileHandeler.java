package ISGC.up.edu.Comertial_Gerator.FileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

// This class is responsible for selecting files from a folder for usage.
public class FileHandeler implements reader {
    private String filter;
    private String folderDir;
    private final List<BufferedReader> bufferedReaders = new ArrayList<>();
    private final List<String> fileNames = new ArrayList<>(); // To store file names

    // Setter for folder directory
    public void setFolderDir(String folderDir) {
        this.folderDir = folderDir;
    }

    // Setter for filter (file extension)
    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public BufferedReader getReader() {
        if (!bufferedReaders.isEmpty()) {
            return bufferedReaders.get(0); // Return the first reader as an example
        }
        return null;
    }

    @Override
    public void addReader(String fileName) {
        try {
            FileReader fileReader = new FileReader(fileName);
            bufferedReaders.add(new BufferedReader(fileReader));
            fileNames.add(fileName);
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + fileName);
        }
    }

    // This function reads and selects the files in the folder based on the filter.
    public void TakeFiles() {
        if (folderDir == null || filter == null) {
            System.out.println("Folder directory or filter not set.");
            return;
        }

        File folder = new File(folderDir);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid directory path: " + folderDir);
            return;
        }

        File[] listOfFiles = folder.listFiles((dir, name) -> name.endsWith(filter));

        if (listOfFiles != null && listOfFiles.length > 0) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    addReader(file.getAbsolutePath()); // Add reader for each valid file
                }
            }
        } else {
            System.out.println("No files found with the filter: " + filter);
        }
    }

    // Prints the names of the selected files
    public void printFiles() {
        if (fileNames.isEmpty()) {
            System.out.println("No files selected.");
            return;
        }

        System.out.println("Selected Files:");
        for (String fileName : fileNames) {
            System.out.println(fileName);
        }
    }
}
