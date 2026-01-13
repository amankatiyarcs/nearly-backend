# Nearly Microservices Backend

A comprehensive microservices architecture for the Nearly platform, built with Spring Boot, Apache Kafka, Redis, MongoDB, Elasticsearch, and more.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                   Client                                         │
│                            (React/TypeScript)                                    │
└─────────────────────────────────┬───────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway (8080)                                  │
│                    Circuit Breaker | Rate Limiting | Auth                        │
└─────────────────────────────────┬───────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Discovery (8761)│     │  Config (8888)  │     │   Services      │
│    (Eureka)     │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Services

### Infrastructure Services

| Service | Port | Description |
|---------|------|-------------|
| Discovery Service | 8761 | Eureka Server for service registration |
| Config Service | 8888 | Centralized configuration management |
| API Gateway | 8080 | Entry point with routing, rate limiting, circuit breaker |

### Core Services

| Service | Port | Description | Database |
|---------|------|-------------|----------|
| Auth Service | 8081 | Authentication & anonymous sessions | MongoDB + Redis |
| User Service | 8085 | User profiles, follows | MongoDB + Redis |
| Activity Service | 8086 | Activities management | MongoDB |
| Event Service | 8087 | Events, guests, comments | MongoDB |
| Group Service | 8088 | Groups, members | MongoDB |
| News Service | 8089 | Community news | MongoDB |
| Messaging Service | 8090 | Chat (direct & group) | MongoDB + Redis |
| Moments Service | 8091 | Stories, streaks | MongoDB |
| Marketplace Service | 8092 | Jobs, Deals, Places, Pages | MongoDB |
| Notification Service | 8093 | Push notifications | MongoDB + Redis |
| Search Service | 8094 | Elasticsearch search | Elasticsearch + Redis |

### Random Chat Features

| Service | Port | Description |
|---------|------|-------------|
| Random Chat Service | 8082 | Anonymous text chat |
| Video Chat Service | 8083 | WebRTC video chat |
| Report Service | 8084 | User reporting system |

## API Endpoints

### User Service (`/api/users`)
```
GET    /api/users                          # List all users
GET    /api/users/:id                      # Get user by ID
GET    /api/users/username/:username       # Get user by username
POST   /api/users                          # Create user
PATCH  /api/users/:id                      # Update user
DELETE /api/users/:id                      # Delete user
GET    /api/users/search?q=                # Search users
POST   /api/users/:id/follow/:targetId     # Follow user
DELETE /api/users/:id/unfollow/:targetId   # Unfollow user
GET    /api/users/:id/followers            # Get followers
GET    /api/users/:id/following            # Get following
```

### Activity Service (`/api/activities`)
```
GET    /api/activities                     # List activities
GET    /api/activities/:id                 # Get activity
POST   /api/activities                     # Create activity
PATCH  /api/activities/:id                 # Update activity
DELETE /api/activities/:id                 # Delete activity
POST   /api/activities/:id/like            # Like activity
POST   /api/activities/:id/join            # Join activity
GET    /api/activities/user/:userId        # Get user's activities
GET    /api/activities/category/:category  # Get by category
```

### Event Service (`/api/events`)
```
GET    /api/events                         # List events
GET    /api/events/:id                     # Get event
POST   /api/events                         # Create event
PATCH  /api/events/:id                     # Update event
DELETE /api/events/:id                     # Delete event
GET    /api/events/:id/guests              # Get guests
POST   /api/events/:id/join                # Join event
GET    /api/events/:id/comments            # Get comments
POST   /api/events/:id/comments            # Add comment
```

### Group Service (`/api/groups`)
```
GET    /api/groups                         # List groups
GET    /api/groups/:id                     # Get group
POST   /api/groups                         # Create group
PATCH  /api/groups/:id                     # Update group
DELETE /api/groups/:id                     # Delete group
GET    /api/groups/:id/members             # Get members
POST   /api/groups/:id/join                # Join group
DELETE /api/groups/:id/leave/:userId       # Leave group
GET    /api/groups/user/:userId            # Get user's groups
```

### News Service (`/api/news`)
```
GET    /api/news                           # List news
GET    /api/news/:id                       # Get news item
POST   /api/news                           # Create news
PATCH  /api/news/:id                       # Update news
DELETE /api/news/:id                       # Delete news
POST   /api/news/:id/vote                  # Vote true/fake
POST   /api/news/:id/like                  # Like news
```

### Messaging Service (`/api/messages`)
```
POST   /api/messages                       # Send message
GET    /api/messages/group/:groupId        # Get group messages
GET    /api/messages/direct/:userId        # Get direct messages
GET    /api/messages/conversations/:userId # Get conversations
POST   /api/messages/read/:recipientId     # Mark as read
WS     /ws/messaging                       # WebSocket endpoint
```

### Moments Service (`/api/moments`)
```
GET    /api/moments                        # List moments
GET    /api/moments/:id                    # Get moment
POST   /api/moments                        # Create moment
DELETE /api/moments/:id                    # Delete moment
POST   /api/moments/:id/like               # Like moment
POST   /api/moments/:id/view               # View moment
POST   /api/moments/:id/send               # Send to user
GET    /api/moments/direct/:userId         # Get direct moments
POST   /api/moments/direct/:id/view        # Mark viewed
GET    /api/moments/streaks/:userId        # Get streaks
```

