package actors

import akka.actor._
import objects.UserRecord
import play.api.Logger

import scala.collection.mutable

/**
  * Created by theer on 02.05.2017.
  */
class UserManagerActor extends Actor {

  import UserManagerActor._
  import actors.UserActor._

  var UserRefList: scala.collection.mutable.Map[Int, Set[(UserRecord, ActorRef)]] = mutable.Map.empty[Int, Set[(UserRecord, ActorRef)]]

  override def receive: Receive = {
    case addNewUser(user: UserRecord, ref: ActorRef, outref: ActorRef) =>
      checkForNewUser(user, ref, outref)
    case checkUserBACK(user: UserRecord) =>
      sender() ! UserRefList.get(user.userid)
    case removeUser(user: UserRecord, ref: ActorRef) =>
      val userSet = UserRefList.getOrElse(user.userid, Set())
      UserRefList += (user.userid -> (userSet - (user -> ref)))
      if (UserRefList.getOrElse(user.userid, Set()).isEmpty) {
        UserRefList -= (user.userid)
      }
  }


  def checkForNewUser(user: UserRecord, ref: ActorRef, outref: ActorRef): Unit = {
    if (UserRefList.contains(user.userid)) {
      Logger.warn("Dublicate Login adding Redirect for User: " + user.username)
      val userSet: Set[(UserRecord, ActorRef)] = UserRefList.get(user.userid).get
      userSet.head._2 ! addWebsocket(outref)
      userSet.head._2 ! sendWebsocketList(ref)
    }
    val userSet = UserRefList.getOrElse(user.userid, Set())
    UserRefList += (user.userid -> (userSet + (user -> ref)))

  }


}


object UserManagerActor {
  def props = Props[UserManagerActor]

  case class removeUser(removeuser: UserRecord, ref: ActorRef)

  case class addNewUser(user: UserRecord, ref: ActorRef, outref: ActorRef)

  case class checkUserBACK(user: UserRecord)

}



