(function () {
    "use strict";
    const currentLocation = window.location;
    let wsUri =
        "ws://" + currentLocation.hostname + ":" + currentLocation.port + "/socket";
    let websocket;
    let activChat;
    let PrimUser;
    let messageList = new Map();
    let userList = new Map();
    let ChatRoomArray;

    const ConvInfo = ({name}) => `
    <div class="contact">
        <div class="media-body">
            <h5 class="media-heading">${name}</h5>
        </div>
    </div>
`;

    const GroupSearch = ({name, userid}) => `
    <div class="contact GroupButton" userid=${userid}>
        <div class="media-body">
            <h5 class="media-heading">${name}</h5>
        </div>
    </div>
`;

    const ChatName = ({name, chatid, img}) => `
      <div class="conversation btn ChatButton" chatid=${chatid}>
        <div class="media-body">
        <div class="imgusername">
            <img src="${img}" alt="fa fa-user-circle-o" style="width:50px;height:50px;">
            <h5 class="media-heading">${name}</h5>
        </div>
        </div>
      </div>
`;

    const UserSearch = ({userName, name, userid}) => `
      <div class="conversation btn UserButton"  userid=${userid}>
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

    const TextMessage = ({text}) =>
        `<medium id="TextMessage">${text}</medium><br>`;

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

    function addListeners() {
        /* Chat Select Button */
        $(document).on("click", ".ChatButton", function () {
            activChat = this.getAttribute("chatid");
            updateView();
        });
        $(document).on("click", ".UserButton", function () {
            const userid = this.getAttribute("userid");
            updateChatRooms();
            doSend({type: "addNewChat", userid});
            $(".searchbar").val("");
            updateView();
        });
        $(document).on("click", "#trash", function () {
            doSend({
                type: "removeChat",
                chatid: activChat
            });
            activChat = undefined;
        });
        $(document).on("click", ".GroupButton", function () {
            const userid = this.getAttribute("userid");
            doSend({
                type: "NewUserToGroup",
                chatid: activChat,
                userid
            });
            $(".searchbar").val("");
            updateView();
        });
        /* Slide MembersInfo */
        $(".info-btn").on("click", function () {
            $("#Messages").toggleClass("col-sm-12 col-sm-9");
        });
        /* Send Button */
        $("#send-button").on("click", function () {
            let textArea = $("#inputArea");
            doSend({
                type: "message",
                text: textArea.val(),
                chatid: activChat,
                timestamp: new Date().getTime()
            });
            textArea.val("");
        });

        $("#inputArea").on("keydown", function (e) {
            if (e.keyCode === 13) {
                $("#send-button").click();
                $(this).val("");
                e.preventDefault();
            }
        });
        $(".searchbar").on("keyup", function (e) {
            if (e.keyCode === 13) {
                $(this).val("");
            }
            let attRole = this.getAttribute("role");
            getSearchedUserFromBackend($(this), attRole);
        });
    }

    function getSearchedUserFromBackend(thissearchbar, attRole) {
        const searchtext = thissearchbar.val();
        if (searchtext) {
            doSend({
                type: "searchRequest",
                searchtext: searchtext,
                displayRole: attRole
            });
        } else {
            updateChatRooms();
        }
    }

    function initWebSocket() {
        console.log("My Websocket");
        websocket.onopen = onEvent;
        websocket.onclose = onEvent;
        websocket.onmessage = onMessage;
        websocket.onerror = onEvent;
    }

    function onEvent(evt) {
        console.log(evt);
        console.log("EVENT!!!!");
    }

    function updateConvInfo() {
        const content = $(document.getElementsByClassName("row content-wrap")[3]);
        content.empty();
        if (activChat !== null) {
            const chatRoomArray = ChatRoomArray;
            for (const chatRoom of chatRoomArray) {
                if (chatRoom.chatid === activChat) {
                    const user = getUser(chatRoom.userid);
                    const name = user.nickname || user.username;
                    content.append(ConvInfo({name}));
                }
            }
            const name = PrimUser.nickname || PrimUser.username;
            content.append(ConvInfo({name}));
        }
    }

    function updateTitle() {
        let content = $("#convTitle");
        if (ChatRoomArray !== null && activChat !== null && userList !== null) {
            for (const chatroom of ChatRoomArray) {
                if (chatroom.chatid === activChat) {
                    content[0].innerText = "Conversation with " + chatroom.name;
                }
            }
        }
    }

    function highLightActivChat() {
        let oldActivChat = document.getElementsByClassName("activchat");
        if (oldActivChat.length !== 0) {
            oldActivChat[0].classList.remove("activchat");
        }
        let newActivChat = document.querySelector('[chatid="' + activChat + '"]');
        if (newActivChat !== null) {
            newActivChat.classList.add("activchat");
        }
    }

    function updateView() {
        updateTitle();
        highLightActivChat();
        setMessagesForChat(activChat);
        updateConvInfo();
        const MessageScreen = $(
            document.getElementsByClassName("row content-wrap messages")
        )[0];
        MessageScreen.scrollTop = MessageScreen.scrollHeight;
    }

    function appendChatRooms(chatRoom, addedChats) {
        const user = getUser(chatRoom.userid);
        if (chatRoom.name === "" || chatRoom.name === "DummyNick") {
            if (user.nickname) {
                chatRoom.name = user.nickname;
            } else {
                chatRoom.name = user.username;
            }
        }
        if (!addedChats.find(x => x === chatRoom.chatid)) {
            addedChats.push(chatRoom.chatid);
            const content = $(document.getElementsByClassName("row content-wrap")[1]);
            content.append(
                $(
                    ChatName({
                        name: chatRoom.name,
                        chatid: chatRoom.chatid,
                        img: getUserPicture(user)
                    })
                )
            );
        }
        return addedChats;
    }

    function setupChatRooms(datarecive) {
        const chatRoomArray = datarecive.chatSeq;
        const content = $(document.getElementsByClassName("row content-wrap")[1]);
        content.empty();
        let addedChats = [];
        if (chatRoomArray.length !== 0) {
            ChatRoomArray = chatRoomArray;
            for (const chatRoom of chatRoomArray) {
                addedChats = appendChatRooms(chatRoom, addedChats);
            }
            if (activChat === null) {
                activChat = chatRoomArray[0].chatid;
            }
        }
        updateView();
    }

    function updateChatRooms() {
        if (ChatRoomArray !== null) {
            const chatRoomArray = ChatRoomArray;
            const content = $(document.getElementsByClassName("row content-wrap")[1]);
            content.empty();
            let addedChats = [];
            for (const chatRoom of chatRoomArray) {
                addedChats = appendChatRooms(chatRoom, addedChats);
            }
            if (activChat === null) {
                activChat = chatRoomArray[0].chatid;
            }
        }
        updateView();
    }

    function getUserPicture(user) {
        if (!user.picture) {
            return "https://yt3.ggpht.com/-V92UP8yaNyQ/AAAAAAAAAAI/AAAAAAAAAAA/zOYDMx8Qk3c/s900-c-k-no-mo-rj-c0xffffff/photo.jpg";
        } else {
            return user.picture;
        }
    }

    function setMessagesForChat(chatid) {
        const content = $(
            document.getElementsByClassName("row content-wrap messages")
        );
        content.empty();
        if (chatid !== undefined) {
            if (messageList.has(Number(chatid))) {
                const messages = messageList.get(Number(chatid));
                for (const message of messages.values()) {
                    const user = getUser(message.userid);
                    const messageContent = $(".chattextmessage:last");
                    const olduserid = messageContent.data("userid");
                    if (olduserid === user.userid) {
                        $(".chattextmessage:last span").append(
                            $(
                                TextMessage({
                                    text: message.messageText
                                })
                            )
                        );
                    } else {
                        if (PrimUser.userid === user.userid) {
                            content.append(
                                $(
                                    MessageIn({
                                        message: TextMessage({text: message.messageText}),
                                        time: new Date(message.messageTime).toLocaleString(),
                                        userid: PrimUser.userid
                                    })
                                )
                            );
                        } else {
                            content.append(
                                $(
                                    MessageEx({
                                        name: user.username,
                                        message: TextMessage({text: message.messageText}),
                                        time: new Date(message.messageTime).toLocaleString(),
                                        img: getUserPicture(user),
                                        userid: user.userid
                                    })
                                )
                            );
                        }
                    }
                }
            } else {
                doSend({
                    type: "messageRequest",
                    chatid: chatid.toString()
                });
            }
        }
    }

    function getUser(userid) {
        if (userList.has(Number(userid))) {
            return userList.get(Number(userid));
        } else {
            getUserFromBackend(userid);
            return {
                username: "Dummy",
                nickname: "DummyNick"
            };
        }
    }

    function getMessageforChatRoomfromBackend(chatid) {
    }

    function updateMessage(data) {
        if (messageList.has(Number(data.chatid))) {
            const message = {
                messageid: data.message.messageid,
                messageText: data.message.messagetext,
                messageTime: data.message.messagetime,
                userid: data.user.userid
            };
            const massages = messageList.get(Number(data.chatid));
            massages.set(message.messageid, message);
            if (!userList.has(Number(message.userid))) {
                userList.set(data.user.userid, data.user);
            }
            updateView();
        } else {
            doSend({
                type: "messageRequest",
                chatid: data.chatid
            });
        }
    }

    function getUserFromBackend(userid) {
        doSend({
            type: "UserRequest",
            userid: userid
        });
        let temp = {
            username: "Dummy",
            nickname: "DummyNick"
        };
        userList.set(userid, temp);
    }

    function setupMessageChat(datarecive) {
        const chats = datarecive.data;
        const messageMap = new Map();
        for (const chat of chats.messageSeq) {
            const message = {
                messageid: chat.messageid,
                messageText: chat.messagetext,
                messageTime: chat.messagetime,
                userid: chat.userid
            };
            messageMap.set(message.messageid, message);
            if (!userList.has(Number(message.userid))) {
                getUserFromBackend(message.userid);
            }
        }
        messageList.set(chats.chatid, messageMap);
        updateView();
    }

    function showSearchResult(datarecive) {
        const searchUserList = datarecive.data;
        const displayRole = datarecive.displayRole;
        console.log(displayRole);
        let content, templete;
        if (displayRole === "User") {
            content = $(document.getElementsByClassName("row content-wrap")[1]);
            templete = UserSearch;
        } else {
            content = $(document.getElementsByClassName("row content-wrap")[3]);
            templete = GroupSearch;
        }
        content.empty();
        for (const user of searchUserList) {
            if (!userList.has(Number(user.userid))) {
                userList.set(user.userid, user);
            }
            content.append(
                $(
                    templete({
                        name: user.firstname + " " + user.lastname,
                        userName: user.username,
                        userid: user.userid
                    })
                )
            );
        }
    }

    function setupUser(datarecive) {
        const user = datarecive.user;
        document.getElementById("username").innerHTML = user.username;
        PrimUser = user;
        userList.set(PrimUser.userid, PrimUser);
        sessionStorage.setItem("userid", PrimUser.userid);
        document.cookie = "userid=" + PrimUser.userid;

        $("#userPic").attr("src", getUserPicture(user));
    }

    function addUser(datarecive) {
        userList.set(datarecive.user.userid, datarecive.user);
        updateChatRooms();
        updateView();
    }

    const messageResolvers = new Map(
        Object.entries({
            SetupUser: setupUser,
            ChatRooms: setupChatRooms,
            UpdateMessage: updateMessage,
            setupMessageChat: setupMessageChat,
            AddUser: addUser,
            searchResult: showSearchResult
        })
    );

    function onMessage(evt) {
        let datarecive = JSON.parse(evt.data);
        console.log("Websocket got message:" + evt.data);

        if (messageResolvers.has(datarecive.msgType)) {
            messageResolvers.get(datarecive.msgType)(datarecive);
        }
    }

    function doSend(message) {
        for (const key of Object.keys(message)) {
            message[key] = message[key].toString();
        }
        message = JSON.stringify(message);
        console.log("Sending Message: " + message);
        websocket.send(message);
    }

    $(document).ready(function () {
        websocket = new WebSocket(wsUri);
        initWebSocket();
        addListeners();
    });
})();
