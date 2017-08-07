package model

import model.Directions.Direction


object Messages {

  /** Request for an elevator system status update. */
  case object SystemStatusRequest

  /** Reply with an elevator system status update. */
  case class SystemStatusResponse(elevatorStates: Seq[ElevatorState], queue: Seq[PickupRequest])

  /** Request for an single elevator status update. */
  case object ElevatorStatusRequest

  /** Reply with a single elevator status update. */
  case class ElevatorStatusUpdate(elevatorState: ElevatorState)

  /**
    * Request for the selected elevator to go to a certain floor.
    * That's a call a passenger makes when inside an elevator.
    */
  case class FloorRequest(elevatorId: Int, floor: Int)

  /**
    * Request for any elevator to go to a certain floor (and then -- in a certain direction).
    * That's a call a passenger makes when outside an elevator.
    *
    * @param floor     floor number
    * @param direction Up or Down
    */
  case class PickupRequest(floor: Int, direction: Direction)

  /** Request to play 1 step of the simulation. */
  case object Step

}
