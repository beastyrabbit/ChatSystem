package actors

/**
  * Created by theer on 02.05.2017.
  */

import java.sql.Timestamp

import actors.UserActor.setupUserChats
import akka.actor._
import objects.{Tables, UserRecord}
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Logger, Play}
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import models._

class DatenBankActor extends Actor {

  import actors.DatenBankActor._

  val tables = new Tables


  def saveMessageImp(userRecord: UserRecord, chatMessageElement: ChatMessageElement, chatid: Int): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val historyDB = tables.historyQuery
    val insertMsg = DBIO.seq(
      historyDB += (None, chatMessageElement.messageText, chatid, userRecord.userid, chatMessageElement.messageTime)
    )
    db.run(insertMsg)
  }

  def receive = {
    case sendUserData(user: UserRecord) =>
      sendUserDataImp(user, sender())
    case checkCredentials(user: UserRecord) =>
      sender() ! checkCredentialsImp(user: UserRecord)
    case saveMessage(userRecord, chatMessageElement, chatid) =>
      saveMessageImp(userRecord, chatMessageElement, chatid)
    case getFriends(user, sendto) =>
      println("test")
    case addFriend(user, newFriend) =>
      println("test")
    case getChats(user, sendto) =>
      sendChatsImp(user, sendto)
    case getMessagefromDB(chat, userRecord, sendto) =>
      getMessagefromDBImp(chat, userRecord, sendto)

  }

  def getMessagefromDBImp(chatid: Int, userRecord: UserRecord, sendto: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val historyDB = tables.historyQuery
    val historyquery = historyDB.filter(t => t.userid === userRecord.userid && t.chatid === chatid)
    val historyfuture = db.run(historyquery.result)
    historyfuture.onSuccess {
      case sql: Seq[(Option[Int], String, Int, Int, Timestamp)] =>
        val chatMessage = new ChatMessages(chatid, sql.map(elem => new ChatMessageElement(elem._1, elem._2, elem._5, new UserRecord(userid = elem._4))))
        println(chatMessage)
    }
  }

  def sendChatsImp(user: UserRecord, sendto: ActorRef) = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val chatDB = tables.chatQuery
    val chattoUserDB = tables.userToChatQuery
    val chatjoinUser = for {
      (c, u) <- chatDB join chattoUserDB on (_.chatid === _.chatid)
    } yield (c.chatid, c.name, u.userid)
    val readoutChatFuture: Future[Seq[(Int, String, Int)]] = db.run(chatjoinUser.result)
    readoutChatFuture.onSuccess {
      case sql: Seq[(Int, String, Int)] => {
        val chatrooms = {
          new ChatRooms(sql.filter(p => p._3 == user.userid).map(elem => new ChatRoomElement(chatid = elem._1, name = elem._2, userid = elem._3)))
        }
        sendto ! setupUserChats(chatrooms)
      }
    }
  }

  def checkCredentialsImp(olduser: UserRecord): Boolean = {
    val readoutuserFuture = getUserDataFuture(olduser)
    val userTuple = Await.result(readoutuserFuture, 10 second)
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
        val userTuple = sql
        val user = new UserRecord(userTuple._1.get, userTuple._2, userTuple._3, userTuple._4, userTuple._5, userTuple._6, userTuple._7, userTuple._8, userTuple._9)
        sendto ! user
      }
      case Failure(ex) => throw ex

    }
  }


  private def getUserDataFuture(olduser: UserRecord): Future[(Option[Int], String, String, String, String, String, Option[String], Option[Timestamp], Option[String])] = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val userDB = tables.userQuery
    if (olduser.userid == -1) {
      val userquery = userDB.filter(_.username === olduser.username)
      val run = db.run(userquery.result.head)
      return run
    }
    else {
      val userquery = userDB.filter(_.userid === olduser.userid)
      val run = db.run(userquery.result.head)
      return run
    }
  }


}

object DatenBankActor {
  def props(): Props = Props(new DatenBankActor())

  case class getChats(user: UserRecord, sendto: ActorRef)

  case class sendUserData(user: UserRecord)

  case class checkCredentials(user: UserRecord)

  case class addFriend(user: UserRecord, newFriend: ActorRef)

  case class getFriends(user: UserRecord, sendto: ActorRef)

  case class saveMessage(userRecord: UserRecord, chatMessageElement: ChatMessageElement, chatid: Int)

  case class getMessagefromDB(chat: Int, userRecord: UserRecord, sendto: ActorRef)

}