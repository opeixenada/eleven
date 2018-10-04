import models.Directions.{Direction, Down, Up}
import models.{Elevator, PickupRequest}

package object utils {

  /** Checks if request parameters are feasible. */
  def validateRequest(floor: Int,
                      direction: Option[Direction],
                      floors: Int): Boolean = {
    val directionCheck = direction match {
      case Some(dir) =>
        !(floor == 0 && dir == Down) && !(floor == floors - 1 && dir == Up)
      case _ => true
    }

    val floorCheck = 0 <= floor && floor < floors

    floorCheck && directionCheck
  }

  /**
    * Elevators are ranked accordingly to the suitability function and the most suitable gets
    * chosen. See `Elevator.suitability`.
    *
    * @param floor     request floor
    * @param direction request direction
    * @return Left - the request is already in the system
    *         Right(None) - there's no suitable elevator for the request, queue the request
    *         Right(Some(id)) - ID of the most suitable elevator is `id`
    */
  def chooseElevator(floor: Int,
                     direction: Direction,
                     elevators: Map[Int, Elevator],
                     elevatorsUpdated: Set[Int],
                     floors: Int): Either[Unit, Option[Elevator]] = {
    elevators
      .filter(es => elevatorsUpdated.contains(es._1))
      .values
      .map { s =>
        (s, s.suitability(floor, direction, floors))
      }
      .filter(_._2.isDefined) match {
      case Nil => Right(None)
      case availableElevators =>
        val maxSuitability = availableElevators.map(_._2.get).max
        val mostSuitableElevators =
          availableElevators.filter(_._2.contains(maxSuitability))
        if (mostSuitableElevators.exists(_._1.goals.contains(floor))) {
          Left(Unit)
        } else {
          Right(Option(mostSuitableElevators.minBy(_._1.goals.size)._1))
        }
    }
  }

  /** Get string description of the current state of the system. */
  def statusDescription(elevators: Seq[Elevator],
                        queue: Seq[PickupRequest]): String = {
    val elevatorsStatus = elevators.sortBy(_.id).map { state =>
      val route =
        if (state.goals.isEmpty) "" else s" -> ${state.goals.mkString(" -> ")}"

      val lastDirection = state.lastDirection match {
        case Some(d) => d
        case _       => "[]"
      }

      val arrived = if (state.arrived) "Idle" else "Active"

      Seq(s"${state.id}: ${state.floor}$route", arrived, lastDirection)
        .mkString("; ")
    }

    elevatorsStatus.mkString("\n")

    val queueLine = if (queue.nonEmpty) {
      val qLine = queue
        .map { r =>
          s"${r.floor}(${r.direction})"
        }
        .mkString(", ")
      s"Queue: $qLine"
    } else {
      "Queue: empty"
    }

    (elevatorsStatus :+ queueLine).mkString("\n")
  }

}
