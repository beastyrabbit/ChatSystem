package actors

import akka.actor._
import objects.UserRecord

/**
  * Created by theer on 02.05.2017.
  */
class UserManagerActor extends Actor {

  import UserManagerActor._

  var UserRefList = scala.collection.mutable.Map[Int, UserRecord]()

  override def receive: Receive = {
    case addNewUser(user: UserRecord) =>
      UserRefList += (user.userid -> user)
  }


}


object UserManagerActor {
  def props = Props[UserManagerActor]

  case class addNewUser(user: UserRecord)

}

