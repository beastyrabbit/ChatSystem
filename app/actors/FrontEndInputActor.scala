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
import actors.DatenBankActor.{getChats, getMessagefromDB, saveMessage, sendUserData}
import actors.UserManagerActor.addNewUser
import akka.util.Timeout
import models.ChatMessageElement
import play.api.Logger
import play.api.mvc.WebSocket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

class FrontEndInputActor(system: AKKASystem) extends Actor {

  import FrontEndInputActor._

  def messageprossesor(msg: JsValue, user: UserRecord) = {
    val chatMessageElement = new ChatMessageElement(messageid = None, messageText = (msg \ "text").get.as[String], messageTime = new Timestamp((msg \ "timestamp").get.as[Long]), userRecord = user)
    system.dataBaseActor ! saveMessage(user, chatMessageElement, (msg \ "chatid").get.as[String].toInt)

  }


  def checkType(msg: JsValue, userRecord: UserRecord, webSocket: ActorRef): Unit = {
    val msgType = (msg \ "type").get
    msgType match {
      case JsString("message") => messageprossesor(msg, userRecord)
      case JsString("messageRequest") => system.dataBaseActor ! getMessagefromDB((msg \ "chatid").as[String].toInt, userRecord, webSocket)
      case JsString("UserRequest") => sendUserDate(msg, webSocket)
      case JsString("searchrequest") => ???
      case JsString("") => ???
      case _ => println("Das kenn ich nicht " + msg)
    }
  }

  def receive = {
    case newMessage(msg: JsValue, userRecord: UserRecord, webSocket) =>
      Logger.info("A new Message will be checked from User: " + userRecord.username)
      Logger.info("The Message is: " + msg)
      checkType(msg, userRecord, webSocket)
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
    implicit val timeout = Timeout(5 seconds)
    val preUser = new UserRecord(userid = (msg \ "userid").get.as[String].toInt)
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