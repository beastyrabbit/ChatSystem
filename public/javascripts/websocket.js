var currentLocation = window.location;
let wsUri = "ws://" + currentLocation.hostname + ":" + currentLocation.port + "/socket";
let websocket
let activChat = undefined;
let PrimUser
let messageList = new Map() // (chatid,new Map())
let userList = new Map();
let ChatRoomArray = undefined;

const ConvInfo = ({name}) => `
    <div class="contact">
        <div class="media-body">
            <h5 class="media-heading">${name}</h5>
            <i id="trash" class="fa fa-trash " aria-hidden="true"></i>
        </div>
    </div>
`;

const ChatName = ({name, chatid, img}) => `
      <div class="conversation btn" id="ChatButton" chatid=${chatid}>
        <div class="media-body">
        <div class="imgusername">
            <img src="${img}" alt="fa fa-user-circle-o" style="width:50px;height:50px;">
            <h5 class="media-heading">${name}</h5>
        </div>
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


const MessageIn = ({message, time, userid}) => `
<div class="msgIn">
    <div class="media-body">
<div class="col-sm-11 ownmessage chattextmessage" data-userid="${userid}"><span>${message}</span></div>
    <small class="pull-left time"><i class="fa fa-clock-o"></i> ${time}</small>
</div>
`;

const TextMessage = ({text}) => `<medium id="TextMessage">${text}</medium><br>`;

const MessageEx = ({name, message, time, img, userid}) => `
<div class="msgEx">
    <div class="media-body">
     <div class="imgname">
            <img src="${img}" alt="fa fa-user-circle-o" style="width:20px;height:20px;">
            <h5 class="media-heading">${name}</h5>
        </div>
     <div class="col-sm-11 exmessage chattextmessage" data-userid="${userid}">
   <span>${message}</span></div>
       <small class="time"><i class="fa fa-clock-o"></i> ${time}</small>
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
    $("#inputArea").on("keydown", function (e) {
        if (e.keyCode == 13) {
            $('#send-button').click()
            $(this).val('')
            e.preventDefault();
        }
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
    $(document).on("click", "#trash", function (e) {
        console.log("hier")
        e.stopPropagation()
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

function updateConvInfo() {
    content = $(document.getElementsByClassName("row content-wrap")[3])
    content.empty()
    if (!(activChat == null)) {
        chatRoomArray = ChatRoomArray
        for (chatRoom of chatRoomArray) {
            if (chatRoom.chatid == activChat) {
                user = getUser(chatRoom.userid)
                var name
                if (user.nickname) {
                    name = user.nickname
                } else {
                    name = user.username
                }
                content.append(ConvInfo({name: name}))

            }
        }
        if (PrimUser.nickname) {
            name = PrimUser.nickname
        } else {
            name = PrimUser.username
        }
        content.append(ConvInfo({name: name}))

    }

}
let updateView = function () {
    setMessagesForChat(activChat)
    updateConvInfo()
    MessageScreen = $(document.getElementsByClassName("row content-wrap messages"))[0]
    MessageScreen.scrollTop = MessageScreen.scrollHeight
    $("#searchbar").val('')
}

function appendChatRooms(chatRoom, addedChats) {
    var user = getUser(chatRoom.userid)
    if (chatRoom.name == "" || chatRoom.name == "DummyNick") {
        if (user.nickname) {
            chatRoom.name = user.nickname
        } else {
            chatRoom.name = user.username
        }
    }
    if (!(addedChats.find(x => x === chatRoom.chatid))) {
        addedChats.push(chatRoom.chatid)
        content.append($(ChatName({
            name: chatRoom.name,
            chatid: chatRoom.chatid,
            img: getUserPicture(user)
        })));
    }
    return addedChats
}
function setupChatRooms(chatRoomArray) {
    content = $(document.getElementsByClassName("row content-wrap")[1])
    content.empty()
    var addedChats = []
    if (chatRoomArray.length != 0) {
        ChatRoomArray = chatRoomArray
        for (chatRoom of chatRoomArray) {
            addedChats = appendChatRooms(chatRoom, addedChats);
        }
        if (activChat == null) {
            activChat = chatRoomArray[0].chatid
        }
    }
    updateView()
}
function updateChatRooms() {
    if (ChatRoomArray != null) {
        chatRoomArray = ChatRoomArray
        content = $(document.getElementsByClassName("row content-wrap")[1])
        content.empty()
        var addedChats = []
        for (chatRoom of chatRoomArray) {
            addedChats = appendChatRooms(chatRoom, addedChats);
        }
        if (activChat == null) {
            activChat = chatRoomArray[0].chatid
        }
    }
    updateView()
}

function getUserPicture(user) {
    if (!user.picture) {
        return user.picture = "https://yt3.ggpht.com/-V92UP8yaNyQ/AAAAAAAAAAI/AAAAAAAAAAA/zOYDMx8Qk3c/s900-c-k-no-mo-rj-c0xffffff/photo.jpg"
    } else {
        return user.picture
    }
}
function setMessagesForChat(chatid) {
    content = $(document.getElementsByClassName("row content-wrap messages"))
    content.empty()
    if (chatid != null) {
        if (messageList.has(Number(chatid))) {
            messages = messageList.get(Number(chatid))
            for (message of messages.values()) {
                user = getUser(message.userid)
                messageContent = $('.chattextmessage:last')
                olduserid = messageContent.data('userid')
                if (olduserid == user.userid) {
                    $('.chattextmessage:last span').append($(TextMessage({
                            text: message.messageText
                        }
                    )))
                } else {
                    if (PrimUser.userid == user.userid) {
                        content.append($(MessageIn({
                            message: TextMessage({text: message.messageText}),
                            time: new Date(message.messageTime).toLocaleString(),
                            userid: PrimUser.userid
                        })))
                    }

                    else {
                        content.append($(MessageEx({
                            name: user.username,
                            message: TextMessage({text: message.messageText}),
                            time: new Date(message.messageTime).toLocaleString(),
                            img: getUserPicture(user),
                            userid: user.userid
                        })))
                    }
                }
            }
        } else {
            getMessageforChatRoomfromBackend(chatid)
        }
    }
}
function getUser(userid) {
    if (userList.has(Number(userid))) {
        return userList.get(Number(userid))
    } else {
        getUserfromBackend(userid)
        let temp = {
            username: "Dummy",
            nickname: "DummyNick"
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
        username: "Dummy",
        nickname: "DummyNick"

    };
    userList.set(userid, temp)

}
function setupMessageChat(chats) {
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
            console.log(datarecive.user)
            document.getElementById("username").innerHTML = datarecive.user.username
            PrimUser = datarecive.user
            userList.set(PrimUser.userid, PrimUser)
            sessionStorage.setItem("userid", PrimUser.userid)
            document.cookie = "userid=" + PrimUser.userid
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
            userList.set(datarecive.user.userid, datarecive.user)
            updateChatRooms()
            updateView()
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
