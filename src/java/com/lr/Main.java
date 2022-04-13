package com.lr;

import com.lr.utils.ScreenUtil;
import com.lr.utils.WinUtils;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGR;

public class Main {


    public static void main(String[] args) throws IOException, AWTException {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        List<Integer> pidsBS = WinUtils.findPidsMatching("BlueStacks_nxt");
        List<WinUtils.WindowInfo> hwndList = WinUtils.findAllWindowsMatching(pidsBS, List.of("Deciphere", "Meph a new"));

        hwndList.stream().forEach(
                windowInfo -> {
                    try {
                        String fullImagePath = ScreenUtil.takeScreenCapture(windowInfo);
                        Mat fullScreen = Imgcodecs.imread(fullImagePath, COLOR_RGB2BGR);

                        System.out.println("Loaded image dimensions" + fullScreen.size().toString());
                        File f = new File(ClassLoader.getSystemResource("combat_reports.png").toURI());
                        Mat toMatch = Imgcodecs.imread(f.getPath(), COLOR_RGB2BGR);
                        System.out.println("Loaded image dimensions" + toMatch.size().toString());




                        int machMethod = Imgproc.TM_CCOEFF_NORMED;
                        Mat outputImage = new Mat();
                        Imgproc.matchTemplate(fullScreen, toMatch, outputImage, machMethod);
                        Core.MinMaxLocResult mmr = Core.minMaxLoc(outputImage);
                        Point matchLoc = mmr.maxLoc;
                        //Draw rectangle on result image
                        Imgproc.rectangle(fullScreen, matchLoc, new Point(matchLoc.x + toMatch.cols(),
                                matchLoc.y + toMatch.rows()), new Scalar(255, 255, 255));

                        Double offsetX = matchLoc.x;
                        Double offsetY = matchLoc.y;



                        //while(true) {
                        //    System.out.println(MouseInfo.getPointerInfo().getLocation());
                        //
                        //}

                        Robot robot = new Robot();
                        Double absXcoords = windowInfo.getRect().left + offsetX + toMatch.size().width/2;
                        Double absYcoords = windowInfo.getRect().top + offsetY + toMatch.size().height/2;

                        robot.mouseMove(absXcoords.intValue(),absYcoords.intValue());
                        Thread.sleep(2000);
                        robot.mousePress(BUTTON1_DOWN_MASK);
                        robot.mouseRelease(BUTTON1_DOWN_MASK);

                        System.out.println("Coords of matched template:" + absXcoords + ":" + absYcoords);
//

                    } catch (AWTException e) {
                        e.printStackTrace();
                    } catch (IOException | URISyntaxException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );


    }


}
