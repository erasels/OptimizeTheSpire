package OptimizeTheSpire.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import java.util.HashMap;

public class RelicOptimizationPatches {
    //Optimize relic access
    public static int rIterSkipped = 0;
    public static int wrongPosition = 0;
    public static int relicsSize = -1;
    public static HashMap<String, Integer> relicLocs = new HashMap<>();

    @SpirePatch(clz = AbstractPlayer.class, method = "getRelic")
    public static class OptimizeGetRelic {
        @SpirePrefixPatch
        public static SpireReturn<AbstractRelic> patch(AbstractPlayer __instance, String key) {
            AbstractRelic rel;
            Integer i = relicLocs.get(key);
            if (i != null) {
                int size = __instance.relics.size();
                if(size != relicsSize) {
                    relicsSize = size;
                    //Remove removed relics when relics list size suddenly changes unexpectedly (like Ruina removing all relics for a time)
                    relicLocs.values().removeIf(val -> val == -1);
                }

                if (i != -1) {
                    if (i < size && __instance.relics.get(i).relicId.equals(key)) {
                        rIterSkipped += i - 1;
                        return SpireReturn.Return(__instance.relics.get(i));
                    } else {
                        wrongPosition++;
                        rel = manualGetRelic(__instance, key);
                        if(rel != null) {
                            return SpireReturn.Return(rel);
                        }
                    }
                } else {
                    rIterSkipped += size;
                }
            } else {
                rel = manualGetRelic(__instance, key);
                //may return null but that's valid
                return SpireReturn.Return(rel);
            }
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch(clz = AbstractPlayer.class, method = "hasRelic")
    public static class OptimizeHasRelic {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> patch(AbstractPlayer __instance, String key) {
            SpireReturn<AbstractRelic> sp = OptimizeGetRelic.patch(__instance, key);
            return SpireReturn.Return(sp.get() != null);
        }
    }

    public static void doReceiveRelic(AbstractRelic r) {
        //Relic hasn't been added yet so setting position as size directly functions as last position + 1
        relicLocs.put(r.relicId, AbstractDungeon.player.relics.size());
        relicsSize++;
    }

    public static AbstractRelic manualGetRelic(AbstractPlayer p, String key) {
        for (int j = 0; j < p.relics.size(); j++) {
            AbstractRelic r = p.relics.get(j);
            if (r.relicId.equals(key)) {
                relicLocs.put(key, j);
                return r;
            }
        }
        relicLocs.put(key, -1);
        return null;
    }
}
