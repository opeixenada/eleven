package actors

import akka.actor.{Actor, ActorRef}
import model.ElevatorState
import model.Messages._


/**
  * Actor representation of a single elevator.
  *
  * Accepts:
  * `ElevatorStatusRequest`
  * `PickupRequest`
  * `FloorRequest`
  * `Step`
  **/
class ElevatorActor(initState: ElevatorState) extends Actor {
  def this(id: Int) = this(ElevatorState(id))

  private var state = initState

  private def update(newState: ElevatorState, sender: ActorRef) = {
    state = newState
    sender ! ElevatorStatusUpdate(state)
  }

  def receive: Receive = {
    case ElevatorStatusRequest => sender ! ElevatorStatusUpdate(state)
    case PickupRequest(floor, direction) => update(state.addGoal(floor, Some(direction)), sender)
    case FloorRequest(_, floor) => update(state.addGoal(floor, None), sender)
    case Step => update(state.next(), sender)
  }
}
