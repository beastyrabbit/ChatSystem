package actors

import java.sql.Timestamp

import akka.event.{ActorEventBus, LookupClassification}
import objects.{DBMessage, UserRecord}
import org.joda.time.DateTime
import play.api.libs.json.Json.{fromJson, toJson}
import play.api.libs.json.{Format, JsResult, JsValue, Json}

/**
  * Created by theer on 23.05.2017.
  * This Methode is Handling the Sending of Message to all Users connected to a Chat
  */

class SubscribeChat extends ActorEventBus with LookupClassification {
  type Event = ChatMessage
  type Classifier = String

  implicit val timestampFormat = new Format[Timestamp] {

    def writes(t: Timestamp): JsValue = toJson(timestampToDateTime(t))

    def reads(json: JsValue): JsResult[Timestamp] = fromJson[DateTime](json).map(dateTimeToTimestamp)

  }

  implicit val formatMessage = Json.format[DBMessage]
  implicit val formatChat = Json.format[ChatMessage]

  def timestampToDateTime(t: Timestamp): DateTime = {
    new DateTime(t.getTime)
  }

  def dateTimeToTimestamp(dt: DateTime): Timestamp = {
    new Timestamp(dt.getMillis)
  }

  override protected def mapSize(): Int = 10

  override protected def classify(event: Event): Classifier = event.chatid

  override protected def publish(event: ChatMessage, subscriber: Subscriber): Unit = {
    val json = Json.toJson(event)
    subscriber ! json
  }
}

case class ChatMessage(chatid: String, message: DBMessage, user: UserRecord, msgType: String = "UpdateMessage")
