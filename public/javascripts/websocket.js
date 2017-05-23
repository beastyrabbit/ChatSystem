var wsUri = "ws://localhost:9000/socket";
var websocket
var activChat = 0
var user

const Chat = ({name, onlinestate, chatid}) => `
      <div class="conversation btn" id="ChatButton" chatid=${chatid}>
        <div class="media-body">
            <h5 class="media-heading">${name}</h5>
            <small class="pull-right time">${onlinestate}</small>
        </div>
      </div>
`;

$(document)
$(document).ready(function () {
    websocket = new WebSocket(wsUri);
    initWebSocket();
    addButtons();
    /! * Slide MembersInfo * ! /
    $('.info-btn').on('click', function () {
        $("#Messages").toggleClass('col-sm-12 col-sm-9');
    });
    /!* Send Button *!/
    $('#send-button').on('click', function () {
        let textArea = $("#inputArea")
        let message = {
            "type": "message", "text": textArea.val().toString(),
            "chatid": activChat.toString(),
            "timestamp": new Date().getTime()
        }
        doSend(message)
        textArea.val('')
    })
});
function addButtons() {
    /! * Chat Select Button * ! /
    $(document).on("click", "#ChatButton", function (e) {
        activChat = this.getAttribute("chatid")
        updateView()
    });
    ;
}

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

let updateView = function () {

}

function setupChatRooms(chatRoomArray) {
    content = $(document.getElementsByClassName("row content-wrap")[1])
    for (chatRoom of chatRoomArray) {
        content.append($(Chat({name: chatRoom.name, onlinestate: "online", chatid: chatRoom.chatid})));
    }
    addButtons();
}
function onMessage(evt) {
    let datarecive = JSON.parse(evt.data)
    console.log("Websocket got message: " + evt.data)
    switch (datarecive.msgType) {
        case
        "SetupUser":
            document.getElementById("username").innerHTML = datarecive.user.username
            user = datarecive.user
        case
        "SetupChatRooms":
            setupChatRooms(datarecive.chatSeq)


    }
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
