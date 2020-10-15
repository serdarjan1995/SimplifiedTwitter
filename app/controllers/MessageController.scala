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
     */
    def read(id: String) = Action { implicit request =>
        Ok("Not implemented yet. Requested id "+id)

    }

    /**
     * Update Message
     */
    def update = Action {
        Ok("Not implemented yet")
    }

    /**
     * Delete Message
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
            Future.successful(Ok(Json.obj(
                "status" -> "success",
                "errors" -> true,
                "message"-> "Not implemented yet")))
        }


    }

    /**
     * Find Messages
     */
    def find = Action { implicit request =>

        val tagQuery = request.queryString.get("tag")
            .flatMap(_.headOption).getOrElse("none")
        val countQuery = request.queryString.get("count")
          .flatMap(_.headOption).getOrElse("none")
        val pageQuery = request.queryString.get("page")
          .flatMap(_.headOption).getOrElse("none")
        Ok(tagQuery+countQuery+pageQuery)
    }

}
