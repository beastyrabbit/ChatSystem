package actors

import java.sql.Timestamp

import actors.DatenBankActor.{getChats, sendUserData}
import actors.FrontEndInputActor.newMessage
import actors.UserManagerActor.addNewUser
import akka.actor._
import objects.UserRecord
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import actors.UserManagerActor._
import akka.util.Timeout
import org.joda.time.DateTime

import scala.Option
import scala.annotation.tailrec
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by theer on 02.05.2017.
  */


class UserActor(preUser: UserRecord, out: ActorRef, system: AKKASystem) extends Actor {

  var USER = preUser
  var listWebsocket: scala.collection.mutable.Set[ActorRef] = mutable.Set.empty[ActorRef]

  import UserActor._

  override def postStop(): Unit = {
    system.userManagerActor ! removeUser(USER, context.self)
  }

  override def preStart(): Unit = {
    super.preStart()
    Logger.debug("User " + preUser.username + " on Setup")
    listWebsocket += out
    implicit val timeout = Timeout(5 seconds)
    val UserFuture = system.dataBaseActor ? sendUserData(preUser)
    UserFuture onComplete {
      case Success(user: UserRecord) => {
        USER = user;
        sendSetupOutUser(USER)
        system.dataBaseActor ! getChats(user, context.self)
        system.userManagerActor ! addNewUser(user, context.self, out)
      }
      case Failure(ex) => throw ex
    }
    Logger.debug("New UserActor is started: " + context.self)
  }


  def doSend(msg: JsValue): Unit = {
    Logger.debug("Sending msg to " + USER.username + " WebSocket : " + msg)
    listWebsocket.foreach(outref => {
      outref ! msg
    })
  }

  def receive = {
    case addWebsocket(newOut: ActorRef) =>
      listWebsocket += newOut
    case sendWebsocketList(ref: ActorRef) =>
      listWebsocket.foreach(outref => ref ! addWebsocket(outref))
    case setupUserChats(chatrooms: ChatRooms) =>
      sendSetupOutChats(chatrooms)
    case msg: JsValue =>
      Logger.debug(sender() + " recived Message res:" + msg)
      system.frontEndInputActor ! newMessage(msg, USER)
    case chatMessage: ChatMessage =>
      println("Back to user" + chatMessage)
      out ! "Here you go"
  }

  def sendSetupOutChats(chatrooms: ChatRooms): Unit = {
    chatrooms.chatSeq.map(chat => system.subscribeChat.subscribe(context.self, "ChatRoom" + chat.chatid.toString))
    implicit val formatchat = Json.format[Chat]
    implicit val format = Json.format[ChatRooms]
    val json: JsValue = Json.toJson(chatrooms.copy(msgType = "SetupChatRooms"))
    doSend(json)
  }


  def sendSetupOutUser(user: UserRecord) = {
    val jsonto: JsValue = Json.toJson(user)

    val json = Json.obj(
      "msgType" -> "SetupUser",
      "user" ->
        jsonto.as[JsObject]

    )
    doSend(json)
  }


}

object UserActor {

  case class sendWebsocketList(ref: ActorRef)

  case class addWebsocket(newOut: ActorRef)

  def props(preUser: UserRecord, out: ActorRef, system: AKKASystem) = Props(new UserActor(preUser, out, system))

  case class openWebsocket()

  case class setupUserChats(chatrooms: ChatRooms)

  case class ChatRooms(chatSeq: Seq[Chat], msgType: String = "")

  case class Chat(chatid: Int, userid: Int, name: String)


}