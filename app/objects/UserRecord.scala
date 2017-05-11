package objects

import java.sql.Timestamp

/**
  * Created by theer on 02.05.2017.
  */
case class UserRecord(
                       userid: Int = -1,
                       username: String = "",
                       password: String = "",
                       firstname: String = "",
                       lastname: String = "",
                       email: String = "",
                       nickname: Option[String] = Option(""),
                       lastlogin: Option[Timestamp] = Option(new Timestamp(0)),
                       picture: Option[String] = Option("")
                     )
