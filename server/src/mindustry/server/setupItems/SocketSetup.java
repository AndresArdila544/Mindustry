package mindustry.server.setupItems;

import arc.Core;
import arc.Events;
import mindustry.game.EventType;
import mindustry.net.Administration;
import mindustry.server.ServerControl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;

import static arc.util.Log.err;
import static arc.util.Log.info;

public class SocketSetup extends SetupItem
{
    private Thread socketThread;
    private ServerSocket serverSocket;
    private PrintWriter socketOutput;

    public SocketSetup() {
    }

    @Override
    public void initiateEvents() {

        Events.run(EventType.Trigger.socketConfigChanged, () -> {
            toggleSocket(false);
            toggleSocket(Administration.Config.socketInput.bool());
        });

    }

    @Override
    public void setupSteps() {
        toggleSocket(Administration.Config.socketInput.bool());
    }


    public void toggleSocket(boolean on){
        if(on && socketThread == null){
            socketThread = new Thread(() -> {
                try{
                    serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(Administration.Config.socketInputAddress.string(), Administration.Config.socketInputPort.num()));
                    while(true){
                        Socket client = serverSocket.accept();
                        info("&lkReceived command socket connection: &fi@", serverSocket.getLocalSocketAddress());
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        socketOutput = new PrintWriter(client.getOutputStream(), true);
                        String line;
                        while(client.isConnected() && (line = in.readLine()) != null){
                            String result = line;
                            Core.app.post(() -> ServerControl.instance.handleCommandString(result));
                        }
                        info("&lkLost command socket connection: &fi@", serverSocket.getLocalSocketAddress());
                        socketOutput = null;
                    }
                }catch(BindException b){
                    err("Command input socket already in use. Is another instance of the server running?");
                }catch(IOException e){
                    if(!e.getMessage().equals("Socket closed") && !e.getMessage().equals("Connection reset")){
                        err("Terminating socket server.");
                        err(e);
                    }
                }
            });
            socketThread.setDaemon(true);
            socketThread.start();
        }else if(socketThread != null){
            socketThread.interrupt();
            try{
                serverSocket.close();
            }catch(IOException e){
                err(e);
            }
            socketThread = null;
            socketOutput = null;
        }

        // set the socket-related valiables in the server control
        ServerControl.instance.setServerSocket(serverSocket);
        ServerControl.instance.setSocketThread(socketThread);
    }
}
