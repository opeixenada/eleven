package solutions.imperative

import models.{Elevator, PickupRequest}

package object messages {

  /** Request for an elevator system status update. */
  case object SystemStatusRequest

  /** Reply with an elevator system status update. */
  case class SystemStatusResponse(elevatorStates: Seq[Elevator],
                                  queue: Seq[PickupRequest])

  /** Request for an single elevator status update. */
  case object ElevatorStatusRequest

  /** Reply with a single elevator status update. */
  case class ElevatorStatusUpdate(elevatorState: Elevator)

}
