# Java 21 for Golf Event Manager

Golf Event Manager targets Java 21, while Golf League Manager can continue to use Java 17.

The backend test launcher explicitly selects:

`C:\Program Files\Java\jdk-21.0.10`

Run backend tests with:

`scripts\test-backend.cmd`

This keeps the project's command-line build independent of the machine-wide `JAVA_HOME` used by other projects.

If the Java 21 JDK is moved or upgraded, update `EVENT_MANAGER_JAVA_HOME` in `scripts\test-backend.cmd`.
