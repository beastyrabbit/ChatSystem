package actors

/**
  * Created by theer on 02.05.2017.
  */

import java.sql.Timestamp
import javax.inject.Inject

import actors.FrontEndInputActor.publishMessage
import actors.UserActor.setupUserChats
import akka.actor._
import objects.{DBChat, DBMessage, Tables, UserRecord}
import play.api.db.slick.DatabaseConfigProvider
import play.api.{Logger, Play}
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import models._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Json.{fromJson, toJson}

import scala.annotation.tailrec

class DatenBankActor extends Actor {

  import actors.DatenBankActor._

  val tables = new Tables
  val userDB = tables.userQuery
  val chatDB = tables.chatQuery
  val chattoUserDB = tables.userToChatQuery
  val historyDB = tables.historyQuery

  def receive = {
    case sendUserData(user: UserRecord) =>
      sendUserDataImp(user, sender())
    case checkCredentials(user: UserRecord) =>
      sender() ! checkCredentialsImp(user: UserRecord)
    case saveMessage(userRecord, chatMessageElement, chatid) =>
      saveMessageImp(userRecord, chatMessageElement, chatid, sender())
    case getFriends(user, sendto) =>
      println("test")
    case addFriend(user, newFriend) =>
      println("test")
    case getChats(user, sendto) =>
      sendChatsImp(user, sendto)
    case getMessagefromDB(chat, userRecord, sendto) =>
      getMessagefromDBImp(chat, userRecord, sendto)
    case saveUser(record: UserRecord) =>
      saveUserImp(record)
    case updateUser(record: UserRecord) =>
      updateUserImp(record)
    case searchforUser(search: String, displayRole: String, webSocket: ActorRef) =>
      searchforUserImp(search, displayRole, webSocket)
    case addChat(chatname: String, user1: UserRecord, user2: UserRecord, sendto: ActorRef) =>
      addChatImp(chatname, user1, user2, sendto, sender())
    case removeUserFromChat(chatid: Int, userRecord: UserRecord) =>
      removeUserFromChatImp(chatid, userRecord, sender())

  }

  def removeUserFromChatImp(chatid: Int, userRecord: UserRecord, sendto: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val filter = chattoUserDB.filter(row => row.chatid === chatid && row.userid === userRecord.userid)
    val action = filter.delete
    val future = db.run(action)
    future onComplete { case _ => (sendto ! "done") }
  }

  def updateUserImp(record: UserRecord): Unit = {
    println(record)
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val action = userDB.filter(_.userid === record.userid).map(user => (user.password, user.firstname, user.lastname, user.email, user.nickname, user.picture)).update((record.password, record.firstname, record.lastname, record.email, record.nickname, record.picture))
    println(action.statements)
    db.run(action)
  }

  def addChatImp(chatname: String, user1: UserRecord, user2: UserRecord, sendto: ActorRef, sendfrom: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val insertChat = chatDB returning chatDB.map(_.chatid) into ((item, chatid) => item.copy(chatid = Some(chatid))
      ) += DBChat(None, chatname)
    val future = db.run(insertChat)
    future.onSuccess {
      case dBChat: DBChat =>
        val chatid = dBChat.chatid.get
        val insertChatUser = DBIO.seq(chattoUserDB ++= Seq((chatid, user1.userid.get), (chatid, user2.userid.get)))
        val future = db.run(insertChatUser)
        future.onSuccess {
          case _ =>
            sendChatsImp(user1, sendto)
            sendfrom ! "done"
        }
    }

  }

  def searchforUserImp(search: String, displayRole: String, webSocket: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val searchWithLike = "%" + search + "%"
    val query = userDB.filter(user => user.username like searchWithLike)
    val future = db.run(query.result)
    future onSuccess {
      case sql: Seq[UserRecord] =>
        val jsonto: JsValue = Json.toJson(sql)
        val json = Json.obj(
          "msgType" -> "searchResult",
          "displayRole" -> displayRole,
          "data" -> jsonto.as[JsValue]
        )
        webSocket ! json
    }
  }

  @tailrec
  final def convertUsers(sqlreadout: Seq[(Option[Int], String, String, String, String, String, Option[String], Option[Timestamp], Option[String])], userList: List[UserRecord] = List.empty): List[UserRecord] = {
    if (sqlreadout.isEmpty) return userList
    val userTuple = sqlreadout.head
    val tempList = userList :+ new UserRecord(userTuple._1, userTuple._2, "", userTuple._4, userTuple._5, userTuple._6, userTuple._7, userTuple._8, userTuple._9)
    convertUsers(sqlreadout.tail, tempList)
  }


