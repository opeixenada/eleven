import solutions.imperative.ElevatorSystem

import scala.concurrent.ExecutionContext.Implicits.global

/** Very basic CLI for `ElevatorSystem`. */
object Cli extends App {

  private var ok = true
  private var system: Option[ElevatorSystem] = None

  private def doSomething(smth: ElevatorSystem => Unit) = system match {
    case Some(s) => smth(s)
    case _       => println("System is not initialized")
  }

  locally {
    println("Hey, it's elevator system simulation!")
    println(
      """Usage:
        |  status     print out current system status
        |  init x y   initialize system with x elevators and y floors
        |  req x y    pickup request on floor x to go to direction y (y < 0 -- Down; y >= 0 -- Up)
        |  floor x y  floor request for elevator x, floor y
        |  step       perform 1 step on simulation
        |  exit""".stripMargin)

    while (ok) {
      val cmnd = scala.io.StdIn.readLine()

      try {
        cmnd.split(" ").toList match {
          case "init" :: numElevators :: numFloors :: Nil =>
            system =
              Option(new ElevatorSystem(numElevators.toInt, numFloors.toInt))
            Thread.sleep(100)
            system.foreach(_.statusDescription().foreach(println(_)))

          case "status" :: Nil =>
            doSomething(_.statusDescription().foreach(println(_)))

          case "step" :: Nil =>
            doSomething { s =>
              s.step()
              Thread.sleep(100)
              s.statusDescription().foreach(println(_))
            }

          case "req" :: floor :: dir :: Nil =>
            doSomething { s =>
              s.pickupRequest(floor.toInt, dir.toInt)
              Thread.sleep(100)
              s.statusDescription().foreach(println(_))
            }

          case "floor" :: elevator :: floor :: Nil =>
            doSomething { s =>
              s.floorRequest(elevator.toInt, floor.toInt)
              Thread.sleep(100)
              s.statusDescription().foreach(println(_))
            }

          case "exit" :: Nil =>
            system.foreach(_.terminate())
            println("Bye")
            ok = false

          case _ => println(s"Unknown command: $cmnd")
        }
      } catch {
        case e: Exception =>
          println(s"Failed to execute: $cmnd")
          println(e)
      }
    }
  }
}
