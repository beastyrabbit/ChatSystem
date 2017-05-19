package actors

/**
  * Created by theer on 02.05.2017.
  */

import actors.UserActor.{setupUserChats, setupUserRecord}
import akka.actor._
import objects.{Tables, UserRecord}
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Logger, Play}
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import actors.UserActor.{ChatRooms, Chat}

class DatenBankActor extends Actor {

  import actors.DatenBankActor._

  val tables = new Tables


  def receive = {
    case sendUserData(user: UserRecord) =>
      sendUserDataImp(user, sender())
    case checkCredentials(user: UserRecord) =>
      sender() ! checkCredentialsImp(user: UserRecord)
    case saveMessage(sender, reciver, msg) =>
      println("TEMPPPER")
    case getFriends(user, sendto) =>
      println("test")
    case addFriend(user, newFriend) =>
      println("test")
    case getChats(user, sendto) =>
      Logger.info(user.toString)
      sendChatsImp(user, sendto)
  }

  def sendChatsImp(user: UserRecord, sendto: ActorRef) = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val chatDB = tables.chatQuery
    val chatquery = chatDB.filter(_.userid === user.userid)
    val readoutChatFuture = db.run(chatquery.result)
    readoutChatFuture onComplete {
      case Success(sql: Seq[(Option[Int], Int, String)]) => {
        val chatrooms = new ChatRooms(sql.map(elem => new Chat(elem._1.get, elem._2, elem._3)))
        sendto ! setupUserChats(chatrooms)
      }
      case Failure(ex) => throw ex

    }

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
        sendto ! user
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

  case class getChats(user: UserRecord, sendto: ActorRef)

  case class sendUserData(user: UserRecord)

  case class checkCredentials(user: UserRecord)

  case class addFriend(user: UserRecord, newFriend: ActorRef)

  case class getFriends(user: UserRecord, sendto: ActorRef)

  case class saveMessage(sender: UserRecord, reciver: UserRecord, msg: String)

}