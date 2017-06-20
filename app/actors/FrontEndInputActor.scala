package actors

/**
  * Created by theer on 17.05.2017.
  */

import akka.actor._
import objects.{DBMessage, UserRecord}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import java.sql
import java.sql.Timestamp

import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
import akka.pattern.{ask, pipe}
import actors.DatenBankActor._
import actors.UserManagerActor.{addNewUser, checkUserBACK}
import akka.util.Timeout
import models.ChatMessageElement
import play.api.Logger
import play.api.mvc.WebSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class FrontEndInputActor(system: AKKASystem) extends Actor {
  implicit val timeout = Timeout(5 seconds)

  import FrontEndInputActor._

  def messageprossesor(msg: JsValue, user: UserRecord) = {
    val chatMessageElement = new ChatMessageElement(messageid = None, messageText = (msg \ "text").get.as[String], messageTime = new Timestamp((msg \ "timestamp").get.as[Long]), userRecord = user)
    system.dataBaseActor ! saveMessage(user, chatMessageElement, (msg \ "chatid").get.as[String].toInt)

  }

  def setupRemoveChat(msg: JsValue, user: UserRecord, sendfrom: ActorRef): Unit = {
    val checkback = system.dataBaseActor ? removeUserFromChat((msg \ "chatid").as[String].toInt, user)
    checkback onComplete { case Success(_) =>
      system.dataBaseActor ! getChats(user, sendfrom)

    }
  }

  def setupNewChat(userid: Int, user1: UserRecord, sendto: ActorRef): Unit = {
    val UserFuture = system.dataBaseActor ? sendUserData(new UserRecord(userid = Some(userid)))
    UserFuture onComplete {
      case Success(user: UserRecord) => {
        system.dataBaseActor ? addChat("", user1, user, sendto) onComplete {
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
  }


  def checkType(msg: JsValue, userRecord: UserRecord, webSocket: ActorRef, sendfrom: ActorRef): Unit = {
    val msgType = (msg \ "type").get
    msgType match {
      case JsString("message") => messageprossesor(msg, userRecord)
      case JsString("messageRequest") => system.dataBaseActor ! getMessagefromDB((msg \ "chatid").as[String].toInt, userRecord, webSocket)
      case JsString("UserRequest") => sendUserDate(msg, webSocket)
      case JsString("searchrequest") => system.dataBaseActor ! searchforUser((msg \ "searchtext").as[String], (msg \ "displayRole").as[String], webSocket)
      case JsString("addNewChat") => setupNewChat((msg \ "userid").as[String].toInt, userRecord, sender())
      case JsString("removechat") => setupRemoveChat(msg, userRecord, sendfrom);
      case JsString("NewUserToGroup") => {
        system.dataBaseActor ! addUserToChat((msg \ "chatid").as[String].toInt, (msg \ "userid").as[String].toInt)
      }
      case JsString("") => ???
      case _ => println("Das kenn ich nicht " + msg)
    }
  }

  def receive = {
    case newMessage(msg: JsValue, userRecord: UserRecord, webSocket) =>
      Logger.info("User: " + userRecord.username + " ID: " + userRecord.userid.getOrElse("None") + " wants: " + (msg \ "type").as[String])
      checkType(msg, userRecord, webSocket, sender())
    case publishMessage(chatMessage: ChatMessage) =>
      system.subscribeChat.publish(chatMessage);
    case TEMPPPER3() =>
      println("TEMPPPER")
    case TEMPPPER4() =>
      println("TEMPPPER")
    case TEMPPPER5() =>
      println("TEMPPPER")
    case TEMPPPER6() =>
      println("TEMPPPER")
  }

  def sendUserDate(msg: JsValue, webSocket: ActorRef): Unit = {
    val preUser = new UserRecord(userid = Some((msg \ "userid").as[String].toInt))
    val UserFuture = system.dataBaseActor ? sendUserData(preUser)
    UserFuture onComplete {
      case Success(user: UserRecord) => {
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
}


object FrontEndInputActor {
  def props(system: AKKASystem): Props = Props(new FrontEndInputActor(system: AKKASystem))

  case class newMessage(msg: JsValue, userRecord: UserRecord, webSocket: ActorRef)

  case class publishMessage(chatMessage: ChatMessage)

  case class TEMPPPER3()

  case class TEMPPPER4()

  case class TEMPPPER5()

  case class TEMPPPER6()

}    