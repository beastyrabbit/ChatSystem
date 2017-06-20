package actors

/**
  * Created by theer on 02.05.2017.
  */

import java.sql.Timestamp

import actors.FrontEndInputActor.publishMessage
import actors.UserActor.setupUserChats
import actors.UserManagerActor.checkUserBACK
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.github.t3hnar.bcrypt._
import models.{ChatMessageElement, _}
import objects.{DBChat, DBMessage, Tables, UserRecord}
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.Json.{fromJson, toJson}
import play.api.libs.json.{JsObject, JsValue, Json, _}
import play.api.{Logger, Play}
import slick.jdbc.H2Profile.api._
import slick.jdbc.JdbcProfile

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
  * This Actor does all the DB access
  *
  * @param system for Reference
  */

class DatenBankActor(system: AKKASystem) extends Actor {

  import actors.DatenBankActor._

  implicit val timeout = Timeout(5 seconds)
  val tables = new Tables
  val userDB = tables.userQuery
  val chatDB = tables.chatQuery
  val chattoUserDB = tables.userToChatQuery
  val historyDB = tables.historyQuery

  /**
    * Message Handler for Actor
    *
    * @return
    */
  def receive = {
    case sendUserData(userRecord: UserRecord) =>
      sendUserDataImpl(userRecord, sender())
    case checkCredentials(userRecord: UserRecord) =>
      sender() ! checkCredentialsImpl(userRecord: UserRecord)
    case saveMessage(userRecord, chatMessageElement, chatid) =>
      addMessageImpl(userRecord, chatMessageElement, chatid, sender())
    case getChats(userRecord, sendto) =>
      sendChatsImp(userRecord, sendto)
    case getMessages(chat, websocket) =>
      getMessagesImpl(chat, websocket)
    case saveUser(userRecord: UserRecord) =>
      addUserImpl(userRecord)
    case updateUser(userRecord: UserRecord) =>
      updateUserImp(userRecord)
    case searchforUser(search: String, displayRole: String, webSocket: ActorRef) =>
      searchforUserImp(search, displayRole, webSocket)
    case addChat(chatname: String, initUser: UserRecord, effectedUser: UserRecord, sendto: ActorRef) =>
      addChatImp(chatname, initUser, effectedUser, sendto, sender())
    case removeUserFromChat(chatid: Int, userRecord: UserRecord) =>
      removeUserFromChatImpl(chatid, userRecord, sender())
    case addUserToChat(chatid, userid) =>
      addUserToChatImpl(chatid, userid)

  }

