package controllers

import models.{RegisterData, UserData}
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
import slick.collection.heterogeneous.Zero.+

import scala.concurrent.Future
import scala.util.{Failure, Success}


class Application @Inject()(val messagesApi: MessagesApi, implicit val system: ActorSystem, implicit val materializer: Materializer) extends Controller with I18nSupport {

  val AKKASystemRef = new AKKASystem(system);

  def index = Action {
    Ok(views.html.login(loginForm, ""))
  }

  def gotoRegist = Action {
    Ok(views.html.regestrieren(registForm, ""))
  }

  def gotoLogin = Action {
    Ok(views.html.login(loginForm, ""))
  }

  def loginPost = Action(parse.form(loginForm)) { implicit request =>
    val loginData = request.body
    val newUser = new UserRecord(username = loginData.userName, password = loginData.password)
    if (AKKASystemRef checkCredentialsToAKKA (newUser)) {
      Redirect(routes.Application.chat()).withSession("user" -> newUser.username)
    } else {
      Ok(views.html.login(loginForm.fill(UserData("admin", "admin")), "Damit kannst du dich nicht einloggen"))
    }
  }

  def regestrieren = Action(parse.form(registForm)) { implicit request =>
    println("hier")
    val regiData = request.body
    println(regiData)
    if (regiData.password == regiData.confirm) {
      val nameArrey: Array[String] = (regiData.Name.getOrElse("").split(" ").reverse) :+ " "
      val firstname = nameArrey mkString " "
      val lastname = nameArrey.head
      val newUser = new UserRecord(
        username = regiData.userName,
        password = regiData.password,
        firstname = firstname,
        lastname = lastname,
        email = regiData.email.orNull,
        nickname = regiData.nickName)
      AKKASystemRef registerUser (newUser)
      Ok(views.html.login(loginForm.fill(UserData(newUser.username, "")), ""))
    }

    else {
      Ok(views.html.regestrieren(registForm.fill(RegisterData(regiData.userName, regiData.Name, regiData.nickName, regiData.email, "", "")), "Passwort stimmt nciht überein"))
    }

  }

  def socket = WebSocket.acceptOrResult[JsValue, JsValue] {
    request =>
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
  val registForm = Form(
    mapping(
      "Username" -> nonEmptyText,
      "Name" -> optional(text),
      "Nickname" -> optional(text),
      "Email" -> optional(text),
      "Password" -> nonEmptyText,
      "Confirm" -> nonEmptyText
    )(RegisterData.apply)(RegisterData.unapply)
  )
}