package models

import java.sql.Timestamp

import objects.{DBMessage, UserRecord}

/**
  * Created by theer on 02.05.2017.
  */
case class UserData(userName: String, password: String)

case class RegisterData(
                         userName: String,
                         Name: Option[String],
                         nickName: Option[String],
                         email: Option[String],
                         picture: Option[String],
                         password: String,
                         confirm: String
                       )

case class UpdateData(
                       Name: Option[String],
                       nickName: Option[String],
                       email: Option[String],
                       picture: Option[String],
                       password: String,
                       confirm: String
                     )

case class ChatRooms(chatSeq: Seq[ChatRoomElement], msgType: String = "")

case class ChatRoomElement(chatid: Int, userid: Int, name: String)

case class ChatMessages(chatid: Int, messageSeq: Seq[DBMessage])

case class ChatMessageElement(messageid: Option[Int], messageText: String, messageTime: Timestamp, userRecord: UserRecord)