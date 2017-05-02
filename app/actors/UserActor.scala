package actors

import akka.actor._

/**
  * Created by theer on 02.05.2017.
  */


class UserActor(userName: String) extends Actor {

  import UserActor._

  def receive = {
    case openWebsocket() =>
      println("Hier bin ich")
    case howareyouChild() =>
      println("User: " + userName + " Props: " + this.toString)
  }

}

object UserActor {
  def props(userName: String): Props = Props(new UserActor(userName))

  case class openWebsocket()

  case class howareyouChild()

}