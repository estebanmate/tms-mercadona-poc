        var stomp = null;
        var globalSubscription = null;
        var stompWindow = null;
        var socketSessionId = null;

        var domIdByMessageOrigin = {
            "INTEGRATION": "stomp-integration"
        };

        var domClassByRoutingKey = {
            "global": "global",
            "echo.user": "echo-user"
        };

        function connect() {
	        document.getElementById('user-id').innerHTML = 'USER';

            var socket = new SockJS('/pocsocket');

            stomp = Stomp.over(socket);

            // remove noise from heartbeats for debug purposes
            stomp.heartbeat = {
                outgoing: 0,
                incoming: 0
            };

            stomp.connect({}, function (frame) {
                console.log('STOMP connection established: ' + frame);
                setConnected(true);

                // A message sent to an amq.topic exchange with the routing key "global" will be seen by all users
                globalSubscribe();

                // A message sent to an amq.direct exchange with the routing key "echo.user-user<username>" will be seen
                // by all logged in sessions with the Principal.getName == <username>
                // Opt to a custom exchange instead of amq.direct in case a federated exchange is required but it is not
                // desirable to federate the default amq.direct
                stomp.subscribe('/user/exchange/amq.direct/echo.user', function (message) {
                    processMessage("echo.user", message.body);
                });

//                stomp.subscribe('/user/exchange/amq.direct/echo.user.StompRelay', function (message) {
//                    processMessage("echo.user", message.body);
//                });
            }, stompDisconnectErrorCallback);

            socketSessionId = generateSessionId();
            var socketWindow = new SockJS('/pocsocket', [], {
                sessionId: function () {return socketSessionId;}
            });

            // a separate stomp connection with "useSocketSession" header set on connect,
            // see com.vass.wsamq.gateway.config.WebSocketConfiguration.configureClientInboundChannel
            stompWindow = Stomp.over(socketWindow);

            // remove noise from heartbeats for debug purposes
            stompWindow.heartbeat = {
                outgoing: 0,
                incoming: 0
            };

            stompWindow.connect({useSocketSession: "true"}, function (frame) {
                console.log('STOMP connection established, window session: ' + frame);

                // A message sent to an amq.direct exchange with the routing key "echo.window-user<websocket-session>"
                // will be seen in the current browser window only
                stompWindow.subscribe('/user/exchange/amq.direct/echo.window', function (message) {
                    processMessage("echo.window", message.body);
                });
            }, stompDisconnectErrorCallback);
        }

        function globalSubscribe() {
            if (stomp) {
                // check if connected...
                globalSubscription = stomp.subscribe('/topic/global', function (message) {
                    processMessage("global", message.body);
                });
                setSubscribedToGlobal(true);
            }
        }

        function globalUnsubscribe() {
            if (globalSubscription) {
                globalSubscription.unsubscribe();
                setSubscribedToGlobal(false);
            }
        }

        function stompDisconnectErrorCallback(msg) {
            if (msg && Object.prototype.toString.call(msg) === "[object String]" &&
                msg.lastIndexOf("Whoops! Lost connection to", 0) === 0) {
                setConnected(false);
            }
        }

        function setConnected(connected) {
            document.getElementById('connect').disabled = connected;
            document.getElementById('disconnect').disabled = !connected;
            clearMessages(document.getElementById('stomp-integration'));
        }

        function setSubscribedToGlobal(subscribed) {
            document.getElementById('global-subscribe').disabled = subscribed;
            document.getElementById('global-unsubscribe').disabled = !subscribed;
        }

        function clearMessages(wrapper) {
            document.getElementsByClassName('global').innerHTML = '';
            document.getElementsByClassName('echo-user').innerHTML = '';
        }

        function disconnect() {
            if (stomp != null) {
                stomp.disconnect();
            }
            if (stompWindow != null) {
                stompWindow.disconnect();
            }
            setConnected(false);
        }

        function sendMessage() {
            var message = document.getElementById('message').value;
            stomp.send("/app/message", {"X-Socket-Session-Id": socketSessionId}, message);
        }

        /**
         * @param routingKey    see domClassByRoutingKey
         * @param message       ServerMessageDto
         */
        function processMessage(routingKey, message) {
            var messageDto = JSON.parse(message);
            var wrapperDomId = domIdByMessageOrigin[messageDto.origin];
            var messageDisplayDomClass = domClassByRoutingKey[routingKey];
            var text = "Timestamp: [" + messageDto.timestamp + "] Message: [" + messageDto.text + "]";
            appendMessage(wrapperDomId, messageDisplayDomClass, text);
        }

        function appendMessage(wrapperDomId, messageDisplayDomClass, message) {
            var messageDisplay = document.getElementById(wrapperDomId).getElementsByClassName(messageDisplayDomClass)[0];
            messageDisplay.innerHTML = '';
            messageDisplay.appendChild(document.createTextNode(message));
        }

        function generateSessionId() {
            var length = 8;
            var _randomStringChars = "abcdefghijklmnopqrstuvwxyz012345";
            var max = _randomStringChars.length;
            var bytes = new Uint8Array(length);
            window.crypto.getRandomValues(bytes);
            var ret = [];
            for (var i = 0; i < length; i++) {
                ret.push(_randomStringChars.substr(bytes[i] % max, 1));
            }
            return ret.join("");
        }

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#global-subscribe" ).click(function() { globalSubscribe(); });
    $( "#global-unsubscribe" ).click(function() { globalUnsubscribe(); });
    $( "#send" ).click(function() { sendMessage(); });
});