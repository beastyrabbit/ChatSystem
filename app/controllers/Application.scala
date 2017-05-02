package controllers

import models.UserData
import objects.User
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import javax.inject.Inject
import javax.management.MBeanOperationInfo

import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi

class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {
  def index = Action {
    Ok(views.html.login(loginForm))
  }

  def loginPost = Action(parse.form(loginForm)) { implicit request =>
    val loginData = request.body
    val newUser = new User(loginData.userName, loginData.password)
    println(loginData.userName + " " + loginData.password)
    //Redirect(routes.Application.home(newUser.id))
    Ok("Hello" + loginData.userName + " pw: " + loginData.password)
  }

  val loginForm = Form(
    mapping(
      "Username" -> nonEmptyText,
      "Password" -> nonEmptyText
    )(UserData.apply)(UserData.unapply)
  )

  def home(id: String) = Action {
    Ok("Hallo" + id)
  }
}