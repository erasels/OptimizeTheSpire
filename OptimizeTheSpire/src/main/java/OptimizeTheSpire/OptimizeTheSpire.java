package OptimizeTheSpire;

import OptimizeTheSpire.patches.RelicOptimizationPatches;
import basemod.BaseMod;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PreStartGameSubscriber;
import basemod.interfaces.RelicGetSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

import static OptimizeTheSpire.patches.AntiConsolePrintingPatches.logsSkipped;
import static OptimizeTheSpire.patches.RelicOptimizationPatches.*;

@SpireInitializer
public class OptimizeTheSpire implements
        PostInitializeSubscriber,
        RelicGetSubscriber,
        PostBattleSubscriber,
        PreStartGameSubscriber{
    public static final Logger optimizationLogger = LogManager.getLogger(OptimizeTheSpire.class);
    private static SpireConfig modConfig = null;

    public static void initialize() {
        BaseMod.subscribe(new OptimizeTheSpire());

        try {
            Properties defaults = new Properties();
            defaults.put("SkipLogging", Boolean.toString(true));
            modConfig = new SpireConfig("OptimizeTheSpire", "Config", defaults);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean shouldSL() {
        if (modConfig == null) {
            return false;
        }
        return modConfig.getBool("SkipLogging");
    }

    @Override
    public void receivePostInitialize() {
        ModPanel settingsPanel = new ModPanel();
        ModLabeledToggleButton HHBtn = new ModLabeledToggleButton("Skip all non-essential logging", 350, 700, Settings.CREAM_COLOR, FontHelper.charDescFont, shouldSL(), settingsPanel, l -> {
        },
                button ->
                {
                    if (modConfig != null) {
                        modConfig.setBool("SkipLogging", button.enabled);
                        try {
                            modConfig.save();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        settingsPanel.addUIElement(HHBtn);

        BaseMod.registerModBadge(ImageMaster.loadImage("OptimizeTheSpire/Resources/img/modBadge.png"), "OptimizeTheSpire", "erasels", "TODO", settingsPanel);
    }

    @Override
    public void receiveRelicGet(AbstractRelic abstractRelic) {
        RelicOptimizationPatches.doReceiveRelic(abstractRelic);
    }

    @Override
    public void receivePostBattle(AbstractRoom abstractRoom) {
        if((AbstractDungeon.floorNum % 5) == 0) {
            optimizationLogger.info("Unneeded element iterations skipped: " + rIterSkipped);
            optimizationLogger.info("Logs prevented: " + logsSkipped);
            if (wrongPosition > 0) {
                optimizationLogger.info("Attempted accesses to wrong relic positions: " + wrongPosition);
            }
        }
    }

    @Override
    public void receivePreStartGame() {
        relicLocs.clear();
    }
}