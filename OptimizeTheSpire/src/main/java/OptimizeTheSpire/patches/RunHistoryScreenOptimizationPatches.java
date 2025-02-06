package OptimizeTheSpire.patches;

import basemod.ReflectionHacks;
import com.badlogic.gdx.files.FileHandle;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.runHistory.RunHistoryScreen;
import com.megacrit.cardcrawl.screens.stats.RunData;
import javassist.CtBehavior;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static OptimizeTheSpire.OptimizeTheSpire.optimizationLogger;

public class RunHistoryScreenOptimizationPatches {
    private static Set<String> validCharacters = null;
    private static Set<String> reportedInvalidCharacters = new HashSet<>();

    private static void refreshValidCharacters() {
        if (validCharacters != null) return;
        validCharacters = new HashSet<>();
        try {
            for (AbstractPlayer character : CardCrawlGame.characterManager.getAllCharacters()) {
                validCharacters.add(character.chosenClass.toString());
            }
        } catch (Exception e) {
        }
    }

    @SpirePatch(clz = RunHistoryScreen.class, method = "refreshData")
    public static class FilterInvalidFoldersPatch {
        @SpireInsertPatch(rloc = 2)
        public static void Insert(RunHistoryScreen __instance, @ByRef FileHandle[][] ___subfolders) {
            refreshValidCharacters();
            if (___subfolders[0] == null || ___subfolders[0].length == 0) return;

            List<FileHandle> filteredFolders = new ArrayList<>();
            for (FileHandle folder : ___subfolders[0]) {
                String folderName = folder.name();
                boolean validForSaveSlot;
                if (CardCrawlGame.saveSlot == 0) { // Has no prefix
                    validForSaveSlot = !folderName.startsWith("0_") &&
                            !folderName.startsWith("1_") &&
                            !folderName.startsWith("2_");
                } else {
                    validForSaveSlot = folderName.startsWith(CardCrawlGame.saveSlot + "_");
                }
                if (!validForSaveSlot) continue;

                String charName = folderName;
                if(CardCrawlGame.saveSlot != 0) { // We already check for the lack of prefix for validity
                    int saveSlotIndex = folderName.indexOf('_');
                    if (saveSlotIndex != -1 && saveSlotIndex + 1 < folderName.length()) {
                        charName = folderName.substring(saveSlotIndex + 1);
                    }
                }

                if (!validCharacters.contains(charName)) {
                    reportedInvalidCharacters.add(charName);
                    continue;
                }
                filteredFolders.add(folder);
            }
            ___subfolders[0] = filteredFolders.toArray(new FileHandle[0]);
        }
    }

    @SpirePatch(clz = RunHistoryScreen.class, method = "refreshData")
    public static class FilterInvalidCharactersPatch {
        @SpireInsertPatch(locator = Locator.class, localvars = {"data"})
        public static void Insert(RunHistoryScreen __instance, Object data) {
            if (data != null) {
                RunData runData = (RunData) data;
                if (runData.character_chosen != null && !validCharacters.contains(runData.character_chosen)) {
                    reportedInvalidCharacters.add(runData.character_chosen);
                    optimizationLogger.fatal("Save file filtering didn't work, please post the error log on the OptimizeTheSpire steam Workshop page.");
                }
            }
        }

        private static class Locator extends SpireInsertLocator {
            public int[] Locate(CtBehavior ctBehavior) throws Exception {
                Matcher matcher = new Matcher.MethodCallMatcher(AbstractPlayer.PlayerClass.class.getName(), "valueOf");
                return LineFinder.findInOrder(ctBehavior, matcher);
            }
        }
    }

    @SpirePatch(clz = RunHistoryScreen.class, method = "refreshData")
    public static class EnsureFiltersExistPatch {
        @SpirePostfixPatch
        public static void Postfix(RunHistoryScreen __instance) {
            String[] text = ReflectionHacks.getPrivateStatic(RunHistoryScreen.class, "TEXT");
            initializeFilter(__instance, "characterFilter", new String[]{text[23], text[0], text[1], text[2], text[35]});
            initializeFilter(__instance, "winLossFilter", new String[]{text[24], text[25], text[26]});
            initializeFilter(__instance, "runTypeFilter", new String[]{text[28], text[29], text[30], text[31]});
            initializeFilter(__instance, "runsDropdown", new String[]{" "});
        }

        private static void initializeFilter(RunHistoryScreen screen, String fieldName, String[] options) {
            if (ReflectionHacks.getPrivate(screen, RunHistoryScreen.class, fieldName) == null) {
                DropdownMenu filter = new DropdownMenu(screen, options,
                        fieldName.equals("runsDropdown") ? FontHelper.panelNameFont : FontHelper.cardDescFont_N,
                        Settings.CREAM_COLOR);
                ReflectionHacks.setPrivate(screen, RunHistoryScreen.class, fieldName, filter);
            }
        }
    }
}