  /**
    * This Methode adds an User to a Chat
    *
    * @param chatid of the Chat
    * @param userid of the User
    */
  def addUserToChatImpl(chatid: Int, userid: Int): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val insertStatement = DBIO.seq(chattoUserDB += (chatid, userid))
    db.run(insertStatement)
    val filterStatement = chattoUserDB.filter(row => row.chatid === chatid)
    val future = db.run(filterStatement.result)
    future onSuccess {
      case sql: Seq[(Int, Int)] =>
        sql.foreach(userTuple => {
          val user = new UserRecord(userid = Some(userTuple._2))
          val future: Future[Any] = system.userManagerActor ? checkUserBACK(user)
          future onSuccess {
            case Some(userSet: Set[(UserRecord, ActorRef)]) =>
              system.dataBaseActor ! getChats(user, userSet.head._2)
            case None => Logger.debug("Nutzer: " + user.username + " nicht online")
          }
        })
    }
  }

  /**
    * This Methode removes an User from a given Chat
    *
    * @param chatid     of the Chat
    * @param userRecord of the User to remove
    * @param sendto     an Actor
    */
  def removeUserFromChatImpl(chatid: Int, userRecord: UserRecord, sendto: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val filterStatement = chattoUserDB.filter(row => row.chatid === chatid && row.userid === userRecord.userid)
    val deleteAction = filterStatement.delete
    val future = db.run(deleteAction)
    future onComplete (_ => sendto ! "done")
  }

  /**
    * This Methode updates an UserRecord
    *
    * @param userRecord [[UserRecord]] to Update
    */
  def updateUserImp(userRecord: UserRecord): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val filterStatement = userDB.filter(_.userid === userRecord.userid).map(user => (user.password, user.firstname, user.lastname, user.email, user.nickname, user.picture)).update((userRecord.password, userRecord.firstname, userRecord.lastname, userRecord.email, userRecord.nickname, userRecord.picture))
    db.run(filterStatement)
  }

  /**
    * This Methode adds an Chat between two Users
    *
    * @param chatname     to Call the Chat only for GroupChat
    * @param initUser     to Add
    * @param effectedUser to Add
    * @param sendto       an Actor
    * @param sendfrom     sending Actor to signal done
    */
  def addChatImp(chatname: String, initUser: UserRecord, effectedUser: UserRecord, sendto: ActorRef, sendfrom: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val insertStatement = chatDB returning chatDB.map(_.chatid) into ((item, chatid) => item.copy(chatid = Some(chatid))
      ) += DBChat(None, chatname)
    val future = db.run(insertStatement)
    future.onSuccess {
      case dBChat: DBChat =>
        val chatid = dBChat.chatid.get
        val insertStatement = DBIO.seq(chattoUserDB ++= Seq((chatid, initUser.userid.get), (chatid, effectedUser.userid.get)))
        val future = db.run(insertStatement)
        future.onSuccess {
          case _ =>
            sendChatsImp(initUser, sendto)
            sendfrom ! "done"
        }
    }
  }

  /**
    * This Methode gets a Chats for an given [[UserRecord]]
    *
    * @param userRecord to Search for Chats
    * @param sendto     an Actor
    */
  def sendChatsImp(userRecord: UserRecord, sendto: ActorRef) = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val userid = userRecord.userid.get.toString
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
      case sql =>
        println(sql)
        val chatrooms = {
          new ChatRooms(sql.map(elem => new ChatRoomElement(chatid = elem._1, name = elem._3, userid = elem._2)))
        }
        sendto ! setupUserChats(chatrooms)
    }
  }

  /**
    * This Methode looks for a searchString in the UserDB
    *
    * @param search      is a String to Search for
    * @param displayRole Role where to Display just passtrough from Frontend
    * @param webSocket   to send to
    */
  def searchforUserImp(search: String, displayRole: String, webSocket: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val searchWithLike = "%" + search + "%"
    val filterStatement = userDB.filter(user => user.username like searchWithLike)
    val future = db.run(filterStatement.result)
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

  /**
    * This Methode saves a Message of a given User and Chat
    *
    * @param userRecord         of a User
    * @param chatMessageElement ChatElement to Save
    * @param chatid             of a Chat
    * @param sendto             an Actor
    */
  def addMessageImpl(userRecord: UserRecord, chatMessageElement: ChatMessageElement, chatid: Int, sendto: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val insertStatement = historyDB returning historyDB.map(_.messageid) into ((item, messageid) => item.copy(messageid = Some(messageid))
      ) += DBMessage(None, chatMessageElement.messageText, chatid, userRecord.userid.get, chatMessageElement.messageTime)
    val future = db.run(insertStatement)
    future.onSuccess {
      case dBMessage: DBMessage =>
        val chatMessage = {
          new ChatMessage(user = userRecord, chatid = chatid.toString, message = dBMessage)
        }
        sendto ! publishMessage(chatMessage)
    }
  }

  /**
    * This Methode adds an [[UserRecord]] to DB
    *
    * @param userRecord of an User
    */
  def addUserImpl(userRecord: UserRecord): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val updateUserRecord = userRecord.copy(password = userRecord.password.bcrypt)
    val insertStatement = DBIO.seq(
      userDB += userRecord)
    db.run(insertStatement)
  }


  /**
    * This Methode gets Messages for a given chatid and [[UserRecord]]
    *
    * @param chatid    of an Chat in DB
    * @param websocket an ActorRef
    */
  def getMessagesImpl(chatid: Int, websocket: ActorRef): Unit = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val filterStatement = historyDB.filter(t => t.chatid === chatid)
    val future = db.run(filterStatement.result)
    future.onSuccess {
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
        websocket ! json
    }
  }

  /**
    * This Methode checks for a User in DB
    *
    * @param userRecord [[UserRecord]] must contain [[UserRecord.username]] or [[UserRecord.userid]]
    * @return Option of a [[UserRecord]] If on is in DB
    */
  def checkCredentialsImpl(userRecord: UserRecord): Option[UserRecord] = {
    val future: Future[UserRecord] = getUserDataFuture(userRecord)
    val result: Try[UserRecord] = Await.ready(future, Duration.Inf).value.get
    result match {
      case Success(user) => Some(user)
      case Failure(e) => None
    }
  }

  /**
    * This Methode sends a [[UserRecord]] to an [[ActorRef]]
    *
    * @param userRecord [[UserRecord]] must contain [[UserRecord.username]] or [[UserRecord.userid]]
    * @param sendto     Where to send the [[UserRecord]]
    */
  def sendUserDataImpl(userRecord: UserRecord, sendto: ActorRef): Unit = {
    val future = getUserDataFuture(userRecord)
    future onComplete {
      case Success(user) =>
        sendto ! user.copy(password = "")
      case Failure(ex) => throw ex

    }
  }

  /**
    * This Methode ask the DB for a User
    *
    * @param userRecord [[UserRecord]] must contain [[UserRecord.username]] or [[UserRecord.userid]]
    * @return A Future of a [[UserRecord]]
    */
  private def getUserDataFuture(userRecord: UserRecord): Future[UserRecord] = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    if (userRecord.userid.isEmpty) {
      val filterStatement = userDB.filter(_.username === userRecord.username)
      val run: Future[UserRecord] = db.run(filterStatement.result.head)
      run
    }
    else {
      val filterStatement = userDB.filter(_.userid === userRecord.userid)
      val run = db.run(filterStatement.result.head)
      run
    }
  }

  /**
    * This Methode converts a Sequence of [[Tuple9]] into a List of [[UserRecord]]
    *
    * @param sqlreadout Sequence of User tuples
    * @param userList   tempList for tailrec
    * @return returns List of [[UserRecord]]
    */
  @tailrec
  final private def convertUsers(sqlreadout: Seq[(Option[Int], String, String, String, String, String, Option[String], Option[Timestamp], Option[String])], userList: List[UserRecord] = List.empty): List[UserRecord] = {
    if (sqlreadout.isEmpty) return userList
    val userTuple = sqlreadout.head
    val tempList = userList :+ new UserRecord(userTuple._1, userTuple._2, "", userTuple._4, userTuple._5, userTuple._6, userTuple._7, userTuple._8, userTuple._9)
    convertUsers(sqlreadout.tail, tempList)
  }


}

object DatenBankActor {
  def props(system: AKKASystem): Props = {
    Props(new DatenBankActor(system: AKKASystem))
  }

  case class getChats(userRecord: UserRecord, sendto: ActorRef)

  case class sendUserData(userRecord: UserRecord)

  case class checkCredentials(userRecord: UserRecord)

  case class saveMessage(userRecord: UserRecord, chatMessageElement: ChatMessageElement, chatid: Int)

  case class getMessages(chat: Int, websocket: ActorRef)

  case class saveUser(userRecord: UserRecord)

  case class updateUser(userRecord: UserRecord)

  case class searchforUser(search: String, displayRole: String, webSocket: ActorRef)

  case class addChat(chatname: String, initUser: UserRecord, effectedUser: UserRecord, sendto: ActorRef)

  case class removeUserFromChat(chatid: Int, userRecord: UserRecord)

  case class addUserToChat(chatid: Int, userid: Int)

}