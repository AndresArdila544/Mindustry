package mindustry.server.setupItems;

import arc.Core;
import arc.files.Fi;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Strings;
import mindustry.core.GameState;
import mindustry.game.Gamemode;
import mindustry.io.SaveIO;
import mindustry.net.Administration;
import mindustry.server.ServerControl;

import static arc.util.Log.err;
import static arc.util.Log.info;
import static mindustry.Vars.*;
import static mindustry.Vars.netServer;

public class CoreSettingsSetup extends SetupItem {

    String[] args;
    public final CommandHandler handler;

    public CoreSettingsSetup(String[] args) {
        this.args = args;
        handler = ServerControl.instance.handler;;
    }

    @Override
    public void initiateEvents() {

    }

    @Override
    public void setupSteps() {

        Core.settings.defaults(
                "bans", "",
                "admins", "",
                "shufflemode", "custom",
                "globalrules", "{reactorExplosions: false, logicUnitBuild: false}"
        );

        try{
            ServerControl.instance.lastMode = Gamemode.valueOf(Core.settings.getString("lastServerMode", "survival"));
        }catch(Exception e){ //handle enum parse exception
            ServerControl.instance.lastMode = Gamemode.survival;
        }

        Core.app.post(() -> {
            //try to load auto-update save if possible
            if(Administration.Config.autoUpdate.bool()){
                Fi fi = saveDirectory.child("autosavebe." + saveExtension);
                if(fi.exists()){
                    try{
                        SaveIO.load(fi);
                        info("Auto-save loaded.");
                        state.set(GameState.State.playing);
                        netServer.openServer();
                    }catch(Throwable e){
                        err(e);
                    }
                }
            }

            Seq<String> commands = new Seq<>();

            if(args.length > 0){
                commands.addAll(Strings.join(" ", args).split(","));
                info("Found @ command-line arguments to parse.", commands.size);
            }

            if(!Administration.Config.startCommands.string().isEmpty()){
                String[] startup = Strings.join(" ", Administration.Config.startCommands.string()).split(",");
                info("Found @ startup commands.", startup.length);
                commands.addAll(startup);
            }

            for(String s : commands){
                CommandHandler.CommandResponse response = handler.handleMessage(s);
                if(response.type != CommandHandler.ResponseType.valid){
                    err("Invalid command argument sent: '@': @", s, response.type.name());
                    err("Argument usage: &lb<command-1> <command1-args...>,<command-2> <command-2-args2...>");
                }
            }
        });
    }
}
