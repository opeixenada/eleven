package solutions.functional

import models._
import solutions.functional.ElevatorSystem._
import solutions.functional.State._
import utils._

case class ElevatorSystem(elevators: Map[Int, Elevator],
                          elevatorsUpdated: Set[Int],
                          queue: Seq[PickupRequest],
                          floors: Int) {

  def next(): ElevatorSystem = {
    copy(elevators = elevators.mapValues(_.next()),
         elevatorsUpdated = elevators.keySet)
  }

  def applyQueue(): ElevatorSystem = {
    (this /: queue) {
      case (newSystem, req) =>
        chooseElevator(req.floor,
                       req.direction,
                       newSystem.elevators,
                       newSystem.elevatorsUpdated,
                       newSystem.floors) match {
          case Right(Some(elevatorId)) =>
            sendRequest(req, elevatorId, newSystem)
          case Right(None) => newSystem // No available elevators, do nothing
          case _           => newSystem.copy(queue = queue.filterNot(_ == req))
        }
    }
  }

  /** Adds a pickup request to the queue. */
  def addToQueue(req: PickupRequest): ElevatorSystem = {
    if (!queue.contains(req)) copy(queue = queue :+ req)
    else this
  }

}

object ElevatorSystem {

  type ElevatorSystemState[A] = State[ElevatorSystem, A]

  def init(numberElevators: Int, floors: Int): ElevatorSystem = {
    val elevators: Map[Int, Elevator] = (1 to numberElevators).map { count =>
      val id = count
      id -> Elevator(id)
    }.toMap

    ElevatorSystem(
      elevators = elevators,
      elevatorsUpdated = elevators.keySet,
      queue = Seq.empty,
      floors = floors
    )
  }

  private def update: Input => (ElevatorSystem => ElevatorSystem) =
    (input: Input) =>
      (system: ElevatorSystem) =>
        input match {

          case req @ PickupRequest(floor, direction) =>
            if (validateRequest(floor, Some(direction), system.floors))
              system
                .addToQueue(req)
                .applyQueue()
            else system

          case Step => system.next().applyQueue()

          case _ => system
    }

  def simulate(inputs: List[Input]): ElevatorSystemState[ElevatorSystem] =
    for {
      _ <- sequence(inputs.map { input =>
        modify(update(input))
      })
      s <- get
    } yield s

  def sendRequest(request: PickupRequest,
                  elevator: Elevator,
                  system: ElevatorSystem): ElevatorSystem = {

    val newElevator = elevator.addGoal(request.floor, Some(request.direction))

    system.copy(
      elevators = system.elevators + (elevator.id -> newElevator),
      elevatorsUpdated = system.elevatorsUpdated - elevator.id,
      queue = system.queue.filterNot(_ == request)
    )
  }

}
