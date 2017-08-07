package model


object Directions {

  sealed trait Direction

  case object Up extends Direction

  case object Down extends Direction

  def apply(direction: Int): Direction = if (direction < 0) Down else Up

  def unapply(direction: Direction): Int = direction match {
    case Up => 1
    case Down => -1
  }
}