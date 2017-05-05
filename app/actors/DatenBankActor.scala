package actors

/**
  * Created by theer on 02.05.2017.
  */

import java.sql.Timestamp
import javax.inject.Inject
import scala.collection.breakOut
import akka.actor._
import objects.UserRecord
import slick.jdbc.H2Profile.api._
import objects._
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect
import slick.jdbc.JdbcProfile
import slick.sql.FixedSqlStreamingAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class DatenBankActor extends Actor {

  import DatenBankActor._

  def receive = {
    case fillUserData(user: UserRecord, sendto: ActorRef) =>
      getUserData(user)
    case TEMPPPER2() =>
      println("TEMPPPER")
  }

  def fillUserRecord(x: Seq[(Option[Int], String, String, String, String, String, String, Timestamp, String)]): UserRecord = {
    val names = Seq("userid", "username", "password", "firstname", "lastname", "email", "nickname", "lastlogin", "piture")
    val userlist = names zip x toMap

    new UserRecord(userid = userlist.get("userid"))

  }


  def getUserData(user: UserRecord): UserRecord = {
    val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
    val db = dbConfig.db
    val time = new Timestamp(300l)
    val userDB = TableQuery[User]
    val userquery = userDB.filter(_.username === user.username)
    val readoutuserFuture = db.run(userquery.result)
    readoutuserFuture onComplete {
      case Success(x) => fillUserRecord(x)
      case Failure(ex) => println(ex)

    }

    return new UserRecord()
  }
}


object DatenBankActor {
  def props(): Props = Props(new DatenBankActor())

  case class fillUserData(user: UserRecord, sendto: ActorRef)

  case class TEMPPPER2()

}    