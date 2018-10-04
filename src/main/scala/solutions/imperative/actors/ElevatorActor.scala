package solutions.imperative.actors

import akka.actor.{Actor, ActorRef}
import models.{Elevator, _}
import solutions.imperative.messages.{ElevatorStatusRequest, ElevatorStatusUpdate}

/**
  * Actor representation of a single elevator.
  *
  * Accepts:
  * `ElevatorStatusRequest`
  * `PickupRequest`
  * `FloorRequest`
  * `Step`
  **/
class ElevatorActor(initState: Elevator) extends Actor {
  def this(id: Int) = this(Elevator(id))

  private var state = initState

  private def update(newState: Elevator, sender: ActorRef) = {
    state = newState
    sender ! ElevatorStatusUpdate(state)
  }

  def receive: Receive = {
    case ElevatorStatusRequest => sender ! ElevatorStatusUpdate(state)
    case PickupRequest(floor, direction) =>
      update(state.addGoal(floor, Some(direction)), sender)
    case FloorRequest(_, floor) => update(state.addGoal(floor, None), sender)
    case Step                   => update(state.next(), sender)
  }
}
