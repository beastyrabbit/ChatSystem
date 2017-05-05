package objects

/**
  * Created by theer on 02.05.2017.
  */
case class UserRecord(
                       username: String = "",
                       password: String = "",
                       userid: Int = -1,
                       firstname: String = "",
                       lastname: String = "",
                       email: String = "",
                       nickname: String = "",
                       lastlogin: Long = -1,
                       picture: String = ""
                     )
