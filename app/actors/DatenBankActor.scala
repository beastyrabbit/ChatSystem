package actors

/**
  * Created by theer on 02.05.2017.
  */

import java.sql.Timestamp
import javax.inject.Inject

import akka.actor._
import objects.UserRecord
import slick.jdbc.H2Profile.api._
import objects._
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global

class DatenBankActor extends Actor {

  import DatenBankActor._

  def receive = {
    case fillUserData(user: UserRecord, sendto: ActorRef) =>
      fillUserData(user)
    case TEMPPPER2() =>
      println("TEMPPPER")
  }

  def fillUserData(user: UserRecord): UserRecord = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val time = new Timestamp(300l)
    try {
      val user = TableQuery[User]
      val setup = DBIO.seq(
        user += (3, "", "InCode", "", "", "", "", time, "")
      )
      val setupFuture = db.run(setup)
    } finally db.close

    return new UserRecord()
  }


}

object DatenBankActor {
  def props(): Props = Props(new DatenBankActor())

  case class fillUserData(user: UserRecord, sendto: ActorRef)

  case class TEMPPPER2()

}    