package solutions.functional

import solutions.functional.State._

case class State[S, +A](run: S => (A, S)) {

  def map[B](f: A => B): State[S, B] =
    flatMap(a => unit(f(a)))

  def map2[B, C](state2: State[S, B])(f: (A, B) => C): State[S, C] =
    flatMap(a => state2.map(f(a, _)))

  def flatMap[B](f: A => State[S, B]): State[S, B] =
    State(s => {
      val (a, s1) = run(s)
      f(a).run(s1)
    })

}

object State {

  def unit[S, A](a: A): State[S, A] = State((a, _))

  def sequence[S, A](states: List[State[S, A]]): State[S, List[A]] =
    states.foldRight(unit[S, List[A]](Nil))((f, acc) => f.map2(acc)(_ :: _))

  def modify[S](f: S => S): State[S, Unit] =
    for {
      s <- get // Gets the current state and assigns it to `s`.
      _ <- set(f(s)) // Sets the new state to `f` applied to `s`.
    } yield ()

  def get[S]: State[S, S] = State(s => (s, s))

  def set[S](s: S): State[S, Unit] = State(_ => ((), s))

}
