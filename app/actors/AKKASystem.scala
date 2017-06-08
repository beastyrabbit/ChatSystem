package actors

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject._

import akka.pattern.ask
import actors.UserManagerActor.addNewUser

import scala.concurrent.duration._
import akka.actor._
import javax.inject._

import actors.DatenBankActor.{checkCredentials, saveUser, sendUserData, updateUser}
import akka.util.Timeout
import exceptions.WrongCredentials
import objects.UserRecord
import play.api.mvc.{Result, Results}

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import akka.actor.{ActorRef, ActorSystem}
import akka.serialization._
import com.typesafe.config.ConfigFactory

/**
  * Created by theer on 02.05.2017.
  */
@Singleton
class AKKASystem(system: ActorSystem) {


  val userManagerActor = system.actorOf(UserManagerActor.props, "UserManagerActor")
  val dataBaseActor = system.actorOf(DatenBankActor.props, "DatenbankActor")
  val frontEndInputActor = system.actorOf(FrontEndInputActor.props(this), "FrontEndInputActor")
  implicit val timeout: Timeout = 5.seconds
  val subscribeChat = new SubscribeChat

  def checkCredentialsToAKKA(user: UserRecord): Future[Option[UserRecord]] = {
    val eventualRecord: Future[Option[UserRecord]] = (dataBaseActor ? checkCredentials(user)).mapTo[Option[UserRecord]]
    return eventualRecord
  }

  def registerUser(record: UserRecord) = {
    dataBaseActor ! saveUser(record)
  }

  def updateUserData(record: UserRecord) = {
    dataBaseActor ! updateUser(record)
  }

  def getUser(user: UserRecord): Future[UserRecord] = {
    val future: Future[Any] = dataBaseActor ? sendUserData(user)
    return future.mapTo[UserRecord]
  }
}
