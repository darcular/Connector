## Synopsis

A connector for building connections between Android devices and web page. 

This repository includes a Server side service (which can be deployed in tomcat7) and two API for using its service and. 

Client-ends can use the JS/Java API to build connection between Android devices and web page. User can initiate the connection by scanning a QR code on web page with their Android device. One usecase is to use a Android device as a controller to play a web game.

Currently, the communication channel is built on websocket. I plan to use WebRTC in the future with the same API interface. At that time it will be a real 2P2 connection and improve the performance.

Code example will be updated later.

## License

MIT




