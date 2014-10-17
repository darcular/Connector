package com.example.heartbeater.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.*;
import java.net.URI;
import java.util.logging.Logger;

/**
 * @author Yikai Gong
 */

@ClientEndpoint
public abstract class Connector {

    public static JsonParser parser = new JsonParser();
    public ClientManager client = ClientManager.createClient();
    public Logger logger = Logger.getLogger(this.getClass().getName());
    private Session session = null;

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Connected ... " + session.getId());
        this.session = session;
        onServerConnected();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("Got message: "+message);
        JsonObject json = (JsonObject)parser.parse(message);
        String label = json.get("l").getAsString();
        if (label.equals("s")){
            String state = json.get("s").getAsString();
            if(state.equals("0")){
                onConnectionLost();
            }
            else if(state.equals("1")){
                onConnectionOpen();
            }
            else if(state.equals("-1")){
                onConnectionRequestDeny();
            }
        }else if(label.equals("p")){
            onReceivedGameData(json.getAsJsonObject("data"));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info(String.format("Session %s close because of %s", session.getId(), closeReason));
    }

    public void sendGameData (JsonObject gameDate) throws Exception{
        JsonObject json = new JsonObject();
        json.addProperty("l", "p");
        json.add("data", gameDate);
        send(json);
    }

    private void send (JsonObject json) throws Exception{
        if (this.session == null){
            throw new Exception("No session, have you connected?");
        }
        try{
            this.session.getBasicRemote().sendText(json.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void connectServer(String urlStr) throws Exception {
        client.connectToServer(this, new URI(urlStr));
//        client.connectToServer(this.getClass(), new URI(urlStr));
    }

    public void requestControllerChennel(String webSessionID) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("l", "s");
        json.addProperty("data", webSessionID);
        send(json);
    }

    public abstract void onServerConnected();

    public abstract void onConnectionLost();

    public abstract void onConnectionOpen();

    public abstract void onConnectionRequestDeny();

    public abstract void onReceivedGameData(JsonObject jsonObject);

    public String getSessionID(){
        return this.session.getId();
    }

}
