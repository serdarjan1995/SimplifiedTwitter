package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * HealthChecker
   * Returns simple "ok" status in order to tell the system is running
   */
  def healthCheck = Action {
    Ok(Json.obj("status" -> "ok"))
  }


}
