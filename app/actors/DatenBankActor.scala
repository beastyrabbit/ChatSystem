package actors

/**
  * Created by theer on 02.05.2017.
  */

import java.sql.Timestamp
import javax.inject.Inject

import actors.UserActor.getUserRecord

import scala.concurrent.duration._
import actors.UserManagerActor.addNewUser

import scala.collection.breakOut
import akka.actor._
import exceptions.WrongCredentials
import objects.UserRecord
import slick.jdbc.H2Profile.api._
import objects.Tables
import org.h2.engine.User
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect
import slick.jdbc.JdbcProfile
import slick.sql.FixedSqlStreamingAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class DatenBankActor extends Actor {

  import DatenBankActor._
  val tables = new Tables

  def receive = {
    case sendUserData(user: UserRecord, sendto: ActorRef) =>
      sendUserDataImp(user, sendto)
    case checkCredentials(user: UserRecord) =>
      sender() ! checkCredentialsImp(user: UserRecord)
    case saveMessage(sender,reciver,msg) =>
      println("TEMPPPER")
    case getFriends(user, sendto) =>
      println("test")
    case addFriend(user, newFriend) =>
      println("test")
  }


  def checkCredentialsImp(olduser: UserRecord): Boolean = {
    val readoutuserFuture = getUserDataFuture(olduser)
    val sql = Await.result(readoutuserFuture, 10 second)
    val userTuple = sql(0)
    val user = new UserRecord(userTuple._1.get, userTuple._2, userTuple._3, userTuple._4, userTuple._5, userTuple._6, userTuple._7, userTuple._8, userTuple._9)
    if (user.password == olduser.password) {
      true
    } else {
      false
    }
  }


  def sendUserDataImp(olduser: UserRecord, sendto: ActorRef) = {
    val readOutUserDataFuture = getUserDataFuture(olduser)
    readOutUserDataFuture onComplete {
      case Success(sql) => {
        val userTuple = sql(0)
        val user = new UserRecord(userTuple._1.get, userTuple._2, userTuple._3, userTuple._4, userTuple._5, userTuple._6, userTuple._7, userTuple._8, userTuple._9)
        sendto ! getUserRecord(user)
      }
      case Failure(ex) => throw ex

    }
  }

  private def getUserDataFuture(olduser: UserRecord) = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val userDB = tables.userQuery
    val userquery = userDB.filter(_.username === olduser.username)
    val readoutuserFuture = db.run(userquery.result)
    readoutuserFuture
  }




}

object DatenBankActor {
  def props(): Props = Props(new DatenBankActor())

  case class sendUserData(user: UserRecord, sendto: ActorRef)

  case class checkCredentials(user: UserRecord)

  case class addFriend(user: UserRecord, newFriend: ActorRef)

  case class getFriends(user: UserRecord, sendto: ActorRef)
  case class saveMessage(sender: UserRecord, reciver: UserRecord, msg: String)
}