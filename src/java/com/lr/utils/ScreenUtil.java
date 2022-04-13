package com.lr.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ScreenUtil {


    public static String takeScreenCapture(WinUtils.WindowInfo windowInfo) throws AWTException, IOException {
        Rectangle screenRect = new Rectangle(windowInfo.rect.left, windowInfo.rect.top, Math.abs(windowInfo.rect.right
                - windowInfo.rect.left), Math.abs(windowInfo.rect.bottom - windowInfo.rect.top));
        ;
        BufferedImage capture = new Robot().createScreenCapture(screenRect);
        String filePath = "tmp" + windowInfo.title + ".jpg";
        ImageIO.write(capture, "jpg", new File(filePath ));
        return filePath;
    }
}
