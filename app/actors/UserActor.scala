package actors

import java.sql.Timestamp

import actors.DatenBankActor.{getChats, sendUserData}
import actors.UserManagerActor.addNewUser
import akka.actor._
import objects.UserRecord
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import actors.UserManagerActor._
import akka.util.Timeout
import org.joda.time.DateTime

import scala.Option
import scala.annotation.tailrec
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by theer on 02.05.2017.
  */


class UserActor(preUser: UserRecord, out: ActorRef, system: AKKASystem) extends Actor {

  var USER = preUser

  import UserActor._

  override def preStart(): Unit = {
    super.preStart()
    println("New User is here")
    implicit val timeout = Timeout(5 seconds)
    val UserFuture = system.dataBaseActor ? sendUserData(preUser)
    UserFuture onComplete {
      case Success(user: UserRecord) => {
        USER = user;
        system.userManagerActor ! addNewUser(user)
        system.dataBaseActor ! getChats(user, context.self)
      }
      case Failure(ex) => throw ex
    }
  }


  def receive = {
    case setupUserRecord(inputUser: UserRecord) =>
      println("recived UserData: " + inputUser)
      USER = inputUser
      sendSetupOutUser(inputUser)
    case setupUserChats(chatrooms: ChatRooms) =>
      sendSetupOutChats(chatrooms)
    case msg: JsValue =>
      println("Message res:" + msg)
      out ! msg
  }

  def sendSetupOutChats(chatrooms: ChatRooms): Unit = {
    implicit val formatchat = Json.format[Chat]
    implicit val format = Json.format[ChatRooms]
    val json: JsValue = Json.toJson(chatrooms.copy(msgType = "SetupChatrooms"))
    Logger.info(json.toString())
    //out ! json
  }


  def sendSetupOutUser(user: UserRecord) = {
    val jsonto = Json.toJson(user)
    Logger.info(jsonto.toString())
    val json = Json.obj(
      "msgType" -> "SetupUser",
      "user" -> Json.obj(
        "username" -> user.username
      )
    )
    out ! json
  }

}

object UserActor {

  def props(preUser: UserRecord, out: ActorRef, system: AKKASystem) = Props(new UserActor(preUser, out, system))

  case class openWebsocket()

  case class setupUserChats(chatrooms: ChatRooms)

  case class setupUserRecord(user: UserRecord)

  case class ChatRooms(chatSeq: Seq[Chat], msgType: String = "")

  case class Chat(chatid: Int, userid: Int, name: String)


}