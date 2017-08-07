package model

import model.Directions._


/**
  * Representation of the elevator's state.
  *
  * @param id            elevator ID
  * @param floor         current floor
  * @param goals         list of goal floors
  * @param lastDirection direction in which the elevator will move on its last segment of the
  *                      currently planned trajectory
  * @param arrived       `true` if elevator just arrived to its goal and has to spend one
  *                      simulation step being idle
  */
case class ElevatorState(id: Int, floor: Int = 0, goals: Seq[Int] = Seq.empty,
                         lastDirection: Option[Direction] = None, arrived: Boolean = false) {

  /** Next goal floor towards which the elevator is moving. `None` if the elevator is idle. */
  lazy val nextGoal: Option[Int] = goals.headOption

  /** Current direction. `None` if the elevator is idle. */
  lazy val direction: Option[Direction] = nextGoal.flatMap { goal =>
    if (goal < floor) Some(Down)
    else if (goal > floor) Some(Up)
    else None
  }

  /**
    * Generates elevator's state in the next time step. We assume that moving between floors
    * and waiting for passenger to board/unboard take one time step.
    *
    * @return elevator's state in the next time step
    */
  def next(): ElevatorState = {
    (nextGoal, direction) match {
      case (None, _) =>
        // Elevator doesn't have any goals, stay still
        this.copy(lastDirection = None, arrived = false)

      case (Some(goal), _) if arrived =>
        // Elevator has just arrived to its goal, stay still for one step now
        val nextArrived = floor == goal
        this.copy(arrived = nextArrived)

      case (Some(goal), Some(dir)) =>
        // Move towards the next goal
        val nextFloor = floor + Directions.unapply(dir)
        val nextGoals = if (nextFloor == goal) goals.tail else goals
        val nextArrived = nextFloor == goal

        val nextLastDirection = (nextArrived, lastDirection, nextGoals) match {
          case (false, _, Nil) => None
          case (_, d, _) => d
        }

        this.copy(floor = nextFloor, goals = nextGoals, lastDirection = nextLastDirection,
          arrived = nextArrived)
    }
  }

  /**
    * Generates a new elevators's state with a goal added.
    *
    * @param newFloor     goal floor
    * @param newDirection direction in which the passenger wants to go
    * @return new elevator's state with a goal added
    */
  def addGoal(newFloor: Int, newDirection: Option[Direction]): ElevatorState = {

    lazy val goalsInsertion = goals.foldLeft[(Seq[Int], Boolean)]((Seq(floor), false)) {
      case ((floors, inserted), goal) =>
        val floor1 = floors.last
        if (!inserted && ((floor1 <= newFloor && newFloor <= goal) || (goal <= newFloor &&
          newFloor <= floor1))) {
          (floors :+ newFloor :+ goal, true)
        } else {
          (floors :+ goal, inserted)
        }
    }

    val newGoalDirection =
      if (floor == newFloor || goals.contains(newFloor)) {
        // Goal is already in the list, do nothing
        (goals, lastDirection)
      } else if (goalsInsertion._2) {
        // Goal was inserted in the list of goals
        (goalsInsertion._1.tail, lastDirection)
      } else {
        // There was no way to fit the new goal into the list of goals, so let's add it to the
        // end of the list
        val newGoals = goals :+ newFloor
        (newGoals, newDirection match {
          case None => ElevatorState.directions(floor, newGoals).lastOption
          case d => d
        })
      }

    this.copy(goals = newGoalDirection._1, lastDirection = newGoalDirection._2)
  }

  /**
    * Suitability value for an elevator and request parameters.
    *
    * 1. If the elevator has just arrived to its goal floor
    * or is moving away from the requested floor
    * or will change direction while executing its current trajectory,
    * it's not suitable at all.
    *
    * 2. Otherwise Suitability = Number_of_floors - Distance(Request_floor, Elevator_floor)
    *
    * @param reqFloor     request floor
    * @param reqDirection request direction
    * @param numFloors    number of floors in the system
    * @return `None` if the elevator is not suitable
    */
  def suitability(reqFloor: Int, reqDirection: Direction, numFloors: Int): Option[Int] = {
    if (arrived || isMovingAway(reqFloor) || willChangeDirection() ||
      !direction.forall(_ == reqDirection)) {
      None
    } else {
      Some(numFloors - math.abs(reqFloor - floor))
    }
  }

  /** Checks is the elevator is moving away from the given floor. */
  private def isMovingAway(requestFloor: Int): Boolean = {
    direction match {
      case Some(d) => (floor > requestFloor && d == Up) || (floor < requestFloor && d == Down)
      case _ => false
    }
  }

  /**
    * Checks if the elevator is going to move in the same direction along its currently planned
    * trajectory. `true` if direction will be changed at some point.
    */
  private def willChangeDirection(): Boolean = {
    val directions = ElevatorState.directions(floor, goals) ++ lastDirection.toSeq
    directions.distinct.size > 1
  }

}

object ElevatorState {
  def directions(floor: Int, goals: Seq[Int]): Seq[Direction] = {
    (floor +: goals).zip(goals).map {
      case (floor1, floor2) => Directions(floor2 - floor1)
    }
  }
}
