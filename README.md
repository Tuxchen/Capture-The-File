# Capture-The-File
A simple hacking game.

# Compile the program

To compile the CTF.java use the follow command

> javac CTF.java

To run the program use

> java CTF [arguments...]

# Usage of the program

The program accepts 4 arguments, but you have to give them 3 arguments. For example:

> java CTF [secure folder] [visible] [start folder] [wincode]

The order of the arguments must follow this example, but the last argument is optional.

> [secure folder]

No user can break out of this folder. The path must be absolute.
  
> [visible]

"yes" or "no" are excepted. On "yes" you get a GUI with logscreen. On "no" the program runs without a GUI and no logs will be written.

> [start folder]

Each user starts in this folder. The path must be absolute.
  
> [wincode]

Optional: The user needs this wincode to win the game with the "win" command inside the tcp-session. default value: §§§!iMpOsSiBlE_!_wInCoDe!§§§
