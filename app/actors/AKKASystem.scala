package actors

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject._

import akka.pattern.ask
import actors.UserManagerActor.createNewUser

import scala.concurrent.duration._
import play.api.mvc._
import akka.actor._
import javax.inject._
import actors.DatenBankActor.fillUserData
import akka.util.Timeout
import objects.UserRecord

/**
  * Created by theer on 02.05.2017.
  */
@Singleton
class AKKASystem(system: ActorSystem) {

  val userManagerActor = system.actorOf(UserManagerActor.props, "UserManagerActor")
  val dataBaseActor = system.actorOf(DatenBankActor.props, "DatenbankActor")
  implicit val timeout: Timeout = 5.seconds

  def createUser(user: UserRecord) = {
    (dataBaseActor ! fillUserData(user, userManagerActor))
  }
}
