package actors

import java.sql.Timestamp

import akka.actor.ActorRef
import akka.event.{ActorEventBus, LookupClassification}
import objects.UserRecord

/**
  * Created by theer on 23.05.2017.
  */

class SubscribeChat extends ActorEventBus with LookupClassification {
  type Event = ChatMessage
  type Classifier = String

  override protected def mapSize(): Int = 10

  override protected def classify(event: Event): Classifier = event.channel

  override protected def publish(event: ChatMessage, subscriber: Subscriber): Unit = {
    println("Actor: " + subscriber + "publish: " + event)
    subscriber ! event
  }
}

case class ChatMessage(channel: String, message: Message, user: UserRecord)

case class Message(userid: String, timestamp: Timestamp, text: String)