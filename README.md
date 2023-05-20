# Agar Winner

## Tech Stack 
- backend: functional scala with cats, fs2, http4s
- front-end: vanilla js, canvas

## Instructions
  - Move mouse on the screen to move own character.
  - Absorb foods (orbs) by running over them in order to grow.
  - Absorb smaller players to get larger.

## Tasks List
 - [x] Create scala project with cats, fs2, http4s
 - [x] Create ui directory that contains front-end related logic 
 - [x] Player enters their name in ui which load game board and connects to server via websocket
 - [x] Once player connected it gets list of foods from the server
 - [x] Server sends the player list to the client side with 30fps
 - [x] Player sends its direction vector to the server & server calculates the new location of the player
 - [x] Client side renders all the players & foods on the canvas
 - [x] Server calculates the collision with food or other player 
 - [x] Server sends the leader board & player death to the client side
 - [x] Client side update the leader board, foods, players
