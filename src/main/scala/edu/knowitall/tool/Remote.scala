package edu.knowitall.tool

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

import dispatch.Http
import dispatch.as
import dispatch.url

trait Remote {
  def urlString: String
  def timeout = 5.minutes

  val svc = url(urlString)

  def post(string: String) =
    Await.result(Http(svc << string OK as.String), timeout)
}
