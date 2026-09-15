# lock-manager

## Project Overview

Tosan Lock Manager is a logical distributed lock library that provides unified locking mechanisms across multiple backend technologies:
DBMS (Oracle, DB2, PostgreSQL), Zookeeper, Redis, Hazelcast, and an in-memory (pure-JVM, non-distributed) option.

- **Java Version**: 17
- **Build Tool**: Maven
- **Key Dependencies**: Spring ORM, Hibernate, Hazelcast, Redisson, Apache Curator

## Architecture

### Core Abstraction Layer

The library is built around a single interface `LockManagementService` (in `api/`) that defines the contract for all locking operations:
- Read/Write lock acquisition with configurable timeout
- Lock release (explicit or transaction-based via `releaseOnCommit` flag)
- Lock conversion between read and write modes

### Implementation Strategy Pattern

Each backend technology has its own implementation package under `impl/`:

**DBMS Implementation** (`impl/dbms/`):
- Uses a factory pattern (`DbmsLockServiceFactory`) to auto-detect database dialect via Hibernate
- Supports Oracle, DB2, and PostgreSQL through database-specific stored procedures
- Each database has separate DAO and Service layers:
    - DAOs (`dao/`) handle database-specific SQL/procedure calls
    - Services (`service/`) implement `DbmsLockService` interface
- Requires Spring `@Transactional` context with `REQUIRED` propagation
- Lock names are prefixed with schema name for uniqueness (max 128 chars: 30 for schema + 96 for lockNameType+lockName)

**Distributed Systems** (`impl/hazelcast/`, `impl/redis/`, `impl/zookeeper/`):
- Each provides a `*LockManagementService` implementation
- Redis uses Redisson library for distributed locks
- Zookeeper uses Apache Curator recipes
- Hazelcast uses native distributed lock primitives

**In-Memory Implementation** (`impl/inmemory/`):
- `MemoryLockManagementService` backed by `MemoryLockService`, which keeps one `java.util.concurrent.locks.ReentrantReadWriteLock` per lock handle in a process-local map
- Not distributed: it only coordinates threads within a single JVM, so it does not require (or support) coordination across multiple application instances
- Requires no external client - it can be instantiated directly with `new InMemoryLockManagementService()`

### Lock Name Constraints

For DBMS implementations:
- Total lock name length: max 128 characters
- Schema name: max 30 characters (Oracle constraint)
- Combined `lockNameType` + `lockName`: max 96 characters
- Schema name is automatically prepended for uniqueness

### Exception Hierarchy

- `LockManagerTimeoutException`: Thrown when lock cannot be acquired within timeout period
- `LockManagerRunTimeException`: Thrown for internal errors (e.g., unsupported database dialect)

## Code Patterns

### Transaction Management
All DBMS lock operations require active Spring transactions with `Propagation.REQUIRED`. Non-transactional calls will fail.

### Service Instantiation
- **DBMS**: Pass `EntityManager` to constructor → factory auto-detects database type
- **Redis/Zookeeper/Hazelcast**: Each has specific configuration requirements for their respective clients
- **In-Memory**: No configuration or external client needed - `new InMemoryLockManagementService()`

### Lock Release Behavior
The `releaseOnCommit` parameter controls when locks are released:
- `true`: Lock released automatically on transaction commit/rollback (DBMS only)
- `false`: Lock held until explicit `unlock()` call or session/transaction end
