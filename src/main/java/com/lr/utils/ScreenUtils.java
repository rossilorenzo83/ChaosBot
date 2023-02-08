package com.lr.utils;

import com.lr.business.ImageNotMatchedException;
import com.lr.config.GeneralConfig;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;

import static com.lr.business.CoreMechanics.CONVERT_IMG_FLAG;
import static org.opencv.core.Core.*;
import static org.opencv.imgproc.Imgproc.*;

@Slf4j
public class ScreenUtils {


    //Somehow this have to be kept low. Investigate / find a decent tutorial on appropriate flags
    public static final double MIN_QUALITY_THRESHOLD = 0.70;


    /**
     * Return a string containing the filePath of the captured image
     *
     * @param windowInfo
     * @return
     * @throws AWTException
     * @throws IOException
     */
    public static String takeScreenCapture(WinUtils.WindowInfo windowInfo) throws AWTException, IOException {
        return takeScreenCapture(windowInfo, "");
    }

    public static String takeScreenCapture(WinUtils.WindowInfo windowInfo, String postfix) throws AWTException, IOException {
        Rectangle screenRect = new Rectangle(windowInfo.rect.left, windowInfo.rect.top, Math.abs(windowInfo.rect.right
                - windowInfo.rect.left), Math.abs(windowInfo.rect.bottom - windowInfo.rect.top));

        BufferedImage capture = new Robot().createScreenCapture(screenRect);
        String filePath = "tmp" + windowInfo.title + postfix + ".jpg";
        ImageIO.write(capture, "jpg", new File(filePath ));
        return filePath;
    }

    public static Double[] findCoordsOnScreen(String pathImgToFind, Mat fullScreenImg, WinUtils.WindowInfo windowInfo, Boolean inMainMap) throws URISyntaxException, ImageNotMatchedException, IOException {

        try {


            InputStream inputStream = new ClassPathResource(pathImgToFind).getInputStream();
            File f = new File("targetFile-"+windowInfo.getTitle()+".PNG");
            java.nio.file.Files.copy(
                    inputStream,
                    f.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            inputStream.close();

            Mat toMatch = Imgcodecs.imread(f.getPath(), CONVERT_IMG_FLAG);
            log.info("Loaded template image dimensions: {}", toMatch.size().toString());

            Double scaleFactor = computeScaleFactor(fullScreenImg);

            Mat resizedToMatch = resizeImage(toMatch, scaleFactor);
            log.info("Resized template image dimensions: {}", resizedToMatch.size().toString());


            Mat outputImage = new Mat();
            matchTemplate(fullScreenImg, resizedToMatch, outputImage, TM_CCOEFF_NORMED);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(outputImage);

            if (mmr.maxVal >= MIN_QUALITY_THRESHOLD) {

                log.info("Template matched with confidence: {}", mmr.maxVal);
                org.opencv.core.Point matchLoc = mmr.maxLoc;
                //Draw rectangle on result image
                rectangle(fullScreenImg, matchLoc, new Point(matchLoc.x + toMatch.cols(),
                        matchLoc.y + toMatch.rows()), new Scalar(255, 255, 255));

                Double offsetX = matchLoc.x;
                Double offsetY = matchLoc.y;
                Double absXCoord = windowInfo.getRect().left + offsetX + toMatch.size().width / 2;
                Double absYCoord = windowInfo.getRect().top + offsetY + toMatch.size().height / 2;
                return new Double[]{absXCoord, absYCoord};
            } else{
                log.error("Insufficient confidence {} matching the provided template", mmr.maxVal);
                throw new ImageNotMatchedException("Cannot find img: " + pathImgToFind, inMainMap);
            }

        }
        catch(CvException e){
            throw new ImageNotMatchedException(e.getMessage(), inMainMap);
        }

    }

    public static String extractTextFromImage(String imgPath, Tesseract ocrEngine, ResourceLoader resourceLoader) throws TesseractException {
        ocrEngine.setLanguage("eng");
        ocrEngine.setDatapath("C:\\Users\\Lorenzo\\tessdata");
        ocrEngine.setPageSegMode(12);
        ocrEngine.setOcrEngineMode(1);
        return ocrEngine.doOCR(new File(imgPath));
    }


    private static Double computeScaleFactor(Mat originalImage) {
        //scale for height
        Double scaleHeight = originalImage.height()/ GeneralConfig.SUPPORTED_IMG_HEIGHT;
        //scale for width
        Double scaleWidth = originalImage.width()/ GeneralConfig.SUPPORTED_IMG_WIDTH;

        return Math.max(scaleHeight, scaleWidth);
    }


    private static Mat resizeImage(Mat originalImage, Double scaleFactor){
        Mat resizedImage = new Mat();
        log.info("Resizing with scale factor: {}", scaleFactor);

        Size size = new Size(originalImage.width()*scaleFactor, originalImage.height()*scaleFactor);
        resize(originalImage, resizedImage, size);
        return resizedImage;
    }

    public static Boolean isSameImage(Mat image1, Mat image2){

        Mat difference = new Mat();
        Mat grey1 =  new Mat();
        Mat grey2 =  new Mat();

        cvtColor(image1, grey1, COLOR_BGR2GRAY);
        cvtColor(image2, grey2, COLOR_BGR2GRAY);
        compare(grey1, grey2, difference, CMP_NE);


        return countNonZero(difference) == 0;
    }
}
