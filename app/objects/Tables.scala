package objects

import java.sql.Timestamp

import slick.jdbc.H2Profile.api._

/**
  * Created by theer on 05.05.2017.
  * This Class is needed for Slick to work with the DB
  */
class Tables {
  val userQuery = TableQuery[User]
  val chatQuery = TableQuery[Chat]
  val timelineentryQuery = TableQuery[Timelineentry]
  val historyQuery = TableQuery[History]
  val friendreQuestQuery = TableQuery[Friendrequest]
  val guiSettingsQuery = TableQuery[GuiSettings]
  val userToChatQuery = TableQuery[UsertoChat]


  class User(tag: Tag) extends Table[UserRecord](tag, "USER") {
    def * = {
      (userid.?, username, password, firstname, lastname, email, nickname, lastlogin, picture) <> ((UserRecord.apply _).tupled, UserRecord.unapply)
    }

    def userid = column[Int]("USERID", O.PrimaryKey, O.AutoInc)

    def username = column[String]("USERNAME")

    def password = column[String]("PASSWORD")

    def firstname = column[String]("FIRSTNAME")

    def lastname = column[String]("LASTNAME")

    def email = column[String]("EMAIL")

    def nickname = column[Option[String]]("NICKNAME")

    def lastlogin = column[Option[Timestamp]]("LASTLOGIN")

    def picture = column[Option[String]]("PICTURE")
  }


  class Chat(tag: Tag) extends Table[DBChat](tag, "CHAT") {
    def * = {
      (chatid.?, name) <> ((DBChat.apply _).tupled, DBChat.unapply)
    }

    def chatid = column[Int]("CHATID", O.PrimaryKey, O.AutoInc)

    def name = column[String]("CHATNAME")
  }


  class Timelineentry(tag: Tag) extends Table[(Option[Int], Int, Timestamp, String)](tag, "TIMELINEENTRY") {
    def User = {
      foreignKey("FKTimelineen399460", userid, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def * = {
      (timelineid.?, userid, deadtime, message)
    }

    def userid = {
      column[Int]("USERID")
    }

    def timelineid = {
      column[Int]("TIMELINEID", O.PrimaryKey, O.AutoInc)
    }

    def deadtime = column[Timestamp]("DEADTIME")

    def message = column[String]("MESSAGE")
  }


  class History(tag: Tag) extends Table[DBMessage](tag, "HISTORY") {
    def User = {
      foreignKey("FKHistory174533", userid, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def Chat = {
      foreignKey("FKHistory574831", chatid, chatQuery)(_.chatid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def * = {
      (messageid.?, messagetext, chatid, userid, messagetime) <> ((DBMessage.apply _).tupled, DBMessage.unapply)
    }

    def messageid = column[Int]("MESSAGEID", O.PrimaryKey, O.AutoInc)

    def messagetext = column[String]("MESSAGETEXT")

    def chatid = column[Int]("CHATID")

    def userid = column[Int]("USERID")

    def messagetime = column[Timestamp]("MESSAGETIME")
  }

  class Friendrequest(tag: Tag) extends Table[(Int, Int)](tag, "FRIENDREQUEST") {
    def Userfrom = {
      foreignKey("FKFriendrequ359114", requestfrom, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def requestfrom = {
      column[Int]("REQUESTFROM")
    }

    def Userto = {
      foreignKey("FKFriendrequ51650", requestto, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def * = {
      (requestfrom, requestto)
    }

    def requestto = {
      column[Int]("REQUESTTO")
    }
  }

  class UsertoChat(tag: Tag) extends Table[(Int, Int)](tag, "USERTOCHAT") {
    def UserFK = {
      foreignKey("FKUsertoChat816378", userid, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def userid = {
      column[Int]("USERID")
    }

    def ChatFK = {
      foreignKey("FKUsertoChat816374", chatid, chatQuery)(_.chatid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def * = {
      (chatid, userid)
    }

    def chatid = {
      column[Int]("CHATID")
    }
  }

  class Friends(tag: Tag) extends Table[(Int, Int)](tag, "FRIENDS") {
    def Userfrom = foreignKey("FKFriends855472", userid, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def Userto = {
      foreignKey("FKFriends880936", friendid, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def * = {
      (userid, friendid)
    }

    def friendid = {
      column[Int]("friendid")
    }

    def userid = {
      column[Int]("userid")
    }
  }


  class GuiSettings(tag: Tag) extends Table[(Int, Boolean, Boolean, Int)](tag, "GUISETTINGS") {
    def User = {
      foreignKey("FKGuiSetting68533", userid, userQuery)(_.userid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def Chat = {
      foreignKey("FKGuiSetting816374", lastchat, chatQuery)(_.chatid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
    }

    def lastchat = {
      column[Int]("LASTCHAT")
    }

    def * = {
      (userid, optionmenu, timeline, lastchat)
    }

    def userid = column[Int]("USERID")

    def optionmenu = column[Boolean]("OPTIONMENU", O.Default(true))

    def timeline = column[Boolean]("TIMELINE", O.Default(true))
  }


}

case class DBMessage(messageid: Option[Int], messagetext: String, chatid: Int, userid: Int, messagetime: Timestamp)

case class DBChat(chatid: Option[Int], name: String)