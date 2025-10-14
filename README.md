# Task Tracker - Microservices Application

A modern, backend task management application built with microservices architecture. This application allows users to create projects, organize tasks in customizable columns, and manage their workflow efficiently.

## 🏗️ Architecture

This application follows a microservices architecture with the following components:

### Backend Services
- **Authentication Service** (Port 8081): Handles user registration, login, and JWT token management
- **Task Tracker Service** (Port 8082): Manages projects, task states (columns), and tasks
- **Gateway Service** (Port 8080): API Gateway for routing requests to appropriate services
- **Notification Service** (Port 8083): Handles notifications via Kafka messaging

### Infrastructure
- **PostgreSQL Databases**: Separate databases for authentication and task tracking
- **Apache Kafka**: Message broker for event-driven communication
- **Docker & Docker Compose**: Containerization and orchestration

## 🚀 Features

### Authentication & User Management
- User registration and login
- JWT-based authentication
- Secure password handling
- Session management

### Project Management
- Create, read, update, and delete projects
- Project search and filtering
- User-specific project isolation

### Task Management
- Create, edit, and delete tasks
- Task descriptions and metadata

### Real-time Features
- Event-driven architecture with Kafka
- Automatic notifications for user events

## 🛠️ Technology Stack

### Backend
- **Java 17** with Spring Boot
- **Spring Security** for authentication
- **Spring Data JPA** for data persistence
- **PostgreSQL** for data storage
- **Apache Kafka** for messaging
- **Maven** for dependency management

### DevOps
- **Docker** for containerization
- **Docker Compose** for orchestration

## 📋 Prerequisites

Before running the application, ensure you have the following installed:

- **Docker** (version 20.10 or higher)
- **Docker Compose** (version 2.0 or higher)
- **Git** (for cloning the repository)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd tracker-demo
```

### 2. Start the Application
```bash
# Create executable jar files
./mvnw clean package

# Start all services with Docker Compose
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

### 3. Access the Application
- **API Gateway**: http://localhost:8080
- **Authentication Service**: http://localhost:8081
- **Task Tracker Service**: http://localhost:8082
## 🔧 Development

### Running Individual Services

#### Backend Services
```bash
# Authentication Service
cd authentication-service
mvn spring-boot:run

# Task Tracker Service
cd task-tracker
mvn spring-boot:run

# Gateway Service
cd gateway-service
mvn spring-boot:run
```

### Database Access
- **Authentication DB**: localhost:5433 (user: postgres, password: postgres, db: authentication)
- **Task Tracker DB**: localhost:5434 (user: postgres, password: postgres, db: task-tracker)

### Kafka Management
- **Kafka**: localhost:9092
- **Zookeeper**: localhost:2181

## 📚 API Documentation

### Authentication Service

#### POST /auth/signup
Register a new user.
```json
{
  "username": "string",
  "email": "string",
  "password": "string"
}
```

#### POST /auth/signin
Authenticate user and receive JWT token.
```json
{
  "username": "string",
  "password": "string"
}
```

### Task Tracker Service

All endpoints require `X-Username` header and `Authorization: Bearer <token>` header.

#### Projects
- `GET /api/projects` - List user projects
- `POST /api/projects` - Create new project
- `PATCH /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project

#### Task States (Columns)
- `GET /api/projects/{projectId}/task-states` - List task states
- `POST /api/projects/{projectId}/task-states` - Create task state
- `PATCH /api/task-states/{id}` - Update task state
- `PATCH /api/task-states/{task_state_id}/positions/change` - Change task state position
- `DELETE /api/task-states/{id}` - Delete task state

#### Tasks
- `GET /api/task-states/{taskStateId}/tasks` - List tasks in state
- `POST /api/task-states/{taskStateId}/tasks` - Create task
- `PATCH /api/tasks/{id}` - Update task
- `PATCH /api/tasks/{task_id}/positions/change` - Change task position
- `DELETE /api/tasks/{id}` - Delete task

## 🐳 Docker Configuration

### Services Overview
- **gateway-service**: API Gateway
- **authentication-service**: User management
- **task-tracker**: Task and project management
- **notification-service**: Event handling
- **authentication-db**: PostgreSQL for auth data
- **task-tracker-db**: PostgreSQL for task data
- **kafka**: Message broker
- **zookeeper**: Kafka coordination

## 🔒 Security Features

- JWT-based authentication
- Password encryption using BCrypt
- Input validation and sanitization
- SQL injection prevention through JPA

## 🚨 Troubleshooting

### Common Issues

#### Services Not Starting
```bash
# Check service logs
docker-compose logs <service-name>

# Restart specific service
docker-compose restart <service-name>
```

#### Database Connection Issues
```bash
# Check database status
docker-compose ps

# Restart databases
docker-compose restart authentication-db task-tracker-db
```

### Logs
```bash
# View all logs
docker-compose logs

# View specific service logs
docker-compose logs authentication-service
docker-compose logs task-tracker
```

## 🔄 Data Flow

1. **User Authentication**: Gateway → Authentication Service → JWT Token
2. **Project Management**: Gateway → Task Tracker Service → Database
3. **Task Operations**: Gateway → Task Tracker Service → Database
4. **Event Notifications**: Services → Kafka → Notification Service

## 🧪 Testing

### Manual Testing
1. Test user registration and login
2. Create and manage projects
3. Add task states and tasks
4. Test CRUD operations
5. Verify responsive design on different screen sizes

### API Testing
Use tools like Postman or curl to test API endpoints:
```bash
# Test authentication
curl -X POST http://localhost:8081/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"password123"}'
```

## 📈 Performance Considerations

- **Database Indexing**: Optimized queries for user data
- **Connection Pooling**: Efficient database connections

## 🔮 Future Enhancements

- Real-time collaboration features
- Advanced task filtering and search
- File attachments for tasks
- Web application
- Mobile application
- Advanced analytics and reporting
- Integration with external tools (Slack, GitHub, etc.)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📞 Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the API documentation

---

**Happy Task Tracking! 🎯**