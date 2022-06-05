package com.lr;

import com.lr.business.CoreMechanics;
import com.lr.business.ImageNotMatchedException;
import com.lr.business.MainMapButtons;
import com.lr.business.RssType;
import com.lr.config.GeneralConfig;
import com.lr.config.MarchConfig;
import com.lr.utils.WinUtils;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static com.lr.business.CoreMechanics.CONVERT_IMG_FLAG;
import static com.lr.utils.ScreenUtils.findCoordsOnScreen;
import static com.lr.utils.ScreenUtils.takeScreenCapture;

@SpringBootApplication
@EnableConfigurationProperties({GeneralConfig.class, MarchConfig.class})
@Slf4j
public class ChaosBot implements CommandLineRunner {

    @Autowired
    CoreMechanics coreMechanics;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    Robot robot;
    @Autowired
    Random random;
    @Autowired
    GeneralConfig generalConfig;
    @Autowired
    MarchConfig marchConfig;


    public static void main(String[] args) {
        SpringApplication.run(ChaosBot.class, args);
    }

    @Override
    public void run(String... args) {

        //Load dll from jar dep
        nu.pattern.OpenCV.loadLocally();
        List<Integer> pidsBS = WinUtils.findPidsMatching(generalConfig.getPidName());
        log.info("PIDs matching config found:", pidsBS.size());
        List<WinUtils.WindowInfo> hwndList = WinUtils.findAllWindowsMatching(pidsBS, generalConfig.getWindowsNames());
        log.info("Windows matching config found:", hwndList.size());


        try {

            hwndList.stream().forEach(
                    windowInfo -> {

                        executorService.execute(() -> {
                            mainLogic(windowInfo);

                        });
                    });
        } finally {
            executorService.shutdown();
        }
    }

    private void mainLogic(WinUtils.WindowInfo windowInfo) {
        Integer availMarches = marchConfig.getMarchesAvailable();

        try {


            String fullImagePath = takeScreenCapture(windowInfo);
            Mat fullScreen = Imgcodecs.imread(fullImagePath, CONVERT_IMG_FLAG);
            log.info("Loaded image dimensions" + fullScreen.size().toString());
            Map<MainMapButtons, Double[]> currentWindowCoords = new HashMap<>();
            Boolean hasEncampments = true;

            for (MainMapButtons mainMapButton : MainMapButtons.values()) {
                log.info("Searching coords for control:" + mainMapButton.name());
                try {

                    Double[] absCoords = findCoordsOnScreen(mainMapButton.getImgPath(), fullScreen, windowInfo, true);
                    currentWindowCoords.put(mainMapButton, absCoords);

                } catch (ImageNotMatchedException e) {
                    if (mainMapButton.equals(MainMapButtons.ENCAMPMENTS)) {
                        hasEncampments = false;
                        log.info("No encampments found");
                    } else {
                        log.error(e.getMessage());
                    }
                }

            }

            ConcurrentMap<String, Map<MainMapButtons, Double[]>> existingCoordsMap = this.coreMechanics.getMainMapButtonsCoordsMap();
            if (existingCoordsMap == null)
                existingCoordsMap = new ConcurrentHashMap<>();
            existingCoordsMap.put(windowInfo.getTitle(), currentWindowCoords);
            coreMechanics.setMainMapButtonsCoordsMap(existingCoordsMap);

            Long timeLastActionPerformed = System.currentTimeMillis();
            while (true) {
                // Castle coords

                // Search coords

                if (availMarches == 0 && (System.currentTimeMillis() - timeLastActionPerformed) > (marchConfig.getMarchesIntervalMins() * 60 *1000)) {
                    log.info("Timer expired");
                    availMarches = marchConfig.getMarchesAvailable();
                }


                if (availMarches > 0) {

                    log.info("Exec started . . . ");

                    switch (generalConfig.getActionType()) {

                        case ARMY_FARMING:
                            coreMechanics.armyFarming(marchConfig.getTargetLevel(), availMarches, windowInfo, hasEncampments);
                            break;

                        case RSS_FARMING:
                        default: coreMechanics.findAndFarm(marchConfig.getTargetLevel(), RssType.values()[random.nextInt(RssType.values().length)], windowInfo, hasEncampments);
                                 break;
                    }

                    availMarches--;
                    timeLastActionPerformed = System.currentTimeMillis();
                }
            }


        } catch (AWTException e) {
            e.printStackTrace();
        } catch (IOException | URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}


