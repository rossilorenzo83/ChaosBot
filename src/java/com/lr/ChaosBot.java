package com.lr;

import com.lr.business.CoreMechanics;
import com.lr.business.MainMapButtons;
import com.lr.business.RssType;
import com.lr.config.Config;
import com.lr.utils.WinUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static com.lr.utils.ScreenUtils.takeScreenCapture;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2BGR;

@SpringBootApplication
public class ChaosBot  implements CommandLineRunner{

    private Map<MainMapButtons, Double[]> buttonCoords;

    @Autowired
    CoreMechanics coreMechanics;
    @Autowired
    Robot robot;
    @Autowired
    Random random;

    public static void main(String[] args) {
        SpringApplication.run(ChaosBot.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        this.buttonCoords = new HashMap<>();

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        List<Integer> pidsBS = WinUtils.findPidsMatching(Config.BLUESTACK_PID_NAME);
        System.out.println("PIDs matching config found:" + pidsBS.size());
        List<WinUtils.WindowInfo> hwndList = WinUtils.findAllWindowsMatching(pidsBS, Config.WINDOWS_NAMES);
        System.out.println("Windows matching config found:" + hwndList.size());
        hwndList.stream().forEach(
                windowInfo -> {
                    try {


                        String fullImagePath = takeScreenCapture(windowInfo);
                        Mat fullScreen = Imgcodecs.imread(fullImagePath, COLOR_RGB2BGR);
                        System.out.println("Loaded image dimensions" + fullScreen.size().toString());

                        for (MainMapButtons mainMapButton : MainMapButtons.values()) {
                            System.out.println("Searching coords for control:" + mainMapButton.name());
                            Double[] absCoords = findCoordsOnScreen(mainMapButton.getImgPath(), fullScreen, windowInfo);
                            this.buttonCoords.put(mainMapButton, absCoords);

                        }

                        coreMechanics.setMainMapButtonsCoordsMap(this.buttonCoords);

                        // Castle coords

                        // Search coords

                        Integer availMarches = Config.MARCH_AVAILABLE;
                        Long timeLastActionPerformed = System.currentTimeMillis();



                        while(true) {


                            if (availMarches > 0 || System.currentTimeMillis() - timeLastActionPerformed > Config.TIME_INTERVAL_MILLIS ) {

                                System.out.println("Exec started . . . ");

                                while (availMarches > Config.MARCH_AVAILABLE) {
                                    System.out.println(random.nextInt(RssType.values().length));
                                }


                                coreMechanics.findAndFarm(10, RssType.values()[random.nextInt(RssType.values().length)], windowInfo);

                                timeLastActionPerformed = System.currentTimeMillis();

                                availMarches--;
                            }
                        }

                    } catch (AWTException e) {
                        e.printStackTrace();
                    } catch (IOException | URISyntaxException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );
    }
}
