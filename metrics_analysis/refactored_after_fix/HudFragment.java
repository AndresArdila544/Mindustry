package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;
import static mindustry.gen.Tex.*;

public class HudFragment{
    private static final float dsize = 65f, pauseHeight = 36f;

    public PlacementFragment blockfrag = new PlacementFragment();
    public CoreItemsDisplay coreItems = new CoreItemsDisplay();
    public ToastManager toastManager = new ToastManager();
    public StatusDisplayBuilder statusDisplayBuilder = new StatusDisplayBuilder(this);
    public HudUIBuilder hudUIBuilder = new HudUIBuilder(this);
    public boolean shown = true;

    private ImageButton flip;

    private String hudText = "";
    private boolean showHudText;

    // Getters/setters for HudUIBuilder access
    void setFlipButton(ImageButton button){
        this.flip = button;
    }

    ImageButton getFlipButton(){
        return flip;
    }

    String getHudText(){
        return hudText;
    }

    boolean isShowHudText(){
        return showHudText;
    }

    void setShowHudText(boolean shown){
        this.showHudText = shown;
    }


    public void build(Group parent){
        hudUIBuilder.build(parent);
    }

    @Remote(targets = Loc.both, forward = true, called = Loc.both)
    public static void setPlayerTeamEditor(Player player, Team team){
        if(state.isEditor() && player != null){
            player.team(team);
        }
    }

    public void setHudText(String text){
        showHudText = true;
        hudText = text;
    }

    public void toggleHudText(boolean shown){
        showHudText = shown;
    }

    public boolean hasToast(){
        return toastManager.hasToast();
    }

    public void showToast(String text){
        toastManager.showToast(text);
    }

    public void showToast(Drawable icon, String text){
        toastManager.showToast(icon, text);
    }

    public void showToast(Drawable icon, float size, String text){
        toastManager.showToast(icon, size, text);
    }

    /** Show unlock notification for a new recipe. */
    public void showUnlock(UnlockableContent content){
        toastManager.showUnlock(content);
    }

    void toggleMenus(){
        if(flip != null){
            flip.getStyle().imageUp = shown ? Icon.downOpen : Icon.upOpen;
        }

        shown = !shown;
    }


}
