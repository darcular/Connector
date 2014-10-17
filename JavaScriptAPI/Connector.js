/**
 * @author Yikai Gong
 */
var game = game || {};
game.Connector = function(){
    this.socket = null;
    this.sessionId = null;
    this.onServerConnect = function(){};
    this.onServerClose = function(){};
    this.onPlayerConnect = function(){};
    this.onPlayerClose = function(){};
    this.onPlayerMessage = function(){};
    this.isConnected = false;
    this.init();
}

game.Connector.prototype = {
    init : function() {
        var self = this;
        var pathArray = window.location.pathname.substr(0, window.location.pathname.lastIndexOf('/'));
        this.socket = new WebSocket('ws://' + window.location.host +pathArray+ '/websocket');
        this.socket.onopen = function (event) {
            self.socket.onmessage = function (event) {
                var json = JSON.parse(event.data);
                var label = json.l;
                switch (label) {
                    case "p" : self.playerHandler(json);
                    case "s" : self.sessionHandler(json);
                }
            };
            self.socket.onclose = function (event) {
                console.log('Client notified socket has closed', event);
            };
            window.addEventListener("beforeunload", function () {
                self.socket.close();
            });
        }
    },
    playerHandler : function(json){
        var state = json.s;
        if(state == "1"){
            if(this.isConnected){
                this.onPlayerMessage(json.data);
            }
            else{
                this.isConnected = true;
                this.onPlayerConnect();
            }
        }
        else if(state == 0){
            this.isConnected = false;
            this.onPlayerClose();
        }
    },
    sessionHandler : function(json){
        var state = json.s;
        if(state == "1" && this.sessionId == null){
            this.sessionId = json.data;
            this.onServerConnect(this.sessionId);
        }
        else if(state == "0" && this.sessionId != null){
            this.sessionId = null;
            this.onServerClose();
        }
    },
    sendGameData : function(gamedata){
        var json = {};
        json.l = 'p';
        json.s = '1';
        json.data = gamedata;
        console.log("send")
        console.log(gamedata);
        this.socket.send(JSON.stringify(json));
    },
    sendScore : function(json){
        console.log("attempt to send socre" + json + json.name + json.score);
        $.ajax({
            type:"post",
            url:"rank",
            async:true,
            data:json,
            dataType:"text",
            success:function(){console.log("score submitted successful");},
            error:function(){console.log("error in submitting score");}
        });
    }
}
