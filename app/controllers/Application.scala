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
import akka.actor.ActorSystem
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi

class Application @Inject()(val messagesApi: MessagesApi, system: ActorSystem) extends Controller with I18nSupport {

  val AKKASystemRef = new AKKASystem(system);

  def index = Action {
    Ok(views.html.login(loginForm))
  }

  def loginPost = Action(parse.form(loginForm)) { implicit request =>
    val loginData = request.body
    val newUser = new UserRecord(username = loginData.userName, password = loginData.password)
    AKKASystemRef.createUser(newUser)
    Ok("Hello! " + newUser.username)
  }

  val loginForm = Form(
    mapping(
      "Username" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(UserData.apply)(UserData.unapply)
  )

  def templogin() = Action {
    AKKASystemRef.createUser(new UserRecord())
    Ok("Durch")
  }
}