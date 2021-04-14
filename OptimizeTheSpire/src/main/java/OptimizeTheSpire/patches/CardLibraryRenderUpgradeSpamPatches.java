package OptimizeTheSpire.patches;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.SoulGroup;
import com.megacrit.cardcrawl.screens.MasterDeckViewScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;

import java.util.HashMap;

import static com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen.CurScreen.CARD_LIBRARY;

public class CardLibraryRenderUpgradeSpamPatches {
    public static HashMap<String, AbstractCard> upgrades = new HashMap<>();

    @SpirePatch(clz = AbstractCard.class, method = "renderInLibrary")
    public static class ReplaceCardCopy {
        @SpireInstrumentPatch
        public static ExprEditor patch() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall f) throws CannotCompileException {
                    if (f.getClassName().equals(AbstractCard.class.getName()) && f.getMethodName().equals("makeCopy")) {
                        f.replace("{" +
                                //If the upgraded card was already rendered and saved, skip creating a copy and sub in the saved copy
                                "if ("+CardLibraryRenderUpgradeSpamPatches.class.getName()+".upgrades.containsKey(this.cardID)) {" +
                                //Need to cast to AbstractCard or it crashes due to some weird interaction with generics
                                "$_ = ("+AbstractCard.class.getName()+")"+CardLibraryRenderUpgradeSpamPatches.class.getName()+".upgrades.get(this.cardID);" +
                                "} else {" +
                                "$_ = $proceed($$);" +
                                "}" +
                                "}");
                        return;
                    }

                    //Upgrade the card if it's not saved yet
                    if(f.getClassName().equals(AbstractCard.class.getName()) && (f.getMethodName().equals("upgrade") || f.getMethodName().equals("displayUpgrades"))) {
                        f.replace("{" +
                                "if (!"+CardLibraryRenderUpgradeSpamPatches.class.getName()+".upgrades.containsKey(this.cardID)) {" +
                                "$proceed($$);" +
                                "}" +
                                "}");
                    }
                }
            };
        }

        @SpireInsertPatch(locator = Locator.class, localvars = {"copy"})
        public static void saveUpgrade(AbstractCard __instance, SpriteBatch sb, AbstractCard copy) {
            if(!upgrades.containsKey(__instance.cardID)) {
                upgrades.put(__instance.cardID, copy);
            }
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(AbstractCard.class, "render");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }

    @SpirePatch(clz = MainMenuScreen.class, method = "update")
    public static class ClearUpgradesOnExit {
        @SpirePostfixPatch
        public static void patch(MainMenuScreen __instance) {
            if(__instance.screen != CARD_LIBRARY) {
                upgrades.clear();
            }
        }
    }
}
