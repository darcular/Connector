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

@ServerEndpoint(value ="/mobilesocket")
public class MobileSocketEndPoint {

    //ConcurrentHashMap and JsonParser are thread safe
    public static ConcurrentHashMap<String, MobileSocketEndPoint> endPointMap = new ConcurrentHashMap<>();
    public static JsonParser parser = new JsonParser();

    //instance field
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private Session session = null;
    private WebSocketEndPoint webSocketEndPoint = null;

    public WebSocketEndPoint getWebSocketEndPoint() {
        return webSocketEndPoint;
    }

    public void setWebSocketEndPoint(WebSocketEndPoint webSocketEndPoint) {
        this.webSocketEndPoint = webSocketEndPoint;
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            this.session = session;
            String id = session.getId();
            endPointMap.put(id, this);
            logger.info("Connected ... " + id);
        }catch (Exception e){
            logger.info("onopen error");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        JsonObject request = (JsonObject)parser.parse(message);
        String label = request.get("l").getAsString();
        if(label.equals("p")){
            gameDataHandler(request.getAsJsonObject("data"));
        }
        else if(label.equals("s")){
            requestConnectionHandler(request);
        }
//         System.out.println("On mobile message: " + request.toString());
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        closeController();
    }

    @OnError
    public void onError(Throwable t){
        closeController();
        t.printStackTrace();
    }

    public void requestConnectionHandler(JsonObject request){
        String webSessionId = request.get("data").getAsString();
        WebSocketEndPoint targetEndPoint = WebSocketEndPoint.endPointMap.get(webSessionId);
        if (targetEndPoint == null){
            System.out.println("targetEndPoint is null");
            notifyControllerState("-1");
            return;
        }
        String state;
        synchronized (targetEndPoint){
            if(targetEndPoint.getControllerEndPoint() == null){
                targetEndPoint.setControllerEndPoint(this);
                this.webSocketEndPoint = targetEndPoint;
                state = "1";  //synchronize success
                System.out.println("Connection request success");
            }else{
                state = "-1"; //already in use
                System.out.println("target has been used");
            }
        }
       notifyControllerState(state);
    }

    //to web
    public void gameDataHandler(JsonObject gameData){
        JsonObject response = new JsonObject();
        response.addProperty("l", "p");
        response.addProperty("s", "1");
        response.add("data", gameData);
        if(webSocketEndPoint!=null){
            webSocketEndPoint.send(response);
        }else{
            System.err.println("webSocketEndPoint is null in mobileSocketEndPoint");
            notifyControllerState("0");
        }
    }

    //it is called when lost mobile
    public void closeController(){
        System.out.println("executing closeController");
        if(this.session!=null)
            endPointMap.remove(session.getId());
        if(webSocketEndPoint!=null && webSocketEndPoint.getControllerEndPoint() == this){
            webSocketEndPoint.notifyControllerState("0");
            webSocketEndPoint.setControllerEndPoint(null);
        }
        webSocketEndPoint = null;
    }

    //to mobile 0 for lost connected, 1 for connected. -1 for connection deny
    public void notifyControllerState(String state){
        JsonObject response = new JsonObject();
        response.addProperty("l","s");
        response.addProperty("s", state);
        if(this.session != null){
            send(response);
        }
    }

    public void send(JsonObject json){
        try{
            this.session.getBasicRemote().sendText(json.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
