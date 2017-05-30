package actors

import java.sql.Timestamp

import akka.actor.ActorRef
import akka.event.{ActorEventBus, LookupClassification}
import objects.UserRecord
import org.joda.time.DateTime
import play.api.libs.json.{Format, JsResult, JsValue, Json}
import play.api.libs.json.Json.{fromJson, toJson}

/**
  * Created by theer on 23.05.2017.
  */

class SubscribeChat extends ActorEventBus with LookupClassification {

  def timestampToDateTime(t: Timestamp): DateTime = new DateTime(t.getTime)

  def dateTimeToTimestamp(dt: DateTime): Timestamp = new Timestamp(dt.getMillis)

  implicit val timestampFormat = new Format[Timestamp] {

    def writes(t: Timestamp): JsValue = toJson(timestampToDateTime(t))

    def reads(json: JsValue): JsResult[Timestamp] = fromJson[DateTime](json).map(dateTimeToTimestamp)

  }

  implicit val formatMessage = Json.format[Message]
  implicit val formatChat = Json.format[ChatMessage]

  type Event = ChatMessage
  type Classifier = String

  override protected def mapSize(): Int = 10

  override protected def classify(event: Event): Classifier = event.chatid

  override protected def publish(event: ChatMessage, subscriber: Subscriber): Unit = {
    val json = Json.toJson(event)
    subscriber ! json
  }
}

case class ChatMessage(chatid: String, message: Message, user: UserRecord, msgType: String = "UpdateMessage")

case class Message(userid: String, timestamp: Timestamp, text: String)
