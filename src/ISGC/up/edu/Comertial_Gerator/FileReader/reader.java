package ISGC.up.edu.Comertial_Gerator.FileReader;

import java.io.BufferedReader;

interface reader {
    BufferedReader getReader();
    void addReader(String fileName);
    void setFolderDir(String folderDir);    //Direction of the folder
    void TakeFiles();

}
