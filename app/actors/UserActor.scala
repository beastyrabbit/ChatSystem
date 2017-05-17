package actors

import actors.DatenBankActor.sendUserData
import actors.UserManagerActor.addNewUser
import akka.actor._
import objects.UserRecord
import play.api.libs.json.{JsValue, Json}

/**
  * Created by theer on 02.05.2017.
  */


class UserActor(preUser: UserRecord, out: ActorRef, system: AKKASystem) extends Actor {


  import UserActor._

  override def preStart(): Unit = {
    super.preStart()
    println("New User is here")
    system.dataBaseActor ! sendUserData(preUser, context.self)
    system.userManagerActor ! addNewUser(preUser.copy(out = context.self))
  }

  def receive = {
    case getUserRecord(inputUser: UserRecord) =>
      println("recived UserData: " + inputUser)
      setUser(inputUser)
    case howareyouChild() =>
    //println("User: " + user.username + " Props: " + this.toString)
    case msg: JsValue =>
      println("Message res:" + msg)
      out ! msg
  }

  def setUser(user: UserRecord) = {
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

  case class howareyouChild()

  case class getUserRecord(user: UserRecord)

}