# ChaosBot
Tool to automate repetitive tasks into Chaos Conquest game


## Howto run:
`java -jar <folder containing the jar file>\java -jar .\chaos-conquest-bot-0.0.1-alpha.jar --general.marchesAvailable=1 general.windowsNames[0]=Toto`


## Overridable properties by command line, with current default values
### Name of process (if switching to Steam)
general.pidName = BlueStacks_nxt 
### List of windows names if using multiple accounts
general.windowsNames[0] = My Window
### Time in ms between one action and next
general.actionIntervalMs = 5000
### Type of action, currently supporting ARMY_FARMING or RSS_FARMING
general.actionType = ARMY_FARMING
###Locale used for text on buttons (only fr supported so far)
general.gameLanguage = fr
###Marches available
farm.marchesAvailable = 3
###Interval between attempts (ms), to be used differently in case of farming armies or rss, in ms
farm.marchesIntervalMs = 600000



## Libs used underneath
https://docs.opencv.org/4.x/index.html to identify images fragments in windows 
Robot class to perform automated tasks on coords found

Tutorials:
https://opencv-java-tutorials.readthedocs.io/
