package connector.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author Yikai Gong
 */

@ServerEndpoint(value ="/websocket")
public class WebSocketEndPoint {

    //ConcurrentHashMap and JsonParser are thread safe
    public static ConcurrentHashMap<String, WebSocketEndPoint> endPointMap = new ConcurrentHashMap<>();
    public static JsonParser parser = new JsonParser();

    //instance field
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private Session session = null;
    private MobileSocketEndPoint controllerEndPoint;

    public MobileSocketEndPoint getControllerEndPoint() {
        return controllerEndPoint;
    }

    public void setControllerEndPoint(MobileSocketEndPoint controllerEndPoint) {
        this.controllerEndPoint = controllerEndPoint;
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            this.session = session;
            String id = session.getId();
            endPointMap.put(id, this);
            JsonObject obj = new JsonObject();
            obj.addProperty("l", "s");
            obj.addProperty("s", "1");
            obj.addProperty("data", id);
            session.getBasicRemote().sendText(obj.toString());
            logger.info("Connected ... " + id);
        }catch (Exception e){
            System.out.println("onopen error");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JsonObject request = (JsonObject)parser.parse(message);
        String label = request.get("l").getAsString();
        if(label.equals("p")){
            gameDataHandler(request.getAsJsonObject("data"));
        }
//        System.out.println("On web message: " + request.toString());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        // Session closed handler
        closeWebapp();
    }

    @OnError
    public void onError(Throwable t){
        closeWebapp();
        t.printStackTrace();
    }

    public void send(JsonObject json){
        try{
            this.session.getBasicRemote().sendText(json.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //to web  0 for lost connected, 1 for connected. -1 for connection deny
    public void notifyControllerState(String state){
        JsonObject response = new JsonObject();
        response.addProperty("l", "p");
        response.addProperty("s", state);
        if(this.session != null){
            send(response);
        }
    }

    //to mobile
    public void gameDataHandler(JsonObject gameData){
        JsonObject response = new JsonObject();
        response.addProperty("l", "p");
        response.addProperty("s", "1");
        response.add("data", gameData);
        if(controllerEndPoint!=null){
            controllerEndPoint.send(response);
        }else{
            System.err.println("controllerEndPoint is null in webSocketEndPoint");
            notifyControllerState("0");
        }
    }

    public void closeWebapp(){
        if(this.session!=null)
            endPointMap.remove(session.getId());
        if(controllerEndPoint!=null && controllerEndPoint.getWebSocketEndPoint() == this){
            controllerEndPoint.notifyControllerState("0");
            controllerEndPoint.setWebSocketEndPoint(null);
        }
        controllerEndPoint = null;
    }
}