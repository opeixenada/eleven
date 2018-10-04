package solutions.imperative.actors

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern._
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import models.Directions.{Down, Up}
import models.{Elevator, _}
import org.scalatest._
import solutions.imperative.messages.{SystemStatusRequest, SystemStatusResponse}

import scala.concurrent.Await
import scala.concurrent.duration._

class SystemActorTest(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("SystemActorTest"))

  override def afterAll() {
    system.terminate()
  }

  private def systemActor(x: Int, y: Int) =
    system.actorOf(Props(new SystemActor(x, y)))

  private def doSteps(actor: ActorRef, n: Int) = {
    for (i <- 0 until n) {
      actor ! Step
    }
  }

  implicit val timeout = Timeout(1.seconds)

  "System actor" should {
    "send `SystemStatusResponse` on `SystemStatusRequest`" in {
      val elevatorSystem = systemActor(2, 10)
      elevatorSystem ! SystemStatusRequest
      expectMsg(SystemStatusResponse(Seq(Elevator(1), Elevator(2)), Seq.empty))
    }

    "accept `FloorRequest`" in {
      val actor = system.actorOf(Props(new SystemActor(1, 10)))

      actor ! FloorRequest(1, 1)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      ssr.elevatorStates.count(_.goals == Seq(1)) should be(1)
      ssr.queue should be(Seq.empty)
    }

    /**
      * PickupRequest(1, Up)
      *
      * Target state:
      * elevator1: PickupRequest(1, Up)
      * elevator2: -
      * queue: -
      */
    "select 1st available elevator" in {
      val actor = system.actorOf(Props(new SystemActor(2, 10)))

      actor ! PickupRequest(1, Up)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      ssr.elevatorStates.count(_.goals == Seq(1)) should be(1)
      ssr.queue should be(Seq.empty)
    }

    /**
      * PickupRequest(1, Up)
      * PickupRequest(3, Up)
      *
      * Target state:
      * elevator1: PickupRequest(1, Up)
      * elevator2: PickupRequest(3, Up)
      * queue: -
      */
    "select available elevator with shortest trajectory" in {
      val actor = system.actorOf(Props(new SystemActor(2, 10)))

      actor ! PickupRequest(1, Up)
      actor ! PickupRequest(3, Up)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      ssr.elevatorStates.count(_.goals == Seq(1)) should be(1)
      ssr.elevatorStates.count(_.goals == Seq(3)) should be(1)
      ssr.queue should be(Seq.empty)
    }

    /**
      * PickupRequest(1, Up)
      * PickupRequest(3, Up)
      * PickupRequest(4, Down)
      *
      * Target state:
      * elevator1: PickupRequest(1, Up)
      * elevator2: PickupRequest(3, Up)
      * queue: PickupRequest(4, Down)
      */
    "put a request in a queue if there're no available elevators" in {
      val actor = system.actorOf(Props(new SystemActor(2, 10)))

      actor ! PickupRequest(1, Up)
      actor ! PickupRequest(3, Up)
      actor ! PickupRequest(4, Down)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      ssr.elevatorStates.count(_.goals == Seq(1)) should be(1)
      ssr.elevatorStates.count(_.goals == Seq(3)) should be(1)
      ssr.queue should be(Seq(PickupRequest(4, Down)))
    }

    /**
      * PickupRequest(1, Up)
      * PickupRequest(3, Up)
      * PickupRequest(4, Down)
      * 2x Step
      *
      * Target state:
      * 1. After requests:
      * elevator1 (floor 0): PickupRequest(1, Up)
      * elevator2 (floor 1): PickupRequest(3, Up)
      * queue: PickupRequest(4, Down)
      *
      * 2. After 1st Step:
      * elevator1 (floor 1): -
      * elevator2 (floor 1): PickupRequest(3, Up)
      * queue: PickupRequest(4, Down)
      *
      * 3. After 2nd Step:
      * elevator1 (floor 1): PickupRequest(4, Down)
      * elevator2 (floor 2): PickupRequest(3, Up)
      * queue: -
      */
    "assign request from the queue to the elevator that just became available" in {
      val actor = system.actorOf(Props(new SystemActor(2, 10)))

      actor ! PickupRequest(1, Up)
      actor ! PickupRequest(3, Up)
      actor ! PickupRequest(4, Down)
      doSteps(actor, 2)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      ssr.elevatorStates.count(_.goals == Seq(4)) should be(1)
      ssr.elevatorStates.count(_.goals == Seq(3)) should be(1)
      ssr.queue should be(Seq.empty)
    }

    /**
      * PickupRequest(1, Up)
      * PickupRequest(3, Up)
      * PickupRequest(4, Down)
      * PickupRequest(5, Up)
      *
      * Target state:
      * elevator1: PickupRequest(1, Up)
      * elevator2: PickupRequest(3, Up)
      * queue: PickupRequest(4, Down); PickupRequest(5, Up)
      */
    "assign request from the queue even if the 1st request of the queue is not assigned" in {
      val actor = system.actorOf(Props(new SystemActor(2, 10)))

      actor ! PickupRequest(1, Up)
      actor ! PickupRequest(3, Up)
      actor ! PickupRequest(4, Down)
      actor ! PickupRequest(5, Up)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      ssr.elevatorStates.count(_.goals == Seq(1, 5)) should be(1)
      ssr.elevatorStates.count(_.goals == Seq(3)) should be(1)
      ssr.queue should be(Seq(PickupRequest(4, Down)))
    }

    /**
      * PickupRequest(1, Up)
      * PickupRequest(3, Up)
      * PickupRequest(5, Up)
      *
      * Target state:
      * elevator1: PickupRequest(1, Up); PickupRequest(5, Up)
      * elevator2: PickupRequest(3, Up)
      * queue: -
      */
    "chain requests if possible" in {
      val actor = system.actorOf(Props(new SystemActor(2, 10)))

      actor ! PickupRequest(1, Up)
      actor ! PickupRequest(3, Up)
      actor ! PickupRequest(5, Up)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      ssr.elevatorStates.count(_.goals == Seq(1, 5)) should be(1)
      ssr.elevatorStates.count(_.goals == Seq(3)) should be(1)
      ssr.queue should be(Seq.empty)
    }

    /**
      * Initial state:
      * elevator 1: floor 1
      * elevator 2: floor 3
      * elevator 3: floor 5
      * elevator 4: floor 7
      * elevator 5: floor 9
      *
      * PickupRequest(6, Up)
      *
      * Target state:
      * Elevator 3 or 4 gets the request.
      */
    "assign request to the closest elevator" in {
      val actor = system.actorOf(Props(new SystemActor(5, 10)))

      actor ! PickupRequest(1, Up)
      actor ! PickupRequest(3, Up)
      actor ! PickupRequest(5, Up)
      actor ! PickupRequest(7, Up)
      actor ! PickupRequest(9, Up)
      Thread.sleep(100)

      doSteps(actor, 10)
      Thread.sleep(100)

      actor ! PickupRequest(6, Down)
      Thread.sleep(100)

      val ssr = Await
        .result(actor ? SystemStatusRequest, timeout.duration)
        .asInstanceOf[SystemStatusResponse]

      Seq(5, 7) should contain(
        ssr.elevatorStates.find(_.goals == Seq(6)).get.floor)
    }
  }
}
