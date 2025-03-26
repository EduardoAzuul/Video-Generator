package ISGC.up.edu.Comertial_Gerator;


import ISGC.up.edu.Comertial_Gerator.FileReader.FileHandeler;
import ISGC.up.edu.Comertial_Gerator.MetaData.MetaData;
import ISGC.up.edu.Comertial_Gerator.videoGenerator.VideoWrapper;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        FileHandeler fh = FileHandeler.getInstance();
        fh.structure("C:/Users/josem/OneDrive/Imágenes/ComertialGenerator");    //This needs to be changed for the actual Path
        MetaData mData = MetaData.getInstance();
        String[][] metaData = mData.getMetaData(fh.getFilesList());


        MetaData.printMetaDataList(metaData);


        /*ChatAPIHandler chatAPI = new ChatAPIHandler();
        String userMessage = "tell me about the cold war";
        String apiResponse = chatAPI.messageAPI(userMessage);
        System.out.println("API Response: " + apiResponse);
        String imageUrl = chatAPI.generateImageFromPrompt("perros felices", 1024, 1024);
        System.out.println(imageUrl);*/


        //VideoModifier videoModifier = VideoModifier.getInstance(1080,720);
        VideoWrapper wrapper = VideoWrapper.getInstance(1080,720);




        String resultVideo = wrapper.createVideoFromMediaArray(metaData, "C:/Users/josem/OneDrive/Imágenes/ComertialGenerator/output.mp4");
        //String path = resultVideo.getAbsolutePath();
        openFile(resultVideo);

    }

// Create the combined video

    public static void openFile(String path) {
        File file = new File(path);

        if (!file.exists()) {
            System.out.println("File does not exist: " + path);
            return;
        }

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Desktop is not supported on this system.");
        }
    }

}