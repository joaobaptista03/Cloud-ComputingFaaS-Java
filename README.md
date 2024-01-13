# Cloud Computing (FaaS)

This project implements a distributed system for managing and executing tasks. It consists of a central server that handles client requests and a client application for interacting with the server.

## Features

- **User Registration and Authentication**: Users can register and log in to the system.
- **Task Execution**: Clients can submit tasks to be executed by the server.
- **Service Status Query**: Clients can query the current status of the server, including available memory and pending tasks.
- **Client-Server Communication**: Implemented using Java Sockets and multithreading.
- **Concurrent Task Handling**: The server uses a custom thread pool to manage task execution concurrently.

## Getting Started

### Prerequisites

- Java JDK 11 or later
- IDE for Java development (e.g., IntelliJ IDEA, Eclipse)

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/joaobaptista03/UMinho-Cloud-ComputingFaaS-Java/
   ```
2. Navigate to the repository directory and run a CentralServer, and 1 or more ClientUI's.
