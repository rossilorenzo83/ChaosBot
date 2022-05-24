package com.lr.utils;

import com.lr.business.ImageNotMatchedException;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.lr.business.CoreMechanics.CONVERT_IMG_FLAG;

@Slf4j
public class ScreenUtils {


    //Somehow this have to be kept low. Investigate / find a decent tutorial on appropriate flags
    public static final double MIN_QUALITY_THRESHOLD = 0.44;

    public static String takeScreenCapture(WinUtils.WindowInfo windowInfo) throws AWTException, IOException {
        Rectangle screenRect = new Rectangle(windowInfo.rect.left, windowInfo.rect.top, Math.abs(windowInfo.rect.right
                - windowInfo.rect.left), Math.abs(windowInfo.rect.bottom - windowInfo.rect.top));
        ;
        BufferedImage capture = new Robot().createScreenCapture(screenRect);
        String filePath = "tmp" + windowInfo.title + ".jpg";
        ImageIO.write(capture, "jpg", new File(filePath ));
        return filePath;
    }

    public static Double[] findCoordsOnScreen(String pathImgToFind, Mat fullScreenImg, WinUtils.WindowInfo windowInfo) throws URISyntaxException, ImageNotMatchedException {

        try {
            File f = new File(ClassLoader.getSystemResource(pathImgToFind).toURI());
            Mat toMatch = Imgcodecs.imread(f.getPath(), CONVERT_IMG_FLAG);
            System.out.println("Loaded image dimensions" + toMatch.size().toString());


            int machMethod = Imgproc.TM_CCOEFF_NORMED;
            Mat outputImage = new Mat();
            Imgproc.matchTemplate(fullScreenImg, toMatch, outputImage, machMethod);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(outputImage);

            if (mmr.maxVal >= MIN_QUALITY_THRESHOLD) {

                org.opencv.core.Point matchLoc = mmr.maxLoc;
                //Draw rectangle on result image
                Imgproc.rectangle(fullScreenImg, matchLoc, new Point(matchLoc.x + toMatch.cols(),
                        matchLoc.y + toMatch.rows()), new Scalar(255, 255, 255));

                Double offsetX = matchLoc.x;
                Double offsetY = matchLoc.y;
                Double absXCoord = windowInfo.getRect().left + offsetX + toMatch.size().width / 2;
                Double absYCoord = windowInfo.getRect().top + offsetY + toMatch.size().height / 2;
                return new Double[]{absXCoord, absYCoord};
            } else{

                log.error("Insufficient confidence {} matching the provided template", mmr.maxVal);
                throw new ImageNotMatchedException("Cannot find img: " + pathImgToFind);
            }

        }
        catch(CvException e){
            throw new ImageNotMatchedException(e.getMessage());
        }

    }
}
