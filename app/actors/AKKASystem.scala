package actors

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject._

import akka.pattern.ask
import actors.UserManagerActor.createNewUser

import scala.concurrent.duration._
import play.api.mvc._
import akka.actor._
import javax.inject._

import akka.util.Timeout

/**
  * Created by theer on 02.05.2017.
  */
@Singleton
class AKKASystem(system: ActorSystem) {

  val userManagerActor = system.actorOf(UserManagerActor.props, "UserManagerActor")
  implicit val timeout: Timeout = 5.seconds

  def createUser(userName: String) = {
    (userManagerActor ! createNewUser(userName))
  }
}
