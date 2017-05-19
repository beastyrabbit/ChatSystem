package actors

import actors.DatenBankActor.{getChats, sendUserData}
import actors.UserManagerActor.addNewUser
import akka.actor._
import objects.UserRecord
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import actors.UserManagerActor._

import scala.Option
import scala.annotation.tailrec

/**
  * Created by theer on 02.05.2017.
  */


class UserActor(preUser: UserRecord, out: ActorRef, system: AKKASystem) extends Actor {

  var USER = preUser

  import UserActor._

  override def preStart(): Unit = {
    super.preStart()
    println("New User is here")
    system.dataBaseActor ! sendUserData(preUser, context.self)
    system.userManagerActor ! addNewUser(preUser.copy(out = context.self))

  }


  def receive = {
    case setupUserRecord(inputUser: UserRecord) =>
      println("recived UserData: " + inputUser)
      USER = inputUser
      sendSetupOutUser(inputUser)
      system.dataBaseActor ! getChats(USER, context.self)
    case setupUserChats(sql: Seq[(Option[Int], Int, String)]) =>
      sendSetupOutChats(sql, new Chats(Seq.empty))
    case msg: JsValue =>
      println("Message res:" + msg)
      out ! msg
  }

  @tailrec
  private def sendSetupOutChats(input: Seq[(Option[Int], Int, String)], output: Chats): Unit = {
    if (input.length < 1) {
      implicit val formatchat = Json.format[Chat]
      implicit val format = Json.format[Chats]
      val json = Json.toJson(output)
      Logger.info(json.toString())
      //out ! json
    } else {
      val head = input.head
      sendSetupOutChats(input.tail, new Chats(output.chatSeq :+ new Chat(head._1.get, head._2, head._3)))
    }


  }


  def sendSetupOutUser(user: UserRecord) = {
    val json = Json.obj(
      "type" -> "SetupUser",
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

  case class setupUserChats(sql: Seq[(Option[Int], Int, String)])

  case class setupUserRecord(user: UserRecord)

  case class Chats(chatSeq: Seq[Chat])

  case class Chat(chatid: Int, userid: Int, name: String)


}