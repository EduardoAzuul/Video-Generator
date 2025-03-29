package ISGC.up.edu.Comertial_Gerator.FileReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;

public class ImageDownloader {
    String url;
    String fileName;
    String path;

    public ImageDownloader(String url, String fileName, String path) {
        setUrl(url);
        setFileName(fileName);
        setPath(path);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void imageDownload (String url, String fileName, String Path) {
        setUrl(url);
        setFileName(fileName);
        setPath(Path);

        try {
            URL imageUrl = new URL(url);
            BufferedImage image = ImageIO.read(imageUrl);
            ImageIO.write(image, "jpg", new File(getPath(), getFileName()));
            System.out.println("Image downloaded successfully: " + getFileName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