  def saveMessageImp(userRecord: UserRecord, chatMessageElement: ChatMessageElement, chatid: Int, sendto: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val insertMsg = historyDB returning historyDB.map(_.messageid) into ((item, messageid) => item.copy(messageid = Some(messageid))
      ) += DBMessage(None, chatMessageElement.messageText, chatid, userRecord.userid.get, chatMessageElement.messageTime)
    val future = db.run(insertMsg)
    future.onSuccess {
      case dBMessage: DBMessage =>
        val chatMessage = {
          new ChatMessage(user = userRecord, chatid = chatid.toString, message = dBMessage)
        }
        sendto ! publishMessage(chatMessage)
    }
  }

  def saveUserImp(record: UserRecord): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val insert = DBIO.seq(
      userDB += record)
    db.run(insert)
  }


  def getMessagefromDBImp(chatid: Int, userRecord: UserRecord, sendto: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val historyquery = historyDB.filter(t => t.chatid === chatid)
    val historyfuture = db.run(historyquery.result)
    historyfuture.onSuccess {
      case sql: Seq[DBMessage] =>
        val chatMessage = ChatMessages(chatid, sql)

        def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)

        def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)

        implicit val timestampFormat = new Format[Timestamp] {

          def writes(t: Timestamp): JsValue = toJson(timestampToDateTime(t))

          def reads(json: JsValue): JsResult[Timestamp] = fromJson[DateTime](json).map(dateTimeToTimestamp)

        }

        implicit val formatMessage = Json.format[DBMessage]
        implicit val formatChat = Json.format[ChatMessages]
        val jsonto: JsValue = Json.toJson(chatMessage)
        val json = Json.obj(
          "msgType" -> "setupMessageChat",
          "data" -> jsonto.as[JsObject]

        )
        sendto ! json
    }
  }

  def sendChatsImp(user: UserRecord, sendto: ActorRef) = {
    println(user)
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val userid = user.userid.get.toString
    val query = sql"""SELECT USERTOCHAT.CHATID,USERTOCHAT.USERID,CHAT.CHATNAME
                        FROM USERTOCHAT
                        INNER JOIN CHAT
                        ON CHAT.CHATID=USERTOCHAT.CHATID
                        WHERE USERTOCHAT.CHATID IN
                          (SELECT CHATID FROM USERTOCHAT WHERE USERID = #$userid)
                        AND USERID != #$userid
                     """.as[(Int, Int, String)]
    val readoutChatFuture: Future[Vector[(Int, Int, String)]] = db.run(query)
    readoutChatFuture.onSuccess {
      case sql => {
        println(sql)
        val chatrooms = {
          new ChatRooms(sql.map(elem => new ChatRoomElement(chatid = elem._1, name = elem._3, userid = elem._2)))
        }
        sendto ! setupUserChats(chatrooms)
      }
    }
  }

  def checkCredentialsImp(olduser: UserRecord): Option[UserRecord] = {
    val future: Future[UserRecord] = getUserDataFuture(olduser)
    val result: Try[UserRecord] = Await.ready(future, Duration.Inf).value.get
    result match {
      case Success(user) => Some(user)
      case Failure(e) => None
    }
  }


  def sendUserDataImp(olduser: UserRecord, sendto: ActorRef) = {
    val readOutUserDataFuture = getUserDataFuture(olduser)
    readOutUserDataFuture onComplete {
      case Success(user) => {
        sendto ! user
      }
      case Failure(ex) => throw ex

    }
  }


  private def getUserDataFuture(olduser: UserRecord): Future[UserRecord] = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    if (olduser.userid == None) {
      val userquery = userDB.filter(_.username === olduser.username)
      val run: Future[UserRecord] = db.run(userquery.result.head)
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

  case class saveUser(record: UserRecord)

  case class updateUser(record: UserRecord)

  case class searchforUser(search: String, displayRole: String, webSocket: ActorRef)

  case class addChat(chatname: String, user1: UserRecord, user2: UserRecord, sendto: ActorRef)

  case class removeUserFromChat(chatid: Int, userRecord: UserRecord)

}