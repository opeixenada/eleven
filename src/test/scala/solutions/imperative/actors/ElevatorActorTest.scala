package solutions.imperative.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import models.Directions.Up
import models.{Elevator, _}
import org.scalatest._
import solutions.imperative.messages.{ElevatorStatusRequest, ElevatorStatusUpdate}

class ElevatorActorTest(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("ElevatorActorTest"))

  override def afterAll() {
    system.terminate()
  }

  "Elevator actor" should {
    "send `ElevatorStatusUpdate` on `ElevatorStatusRequest`" in {
      val elevator = system.actorOf(Props(new ElevatorActor(0)))
      elevator ! ElevatorStatusRequest
      expectMsg(ElevatorStatusUpdate(Elevator(0)))
    }

    "add goal on `PickupRequest`" in {
      val elevator = system.actorOf(Props(new ElevatorActor(0)))
      elevator ! PickupRequest(2, Up)
      expectMsg(
        ElevatorStatusUpdate(
          Elevator(0, goals = Seq(2), lastDirection = Some(Up))))
    }

    "add goal on `FloorRequest`" in {
      val elevator = system.actorOf(Props(new ElevatorActor(0)))
      elevator ! FloorRequest(0, 3)
      expectMsg(
        ElevatorStatusUpdate(
          Elevator(0, goals = Seq(3), lastDirection = Some(Up))))
    }

    "update state on `Step`" in {
      val es = Elevator(0, goals = Seq(3), lastDirection = Some(Up))
      val elevator = system.actorOf(Props(new ElevatorActor(es)))
      elevator ! Step
      expectMsg(ElevatorStatusUpdate(es.copy(floor = 1)))
    }
  }

}
