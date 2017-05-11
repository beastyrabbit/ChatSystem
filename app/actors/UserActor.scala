package actors

import akka.actor._
import objects.UserRecord

/**
  * Created by theer on 02.05.2017.
  */


class UserActor(user: UserRecord) extends Actor {

  import UserActor._

  def receive = {
    case openWebsocket() =>
      println("Hier bin ich")
    case howareyouChild() =>
      println("User: " + user.username + " Props: " + this.toString)
  }

}

object UserActor {
  def props(user: UserRecord): Props = Props(new UserActor(user))

  case class openWebsocket()

  case class howareyouChild()

}