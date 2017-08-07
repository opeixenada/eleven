package model

import model.Directions.{Down, Up}
import org.scalatest._


class ElevatorStateTest extends WordSpec with Matchers {

  val s0 = ElevatorState(id = 0)

  "Directions" should {
    "be correctly calculated for goals trajectory" in {
      ElevatorState.directions(1, Seq(6, 2, 7, 12, 11)) should be(Seq(Up, Down, Up, Up, Down))
    }
  }

  "Next goal" should {
    "be returned if exists" in {
      val es = s0.copy(goals = Seq(1))
      es.nextGoal should be(Some(1))
    }

    "be None if there're no goals" in {
      val es = s0.copy(goals = Seq.empty)
      es.nextGoal should be(None)
    }
  }

  "Current direction" should {
    "be Up" in {
      val es = s0.copy(floor = 0, goals = Seq(1))
      es.direction should be(Some(Up))
    }

    "be Down" in {
      val es = s0.copy(floor = 2, goals = Seq(1))
      es.direction should be(Some(Down))
    }

    "be None if idle" in {
      val es = s0.copy(goals = Seq.empty)
      es.direction should be(None)
    }

    "be None if next goal is current floor" in {
      val es = s0.copy(floor = 1, goals = Seq(1))
      es.direction should be(None)
    }
  }

  "On next step" should {
    "stay still if there are no goals" in {
      val es = s0.copy(goals = Seq.empty, lastDirection = Some(Down), arrived = true)
      val target = es.copy(lastDirection = None, arrived = false)
      es.next() should be(target)
    }

    "stay still if just arrived to the goal floor, update `arrived` flag" in {
      val es = s0.copy(floor = 1, goals = Seq(2, 3), arrived = true)
      val target = es.copy(arrived = false)
      es.next() should be(target)
    }

    "move towards the next goal up" in {
      val es = s0.copy(floor = 0, goals = Seq(2, 3), arrived = false)
      val target = es.copy(floor = 1)
      es.next() should be(target)
    }

    "move towards the next goal down" in {
      val es = s0.copy(floor = 4, goals = Seq(2, 1), arrived = false)
      val target = es.copy(floor = 3)
      es.next() should be(target)
    }

    "set `arrived` flag and update goals when reached goal" in {
      val es = s0.copy(floor = 4, goals = Seq(3), arrived = false)
      val target = es.copy(floor = 3, goals = Seq.empty, arrived = true)
      es.next() should be(target)
    }

    "wipe out `lastDirection` when there're no goals" in {
      val es = s0.copy(goals = Seq(), arrived = false, lastDirection = Some(Down))
      val target = es.copy(lastDirection = None)
      es.next() should be(target)
    }

    "not wipe out `lastDirection` when just arrived to the goal floor" in {
      val es = s0.copy(floor = 1, goals = Seq(1), arrived = true, lastDirection = Some(Down))
      val target = es.copy()
      es.next() should be(target)
    }
  }

  "New goal" should {
    "be inside the current trajectory if possible" in {
      val es = s0.copy(floor = 6, goals = Seq(7, 10, 3))
      val target = es.copy(goals = Seq(7, 10, 5, 3))
      es.addGoal(5, None) should be(target)
    }

    "be after the current trajectory if not possible to fit it in" in {
      val es = s0.copy(floor = 4, goals = Seq(5, 10, 3), lastDirection = Some(Up))
      val target = es.copy(goals = Seq(5, 10, 3, 11))
      es.addGoal(11, None) should be(target)
    }

    "not be inserted if already in the trajectory" in {
      val es = s0.copy(goals = Seq(5, 10, 3))
      val target = es.copy()
      es.addGoal(10, None) should be(target)
    }

    "update last direction if directed" in {
      val es = s0.copy(goals = Seq(5, 10, 3), lastDirection = Some(Down))
      val target = es.copy(goals = Seq(5, 10, 3, 11), lastDirection = Some(Up))
      es.addGoal(11, Some(Up)) should be(target)
    }
  }

  "Suitability" should {
    "be None if just arrived to the goal floor" in {
      val es = s0.copy(goals = Seq(5), lastDirection = Some(Up), arrived = true)
      es.suitability(6, Up, 10) should be(None)
    }

    "be None if is moving away from target (up)" in {
      val es = s0.copy(floor = 3, goals = Seq(5), lastDirection = Some(Up), arrived = false)
      es.suitability(2, Up, 10) should be(None)
    }


    "be None if is moving away from target (down)" in {
      val es = s0.copy(floor = 6, goals = Seq(5), lastDirection = Some(Up), arrived = false)
      es.suitability(7, Up, 10) should be(None)
    }

    "be None if is going to change direction" in {
      val es = s0.copy(floor = 7, goals = Seq(5), lastDirection = Some(Up), arrived = false)
      es.suitability(6, Down, 10) should be(None)
    }

    "be None if moving to a different direction" in {
      val es = s0.copy(floor = 7, goals = Seq(5), arrived = false)
      es.suitability(6, Up, 10) should be(None)
    }

    "be `#Floors - Distance(Request_floor, Elevator_floor)` (up)" in {
      val es = s0.copy(floor = 1, goals = Seq(5), arrived = false)
      es.suitability(4, Up, 10) should be(Some(7))
    }

    "be `#Floors - Distance(Request_floor, Elevator_floor)` (down)" in {
      val es = s0.copy(floor = 9, goals = Seq(5), arrived = false)
      es.suitability(4, Down, 10) should be(Some(5))
    }
  }

}


