package com.lancat

import akka.actor.{Actor, ActorLogging, Props, ActorSelection}
import Receiver._

class Receiver(sender: ActorSelection) extends Actor with ActorLogging {
  import Receiver._

  var text: String = ""

  sender ! Sender.Text("RECEIVER SAYS HI")

  def receive = {
    case Ping => log.info(s"reciever got ping")
    case Text(text) => log.info(s"reciever got text $text")
  }
}

object Receiver {
  def props(sender: ActorSelection): Props = Props(new Receiver(sender))
  case class Text(text: String)
  case object Ping
}
