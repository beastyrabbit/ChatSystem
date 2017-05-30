package actors

/**
  * Created by theer on 17.05.2017.
  */

import akka.actor._
import objects.UserRecord
import play.api.libs.json.{JsString, JsValue, Json}
import java.sql


class FrontEndInputActor(system: AKKASystem) extends Actor {

  import FrontEndInputActor._

  def messagepros(msg: JsValue, userRecord: UserRecord) = {
    val chatMessage = {
      new ChatMessage(user = userRecord, channel = "ChatRoom" + (msg \ "chatid").as[String], message = new Message(userid = userRecord.userid.toString, timestamp = new java.sql.Timestamp((msg \ "timestamp").get.as[Long]), text = (msg \ "text").get.as[String]))
    }
    system.subscribeChat.publish(chatMessage)
  }

  def checkType(msg: JsValue, userRecord: UserRecord): Unit = {
    val msgType = (msg \ "type").get
    msgType match {
      case JsString("message") => messagepros(msg, userRecord)
      case JsString("") => ???
      case _ => println("Das kenn ich nicht " + msg)
    }
  }

  def receive = {
    case newMessage(msg: JsValue, userRecord: UserRecord) =>
      checkType(msg, userRecord)
    case TEMPPPER2() =>
      println("TEMPPPER")
    case TEMPPPER3() =>
      println("TEMPPPER")
    case TEMPPPER4() =>
      println("TEMPPPER")
    case TEMPPPER5() =>
      println("TEMPPPER")
    case TEMPPPER6() =>
      println("TEMPPPER")
  }

}


object FrontEndInputActor {
  def props(system: AKKASystem): Props = Props(new FrontEndInputActor(system: AKKASystem))

  case class newMessage(msg: JsValue, userRecord: UserRecord)

  case class TEMPPPER2()

  case class TEMPPPER3()

  case class TEMPPPER4()

  case class TEMPPPER5()

  case class TEMPPPER6()

}    