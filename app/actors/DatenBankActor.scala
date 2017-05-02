package actors

/**
  * Created by theer on 02.05.2017.
  */

import akka.actor._


class DatenBankActor extends Actor {

  import DatenBankActor._

  def receive = {
    case TEMPPPER1() =>
      println("TEMPPPER")
    case TEMPPPER2() =>
      println("TEMPPPER")
  }

}

object DatenBankActor {
  def props(): Props = Props(new DatenBankActor())

  case class TEMPPPER1()

  case class TEMPPPER2()

}    