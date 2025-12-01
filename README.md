# Adjudicator Java Client SDK

Java client library for the Adjudicator Chess Contest Platform. This SDK provides a simple interface for building chess agents that can compete on the platform using gRPC protocol.

## Features

- **High-performance gRPC communication** with bidirectional streaming
- **Simple Agent interface** - just implement 4 methods
- **Maven-based build system** with automatic Protocol Buffer code generation
- **Example agents** to get you started quickly
- **Support for all game modes**: Training, Open, and Ranked
- **Java 11+ compatible**

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- No external tools required for code generation — `protoc` and the gRPC compiler are downloaded automatically by Maven

## Installation

### Building from Source

1. Clone the repository and navigate to the Java client directory:

```bash
cd adjudicator-agent
```

2. Build the project with Maven (this will automatically generate Protocol Buffer code):

```bash
mvn clean compile
```

3. To create a JAR with dependencies for distribution:

```bash
mvn package assembly:single
```

### Adding to Your Maven Project

Add the following dependency to your `pom.xml` (after building locally):

```xml
<dependency>
    <groupId>ch.adjudicator</groupId>
    <artifactId>adjudicator-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Protocol Buffer code generation (protoc via Maven)

This project uses the `protobuf-maven-plugin` to generate Java classes and gRPC stubs from the `.proto` files under `src/main/proto`.

Under the hood, Maven automatically downloads platform-specific binaries for `protoc` and the `grpc-java` codegen plugin via the `os-maven-plugin`. You do not need to install `protoc` or any external CLI.

### Automatic generation via Maven (recommended)

Simply run:

```bash
mvn compile
```

Maven is configured to:
- Run `protoc` and the `grpc-java` plugin during the `generate-sources` phase
- Write generated sources to `target/generated-sources/proto`
- Add that directory to the compilation sources automatically

### Manual regeneration (optional)

Normally you only need to run Maven. To force regeneration, run:

```bash
mvn clean compile
```

## Quick Start

### 1. Implement the Agent Interface

```java
package com.example;

import ch.adjudicator.client.Agent;
import ch.adjudicator.client.GameInfo;
import ch.adjudicator.client.GameOverInfo;
import ch.adjudicator.client.MoveRequest;

public class MyAgent implements Agent {

    @Override
    public String getMove(MoveRequest request) throws Exception {
        // Return your move in Long Algebraic Notation (e.g., "e2e4")
        // request.getOpponentMove() contains the last move (empty for first move)
        // request.getYourTimeMs() and request.getOpponentTimeMs() contain remaining time
        return "e2e4";
    }

    @Override
    public void onGameStart(GameInfo info) {
        System.out.println("Game started! Playing as " + info.getColor());
    }

    @Override
    public void onGameOver(GameOverInfo info) {
        System.out.println("Game over! Result: " + info.getResult() +
                ", Reason: " + info.getReason());
    }

    @Override
    public void onError(String message) {
        System.err.println("Error: " + message);
    }
}
```

### 2. Connect and Play

```java
import ch.adjudicator.client.AdjudicatorClient;
import ch.adjudicator.client.GameMode;

public class Main {
    public static void main(String[] args) throws Exception {
        // Create client
        AdjudicatorClient client = new AdjudicatorClient(
                "grpc.adjudicator.ch",  // Server address
                "your-api-key",         // API key
                false                   // Use TLS
        );

        // Create your agent
        MyAgent agent = new MyAgent();

        // Play a game using gRPC
        client.playGame(
                agent,
                GameMode.TRAINING,
                "180+2",   // 3 minutes + 2 second increment
                "MyAgent"  // Agent name (optional)
        );

        // Play a game using REST
        client.playGameRest(
                agent,
                GameMode.TRAINING,
                "180+2"
        );
    }
}
```

## Running the Example

An easy example agent is provided (plays legal random moves):

```bash
# Build the fat JAR
mvn -DskipTests package assembly:single

# Run the example
java -cp target/adjudicator-client-1.0.0-jar-with-dependencies.jar \
  ch.adjudicator.examples.EasyAgent --key YOUR_API_KEY --server grpc.adjudicator.ch
```

Or build and run the JAR:

```bash
mvn package assembly:single
java -cp target/adjudicator-client-1.0.0-jar-with-dependencies.jar \
  ch.adjudicator.examples.EasyAgent --key YOUR_API_KEY
```

### Command Line Options

- `--key <api-key>`: Your API key (required)
- `--server <host:port>`: Server address (default: localhost:50051)
- `--mode <mode>`: Game mode - TRAINING, OPEN, or RANKED (default: TRAINING)
- `--time <control>`: Time control, e.g., "180+2" (default: 300+0)
- `--name <name>`: Agent name for logging (default: EasyBot)
- Default values for the included `EasyAgent`: server `localhost:50051`, name `EasyBot`.

Example:

```bash
java -cp target/adjudicator-client-1.0.0-jar-with-dependencies.jar \
  ch.adjudicator.examples.EasyAgent \
  --key my-api-key \
  --server adjudicator.example.com:50051 \
  --mode RANKED \
  --time 180+2 \
  --name MyBot
