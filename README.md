# Elevator Control System


### Implementation details

System is modeled with Akka actors.

Every elevator is represented by `ElevatorActor`. Whole system is represented by `SystemActor`. 
It accepts:
- `SystemStatusRequest`: request for status. Replies with `SystemStatusResponse`.
- `PickupRequest(floor, direction)`: request for any elevator to go to a certain floor (and then
 in a certain direction). That's a call a passenger makes when outside an elevator.
- `FloorRequest(elevator ID, floor)`: request for a specific elevator to go to a certain floor. 
That's a call a passenger makes when inside an elevator.
- `Step`: request to play 1 step of the simulation.

Every elevator holds a state with the following fields:
- `id`: integer elevator ID,
- `floor`: current floor,
- `goals`: list of goal floors,
- `lastDirection`: direction in which the elevator will move on its last segment of the planned 
trajectory,
- `arrived`: `true` if elevator just arrived to its goal and has to spend one simulation step 
being idle (this models opening/closing doors and gives time for new passengers to select floors).

#### Requests assigning algorithm
 
There are two types of requests: `FloorRequest` specifies a particular elevator and its goal 
gets added to the current trajectory instantly. For `PickupRequest` the most suitable elevator 
gets selected.

To find the most suitable elevator we compute suitability function for every (elevator, 
request) pair (see `ElevatorState.suitability`). Elevator is suitable for a request only if the 
request is within its known trajectory, then 
`Suitability = Number_of_floors - Distance(Request_floor, Elevator_floor)`. Elevator with the 
 highest suitability gets the request; if no elevator is suitable, requests stays in the queue 
 until the next state update.


### CLI

Run CLI:
```
sbt run
```

```
Usage:
  init x y   initialize system with x elevators and y floors
  req x y    pickup request on floor x to go to direction y (y < 0 -- Down; y >= 0 -- Up)
  floor x y  floor request for elevator x, floor y
  step       perform 1 step on simulation
  exit
```

Example status output:

```
1: 4 -> 9; Active; Down
2: 5; Idle; Up
3: 8; Active; []
Queue: 1(Up), 2(Down)
```

It means:

- There are 3 elevators in the system.
- 1st elevator is on 4th floor. Then it will go to 9th floor and then down.
- 2nd elevator is on the 5th floor. It just arrived there and is idle for one time step. Then it 
should go up.
- 3rd elevator is on the 8th floor. It's ready to accept new goals.
- Two pickup requests are queued up: to go to the 1st floor and then up and to the 2nd floor and 
then down.