package controllers

import javax.inject._
import play.api.mvc._
import org.joda.time.DateTime

import scala.concurrent.Future
import play.api.libs.json._
import reactivemongo.api.bson.{BSONDocument, BSONObjectID, BSONValue}
import reactivemongo.play.json._
import compat._
import json2bson._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.Cursor
import models.Message
import Message._
import java.util.UUID

/**
 * This controller creates an `Action` to handle HTTP requests to work
 * with Message entities.
 */
@Singleton
class MessageController @Inject()(
                                     cc: ControllerComponents,
                                     val reactiveMongoApi: ReactiveMongoApi
                                 ) extends AbstractController(cc)
    with MongoController with ReactiveMongoComponents {

    implicit def ec = cc.executionContext

    // map the collection 'Messages'
    def collection = reactiveMongoApi.database.map(_.collection("Messages"))


    /**
     * Create Message
     */
    def create  = Action.async { implicit request =>
        val username = request.headers.get("x-username").orNull
        if (username == null || username.length == 0) {
            Future.successful(BadRequest(Json.obj(
                "status" -> "fail",
                "errors" -> true,
                "message"-> "X-Username header is null")))
        }
        else {
            Message.form.bindFromRequest.fold(
                formWithErrors => {
                    Future.successful(BadRequest(Json.obj(
                        "status" -> "fail",
                        "errors" -> true
                    )))
                },

                // if no error, then insert the message into the collection
                message => {
                    val uuid = UUID.randomUUID().toString
                    val dbEntry = message.copy(
                        id = message.id.orElse(Some(uuid)),
                        username = Option(username),
                        tags = Message.parseTags(message.message),
                        creationDate = Some(new DateTime()),
                        updateDate = Some(new DateTime()),
                    )

                    collection.flatMap(_.insert.one(dbEntry))
                      .map(_ => Created(Json.toJsObject(dbEntry)))
                }
            )
        }
    }

    /**
     * Read Message
     * TODO
     */
    def read(id: String) = Action.async { implicit request =>
        //Ok("Not implemented yet. Requested id "+id)
        collection.flatMap(_.find(BSONDocument("_id" -> id)).one[Message])
          .map(message => Ok(Json.toJsObject(message)))
    }

    /**
     * Update
     * TODO
     */
    def update = Action {
        Ok("Not implemented yet")
    }

    /**
     * Delete Message
     * TODO
     */
    def delete(id: String) = Action.async { implicit request =>
        val username = request.headers.get("x-username").orNull
        if (username == null || username.length == 0) {
            Future.successful(BadRequest(Json.obj(
                "status" -> "fail",
                "errors" -> true,
                "message"-> "X-Username header is null")))
        }
        else{
            collection.flatMap(_.delete.one(BSONDocument("_id" -> id,"username" -> username)))
              .map(_ => NoContent)
        }


    }

    /**
     * Find Messages
     * TODO
     */
    def find = Action { implicit request =>

        val tagQuery : String = request.queryString.get("tag")
            .flatMap(_.headOption).getOrElse("none")
        val countQuery : Int = request.queryString.get("count")
          .flatMap(_.headOption).getOrElse(10).asInstanceOf[Int]
        val pageQuery : Int = request.queryString.get("page")
          .flatMap(_.headOption).getOrElse("none").asInstanceOf[Int]


        val query = BSONDocument("tags.tag" -> tagQuery)

        val result = collection.map(
            _.find(query).batchSize(countQuery).cursor[Message]()
              .collect[List](countQuery, Cursor.FailOnError[List[Message]]())
        )


        Ok(Json.obj("messages" -> "result"))

    }

}
