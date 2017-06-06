let wsUri = "ws://localhost:9000/socket";
let websocket
let activChat = undefined;
let user
let messageList = new Map() // (chatid,new Map())
let userList = new Map();
let ChatRoomArray = undefined;

const ChatName = ({name, onlinestate, chatid}) => `
      <div class="conversation btn" id="ChatButton" chatid=${chatid}>
        <div class="media-body">
            <h5 class="media-heading">${name}</h5>
            <small class="pull-right time">${onlinestate}</small>
        </div>
      </div>
`;
const UserSearch = ({userName, name, userid}) => `
      <div class="conversation btn" id="UserButton" userid=${userid}>
        <div class="media-body">
            <h5 class="media-heading">${userName}</h5>
            <small class="pull-right time">${"Name: " + name}</small>
        </div>
      </div>
`;

const Message = ({name, message, time}) => `
<div class="msg">
    <div class="media-body">
    <small class="pull-right time"><i class="fa fa-clock-o"></i> ${time}</small>

<h5 class="media-heading">${name}</h5>
<small class="col-sm-11">
   ${message}</div>
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
    });
    $("#searchbar").on("keyup", function (e) {
        getSearchedUserFromBackend()
    });
});


function getNewChatfromBackend(userid) {
    message = {
        "type": "addNewChat",
        "userid": userid
    }
    doSend(message)
}
function addButtons() {
    /! * Chat Select Button * ! /
    $(document).on("click", "#ChatButton", function (e) {
        activChat = this.getAttribute("chatid")
        updateView()
    });
    ;
    $(document).on("click", "#UserButton", function (e) {
        userid = this.getAttribute("userid")
        updateChatRooms()
        updateView()
        getNewChatfromBackend(userid)
    });
    ;

}

function getSearchedUserFromBackend() {
    searchtext = $("#searchbar").val()
    if (searchtext) {
        message = {
            "type": "searchrequest",
            "searchtext": searchtext
        }
        doSend(message)
    } else {
        updateChatRooms()
    }
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
    setMessagesForChat(activChat)
    MessageScreen = $(document.getElementsByClassName("row content-wrap messages"))[0]
    MessageScreen.scrollTop = MessageScreen.scrollHeight
    $("#searchbar").val('')
}

function setupChatRooms(chatRoomArray) {
    ChatRoomArray = chatRoomArray
    content = $(document.getElementsByClassName("row content-wrap")[1])
    content.empty()
    for (chatRoom of chatRoomArray) {
        content.append($(ChatName({name: chatRoom.name, onlinestate: "online", chatid: chatRoom.chatid})));
    }
    if (activChat == null) {
        activChat = chatRoomArray[0].chatid
    }
    updateView()
}
function updateChatRooms() {
    if (ChatRoomArray != null) {
        chatRoomArray = ChatRoomArray
        content = $(document.getElementsByClassName("row content-wrap")[1])
        content.empty()
        for (chatRoom of chatRoomArray) {
            content.append($(ChatName({name: chatRoom.name, onlinestate: "online", chatid: chatRoom.chatid})));
        }
        if (activChat == null) {
            activChat = chatRoomArray[0].chatid
        }
    }
}

function setMessagesForChat(chatid) {
    content = $(document.getElementsByClassName("row content-wrap messages"))
    content.empty()
    if (messageList.has(Number(chatid))) {
        messages = messageList.get(Number(chatid))
        for (message of messages.values()) {
            user = getUser(message.userid)
            content.append($(Message({
                name: user.username,
                message: message.messageText,
                time: new Date(message.messageTime).toLocaleString()
            })))
        }
    } else {
        getMessageforChatRoomfromBackend(chatid)
    }
}
function getUser(userid) {
    if (userList.has(Number(userid))) {
        return userList.get(Number(userid))
    } else {
        getUserfromBackend(userid)
        let temp = {
            username: "Dummy"

        };
        return temp
    }
}

function getMessageforChatRoomfromBackend(chatid) {
    let message = {
        "type": "messageRequest",
        "chatid": chatid.toString(),
    }
    doSend(message)
}

function updateMessage(data) {
    if (messageList.has(Number(data.chatid))) {
        var message = {
            "messageid": data.message.messageid,
            "messageText": data.message.messagetext,
            "messageTime": data.message.messagetime,
            "userid": data.user.userid
        }
        massages = messageList.get(Number(data.chatid))
        massages.set(message.messageid, message)
        if (!userList.has(Number(message.userid))) {
            userList.set(data.user.userid, data.user)
        }
        updateView()
    } else {
        getMessageforChatRoomfromBackend(data.chatid);
    }

}
function getUserfromBackend(userid) {
    let message = {
        "type": "UserRequest",
        "userid": userid.toString(),
    }
    doSend(message)
    let temp = {
        username: "Dummy"

    };
    userList.set(userid, temp)

}
function setupMessageChat(chats) {
    console.log(chats)
    var messageMap = new Map()
    for (chat of chats.messageSeq) {
        var message = {
            "messageid": chat.messageid,
            "messageText": chat.messagetext,
            "messageTime": chat.messagetime,
            "userid": chat.userid
        }
        messageMap.set(message.messageid, message)
        if (!userList.has(Number(message.userid))) {
            getUserfromBackend(message.userid)
        }
    }
    messageList.set(chats.chatid, messageMap)
    updateView()
}
function addUser(user) {
    if (!userList.has(Number(user.userid))) {
        userList.set(user.userid, user)
        updateView()
    }
}
function showSearchResult(searchUserList) {
    content = $(document.getElementsByClassName("row content-wrap")[1])
    content.empty()
    for (user of searchUserList) {
        if (!userList.has(Number(user.userid))) {
            userList.set(user.userid, user)
        }
        content.append($(UserSearch({
            name: user.firstname + " " + user.lastname,
            userName: user.username,
            userid: user.userid
        })));
    }
}
function onMessage(evt) {
    let datarecive = JSON.parse(evt.data)
    console.log("Websocket got message:" + evt.data)
    switch (datarecive.msgType) {
        case
        "SetupUser":
            document.getElementById("username").innerHTML = datarecive.user.username
            user = datarecive.user
            userList.set(user.userid, user)
            break;
        case
        "ChatRooms":
            setupChatRooms(datarecive.chatSeq)
            break;
        case
        "UpdateMessage":
            updateMessage(datarecive)
            break;
        case
        "setupMessageChat":
            setupMessageChat(datarecive.data)
            break;
        case
        "AddUser":
            addUser(datarecive.user)
            break;
        case
        "searchResult":
            showSearchResult(datarecive.data)
            break;
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
