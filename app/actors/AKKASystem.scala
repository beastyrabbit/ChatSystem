package actors

import javax.inject._

import actors.DatenBankActor.{checkCredentials, saveUser, sendUserData, updateUser}
import akka.actor.{ActorSystem, _}
import akka.pattern.ask
import akka.util.Timeout
import objects.UserRecord

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by theer on 02.05.2017.
  */
@Singleton
class AKKASystem(system: ActorSystem) {


  val userManagerActor = system.actorOf(UserManagerActor.props, "UserManagerActor")
  val dataBaseActor = system.actorOf(DatenBankActor.props(this), "DatenbankActor")
  val frontEndInputActor = system.actorOf(FrontEndInputActor.props(this), "FrontEndInputActor")
  implicit val timeout: Timeout = 5.seconds
  val subscribeChat = new SubscribeChat

  def checkCredentialsToAKKA(userRecord: UserRecord): Future[Option[UserRecord]] = {
    val eventualRecord: Future[Option[UserRecord]] = (dataBaseActor ? checkCredentials(userRecord)).mapTo[Option[UserRecord]]
    eventualRecord
  }

  def registerUser(userRecord: UserRecord) = {
    dataBaseActor ! saveUser(userRecord)
  }

  def updateUserData(userRecord: UserRecord) = {
    dataBaseActor ! updateUser(userRecord)
  }

  def getUser(userRecord: UserRecord): Future[UserRecord] = {
    val future: Future[Any] = dataBaseActor ? sendUserData(userRecord)
    future.mapTo[UserRecord]
  }
}
