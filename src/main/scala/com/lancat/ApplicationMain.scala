package com.lancat

import akka.actor.ActorSystem

import javax.jmdns._
import java.util.{Hashtable => jHT}
import java.net.InetAddress
import util.Random
import com.lancat.Receiver._
import com.lancat.Sender._
import akka.actor.{Actor, ActorSystem, ActorLogging, Props, ActorRef, ExtendedActorSystem, ExtensionKey, Extension, Address }
import com.typesafe.config.{Config, ConfigFactory}


object ApplicationMain extends App with mDNS {

  def remoteConfig(hostname: String, port: Int, commonConfig: Config): Config = {
    val configStr =
      "akka.actor.provider = akka.remote.RemoteActorRefProvider\n" +
        "akka.remote.netty.tcp.hostname = " + hostname + "\n" +
        "akka.remote.netty.tcp.port = " + port + "\n"

    ConfigFactory.parseString(configStr).withFallback(commonConfig)
  }

  var role: String = "sender"
  if (args.length > 0) {
    println(s"$args")
    role = args(0)
  }

  def getFakePort(): Int = {
    30000 + Random.nextInt(10000)
  }

  var myService = ""
  if (role == "sender") {
    myService = "sender"
  } else {
    myService = "receiver" + System.currentTimeMillis
  }
  val fakePort: Int = getFakePort
  val akkaPort: Int = getFakePort

  start
  register(
    myService,
    fakePort,
    new jHT[String, String] { put("akka_port", s"$akkaPort") })

  Runtime.getRuntime.addShutdownHook(new Thread {
                                       override def run = stop
                                     }
  )

  val appConfig = ConfigFactory.load

  if (role == "sender") {
    println("create sender")
    val senderSystem = ActorSystem("senderSystem", remoteConfig("192.168.0.107", akkaPort, appConfig))
    val sender = senderSystem.actorOf(Props[Sender], "sender")
  } else {
    println("create receiver")
    val receiverSystem = ActorSystem("receiverSystem", remoteConfig("192.168.0.107", akkaPort, appConfig))
    val senderPort = getSenderPort
    val sender = receiverSystem.actorSelection(s"akka.tcp://senderSystem@192.168.0.107:$senderPort/user/sender")
    val receiver = receiverSystem.actorOf(Receiver.props(sender), "receiver")
  }
}

trait mDNS
{
  val mdnsType: String = "_lancat"
  private var jmdns: JmDNS = _

  def getSenderPort(): String = {
    val service = jmdns.list("sender._lancat._tcp.local.")(0)
    val akkaPort = service.getPropertyString("akka_port")
    val addr = service.getAddress
    val port = service.getPort
    println(s"found sender actor: $addr:$akkaPort")
    akkaPort
  }

  def start() {
    val addr = InetAddress.getLocalHost
    jmdns = JmDNS.create(addr)
    println(s"jmdns started on $addr")

    // add listeners for jmDNS events
    // XXX: Change to service type listener
    jmdns.addServiceTypeListener(new ServiceTypeListener
    {
      override def serviceTypeAdded(ev: ServiceEvent) = Option(ev.getType) match {
        case Some(typ) =>
          if (typ.contains(mdnsType))
          {
            jmdns.addServiceListener(typ, new ServiceListener
            {
              override def serviceAdded(ev: ServiceEvent) {
                println(s"request resolution for serviceAdded ${ev.getInfo}")
                jmdns.requestServiceInfo(typ, ev.getName) //force resolution
              }
              override def serviceResolved(ev: ServiceEvent) = discovered(ev.getInfo)
              override def serviceRemoved(ev: ServiceEvent) = removed(ev.getInfo)
            })
          }
          else println("ignore foreign type " + typ)
        case _ => //ignore
      }
      override def subTypeForServiceTypeAdded(ev: ServiceEvent) = Unit
    })
    println("type listener added")
  }

  def discovered(info: ServiceInfo) = {
    val addr = info.getAddress
    val port = info.getPort
  }
  def removed(info: ServiceInfo) = println(s"service removed $info")

  def register(service: String, port: Int, props: jHT[String, String])
  {
    val myInfo = ServiceInfo.create(
      mdnsType,
      service,
      port,
      10, //weight
      1,  //priority
      props)
    jmdns.registerService(myInfo)
    println("registered " + service)
  }

  def printServices() = {
    val services = jmdns.list("_lancat._tcp.local.")
    for (service <- services) {
      val addr = service.getAddress
      val port = service.getPort
      println(s"print service: $addr:$port")
    }
  }

  def unregister(service: String, port: Int, props: jHT[String, String]) = {
    val info = ServiceInfo.create(
      mdnsType,
      service,
      port,
      10,
      1,
      props)
    jmdns.unregisterService(info)
  }

  def unregisterAllServices() = jmdns.unregisterAllServices()

  def stop() = {
    unregisterAllServices()
    jmdns.close
  }
}
