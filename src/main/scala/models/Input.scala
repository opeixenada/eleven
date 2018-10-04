package models

import models.Directions.Direction

sealed trait Input

/**
  * Request for the selected elevator to go to a certain floor.
  * That's a call a passenger makes when inside an elevator.
  */
case class FloorRequest(elevatorId: Int, floor: Int) extends Input

/**
  * Request for any elevator to go to a certain floor (and then -- in a certain direction).
  * That's a call a passenger makes when outside an elevator.
  *
  * @param floor     floor number
  * @param direction Up or Down
  */
case class PickupRequest(floor: Int, direction: Direction) extends Input

/** Request to play 1 step of the simulation. */
case object Step extends Input
