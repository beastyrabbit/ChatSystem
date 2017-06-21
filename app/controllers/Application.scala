package controllers

import javax.inject.Inject

import actors._
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.Timeout
import com.github.t3hnar.bcrypt._
import models.{RegisterData, UpdateData, UserData}
import objects.UserRecord
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * This Class is the Controller for the Application
  *
  * @param messagesApi
  * @param system
  * @param materializer
  */

class Application @Inject()(val messagesApi: MessagesApi, implicit val system: ActorSystem, implicit val materializer: Materializer) extends Controller with I18nSupport {

  val AKKASystemRef = new AKKASystem(system)
  implicit val timeout = Timeout(5 seconds)
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
      "Picture" -> optional(text),
      "Password" -> nonEmptyText,
      "Confirm" -> nonEmptyText
    )(RegisterData.apply)(RegisterData.unapply)
  )
  val updateForm = Form(
    mapping(
      "Name" -> optional(text),
      "Nickname" -> optional(text),
      "Email" -> optional(text),
      "Picture" -> optional(text),
      "Password" -> optional(nonEmptyText),
      "Confirm" -> optional(nonEmptyText)
    )(UpdateData.apply)(UpdateData.unapply)
  )

  /**
    * This is the start of the App
    */
  def index = {
    Action {
      Ok(views.html.login(loginForm, ""))
    }
  }

  /**
    * This Method forwards the Registration
    */
  def gotoRegist = {
    Action {
      Ok(views.html.regestrieren(registForm, ""))
    }
  }

  /**
    * This Method forwards the Login
    */
  def gotoLogin = {
    Action {
      Ok(views.html.login(loginForm, ""))
    }
  }

  /**
    * This Methode Handles the Updating of Userdata from FrontEnd
    */
  def gotoUpdateUserData = {
    Action { implicit request =>
      val userid = request.cookies.get("userid").get.value
      println(userid)
      val future = AKKASystemRef.getUser(new UserRecord(userid = Some(userid.toInt)))
      val userRecord = Await.result(future, 10 second)
      Ok(views.html.updateUser(updateForm.fill(UpdateData(
        Name = Some(userRecord.firstname + " " + userRecord.lastname)
        , nickName = userRecord.nickname
        , email = Some(userRecord.email)
        , picture = userRecord.picture
        , password = None
        , confirm = None
      )), ""
      ))
    }
  }

  /**
    * This Methode is starting the Login
    */
  def loginPost = {
    Action.async(parse.form(loginForm)) { implicit request =>
      val loginData = request.body
      val newUser = new UserRecord(username = loginData.userName.trim, password = loginData.password)
      val future: Future[Option[UserRecord]] = AKKASystemRef checkCredentialsToAKKA newUser
      future.map {
        case Some(user) =>
          if (loginData.password.isBcrypted(user.password)) {
            Redirect(routes.Application.chat()).withCookies(Cookie("user", newUser.username))
          } else {
            Ok(views.html.login(loginForm.fill(UserData("admin", "admin")), "Damit kannst du dich nicht einloggen"))
          }
        case None => Ok(views.html.login(loginForm.fill(UserData("admin", "admin")), "Damit kannst du dich nicht einloggen"))
      }
        .recover {
          case e: Exception =>
            Ok(views.html.login(loginForm.fill(UserData("", "")), e.getMessage))

        }
    }
  }

  /**
    * This Methode is starting the update User
    */
  def updateUser() = {
    Action(parse.form(updateForm)) {
      implicit request =>
        val cookieuserid = request.cookies.get("userid").get.value.toInt
        val regiData = request.body
        Logger.info("Updating User: " + regiData)
        if (regiData.password == regiData.confirm) {
          Logger.info(" User has been updated Name: " + regiData.Name)
          val nameArrey: Array[String] = regiData.Name.getOrElse("").split(" ").map(_.trim).reverse
          val firstname = nameArrey.tail.mkString(" ")
          println("::" + firstname + "::")
          val lastname = nameArrey.head
          val newUser = new UserRecord(
            userid = Some(cookieuserid),
            password = regiData.password.getOrElse(""),
            firstname = firstname,
            lastname = lastname,
            email = regiData.email.orNull,
            nickname = regiData.nickName,
            picture = regiData.picture)
          AKKASystemRef updateUserData newUser
          Ok(views.html.chat())
        }
        else {
          Ok(views.html.updateUser(updateForm.fill(UpdateData(regiData.Name, regiData.nickName, regiData.email, regiData.picture, None, None)), "Passwort stimmt nciht überein"))
        }

    }
  }

  /**
    * This Methode is starting the Registration
    */
  def regestrieren = {
    Action(parse.form(registForm)) {
      implicit request =>
        val regiData = request.body
        if (regiData.password == regiData.confirm) {
          Logger.info("New User has been added Name: " + regiData.Name)
          val nameArrey: Array[String] = regiData.Name.getOrElse("").split(" ").reverse :+ " "
          val firstname = nameArrey mkString " "
          val lastname = nameArrey.head
          val newUser = new UserRecord(
            username = regiData.userName,
            password = regiData.password,
            firstname = firstname,
            lastname = lastname,
            email = regiData.email.orNull,
            nickname = regiData.nickName,
            picture = regiData.picture)
          AKKASystemRef registerUser newUser
          Ok(views.html.login(loginForm.fill(UserData(newUser.username, "")), ""))
        }

        else {
          Ok(views.html.regestrieren(registForm.fill(RegisterData(regiData.userName, regiData.Name, regiData.nickName, regiData.email, regiData.picture, "", "")), "Passwort stimmt nciht überein"))
        }

    }
  }

  /**
    * This Methode opens a Websocket and creates an [[UserActor]]
    */
  def socket = {
    WebSocket.acceptOrResult[JsValue, JsValue] {
      request =>
        println(request.cookies.get("user").get.value)
        Future.successful(request.cookies.get("user").orNull.value match {
          case "" => Left(Forbidden)
          case user => Right(ActorFlow.actorRef(out => UserActor.props(new UserRecord(username = user), out, AKKASystemRef)))
        })
    }
  }

  /**
    * This Methode is forwarting the Chat
    */
  def chat() = {
    Action {
      Ok(views.html.chat())
    }
  }
}