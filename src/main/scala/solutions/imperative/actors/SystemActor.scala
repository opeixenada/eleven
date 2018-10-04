package solutions.imperative.actors

import akka.actor.{Actor, ActorRef, Props}
import models.{Elevator, _}
import solutions.imperative.messages.{ElevatorStatusUpdate, SystemStatusRequest, SystemStatusResponse}

/**
  * Actor representation of the elevator control system.
  * All elevators operate on every floor.
  * Elevators capacity is not taken into account.
  *
  * Accepts:
  * `SystemStatusRequest`
  * `ElevatorStatusUpdate`
  * `PickupRequest`
  * `FloorRequest`
  * `Step`
  *
  * @param numElevators number of elevators in the system
  * @param numFloors    number of floors in the system
  */
class SystemActor(numElevators: Int, numFloors: Int) extends Actor {

  /** Current state of the system as reported by elevator solutions.actors.actors. */
  private var elevatorsState: Map[Int, Elevator] = (1 to numElevators).map {
    count =>
      val id = count
      id -> Elevator(id)
  }.toMap

  /** IDs of elevators that haven't received any request after their last state update. */
  private var elevatorsUpdated: Set[Int] = elevatorsState.keySet

  /** Queue of pickup requests. */
  private var queue = Seq.empty[PickupRequest]

  private val elevators: Map[Int, ActorRef] = elevatorsState.map {
    case (id, state) =>
      id -> context.actorOf(Props(new ElevatorActor(state)), s"Elevator_$id")
  }

  def receive: Receive = {
    case SystemStatusRequest =>
      sender ! SystemStatusResponse(elevatorsState.values.toSeq, queue)

    case ElevatorStatusUpdate(elevatorState) =>
      elevatorsState = elevatorsState + (elevatorState.id -> elevatorState)
      elevatorsUpdated = elevatorsUpdated + elevatorState.id
      applyQueueToElevator(elevatorState)

    case req @ PickupRequest(floor, direction) =>
      if (utils.validateRequest(floor, Some(direction), numFloors)) {
        addToQueue(req)
        applyQueue()
      }

    case FloorRequest(id, floor) =>
      if (utils.validateRequest(floor, None, numFloors)) {
        elevatorsUpdated = elevatorsUpdated - id
        elevators.get(id).foreach(_ ! FloorRequest(id, floor))
      }

    case Step =>
      elevatorsUpdated = Set.empty
      elevators.values.foreach(_ ! Step)
  }

  /** Adds a pickup request to the queue. */
  private def addToQueue(req: PickupRequest) = {
    if (!queue.contains(req)) {
      queue = queue :+ req
    }
  }

  /** Tries to assign requests from the queue to the elevator. */
  private def applyQueueToElevator(elevatorState: Elevator,
                                   q: Seq[PickupRequest] = queue): Unit = {
    for (req <- q.headOption) {
      elevatorState.suitability(req.floor, req.direction, numFloors) match {
        case Some(_) => sendRequest(req, elevatorState.id)
        case _       => applyQueueToElevator(elevatorState, q.tail)
      }
    }
  }

  /** Tries to assign elements from the queue to the most suitable elevators. */
  private def applyQueue(q: Seq[PickupRequest] = queue): Unit = {
    for (req <- q.headOption) {
      utils.chooseElevator(req.floor,
                           req.direction,
                           elevatorsState,
                           elevatorsUpdated,
                           numFloors) match {
        case Right(Some(elevator)) => sendRequest(req, elevator.id)
        case Right(None)           => // No available elevators, do nothing
        case _                     => queue = queue.filterNot(_ == req)
      }
      applyQueue(q.tail)
    }
  }

  /** Sends request to the given elevator, cleans up the state. */
  private def sendRequest(req: PickupRequest, elevatorId: Int) = {
    elevatorsUpdated = elevatorsUpdated - elevatorId
    queue = queue.filterNot(_ == req)
    elevators.get(elevatorId).foreach(_ ! req)
  }
}
