var stompClient = null;
var globalSubscription = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#messages").html("");
}

function connect() {
    var socket = new SockJS('/pocsocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        // A message sent to an amq.topic exchange with the routing key "global" will be seen by all users
        globalSubscribe();
        stompClient.subscribe('/topic/messages', function (receivedMessage) {
            showMessage(JSON.parse(receivedMessage.body).content);
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    stompClient.send("/app/show", {}, JSON.stringify({'name': $("#name").val()}));
}

function showMessage(message) {
    $("#messages").append("<tr><td>" + message + "</td></tr>");
}

function globalSubscribe() {
    if (stompClient) {
        // check if connected...
        globalSubscription = stompClient.subscribe('/topic/global', function (message) {
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

function setSubscribedToGlobal(subscribed) {
    document.getElementById('global-subscribe').disabled = subscribed;
    document.getElementById('global-unsubscribe').disabled = !subscribed;
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#global-subscribe" ).click(function() { globalSubscribe(); });
    $( "#global-unsubscribe" ).click(function() { globalUnsubscribe(); });
    $( "#send" ).click(function() { sendName(); });
});