```

## API Reference

### AdjudicatorClient

Main client class for connecting to the Adjudicator platform.

```text
public AdjudicatorClient(String serverAddress, String apiKey, boolean useTls)
```

**Methods:**
- `playGame(Agent agent, GameMode mode, String timeControl, String agentName)` - Play a game using gRPC
- `playGameRest(Agent agent, GameMode mode, String timeControl)` - Play a game using REST

### Agent Interface

Interface that your agent must implement.

**Methods to implement:**
- `String getMove(MoveRequest request)` - Return your move in LAN format
- `void onGameStart(GameInfo info)` - Called when game starts
- `void onGameOver(GameOverInfo info)` - Called when game ends
- `void onError(String message)` - Called on non-terminal errors

### Enums and Classes

**GameMode**:
- `TRAINING` - Unrated practice mode
- `OPEN` - Unrated casual play
- `RANKED` - Rated competitive play with Elo ratings

**Color**:
- `WHITE`
- `BLACK`

**GameResult**:
- `WIN`
- `LOSS`
- `DRAW`

**MoveRequest**:
- `String getOpponentMove()` - Last move by opponent (empty for first move)
- `int getYourTimeMs()` - Your remaining time in milliseconds
- `int getOpponentTimeMs()` - Opponent's remaining time in milliseconds

**GameInfo**:
- `String getGameId()` - Unique game identifier
- `Color getColor()` - Your assigned color
- `int getInitialTimeMs()` - Starting time in milliseconds
- `int getIncrementMs()` - Time increment per move in milliseconds

**GameOverInfo**:
- `GameResult getResult()` - Game outcome from your perspective
- `String getReason()` - Reason for game end (e.g., "CHECKMATE", "TIMEOUT")
- `String getFinalPgn()` - Complete game in PGN format

## Move Format

Moves must be in **Long Algebraic Notation (LAN)**:
- Normal moves: `e2e4`, `g1f3`
- Castling: `e1g1` (kingside), `e1c1` (queenside)
- Promotion: `e7e8q` (promote to queen), `e7e8r` (promote to rook)
- En passant: `e5d6` (standard notation)

## Error Handling

The SDK will throw exceptions for:
- Connection failures
- Authentication errors
- Invalid moves (results in immediate loss)
- Server errors

Always wrap your game loop in try-catch:

```text
try {
    client.playGame(agent, GameMode.TRAINING, "180+2", "MyAgent");
} catch (Exception e) {
    System.err.println("Game error: " + e.getMessage());
    e.printStackTrace();
}
```

## Troubleshooting

- Generated code missing: Run `mvn clean compile` and check `target/generated-sources/proto`.
- Compilation conflicts with `chess_contest` classes: Ensure you have not manually edited or re-enabled the checked-in generated files under `src/main/java/chess_contest/**`. The build excludes them.
- Network/TLS: The client expects plaintext gRPC to the given address. Terminate TLS at a reverse proxy if needed.

## Best Practices

1. **Maintain Board State**: Track the current position by applying moves
2. **Generate Legal Moves**: Only submit legal moves to avoid instant loss
3. **Time Management**: Monitor `getYourTimeMs()` and plan moves accordingly
4. **Error Handling**: Implement robust error handling in your agent
5. **Testing**: Use TRAINING mode for development and testing
6. **Logging**: Use Java's logging framework for debugging

## Building a Real Chess Agent

For a production agent, consider using existing chess libraries:

### Using chesslib

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.bhlangonijr</groupId>
    <artifactId>chesslib</artifactId>
    <version>1.3.4</version>
</dependency>
```

Example agent:

```java
import ch.adjudicator.client.Agent;
import ch.adjudicator.client.GameInfo;
import ch.adjudicator.client.MoveRequest;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

public class ChessAgent implements Agent {
    private Board board;

    @Override
    public void onGameStart(GameInfo info) {
        board = new Board();
    }

    @Override
    public String getMove(MoveRequest request) throws Exception {
        // Apply opponent's move
        if (!request.getOpponentMove().isEmpty()) {
            Move oppMove = new Move(request.getOpponentMove(), board.getSideToMove());
            board.doMove(oppMove);
        }

        // Generate legal moves
        List<Move> legalMoves = board.legalMoves();

        // Select move (implement your strategy here)
        Move selectedMove = legalMoves.get(0);

        // Apply and return move
        board.doMove(selectedMove);
        return selectedMove.toString();
    }

    // ... implement other methods
}
```

## Project Structure

```
.
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   ├── ch/adjudicator/client/    # SDK classes
        │   └── ch/adjudicator/examples/  # Example agents (EasyAgent)
        └── proto/
            └── adjudicator.proto         # Protobuf definitions

# Generated at build time (not checked in):
target/
└── generated-sources/
    └── proto/                            # Java & gRPC stubs from protoc (Maven plugin)
```

## Support

For issues, questions, or contributions, please refer to the main Adjudicator Protocol repository.

## License

See the main project LICENSE file for details.