### Marketplace Service

#### Jobs (`/api/jobs`)
```
GET    /api/jobs                           # List jobs
GET    /api/jobs/:id                       # Get job
POST   /api/jobs                           # Create job
PATCH  /api/jobs/:id                       # Update job
DELETE /api/jobs/:id                       # Delete job
GET    /api/jobs/search?q=                 # Search jobs
```

#### Deals (`/api/deals`)
```
GET    /api/deals                          # List deals
GET    /api/deals/:id                      # Get deal
POST   /api/deals                          # Create deal
PATCH  /api/deals/:id                      # Update deal
DELETE /api/deals/:id                      # Delete deal
POST   /api/deals/:id/claim                # Claim deal
```

#### Places (`/api/places`)
```
GET    /api/places                         # List places
GET    /api/places/:id                     # Get place
POST   /api/places                         # Create place
PATCH  /api/places/:id                     # Update place
DELETE /api/places/:id                     # Delete place
```

#### Pages (`/api/pages`)
```
GET    /api/pages                          # List pages
GET    /api/pages/:id                      # Get page
GET    /api/pages/username/:username       # Get by username
POST   /api/pages                          # Create page
PATCH  /api/pages/:id                      # Update page
DELETE /api/pages/:id                      # Delete page
POST   /api/pages/:id/follow               # Follow page
```

### Notification Service (`/api/notifications`)
```
POST   /api/notifications                  # Create notification
GET    /api/notifications/:userId          # Get notifications
GET    /api/notifications/:userId/unread   # Get unread
GET    /api/notifications/:userId/unread/count # Get unread count
PATCH  /api/notifications/:id/read         # Mark as read
POST   /api/notifications/:userId/read-all # Mark all as read
WS     /ws/notifications                   # WebSocket endpoint
```

### Search Service (`/api/search`)
```
GET    /api/search?q=&type=&category=      # Search all content
GET    /api/search/type/:type              # Search by type
GET    /api/search/trending                # Get trending searches
POST   /api/search/index                   # Index document
DELETE /api/search/:type/:id               # Delete from index
POST   /api/search/reindex                 # Reindex all
```

### Random Chat (`/api/random-chat`)
```
GET    /api/random-chat/online             # Get online count
WS     /ws/chat                            # WebSocket for chat
```

### Video Chat (`/api/video-chat`)
```
GET    /api/video-chat/ice-servers         # Get ICE servers
GET    /api/video-chat/online              # Get online count
WS     /ws/video                           # WebSocket for video
```

### Reports (`/api/reports`)
```
POST   /api/reports                        # Submit report
```

## Technology Stack

- **Framework**: Spring Boot 3.2.2
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Circuit Breaker**: Resilience4j
- **Database**: MongoDB
- **Cache**: Redis
- **Message Broker**: Apache Kafka
- **Search**: Elasticsearch
- **Logging**: Logbook
- **Containers**: Docker & Docker Compose

## Quick Start

### Prerequisites
- Java 21
- Docker & Docker Compose
- Maven 3.9+

### Build All Services
```bash
cd microservices
mvn clean package -DskipTests
```

### Start with Docker Compose
```bash
# Start all infrastructure
docker-compose up -d mongodb redis kafka zookeeper elasticsearch

# Wait for infrastructure to be healthy, then start services
docker-compose up -d
```

### Start Individual Services (Development)
```bash
# Start in order:
# 1. Discovery Service
cd discovery-service && mvn spring-boot:run

# 2. Config Service
cd config-service && mvn spring-boot:run

# 3. API Gateway
cd api-gateway && mvn spring-boot:run

# 4. Other services as needed
cd user-service && mvn spring-boot:run
# etc...
```

## Monitoring

- **Eureka Dashboard**: http://localhost:8761
- **Kibana**: http://localhost:5601
- **Actuator Health**: http://localhost:{port}/actuator/health

## Service Ports Reference

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| Auth Service | 8081 |
| Random Chat | 8082 |
| Video Chat | 8083 |
| Report Service | 8084 |
| User Service | 8085 |
| Activity Service | 8086 |
| Event Service | 8087 |
| Group Service | 8088 |
| News Service | 8089 |
| Messaging Service | 8090 |
| Moments Service | 8091 |
| Marketplace Service | 8092 |
| Notification Service | 8093 |
| Search Service | 8094 |
| Discovery (Eureka) | 8761 |
| Config Server | 8888 |
| MongoDB | 27017 |
| Redis | 6379 |
| Kafka | 9092 |
| Elasticsearch | 9200 |
| Kibana | 5601 |

## Kafka Topics

| Topic | Publishers | Consumers |
|-------|------------|-----------|
| user-events | User Service | Notification, Search |
| activity-events | Activity Service | Notification, Search |
| event-events | Event Service | Notification, Search |
| group-events | Group Service | Notification, Search |
| news-events | News Service | Notification, Search |
| message-events | Messaging Service | Notification |
| moment-events | Moments Service | Notification, Search |
| report-events | Report Service | - |

## Contributing

1. Follow the existing code style
2. Add tests for new features
3. Update documentation
4. Submit PR with description of changes
