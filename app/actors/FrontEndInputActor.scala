package actors

/**
  * Created by theer on 17.05.2017.
  */

import akka.actor._
import play.api.libs.json.{JsString, JsValue}


class FrontEndInputActor extends Actor {

  import FrontEndInputActor._

  def messagepros(msg: JsValue) = {
    ???
  }

  def checkType(msg: JsValue): Unit = {
    val msgType = (msg \ "type").get
    msgType match {
      case JsString("message") => messagepros(msg)
      case JsString("") => ???
      case _ => println("Das kenn ich nciht " + msg)
    }
  }

  def receive = {
    case getMessage(msg: JsValue) =>
      checkType(msg)
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
  def props(): Props = Props(new FrontEndInputActor())

  case class getMessage(msg: JsValue)

  case class TEMPPPER2()

  case class TEMPPPER3()

  case class TEMPPPER4()

  case class TEMPPPER5()

  case class TEMPPPER6()

}    