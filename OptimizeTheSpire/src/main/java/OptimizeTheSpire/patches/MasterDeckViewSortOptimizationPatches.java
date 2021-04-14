package OptimizeTheSpire.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.evacipated.cardcrawl.modthespire.patcher.PatchingException;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.SoulGroup;
import com.megacrit.cardcrawl.screens.MasterDeckViewScreen;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.FastCardObtainEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

import java.util.ArrayList;
import java.util.Comparator;

public class MasterDeckViewSortOptimizationPatches {
    public static boolean shouldSort = false;

    @SpirePatch(clz = MasterDeckViewScreen.class, method = "setSortOrder")
    public static class CaptureSortingChange {
        @SpirePostfixPatch
        public static void patch(MasterDeckViewScreen __instance, Comparator<AbstractCard> sortOrder) {
            shouldSort = true;
        }
    }

    @SpirePatch(clz = MasterDeckViewScreen.class, method = "updatePositions")
    public static class SkipUnnecessarySorting {
        private static boolean first = true;
        private static int matches = 0;

        @SpireInstrumentPatch
        public static ExprEditor skip() {
            return new ExprEditor() {
                @Override
                public void edit(FieldAccess f) throws CannotCompileException {
                    //Don't re-sort the cards if the sort order hasn't changed
                    if (f.getClassName().equals(MasterDeckViewScreen.class.getName()) && f.getFieldName().equals("sortOrder") && first) {
                        first = false;
                        f.replace("{" +
                                //Skip the sort clause: Sort order wasn't changed last update cycle and cards haven't been unexpectedly added
                                //&& (this.tmpSortedDeck != null && this.tmpSortedDeck.size() == cards.size()) <- replaced by patching into card add effect
                                "if (!"+MasterDeckViewSortOptimizationPatches.class.getName()+".shouldSort) {" +
                                "$_ = null;" +
                                "} else {" +
                                //Let the sort clause naturally proceed
                                "$_ = $proceed($$);" +
                                "}" +
                                "}");
                        return;
                    }

                    //Overwrite sorted tmpdeck if the sort order is default and overwrite unsorted deck if it isn't
                    if (f.getClassName().equals(MasterDeckViewScreen.class.getName()) && f.getFieldName().equals("tmpSortedDeck")) {
                        //Only modify the second access to this variable: when the sort order is null
                        if(matches == 1) {
                            f.replace("{" +
                                    "if (this.sortOrder == null) {" +
                                    "$_ = $proceed($$);" +
                                    "} else {" +
                                    "cards = this.tmpSortedDeck;" +
                                    "}" +
                                    "}");
                        } else {
                            matches++;
                        }
                    }
                }
            };
        }

        @SpirePostfixPatch
        public static void reset(MasterDeckViewScreen __instance) {
            shouldSort = false;
        }
    }

    //Make sure to update sorted cards if new cards were unexpected added (via console)
    @SpirePatch(clz = ShowCardAndObtainEffect.class, method = "update")
    @SpirePatch(clz = FastCardObtainEffect.class, method = "update")
    public static class CaptureMasterdeckCardChange {
        @SpireInsertPatch(locator = Locator.class)
        public static void patch(AbstractGameEffect __instance) {
            shouldSort = true;
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctMethodToPatch) throws CannotCompileException, PatchingException {
                Matcher finalMatcher = new Matcher.MethodCallMatcher(SoulGroup.class, "obtain");
                return LineFinder.findInOrder(ctMethodToPatch, finalMatcher);
            }
        }
    }
}
