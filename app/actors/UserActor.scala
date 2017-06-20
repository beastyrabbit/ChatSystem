package actors

import actors.DatenBankActor.{getChats, sendUserData}
import actors.FrontEndInputActor.newMessage
import actors.UserManagerActor.{addNewUser, _}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import models.{ChatRoomElement, ChatRooms}
import objects.UserRecord
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by theer on 02.05.2017.
  * This Class is handeling the User connected
  *
  * @param out     connected Websocket
  * @param system  for Reference
  * @param preUser [[UserRecord]] with LoginData
  */


class UserActor(preUser: UserRecord, out: ActorRef, system: AKKASystem) extends Actor {

  private var USER = preUser
  private var listWebsocket: scala.collection.mutable.Set[ActorRef] = mutable.Set.empty[ActorRef]

  import UserActor._

  /**
    * This Methode romoves an not longer used UserActor from the connected User list
    */
  override def postStop(): Unit = {
    system.userManagerActor ! removeUser(USER, context.self)
  }

  /**
    * This Methode does the Setup for the FrontEnd
    */
  override def preStart(): Unit = {
    super.preStart()
    Logger.debug("User " + preUser.username + " on Setup")
    listWebsocket += out
    implicit val timeout = Timeout(5 seconds)
    val UserFuture = system.dataBaseActor ? sendUserData(preUser)
    UserFuture onComplete {
      case Success(user: UserRecord) =>
        USER = user;
        sendSetupOutUser(USER)
        system.dataBaseActor ! getChats(user, context.self)
        system.userManagerActor ! addNewUser(user, context.self, out)
      case Failure(ex) => throw ex
    }
    Logger.debug("New UserActor is started: " + context.self)
  }

  /**
    * This Methode converts a User to Json and sends it out
    *
    * @param user
    */
  private def sendSetupOutUser(user: UserRecord): Unit = {
    val jsonto: JsValue = Json.toJson(user)

    val json = Json.obj(
      "msgType" -> "SetupUser",
      "user" ->
        jsonto.as[JsObject]

    )
    doSend(json)
  }

  /**
    * Message Handler for Actor
    *
    * @return
    */
  def receive = {
    case addWebsocket(newOut: ActorRef) =>
      listWebsocket += newOut
    case sendWebsocketList(ref: ActorRef) =>
      listWebsocket.foreach(outref => ref ! addWebsocket(outref))
    case setupUserChats(chatrooms: ChatRooms) =>
      sendSetupOutChats(chatrooms)
    case msg: JsValue =>
      Logger.debug(sender() + " recived Message res:" + msg)
      system.frontEndInputActor ! newMessage(msg, USER, out)
  }

  /**
    * This Methode converts [[ChatRooms]] to Jason and sends it out
    *
    * @param chatrooms
    */
  private def sendSetupOutChats(chatrooms: ChatRooms): Unit = {
    chatrooms.chatSeq.map(chat => system.subscribeChat.subscribe(out, chat.chatid.toString))
    implicit val formatchat = Json.format[ChatRoomElement]
    implicit val format = Json.format[ChatRooms]
    val json: JsValue = Json.toJson(chatrooms.copy(msgType = "ChatRooms"))
    doSend(json)
  }

  /**
    * This Methode sends a [[JsValue]] to the connected Websockets
    *
    * @param msg to send out
    */
  private def doSend(msg: JsValue): Unit = {
    Logger.debug("Sending msg to " + USER.username + " WebSocket : " + msg)
    listWebsocket.foreach(outref => {
      outref ! msg
    })
  }


}

object UserActor {

  def props(preUser: UserRecord, out: ActorRef, system: AKKASystem) = {
    Props(new UserActor(preUser, out, system))
  }

  case class sendWebsocketList(ref: ActorRef)

  case class addWebsocket(newOut: ActorRef)

  case class openWebsocket()

  case class setupUserChats(chatrooms: ChatRooms)


}