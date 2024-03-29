package sample.sharding.kafka

import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.{ ActorRef, ActorSystem, Scheduler }
import org.apache.pekko.util.Timeout
import sample.sharding.kafka.UserEvents.{ Command, GetRunningTotal, RunningTotal }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future }

class UserGrpcService(system: ActorSystem[_], shardRegion: ActorRef[Command]) extends UserService {

  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val sched: Scheduler = system.scheduler
  implicit val ec: ExecutionContextExecutor = system.executionContext

  override def userStats(in: UserStatsRequest): Future[UserStatsResponse] = {
    shardRegion
      .ask[RunningTotal](replyTo => GetRunningTotal(in.id, replyTo))
      .map(runningTotal => UserStatsResponse(in.id, runningTotal.totalPurchases, runningTotal.amountSpent))
  }
}
