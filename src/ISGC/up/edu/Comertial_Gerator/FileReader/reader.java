package ISGC.up.edu.Comertial_Gerator.FileReader;

import java.io.BufferedReader;
import java.util.List;

interface reader {
    void addReader(String fileName);
    void setFolderDir(String folderDir);    //Direction of the folder
    void TakeFiles();
    void printFiles();//Console funtion to see if the files where picked in console
    List<BufferedReader> getFiles();    //Funtion to recive the files

}
