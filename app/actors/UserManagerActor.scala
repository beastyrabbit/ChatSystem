package actors

import actors.UserActor.howareyouChild
import akka.actor._

/**
  * Created by theer on 02.05.2017.
  */
class UserManagerActor extends Actor {

  import UserManagerActor._

  override def receive: Receive = {
    case createNewUser(userName: String) =>
      spawnUserActor(userName)
  }

  def spawnUserActor(userName: String) = {
    val child = context.actorOf(UserActor.props(userName), "UserActor1")
    context.child("UserActor1").get ! howareyouChild()
  }


}


object UserManagerActor {
  def props = Props[UserManagerActor]

  case class createNewUser(userName: String)

}

