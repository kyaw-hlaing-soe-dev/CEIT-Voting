# CEIT Voting Application

A secure, scalable web-based voting system built with Spring Boot for college/university elections. The application enables students to cast votes for candidates across multiple categories (King, Queen, Prince, Princess, Couple) while ensuring vote integrity through multi-factor identification and fraud prevention mechanisms.

---

## 📋 STAR Method Project Description (For Resume)

### **Situation**
The college's Computer Engineering and Information Technology department needed a secure, reliable, and user-friendly online voting system for annual student elections with multiple award categories. The existing manual voting process was time-consuming, prone to errors, and lacked transparency. Additionally, concerns about vote manipulation and duplicate voting required a robust technical solution that could handle concurrent users while maintaining data integrity.

### **Task**
Designed and developed a full-stack web application to digitize the voting process, ensuring:
- Secure voter authentication using PIN-based access with multi-factor device identification
- Prevention of duplicate voting through cookie-based tracking, IP address monitoring, and session management
- Real-time vote counting with database-level integrity constraints
- Admin dashboard for election management, result monitoring, and audit trails
- Scalable architecture capable of handling concurrent voting sessions during peak election hours
- Cloud-ready deployment with containerization support

### **Action**
- **Backend Development**: Built RESTful APIs using **Spring Boot 3.2** with **Java 21**, implementing clean architecture patterns with separation of concerns across controllers, services, repositories, and DTOs
- **Database Design**: Designed a normalized **PostgreSQL** database schema with advanced features including database triggers for automatic vote count synchronization, unique constraints for fraud prevention, and audit logging tables
- **Security Implementation**: Developed custom security filters (`DeviceCookieFilter`) for device fingerprinting, rate limiting service for abuse prevention, and IP-based vote restriction logic
- **Vote Integrity**: Implemented transactional vote submission with `READ_COMMITTED` isolation level, paired category validation (King/Queen, Prince/Princess), and database-level conflict detection
- **Caching & Performance**: Integrated **Caffeine cache** for high-performance candidate and result caching with automatic cache eviction on vote updates
- **Frontend Development**: Created responsive voting UI using **Thymeleaf** templates with category-specific selection pages and success confirmation flows
- **DevOps & Deployment**: Containerized application using multi-stage **Docker** builds, configured **Spring Boot Actuator** with Prometheus metrics for monitoring, and implemented health checks for container orchestration
- **Testing**: Wrote unit tests and concurrency tests (`VotingServiceConcurrencyTest`) to validate vote integrity under parallel execution

### **Result**
- Delivered a production-ready voting system designed to handle **concurrent voting sessions** with robust duplicate vote prevention
- Significantly reduced election processing time through automated vote counting and real-time results
- Ensured high availability through containerized deployment with health monitoring and graceful recovery
- Enabled transparent, real-time result viewing for administrators with comprehensive audit trails
- Built a maintainable, well-documented codebase with clear separation of concerns and test coverage

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.2, Spring Data JPA, Spring MVC |
| **Database** | PostgreSQL (with triggers & constraints) |
| **Caching** | Caffeine Cache, Spring Cache Abstraction |
| **Frontend** | Thymeleaf, HTML5, CSS3, JavaScript |
| **Monitoring** | Spring Boot Actuator, Micrometer, Prometheus |
| **Containerization** | Docker (Multi-stage builds) |
| **Testing** | JUnit 5, Spring Boot Test, H2 (in-memory) |

---

## ✨ Key Features

- **🔐 Secure Authentication**: PIN-based voter verification with device fingerprinting
- **🛡️ Fraud Prevention**: Multi-layer protection using cookies, IP tracking, and database constraints
- **📊 Real-time Results**: Live vote counting with cached result queries
- **👑 Multiple Categories**: Support for King, Queen, Prince, Princess, and Couple voting
- **🔗 Paired Voting Rules**: Business logic preventing same candidate selection across paired categories
- **📱 Responsive UI**: Mobile-friendly voting interface
- **👨‍💼 Admin Dashboard**: Candidate management, result viewing, and audit logs
- **📈 Monitoring**: Built-in health checks and metrics endpoints
- **🐳 Container-Ready**: Production-optimized Docker image with JVM tuning

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- PostgreSQL 15+
- Maven 3.9+

### Run Locally
```bash
# Clone repository
git clone https://github.com/Zephyrus-not-available/CEIT-Voting.git
cd CEIT-Voting

# Set up database
psql -U postgres -f db.sql

# Run application
./mvnw spring-boot:run
```

### Run with Docker
```bash
docker-compose up --build
```

---

## 📁 Project Structure

```
src/main/java/com/KTU/KTUVotingapp/
├── config/          # Configuration classes (Cache, Security, Web)
├── controller/      # REST & MVC Controllers
├── dto/             # Data Transfer Objects
├── exception/       # Custom exception handlers
├── model/           # JPA Entity classes
├── repository/      # Spring Data repositories
└── service/         # Business logic layer
```

---

## 📝 License

This project is developed for educational purposes.

---

## 👤 Author

Developed for CEIT Department Elections
