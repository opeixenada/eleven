import actors.SystemActor
import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import model.Directions
import model.Messages._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


class ElevatorSystem(numElevators: Int, numFloors: Int) {

  private val actorSystem = ActorSystem("ElevatorsSystem")

  private val elevatorsController = actorSystem.actorOf(
    Props(new SystemActor(numElevators, numFloors)), "ElevatorsController")

  /** Perform 1 step of the simulation. */
  def step(): Unit = {
    elevatorsController ! Step
  }

  /**
    * Request for any elevator to go to a certain floor (and then -- in a certain direction).
    * That's a call a passenger makes when outside an elevator.
    */
  def pickupRequest(floor: Int, direction: Int): Unit = {
    elevatorsController ! PickupRequest(floor, Directions(direction))
  }

  /**
    * Request for the selected elevator to go to a certain floor.
    * That's a call a passenger makes when inside an elevator.
    */
  def floorRequest(elevatorId: Int, floor: Int): Unit = {
    elevatorsController ! FloorRequest(elevatorId, floor)
  }

  /** Get string description of the current state of the system. */
  def statusDescription(): Future[String] = {
    implicit val timeout = Timeout(1.seconds)
    (elevatorsController ? SystemStatusRequest).map {
      case ssr: SystemStatusResponse =>
        val elevatorsStatus = ssr.elevatorStates.sortBy(_.id).map { state =>
          val route = if (state.goals.isEmpty) "" else s" -> ${state.goals.mkString(" -> ")}"

          val lastDirection = state.lastDirection match {
            case Some(d) => d
            case _ => "[]"
          }

          val arrived = if (state.arrived) "Idle" else "Active"

          Seq(s"${state.id}: ${state.floor}$route", arrived, lastDirection).mkString("; ")
        }

        elevatorsStatus.mkString("\n")

        val queueLine = if (ssr.queue.nonEmpty) {
          val qLine = ssr.queue.map { r => s"${r.floor}(${r.direction})" }.mkString(", ")
          s"Queue: $qLine"
        } else {
          "Queue: empty"
        }

        (elevatorsStatus :+ queueLine).mkString("\n")

      case _ => "N/A"
    }
  }

  def terminate(): Unit = {
    actorSystem.terminate()
  }
}
