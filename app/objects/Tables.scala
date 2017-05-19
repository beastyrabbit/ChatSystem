package objects

import java.sql.Timestamp

import slick.jdbc.H2Profile.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape}

/**
  * Created by theer on 05.05.2017.
  */
class Tables {
  val userQuery = TableQuery[User]
  val chatQuery = TableQuery[Chat]
  val timelineentryQuery = TableQuery[Timelineentry]
  val historyQuery = TableQuery[History]
  val friendreQuestQuery = TableQuery[Friendrequest]
  val guiSettingsQuery = TableQuery[GuiSettings]


  class User(tag: Tag) extends Table[(Option[Int], String, String, String, String, String, Option[String], Option[Timestamp], Option[String])](tag, "USER") {
    def id = column[Int]("USERID", O.PrimaryKey, O.AutoInc)

    def username = column[String]("USERNAME")

    def password = column[String]("PASSWORD")

    def firstname = column[String]("FIRSTNAME")

    def lastname = column[String]("LASTNAME")

    def email = column[String]("EMAIL")

    def nickname = column[Option[String]]("NICKNAME")

    def lastlogin = column[Option[Timestamp]]("LASTLOGIN")

    def picture = column[Option[String]]("PICTURE")

    def * = (id.?, username, password, firstname, lastname, email, nickname, lastlogin, picture)
  }


  class Chat(tag: Tag) extends Table[(Option[Int], Int, String)](tag, "CHAT") {
    def chatid = column[Int]("CHATID", O.PrimaryKey, O.AutoInc)

    def userid = column[Int]("USERID")

    def name = column[String]("NAME")

    def User = foreignKey("FKChat316436", userid, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def * = (chatid.?, userid, name)
  }


  class Timelineentry(tag: Tag) extends Table[(Int, Int, Timestamp, String)](tag, "TIMELINEENTRY") {
    def timelineid = column[Int]("TIMELINEID", O.PrimaryKey, O.AutoInc)

    def userid = column[Int]("USERID")

    def deadtime = column[Timestamp]("DEADTIME")

    def message = column[String]("MESSAGE")

    def User = foreignKey("FKTimelineen399460", userid, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def * = (timelineid, userid, deadtime, message)
  }

  class History(tag: Tag) extends Table[(Int, Int, Int, Timestamp)](tag, "HISTORY") {
    def messageid = column[Int]("MESSAGEID", O.PrimaryKey, O.AutoInc)

    def chatid = column[Int]("CHATID")

    def userid = column[Int]("USERID")

    def messagetime = column[Timestamp]("MESSAGETIME")

    def User = foreignKey("FKHistory174533", userid, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def Chat = foreignKey("FKHistory574831", chatid, chatQuery)(_.chatid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def * = (messageid, chatid, userid, messagetime)
  }

  class Friendrequest(tag: Tag) extends Table[(Int, Int)](tag, "FRIENDREQUEST") {
    def requestfrom = column[Int]("REQUESTFROM")

    def requestto = column[Int]("REQUESTTO")

    def Userfrom = foreignKey("FKFriendrequ359114", requestfrom, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def Userto = foreignKey("FKFriendrequ51650", requestto, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def * = (requestfrom, requestto)
  }

  class Friends(tag: Tag) extends Table[(Int, Int)](tag, "FRIENDS") {
    def userid = column[Int]("userid")

    def friendid = column[Int]("friendid")

    def Userfrom = foreignKey("FKFriends855472", userid, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def Userto = foreignKey("FKFriends880936", friendid, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def * = (userid, friendid)
  }


  class GuiSettings(tag: Tag) extends Table[(Int, Boolean, Boolean, Int)](tag, "GUISETTINGS") {
    def userid = column[Int]("USERID")

    def optionmenu = column[Boolean]("OPTIONMENU", O.Default(true))

    def timeline = column[Boolean]("TIMELINE", O.Default(true))

    def lastchat = column[Int]("LASTCHAT")

    def User = foreignKey("FKGuiSetting68533", userid, userQuery)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def Chat = foreignKey("FKGuiSetting816374", lastchat, chatQuery)(_.chatid, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)


    def * = (userid, optionmenu, timeline, lastchat)
  }


}