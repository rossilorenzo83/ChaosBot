package com.lr;

import com.lr.business.CoreMechanics;
import com.lr.business.ImageNotMatchedException;
import com.lr.business.MainMapButtons;
import com.lr.business.RssType;
import com.lr.config.Config;
import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
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

import static com.lr.business.CoreMechanics.CONVERT_IMG_FLAG;
import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static com.lr.utils.ScreenUtils.takeScreenCapture;

@SpringBootApplication
@Slf4j
public class ChaosBot  implements CommandLineRunner{

    private Map<MainMapButtons, Double[]> buttonCoords;
    private Boolean hasEncampments = true;

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
    public void run(String... args) {
        this.buttonCoords = new HashMap<>();

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        List<Integer> pidsBS = WinUtils.findPidsMatching(Config.PID_NAME);
        log.info("PIDs matching config found:", pidsBS.size());
        List<WinUtils.WindowInfo> hwndList = WinUtils.findAllWindowsMatching(pidsBS, Config.WINDOWS_NAMES);
        log.info("Windows matching config found:", hwndList.size());



            //FIXME run on multiple thread to enable multi win support
            hwndList.stream().forEach(
                    windowInfo -> {
                        Integer availMarches = Config.MARCH_AVAILABLE;

                            try {


                                String fullImagePath = takeScreenCapture(windowInfo);
                                Mat fullScreen = Imgcodecs.imread(fullImagePath, CONVERT_IMG_FLAG);
                                log.info("Loaded image dimensions" + fullScreen.size().toString());

                                for (MainMapButtons mainMapButton : MainMapButtons.values()) {
                                    log.info("Searching coords for control:" + mainMapButton.name());
                                    try {
                                        Double[] absCoords = findCoordsOnScreen(mainMapButton.getImgPath(), fullScreen, windowInfo);
                                        this.buttonCoords.put(mainMapButton, absCoords);
                                    } catch (ImageNotMatchedException e) {
                                        if (mainMapButton.equals(MainMapButtons.ENCAMPMENTS)) {
                                            hasEncampments = false;
                                            log.info("No encampments found");
                                        } else {
                                            log.error(e.getMessage());
                                        }
                                    }

                                }

                                coreMechanics.setMainMapButtonsCoordsMap(this.buttonCoords);
                                Long timeLastActionPerformed = System.currentTimeMillis();
                                while (true) {
                                    // Castle coords

                                    // Search coords

                                    if (availMarches == 0 && (System.currentTimeMillis() - timeLastActionPerformed) > Config.TIME_INTERVAL_MILLIS) {
                                        log.info("Timer expired");
                                        availMarches++;
                                    }


                                    if (availMarches > 0) {

                                        log.info("Exec started . . . ");

                                        coreMechanics.findAndFarm(10, RssType.values()[random.nextInt(RssType.values().length)], windowInfo, hasEncampments);
                                        availMarches--;
                                        timeLastActionPerformed= System.currentTimeMillis();
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
