package objects

/**
  * Created by theer on 02.05.2017.
  */
class User(userName: String, password: String) {
  val id = java.util.UUID.randomUUID.toString
}
