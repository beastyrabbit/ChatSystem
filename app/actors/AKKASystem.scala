package actors

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject._

import akka.pattern.ask
import actors.UserManagerActor.createNewUser

import scala.concurrent.duration._
import akka.actor._
import javax.inject._

import actors.DatenBankActor.{checkCredentials, sendUserData}
import akka.util.Timeout
import exceptions.WrongCredentials
import objects.UserRecord
import play.api.mvc.{Result, Results}

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by theer on 02.05.2017.
  */
@Singleton
class AKKASystem(system: ActorSystem) {
  val userManagerActor = system.actorOf(UserManagerActor.props, "UserManagerActor")
  val dataBaseActor = system.actorOf(DatenBankActor.props, "DatenbankActor")
  implicit val timeout: Timeout = 5.seconds

  def createUserToAKKA(user: UserRecord) = {
    dataBaseActor ! sendUserData(user, userManagerActor)
  }

  def checkCredentialsToAKKA(user: UserRecord): Boolean = {
    val future = dataBaseActor ? checkCredentials(user)
    Await.result(future, 10 second).asInstanceOf[Boolean]
  }
}
