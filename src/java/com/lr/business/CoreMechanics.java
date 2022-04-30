package com.lr.business;


import com.lr.utils.ScreenUtils;

import java.awt.*;
import java.util.Map;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;

public class CoreMechanics {

    private Robot robot;
    private Map<MainMapButtons, Double[]> mainMapButtonsCoordsMap;
    private Map<SearchViewButtons, Double[]> searchViewButtonsCoordsMap;

    public CoreMechanics(Robot robot, Map<MainMapButtons, Double[]> mainMapButtonsCoordsMap) {
        this.robot = robot;
        this.mainMapButtonsCoordsMap = mainMapButtonsCoordsMap;
    }


    public int findAndFarm(int rssLevel, RssType rssType) throws InterruptedException {

        //gotoMainMap with keystroke

        //go on search icon plus click
        Double[] searchCoords = this.mainMapButtonsCoordsMap.get(MainMapButtons.SEARCH);
        robot.mouseMove(searchCoords[0].intValue(),searchCoords[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);

        Thread.sleep(2000);

        Double[] rssExpander = this.searchViewButtonsCoordsMap.get(SearchViewButtons.SEARCH_EXPANDER);
        robot.mouseMove(rssExpander[0].intValue(),rssExpander[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);

        Double[] rssTypeChoice = this.searchViewButtonsCoordsMap.get(SearchViewButtons.STONE_ICON);
        robot.mouseMove(rssTypeChoice[0].intValue(),rssTypeChoice[1].intValue());
        robot.mousePress(BUTTON1_DOWN_MASK);
        robot.mouseRelease(BUTTON1_DOWN_MASK);

        System.out.println("Coords of matched template:" + absCoords[0] + ":" + absCoords[1]);
//
    }



}
