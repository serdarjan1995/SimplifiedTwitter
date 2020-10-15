package models
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms.{list, longNumber, mapping, nonEmptyText, optional, text}
import play.api.data.validation.Constraints.pattern

import scala.util.matching.Regex

case class Tag(tag: String, slug: String)

case class Message (
  id : Option[String],
  username : Option[String],
  message : String,
  tags : List[Tag],
  creationDate: Option[DateTime],
  updateDate: Option[DateTime]
)

object Message {

  implicit object TagWrites extends OWrites[Tag] {
    def writes(tag: Tag): JsObject = Json.obj(
      "tag" -> tag.tag,
      "slug" -> tag.slug
    )
  }

  implicit object TagReads extends Reads[Tag] {
    def reads(json: JsValue): JsResult[Tag] = json match {
      case obj: JsObject => try {
        val tag = (obj \ "tag").as[String]
        val slug = (obj \ "slug").as[String]

        JsSuccess(Tag(tag, slug))

      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }

  implicit object MessageWrites extends OWrites[Message] {
    def writes(message: Message): JsObject = Json.obj(
      "_id" -> message.id,
      "username" -> message.username,
      "message" -> message.message,
      "tags" -> message.tags,
      "creationDate" -> message.creationDate.fold(-1L)(_.getMillis),
      "updateDate" -> message.updateDate.fold(-1L)(_.getMillis))
  }

  implicit object MessageReads extends Reads[Message] {
    def reads(json: JsValue): JsResult[Message] = json match {
      case obj: JsObject => try {
        val id = (obj \ "_id").asOpt[String]
        val username = (obj \ "username").asOpt[String]
        val message = (obj \ "message").as[String]
        val tags = (obj \ "tags").as[List[Tag]]
        val creationDate = (obj \ "creationDate").asOpt[Long]
        val updateDate = (obj \ "updateDate").asOpt[Long]
        JsSuccess(Message(id, username, message, tags,
          creationDate.map(new DateTime(_)),
          updateDate.map(new DateTime(_))))

      } catch {
        case cause: Throwable => JsError(cause.getMessage)
      }

      case _ => JsError("expected.jsobject")
    }
  }

  val form = Form(
    mapping(
      "id" -> optional(text verifying pattern(
        """[a-fA-F0-9]{24}""".r, error = "error.objectId")),
      "username" -> optional(nonEmptyText),
      "message" -> nonEmptyText,
      "tags" -> list(
        mapping(
          "tag" -> text,
          "slug" -> text
        )(Tag.apply)(Tag.unapply)
      ),
      "creationDate" -> optional(longNumber),
      "updateDate" -> optional(longNumber)
  ) {
      (id : Option[String], username : Option[String], message : String, tags : List[Tag], creationDate, updateDate) =>
        Message(
          id,
          username,
          message,
          tags,
          creationDate.map(new DateTime(_)),
          updateDate.map(new DateTime(_)))
    } { message =>
      Some(
        (message.id,
          message.username,
          message.message,
          message.tags,
          message.creationDate.map(_.getMillis),
          message.updateDate.map(_.getMillis)))
    })

  def parseTags(message: String): List[Tag] ={
    val hashtagPattern: Regex = "#\\w+".r
    var tags : List[Tag] = List()
    for (patternMatch <- hashtagPattern.findAllMatchIn(message)) {
      var tag : String = patternMatch.toString().replace("#","")
      var slug : String = tag.toLowerCase()
      tags = List.concat(tags,(Tag(tag, slug)::Nil))
    }
    tags
  }

}