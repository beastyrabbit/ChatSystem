package actors

import actors.UserActor.howareyouChild
import akka.actor._
import objects.UserRecord

/**
  * Created by theer on 02.05.2017.
  */
class UserManagerActor extends Actor {

  import UserManagerActor._

  override def receive: Receive = {
    case createNewUser(user: UserRecord) =>
      spawnUserActor(user)
  }

  def spawnUserActor(user: UserRecord) = {
    val child = context.actorOf(UserActor.props(user), user.userid.toString)
    context.child(user.userid.toString).get ! howareyouChild()
  }


}


object UserManagerActor {
  def props = Props[UserManagerActor]

  case class createNewUser(user: UserRecord)

}

