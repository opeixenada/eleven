import models.{Directions, PickupRequest, Step}
import solutions.functional.ElevatorSystem
import utils._

/** Very basic CLI for `ElevatorSystem`. */
object FunCli extends App {

  private var ok = true
  private var system: Option[ElevatorSystem] = None

  private def doSomething(smth: ElevatorSystem => Unit) = system match {
    case Some(s) => smth(s)
    case _       => println("System is not initialized")
  }

  private def printStatus() = system.foreach { s =>
    println(statusDescription(s.elevators.values.toSeq, s.queue))
  }

  locally {
    println("Hey, it's elevator system simulation!")
    println(
      """Usage:
        |  status     print out current system status
        |  init x y   initialize system with x elevators and y floors
        |  req x y    pickup request on floor x to go to direction y (y < 0 -- Down; y >= 0 -- Up)
        |  step       perform 1 step on simulation
        |  exit""".stripMargin)

    while (ok) {
      val cmnd = scala.io.StdIn.readLine()

      try {
        cmnd.split(" ").toList match {
          case "init" :: numberElevators :: floors :: Nil =>
            system =
              Option(ElevatorSystem.init(numberElevators.toInt, floors.toInt))
            printStatus()

          case "status" :: Nil =>
            doSomething(_ => printStatus())

          case "step" :: Nil =>
            doSomething { s =>
              val (s1, _) = ElevatorSystem.simulate(List(Step)).run(s)
              system = Some(s1)
              printStatus()
            }

          case "req" :: floor :: dir :: Nil =>
            doSomething { s =>
              val request = PickupRequest(floor.toInt, Directions(dir.toInt))
              val (s1, _) = ElevatorSystem.simulate(List(request)).run(s)
              system = Some(s1)
              printStatus()
            }

          case "exit" :: Nil =>
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
