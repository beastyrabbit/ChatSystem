var wsUri = "ws://localhost:9000/socket";


/!* Slide Members Info *!/
$('.info-btn').on('click', function () {
    $("#Messages").toggleClass('col-sm-12 col-sm-9');
});
/!* Send Button *!/
$('#send-button').on('click', function () {
    textArea = $("#inputArea")
    json = {
        "type": "message",
        "text": (textArea.val().toString())
    }
    doSend(json)
    text.value = ""
})

$(document).ready(function () {
    initWebSocket();
});

function initWebSocket() {
    console.log("My Websocket")
    websocket = new WebSocket(wsUri);
    websocket.onopen = function (evt) {
        onOpen(evt)
    };
    websocket.onclose = function (evt) {
        onClose(evt)
    };
    websocket.onmessage = function (evt) {
        onMessage(evt)
    };
    websocket.onerror = function (evt) {
        onError(evt)
    };
}

function onOpen(evt) {
    console.log("CONNECTED");
    doSend("WebSocket rocks");
}

function onClose(evt) {
    console.log("DISCONNECTED");
}

function onMessage(evt) {
    console.log(evt)
    data = JSON.parse(evt.data)
    switch (data.type) {
        case
        "SetupUser":
            document.getElementById("username").innerHTML = data.user.username

    }
    return false
}

function onError(evt) {
    console.log(evt)
    // console('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function doSend(message) {
    console.log(message)
    websocket.send(message);
}

