package actors

/**
  * Created by theer on 17.05.2017.
  */

import java.sql.Timestamp

import actors.DatenBankActor._
import actors.UserManagerActor.checkUserBACK
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import models.ChatMessageElement
import objects.UserRecord
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Success

/**
  * This Actor gets all Messages from the FrontEnd throw the [[UserActor]]
  *
  * @param system
  */

class FrontEndInputActor(system: AKKASystem) extends Actor {
  implicit val timeout = Timeout(5 seconds)

  import FrontEndInputActor._

  /**
    * Message Handler for Actor
    *
    * @return
    */
  def receive = {
    case newMessage(msg: JsValue, userRecord: UserRecord, webSocket) =>
      Logger.info("User: " + userRecord.username + " ID: " + userRecord.userid.getOrElse("None") + " wants: " + (msg \ "type").as[String])
      checkType(msg, userRecord, webSocket, sender())
    case publishMessage(chatMessage: ChatMessage) =>
      system.subscribeChat.publish(chatMessage);
  }

  /**
    * This Methode checks the Type of the received Message
    *
    * @param msg        the Message
    * @param userRecord the User how is sending the Message
    * @param webSocket  the Websocket connected to the FrontEnd
    * @param sendfrom   the UserActor
    */
  def checkType(msg: JsValue, userRecord: UserRecord, webSocket: ActorRef, sendfrom: ActorRef): Unit = {
    val msgType = (msg \ "type").get
    msgType match {
      case JsString("message") => messagePros(msg, userRecord)
      case JsString("messageRequest") => system.dataBaseActor ! getMessages((msg \ "chatid").as[String].toInt, webSocket)
      case JsString("UserRequest") => sendUserDate(msg, webSocket)
      case JsString("searchRequest") => system.dataBaseActor ! searchforUser((msg \ "searchtext").as[String], (msg \ "displayRole").as[String], webSocket)
      case JsString("addNewChat") => NewChatPros((msg \ "userid").as[String].toInt, userRecord, sendfrom)
      case JsString("removeChat") => RemoveChatPros(msg, userRecord, sendfrom);
      case JsString("NewUserToGroup") =>
        system.dataBaseActor ! addUserToChat((msg \ "chatid").as[String].toInt, (msg \ "userid").as[String].toInt)
      case _ => println("Das kenn ich nicht " + msg)
    }
  }

  /**
    * This Methode is Processing a send Message
    *
    * @param msg  the Message from FrontEnd
    * @param user the User responsible
    */
  def messagePros(msg: JsValue, user: UserRecord) = {
    val chatMessageElement = new ChatMessageElement(messageid = None, messageText = (msg \ "text").get.as[String], messageTime = new Timestamp((msg \ "timestamp").get.as[Long]), userRecord = user)
    system.dataBaseActor ! saveMessage(user, chatMessageElement, (msg \ "chatid").get.as[String].toInt)

  }

  /**
    * This Methode is Processing a send RemoveChat command
    *
    * @param msg      the Message from FrontEnd
    * @param user     the User responsible
    * @param sendfrom [[UserActor]] responsible
    */
  def RemoveChatPros(msg: JsValue, user: UserRecord, sendfrom: ActorRef): Unit = {
    val future = system.dataBaseActor ? removeUserFromChat((msg \ "chatid").as[String].toInt, user)
    future onComplete { case Success(_) =>
      system.dataBaseActor ! getChats(user, sendfrom)

    }
  }

  /**
    * This Methode is Processing a send addChat command
    *
    * @param userid   of adding User
    * @param user     the User responsible
    * @param sendfrom [[UserActor]] responsible
    */
  def NewChatPros(userid: Int, user: UserRecord, sendfrom: ActorRef): Unit = {
    val future = system.dataBaseActor ? sendUserData(new UserRecord(userid = Some(userid)))
    future onComplete {
      case Success(effecteduser: UserRecord) =>
        system.dataBaseActor ? addChat("", user, effecteduser, sendfrom) onComplete {
          case Success(_) =>
            val future = system.userManagerActor ? checkUserBACK(user)
            future onSuccess {
              case Some(userSet: Set[(UserRecord, ActorRef)]) =>
                system.dataBaseActor ! getChats(user, userSet.head._2)
              case None => Logger.debug("Nutzer: " + user.username + " nicht online")
            }
        }
    }
  }

  /**
    * This Methode is Processing a send UserRequest command
    *
    * @param msg       the Message from FrontEnd
    * @param webSocket to Reply to
    */
  def sendUserDate(msg: JsValue, webSocket: ActorRef): Unit = {
    val preUser = new UserRecord(userid = Some((msg \ "userid").as[String].toInt))
    val future = system.dataBaseActor ? sendUserData(preUser)
    future onComplete {
      case Success(user: UserRecord) =>
        val userNoPW = user.copy(password = "")
        val jsonto = Json.toJson(userNoPW)
        val json = Json.obj(
          "msgType" -> "AddUser",
          "user" ->
            jsonto.as[JsObject]

        )
        webSocket ! json
    }
  }
}


object FrontEndInputActor {
  def props(system: AKKASystem): Props = Props(new FrontEndInputActor(system: AKKASystem))

  case class newMessage(msg: JsValue, userRecord: UserRecord, webSocket: ActorRef)

  case class publishMessage(chatMessage: ChatMessage)

}    