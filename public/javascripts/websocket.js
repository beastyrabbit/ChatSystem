var wsUri = "ws://localhost:9000/socket";
var websocket = new WebSocket(wsUri);


$(document).ready(function () {
    initWebSocket();
    /! * Slide MembersInfo * ! /
    $('.info-btn').on('click', function () {
        $("#Messages").toggleClass('col-sm-12 col-sm-9');
    });
    /!* Send Button *!/
    $('#send-button').on('click', function () {
        let textArea = $("#inputArea")
        let message = {
            "type": "message", "text": (textArea.val().toString())
        }
        doSend(JSON.stringify(message))
        textArea.value = ""
    })
});

function initWebSocket() {
    console.log("My Websocket")
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
    console.log(evt)
    console.log("DISCONNECTED");
}

function setupChatRooms(datarecive) {

}
function onMessage(evt) {
    console.log(evt)
    let datarecive = JSON.parse(evt.data)
    console.log(datarecive)
    switch (datarecive.msgType) {
        case
        "SetupUser":
            document.getElementById("username").innerHTML = datarecive.user.username
        case
                "SetupChatRooms":
            setupChatRooms(datarecive)

    }
    return
}

function onError(evt) {
    console.log(evt)
    // console('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function doSend(message) {
    console.log(message)
    websocket.send(message);
}

