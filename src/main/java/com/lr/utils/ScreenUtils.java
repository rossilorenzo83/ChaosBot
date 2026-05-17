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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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


    /**
     * Return a string containing the filePath of the captured image
     *
     * @param windowInfo
     * @param robot Robot instance to use for screen capture
     * @return
     * @throws AWTException
     * @throws IOException
     */
    public static String takeScreenCapture(WinUtils.WindowInfo windowInfo, Robot robot) throws AWTException, IOException {
        return takeScreenCapture(windowInfo, "", robot);
    }

    public static String takeScreenCapture(WinUtils.WindowInfo windowInfo, String postfix, Robot robot) throws AWTException, IOException {
        Rectangle screenRect = new Rectangle(windowInfo.rect.left, windowInfo.rect.top, Math.abs(windowInfo.rect.right
                - windowInfo.rect.left), Math.abs(windowInfo.rect.bottom - windowInfo.rect.top));
        return takeScreenCapture(screenRect, windowInfo.title, postfix, robot);
    }

    public static String takeScreenCapture(Rectangle rectangle, String winTitle, String postfix, Robot robot) throws AWTException, IOException {
        BufferedImage capture = robot.createScreenCapture(rectangle);
        String filePath = "tmp" + winTitle + postfix + ".jpg";
        ImageIO.write(capture, "jpg", new File(filePath));
        return filePath;
    }

    public static Double[] getTemplateImageSizeAdjusted(String pathImgToFind, Mat fullScreenImg, WinUtils.WindowInfo windowInfo) throws  IOException {

           InputStream inputStream = new ClassPathResource(pathImgToFind).getInputStream();
            File f = new File("targetFile-" + windowInfo.getTitle() + ".PNG");
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

            return new Double[]{resizedToMatch.size().width, resizedToMatch.size().height};
    }


    public static Double[] findCoordsOnScreenFlexible(
            String pathImgToFind,
            Mat fullScreenImg,
            WinUtils.WindowInfo windowInfo,
            Boolean inMainMap,
            Double minQualityThreshold
    ) throws URISyntaxException, ImageNotMatchedException, IOException {
        // Default: search entire screen
        return findCoordsOnScreenFlexible(pathImgToFind, fullScreenImg, windowInfo, inMainMap, minQualityThreshold, 0.0, 1.0);
    }

    /**
     * Find coordinates with vertical region constraint.
     * @param verticalStart Start of search region (0.0 = top, 1.0 = bottom)
     * @param verticalEnd End of search region (0.0 = top, 1.0 = bottom)
     */
    public static Double[] findCoordsOnScreenFlexible(
            String pathImgToFind,
            Mat fullScreenImg,
            WinUtils.WindowInfo windowInfo,
            Boolean inMainMap,
            Double minQualityThreshold,
            Double verticalStart,
            Double verticalEnd
    ) throws URISyntaxException, ImageNotMatchedException, IOException {
        try {
            // 1. Load template image
            InputStream inputStream = new ClassPathResource(pathImgToFind).getInputStream();
            File f = new File("targetFile-" + windowInfo.getTitle() + ".PNG");
            java.nio.file.Files.copy(
                    inputStream,
                    f.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            inputStream.close();
            Mat toMatch = Imgcodecs.imread(f.getPath(), Imgcodecs.IMREAD_COLOR);

            double screenshotWidth = fullScreenImg.width();
            double screenshotHeight = fullScreenImg.height();
            log.info("Screenshot size: {}x{}", screenshotWidth, screenshotHeight);

            boolean isPortrait = screenshotHeight > screenshotWidth;
            log.info("Portrait resolution: {}", isPortrait);

            Double resizeFactor = computeScaleFactor(fullScreenImg);
            log.info("Resize factor: {}", resizeFactor);
            Mat resizedTemplate = resizeImage(toMatch, resizeFactor);

            // Apply region constraint if specified
            Mat searchRegion = fullScreenImg;
            int regionYOffset = 0;
            if (verticalStart > 0.0 || verticalEnd < 1.0) {
                int startY = (int)(screenshotHeight * verticalStart);
                int endY = (int)(screenshotHeight * verticalEnd);
                regionYOffset = startY;
                searchRegion = fullScreenImg.submat(startY, endY, 0, (int)screenshotWidth);
                log.info("Searching in region: y={} to y={}", startY, endY);
            }

            // Perform template matching
            Mat result = new Mat();
            matchTemplate(searchRegion, resizedTemplate, result, TM_CCOEFF_NORMED);
            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

            if (mmr.maxVal >= minQualityThreshold) {
                log.info("Template matched with confidence: {} at local ({},{})", mmr.maxVal, mmr.maxLoc.x, mmr.maxLoc.y);

                double offsetX = mmr.maxLoc.x;
                double offsetY = mmr.maxLoc.y + regionYOffset; // Add back the region offset
                double absXCoord = windowInfo.getRect().left + offsetX + resizedTemplate.size().width / 2;
                double absYCoord = windowInfo.getRect().top + offsetY + resizedTemplate.size().height / 2;

                log.info("Absolute coords: ({}, {})", absXCoord, absYCoord);
                return new Double[]{absXCoord, absYCoord};
            } else {
                log.error("Insufficient confidence {} matching the provided template", mmr.maxVal);
                throw new ImageNotMatchedException("Cannot find img: " + pathImgToFind, inMainMap);
            }

        }
        catch (CvException e) {
            throw new ImageNotMatchedException("Cannot find img: " + pathImgToFind, inMainMap);
        }
    }


    public static String extractTextFromImage(String imgPath, Tesseract ocrEngine) throws TesseractException {
        return ocrEngine.doOCR(new File(imgPath));
    }


    private static Double computeScaleFactor(Mat originalImage) {

        boolean isPortrait = originalImage.height() > originalImage.width();


        //scale for height
        Double scaleHeight = originalImage.height()/ (isPortrait?GeneralConfig.SUPPORTED_IMG_HEIGHT: GeneralConfig.SUPPORTED_IMG_WIDTH);
        //scale for width
        Double scaleWidth = originalImage.width()/ (isPortrait?GeneralConfig.SUPPORTED_IMG_WIDTH: GeneralConfig.SUPPORTED_IMG_HEIGHT);

        return Math.min(scaleHeight, scaleWidth);
    }


    private static Mat resizeImage(Mat originalImage, Double scaleFactor) {
        Mat resizedImage = new Mat();
        log.info("Resizing with scale factor: {}", scaleFactor);

        Size size = new Size(originalImage.width() * scaleFactor, originalImage.height() * scaleFactor);
        resize(originalImage, resizedImage, size);
        return resizedImage;
    }

    public static Double[] computeGoIconForSpecificArmy(Double[] rowCoords , WinUtils.WindowInfo windowInfo, String goArmyImagePath, Mat fullScreenImg) throws IOException {

       double xOffset = rowCoords[0] - windowInfo.getRect().left;
       double x = windowInfo.getRect().right -xOffset - getTemplateImageSizeAdjusted(goArmyImagePath, fullScreenImg, windowInfo)[0]/2;

       return new Double[]{x, rowCoords[1]};

    }

}
