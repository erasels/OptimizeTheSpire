package OptimizeTheSpire;

import OptimizeTheSpire.patches.RelicOptimizationPatches;
import basemod.BaseMod;
import basemod.interfaces.PostBattleSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PreStartGameSubscriber;
import basemod.interfaces.RelicGetSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static OptimizeTheSpire.patches.AntiConsolePrintingPatches.logsSkipped;
import static OptimizeTheSpire.patches.RelicOptimizationPatches.*;

@SpireInitializer
public class OptimizeTheSpire implements
        PostInitializeSubscriber,
        RelicGetSubscriber,
        PostBattleSubscriber,
        PreStartGameSubscriber{
    public static final Logger optimizationLogger = LogManager.getLogger(OptimizeTheSpire.class);

    public static void initialize() {
        BaseMod.subscribe(new OptimizeTheSpire());
    }

    @Override
    public void receivePostInitialize() {

    }

    @Override
    public void receiveRelicGet(AbstractRelic abstractRelic) {
        RelicOptimizationPatches.doReceiveRelic(abstractRelic);
    }

    @Override
    public void receivePostBattle(AbstractRoom abstractRoom) {
        if((AbstractDungeon.floorNum % 10) == 0) {
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