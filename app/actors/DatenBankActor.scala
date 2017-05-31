package actors

/**
  * Created by theer on 02.05.2017.
  */

import java.sql.Timestamp

import actors.FrontEndInputActor.publishMessage
import actors.UserActor.setupUserChats
import akka.actor._
import objects.{DBMessage, Tables, UserRecord}
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Logger, Play}
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Json.{fromJson, toJson}

class DatenBankActor extends Actor {

  import actors.DatenBankActor._

  val tables = new Tables


  def saveMessageImp(userRecord: UserRecord, chatMessageElement: ChatMessageElement, chatid: Int, sendto:ActorRef): Unit= {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val historyDB = tables.historyQuery
    val insertMsg = historyDB returning historyDB.map(_.messageid) into ((item, messageid) => item.copy(messageid = Some(messageid))
      ) += DBMessage(None, chatMessageElement.messageText, chatid, userRecord.userid, chatMessageElement.messageTime)
    val future = db.run(insertMsg)
    future.onSuccess {
      case dBMessage: DBMessage =>
        val chatMessage = {
          new ChatMessage(user = userRecord, chatid = chatid.toString, message = dBMessage)
        }
        sendto ! publishMessage(chatMessage)
      case t:Any => println(t)
    }
  }

  def receive = {
    case sendUserData(user: UserRecord) =>
      sendUserDataImp(user, sender())
    case checkCredentials(user: UserRecord) =>
      sender() ! checkCredentialsImp(user: UserRecord)
    case saveMessage(userRecord, chatMessageElement, chatid) =>
      saveMessageImp(userRecord, chatMessageElement, chatid,sender())
    case getFriends(user, sendto) =>
      println("test")
    case addFriend(user, newFriend) =>as kön
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
      case sql: Seq[DBMessage] =>
        val chatMessage = new ChatMessages(chatid, sql)
        println(chatMessage)

        def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)

        def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)

        implicit val timestampFormat = new Format[Timestamp] {

          def writes(t: Timestamp): JsValue = toJson(timestampToDateTime(t))

          def reads(json: JsValue): JsResult[Timestamp] = fromJson[DateTime](json).map(dateTimeToTimestamp)

        }

        implicit val formatMessage = Json.format[DBMessage]
        implicit val formatChat = Json.format[ChatMessages]
        val jsonto = Json.toJson(chatMessage)
        val json = Json.obj(
          "msgType" -> "setupMessageChat",
            "data" -> jsonto.as[JsObject]

        )
        sendto ! json
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