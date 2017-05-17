package controllers

import models.UserData
import objects.UserRecord
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import javax.inject.Inject
import javax.management.MBeanOperationInfo

import actors._
import actors.UserActor._
import akka.actor.ActorSystem
import akka.stream.Materializer
import exceptions.WrongCredentials
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow

import scala.concurrent.Future
import scala.util.{Failure, Success}


class Application @Inject()(val messagesApi: MessagesApi, implicit val system: ActorSystem, implicit val materializer: Materializer) extends Controller with I18nSupport {

  val AKKASystemRef = new AKKASystem(system);

  def index = Action {
    Ok(views.html.login(loginForm.fill(UserData("admin", "admin"))))
  }

  def loginPost = Action(parse.form(loginForm)) { implicit request =>
    val loginData = request.body
    val newUser = new UserRecord(username = loginData.userName, password = loginData.password)
    if (AKKASystemRef checkCredentialsToAKKA (newUser)) {
      Redirect(routes.Application.chat()).withSession("user" -> newUser.username)
    } else {
      Ok("Bad Login")
    }
  }

  def socket = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    Future.successful(request.session.get("user") match {
      case None => Left(Forbidden)
      case Some(user) => Right(ActorFlow.actorRef(out => UserActor.props(new UserRecord(username = user), out, AKKASystemRef)))
    })
  }

  def chat = Action {
    Ok(views.html.chat())
  }

  def errorOutput(message: String) = Action {
    Ok(message)
  }

  val loginForm = Form(
    mapping(
      "Username" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(UserData.apply)(UserData.unapply)
  )

  def templogin() = Action {
    AKKASystemRef.createUserToAKKA(new UserRecord())
    Ok("Durch")
  }
}