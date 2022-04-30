package com.lr;

import com.lr.business.CoreMechanics;
import com.lr.business.MainMapButtons;
import com.lr.business.RssType;
import com.lr.utils.ScreenUtils;
import com.lr.utils.WinUtils;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGR;

public class Main {


    private static class Config {
        public static String BLUESTACK_PID_NAME = "\"BlueStacks_nxt\"";
        public static List<String> WINDOWS_NAMES = List.of("Deciphere", "Meph a new");
    }

    public Map<MainMapButtons, Double[]> getButtonCoords() {
        return buttonCoords;
    }

    private Map<MainMapButtons, Double[]> buttonCoords;




    public static void main(String[] args) {

        Main main = new Main();
        main.buttonCoords = new HashMap<>();

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        List<Integer> pidsBS = WinUtils.findPidsMatching(Config.BLUESTACK_PID_NAME);
        List<WinUtils.WindowInfo> hwndList = WinUtils.findAllWindowsMatching(pidsBS, Config.WINDOWS_NAMES);

        hwndList.stream().forEach(
                windowInfo -> {
                    try {
                        String fullImagePath = ScreenUtils.takeScreenCapture(windowInfo);
                        Mat fullScreen = Imgcodecs.imread(fullImagePath, COLOR_RGB2BGR);
                        System.out.println("Loaded image dimensions" + fullScreen.size().toString());

                        for(MainMapButtons mainMapButton : MainMapButtons.values()){
                            Double[] absCoords = findCoordsOnScreen(mainMapButton.getImgPath(), fullScreen, windowInfo);
                            main.buttonCoords.put(mainMapButton, absCoords);
                        }

                        // Castle coords

                        // Search coords


                        CoreMechanics cm = new CoreMechanics(new Robot(), main.getButtonCoords());
                        cm.findAndFarm(10, RssType.STONE);




                    } catch (AWTException e) {
                        e.printStackTrace();
                    } catch (IOException | URISyntaxException | InterruptedException e) {
                        e.printStackTrace();
                        }
                }
        );


    }




}
