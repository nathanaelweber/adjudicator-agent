# Adjudicator Java Client SDK

Java client library for the Adjudicator Chess Contest Platform. This SDK provides a simple interface for building chess agents that can compete on the platform using gRPC protocol.

## Features

- **High-performance gRPC communication** with bidirectional streaming
- **Simple Agent interface** - just implement 4 methods
- **Maven-based build system** with automatic Protocol Buffer code generation
- **Clean Code architecture** using Lombok and SLF4J
- **Example agents** to get you started quickly
- **Support for all game modes**: Training, Open, and Ranked
- **TLS Support** for secure communication
- **Java 11+ compatible**

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- No external tools required for code generation — `protoc` and the gRPC compiler are downloaded automatically by Maven
- **IDE Setup**: If using IntelliJ IDEA or Eclipse, ensure the **Lombok** plugin is installed and annotation processing is enabled.

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

## Protocol Buffer code generation

This project uses the `protobuf-maven-plugin` to generate Java classes and gRPC stubs from the `.proto` files under `src/proto` (specifically `chess_contest.proto`).

Under the hood, Maven automatically downloads platform-specific binaries for `protoc` and the `grpc-java` codegen plugin.

### Automatic generation via Maven (recommended)

Simply run:

```bash
mvn compile
```

Maven is configured to:
- Run `protoc` and the `grpc-java` plugin during the `generate-sources` phase
- Write generated sources to `target/generated-sources/protobuf`
- Add that directory to the compilation sources automatically

## Configuration

### Logging

The SDK uses **SLF4J** with **Logback** for logging.
Configuration is located in `src/main/resources/logback.xml`.
By default, it logs to both Console and File (`logs/chess-agent.log`).

## Quick Start

### 1. Implement the Agent Interface

```java
package com.example;

import ch.adjudicator.client.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyAgent implements Agent {

    @Override
    public String getMove(MoveRequest request) throws Exception {
        // Return your move in Long Algebraic Notation (e.g., "e2e4")
        log.info("My turn! Time: {}ms", request.getYourTimeMs());
        return "e2e4";
    }

    @Override
    public void onGameStart(GameInfo info) {
        log.info("Game started! Playing as {}", info.getColor());
    }

    @Override
    public void onGameOver(GameOverInfo info) {
        log.info("Game over! Result: {}, Reason: {}", info.getResult(), info.getReason());
    }

    @Override
    public void onError(String message) {
        log.error("Error: {}", message);
    }
}
```

### 2. Connect and Play

```java
import ch.adjudicator.client.AdjudicatorClient;
import ch.adjudicator.client.GameMode;

public class Main {
    public static void main(String[] args) {
        String server = "grpc.adjudicator.ch:9090"; // Example
        String apiKey = "your-api-key";
        
        // Use TLS (true) for production servers
        AdjudicatorClient client = new AdjudicatorClient(server, apiKey, true);
        Agent agent = new MyAgent();
        
        try {
            // Connect and play
            client.playGame(agent, GameMode.TRAINING, "180+2");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Running the Example Agent

The SDK includes a ready-to-use agent `EasyAgent` in `ch.adjudicator.agent.EasyAgent`.
You can run it directly using Maven.

### Command Line Arguments

```bash
mvn exec:java "-Dexec.mainClass=ch.adjudicator.agent.SmartAgent" \
  "-Dexec.args=--key <YOUR_API_KEY> --name MyBot --mode RANKED"
```

Supported arguments:
- `--server`: Server address (default: `grpc.adjudicator.ch`)
- `--key`: Your API Key (required if not in agent.env)
- `--name`: Agent name (default: `EasyBot`)
- `--mode`: Game mode: `TRAINING`, `OPEN`, `RANKED` (default: `TRAINING`)
- `--time`: Time control, e.g., `300+0` (default: `300+0`)

### Configuration via agent.env

Instead of passing arguments every time, you can create a file named `agent.env` in the project root directory.
The agent will automatically load configuration from this file.

**Example `agent.env`:**
```properties
API_KEY=your-secret-key-here
AGENT_NAME=MySuperBot
SERVER=grpc.adjudicator.ch
```

*Note: Command line arguments override settings in `agent.env`.*

## API Reference

### Agent Interface

The `Agent` interface defines the callbacks your bot must handle.

**MoveRequest**:
- `String getOpponentMove()` - Opponent's last move in LAN (empty if first move)
- `int getYourTimeMs()` - Your remaining time
- `int getOpponentTimeMs()` - Opponent's remaining time

**GameInfo**:
- `String getGameId()` - Unique Game ID
- `Color getColor()` - Your color (WHITE/BLACK)
- `int getInitialTimeMs()` - Initial clock time
- `int getIncrementMs()` - Increment per move

**GameOverInfo**:
- `GameResult getResult()` - Game outcome (WIN, LOSS, DRAW)
- `String getReason()` - Reason (e.g., CHECKMATE, TIMEOUT)
- `String getFinalPgn()` - Complete game PGN

### Move Format

Moves must be in **Long Algebraic Notation (LAN)**:
- Normal moves: `e2e4`, `g1f3`
- Castling: `e1g1` (kingside), `e1c1` (queenside)
- Promotion: `e7e8q` (promote to queen), `e7e8r` (promote to rook)

## Troubleshooting

- **Generated code missing**: Run `mvn clean compile` and check `target/generated-sources/protobuf`.
- **Compilation conflicts**: Ensure you have not manually edited generated files.
- **TLS/SSL Errors**: The client supports TLS. If connecting to a local server without TLS, pass `false` to the `AdjudicatorClient` constructor.
- **Lombok Errors**: If your IDE shows errors on getters (e.g., `getGameId()`), install the Lombok plugin and enable annotation processing.

## Project Structure

```
.
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   ├── ch/adjudicator/client/    # SDK classes
        │   └── ch/adjudicator/agent/     # Example agent (EasyAgent)
        └── proto/
            └── chess_contest.proto       # Protobuf definitions
```
