package actors

import akka.actor.{Actor, ActorRef, Props}
import model.Directions._
import model.ElevatorState
import model.Messages._


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

  /** Current state of the system as reported by elevator actors. */
  private var elevatorsState: Map[Int, ElevatorState] = (1 to numElevators).map { count =>
    val id = count
    id -> ElevatorState(id)
  }.toMap

  /** IDs of elevators that haven't received any request after their last state update. */
  private var elevatorsUpdated: Set[Int] = elevatorsState.keySet

  /** Queue of pickup requests. */
  private var queue = Seq.empty[PickupRequest]

  private val elevators: Map[Int, ActorRef] = elevatorsState.map {
    case (id, state) => id -> context.actorOf(Props(new ElevatorActor(state)), s"Elevator_$id")
  }

  def receive: Receive = {
    case SystemStatusRequest =>
      sender ! SystemStatusResponse(elevatorsState.values.toSeq, queue)

    case ElevatorStatusUpdate(elevatorState) =>
      elevatorsState = elevatorsState + (elevatorState.id -> elevatorState)
      elevatorsUpdated = elevatorsUpdated + elevatorState.id
      applyQueueToElevator(elevatorState)

    case req@PickupRequest(floor, direction) =>
      if (requestFilter(floor, Some(direction))) {
        addToQueue(req)
        applyQueue()
      }

    case FloorRequest(id, floor) =>
      if (requestFilter(floor, None)) {
        elevatorsUpdated = elevatorsUpdated - id
        elevators.get(id).foreach(_ ! FloorRequest(id, floor))
      }

    case Step =>
      elevatorsUpdated = Set.empty
      elevators.values.foreach(_ ! Step)
  }

  /** Checks if request parameters are feasible. */
  private def requestFilter(floor: Int, direction: Option[Direction]) = {
    val directionCheck = direction match {
      case Some(dir) => !(floor == 0 && dir == Down) && !(floor == numFloors - 1 && dir == Up)
      case _ => true
    }

    val floorCheck = 0 <= floor && floor < numFloors

    floorCheck && directionCheck
  }

  /** Adds a pickup request to the queue. */
  private def addToQueue(req: PickupRequest) = {
    if (!queue.contains(req)) {
      queue = queue :+ req
    }
  }

  /**
    * Elevators are ranked accordingly to the suitability function and the most suitable gets
    * chosen. See `ControllerActor.suitability`.
    *
    * @param floor     request floor
    * @param direction request direction
    * @return Left - the request is already in the system
    *         Right(None) - there's no suitable elevator for the request, queue the request
    *         Right(Some(id)) - ID of the most suitable elevator is `id`
    */
  private def chooseElevator(floor: Int, direction: Direction): Either[Unit, Option[Int]] = {
    elevatorsState.filter(es => elevatorsUpdated.contains(es._1)).values.map { s =>
      (s, s.suitability(floor, direction, numFloors))
    }.filter(_._2.isDefined) match {
      case Nil => Right(None)
      case availableElevators =>
        val maxSuitability = availableElevators.map(_._2.get).max
        val mostSuitableElevators = availableElevators.filter(_._2.contains(maxSuitability))
        if (mostSuitableElevators.exists(_._1.goals.contains(floor))) {
          Left(Unit)
        } else {
          Right(Option(mostSuitableElevators.minBy(_._1.goals.size)._1.id))
        }
    }
  }

  /** Tries to assign requests from the queue to the elevator. */
  private def applyQueueToElevator(elevatorState: ElevatorState,
                                   q: Seq[PickupRequest] = queue): Unit = {
    for (req <- q.headOption) {
      elevatorState.suitability(req.floor, req.direction, numFloors) match {
        case Some(_) => sendRequest(req, elevatorState.id)
        case _ => applyQueueToElevator(elevatorState, q.tail)
      }
    }
  }

  /** Tries to assign elements from the queue to the most suitable elevators. */
  private def applyQueue(q: Seq[PickupRequest] = queue): Unit = {
    for (req <- q.headOption) {
      chooseElevator(req.floor, req.direction) match {
        case Right(Some(elevatorId)) => sendRequest(req, elevatorId)
        case Right(None) => // No available elevators, do nothing
        case _ => queue = queue.filterNot(_ == req)
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
