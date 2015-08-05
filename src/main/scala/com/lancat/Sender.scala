package com.lancat

import akka.actor.{Actor, ActorLogging, Props}
import Receiver._

class Sender extends Actor with ActorLogging {
  import Sender._
  var counter: Int = 0
  var text: String = ""

  def receive = {
    case Ping => {
      log.info("got ping")
      sender ! Receiver.Text("got ping, SENDER SAYS HI")
    }
    case Text(text) => {
      log.info(s"got text $text")
      sender ! Receiver.Text("SENDER SAYS HI")
    }
  }
}

object Sender {
  val props = Props[Sender]
  case class Text(text: String)
  case object Ping
}
