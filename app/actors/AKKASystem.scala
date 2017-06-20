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

  def checkCredentialsToAKKA(user: UserRecord): Future[Option[UserRecord]] = {
    val eventualRecord: Future[Option[UserRecord]] = (dataBaseActor ? checkCredentials(user)).mapTo[Option[UserRecord]]
    eventualRecord
  }

  def registerUser(record: UserRecord) = {
    dataBaseActor ! saveUser(record)
  }

  def updateUserData(record: UserRecord) = {
    dataBaseActor ! updateUser(record)
  }

  def getUser(user: UserRecord): Future[UserRecord] = {
    val future: Future[Any] = dataBaseActor ? sendUserData(user)
    future.mapTo[UserRecord]
  }
}
