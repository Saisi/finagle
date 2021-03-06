package com.twitter.finagle

import com.twitter.finagle
import com.twitter.finagle.client._
import com.twitter.finagle.dispatch.{GenSerialClientDispatcher, PipeliningDispatcher}
import com.twitter.finagle.netty3.Netty3Transporter
import com.twitter.finagle.param.WithDefaultLoadBalancer
import com.twitter.finagle.pool.SingletonPool
import com.twitter.finagle.redis.protocol.{Command, Reply}
import com.twitter.finagle.transport.Transport

trait RedisRichClient { self: Client[Command, Reply] =>

  def newRichClient(dest: String): redis.Client =
     redis.Client(newService(dest))

  def newRichClient(dest: Name, label: String): redis.Client =
    redis.Client(newService(dest, label))
}

object Redis extends Client[Command, Reply] {

  object Client {
    /**
     * Default stack parameters used for redis client.
     */
    val defaultParams: Stack.Params = StackClient.defaultParams +
      param.ProtocolLibrary("redis")

    /**
     * A default client stack which supports the pipelined redis client.
     */
    def newStack: Stack[ServiceFactory[Command, Reply]] = StackClient.newStack
      .replace(DefaultPool.Role, SingletonPool.module[Command, Reply])
  }

  case class Client(
      stack: Stack[ServiceFactory[Command, Reply]] = Client.newStack,
      params: Stack.Params = Client.defaultParams)
    extends StdStackClient[Command, Reply, Client]
    with WithDefaultLoadBalancer[Client]
    with RedisRichClient {

    override def configured[P](psp: (P, Stack.Param[P])): Client = {
      val (p, sp) = psp
      configured(p)(sp)
    }

    protected def copy1(
      stack: Stack[ServiceFactory[Command, Reply]] = this.stack,
      params: Stack.Params = this.params
    ): Client = copy(stack, params)

    protected type In = Command
    protected type Out = Reply

    protected def newTransporter(): Transporter[In, Out] =
      Netty3Transporter(redis.RedisClientPipelineFactory, params)

    protected def newDispatcher(transport: Transport[In, Out]): Service[Command, Reply] =
      new PipeliningDispatcher(
        transport,
        params[finagle.param.Stats].statsReceiver.scope(GenSerialClientDispatcher.StatsScope)
      )
  }

  val client: Redis.Client = Client()

  def newClient(dest: Name, label: String): ServiceFactory[Command, Reply] =
    client.newClient(dest, label)

  def newService(dest: Name, label: String): Service[Command, Reply] =
    client.newService(dest, label)
}