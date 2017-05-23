var wsUri = "ws://localhost:9000/socket";
var websocket


$(document).ready(function () {
    websocket = new WebSocket(wsUri);
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
        doSend(message)
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
    console.log(evt)
    console.log("CONNECTED");
}

function onClose(evt) {
    console.log(evt)
    console.log("DISCONNECTED");
}

function setupChatRooms(chatRoomArray) {
    console.log(datarecive)
    forEach(chatRoom in chatRoomArray)
    {

    }

}
function onMessage(evt) {
    let datarecive = JSON.parse(evt.data)
    console.log("Websocket got message: " + evt.data)
    switch (datarecive.msgType) {
        case
        "SetupUser":
            document.getElementById("username").innerHTML = datarecive.user.username
        case
        "SetupChatRooms":
            setupChatRooms(datarecive)


    }
    console.log(datarecive.msgType)
    return
}

function onError(evt) {
    console.log(evt)
    // console('<span style="color: red;">ERROR:</span> ' + evt.data);
}

function doSend(message) {
    message = JSON.stringify(message)
    console.log("Sending Message: " + message)
    websocket.send(message);
}
