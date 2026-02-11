# Scientific Products Management Platform

A comprehensive web application for managing scientific products and academic research built with Angular 17 and Spring Boot 3.

## Features

- **User Authentication**: JWT-based authentication with role-based access control
- **Publication Management**: CRUD operations for scientific publications with DOI integration and impact factors
- **Conference Management**: Track conference presentations and proceedings
- **Technology Transfer**: Manage technology transfer activities and patents
- **Intellectual Property**: Track patents, trademarks, and other IP assets
- **Thesis Students**: Manage graduate students and their thesis projects
- **Project Management**: Academic project tracking with funding information
- **Researcher Management**: Search and manage researchers by name, RUT, or ORCID
- **Catalog Management**: Manage various catalogs (journals, participation types, product types, etc.)
- **DOI Integration**: Automatic metadata fetching from DOI
- **Corresponding Author**: Track corresponding authors for publications
- **Responsive UI**: Modern Material Design interface with card and list views
- **Real-time Updates**: Live data synchronization
- **Internationalization**: Multi-language support with database-driven translations
- **Cache Management**: In-memory caching for catalogs and texts with Caffeine Cache for improved performance

## Technology Stack

### Frontend
- Angular 17 (Standalone Components)
- Angular Material
- TypeScript
- **Node.js 18.13+** (Required for Angular 17)
- RxJS
- SCSS

### Backend
- Spring Boot 3.2
- Spring Security with JWT
- Spring Data JPA
- Spring Cache with Caffeine
- H2 Database (Development)
- Maven
- JPA with Hibernate

## Prerequisites

- Node.js 18+ and npm
- Java 17+
- Maven 3.6+

## Installation & Setup

### 1. Clone the repository
```bash
git clone <repository-url>
cd scientific-products-platform
```

### 2. Install Frontend Dependencies
```bash
npm install
```

### 3. Backend Setup
The backend is already configured with Maven. No additional setup required.

## Running the Application

### Development Mode (Both Frontend and Backend)

**Start Backend:**
```bash
cd backend
mvn spring-boot:run
```

**Start Frontend (in another terminal):**
```bash
ng serve
```

### Access URLs

- **Frontend**: `http://localhost:4200`
- **Backend API**: `http://localhost:8081/sigic2.0/api`
- **H2 Console**: `http://localhost:8081/sigic2.0/h2-console`

## Default Credentials

The application comes with pre-configured users:

- **Admin User**: 
  - Username: `admin`
  - Password: `admin`
  - Role: Administrator

- **Regular User**:
  - Username: `user`
  - Password: `user`
  - Role: User

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/auth/me` - Get current user info

### Publications
- `GET /api/publications` - Get all publications
- `POST /api/publications` - Create new publication
- `GET /api/publications/{id}` - Get publication by ID
- `PUT /api/publications/{id}` - Update publication
- `DELETE /api/publications/{id}` - Delete publication

### Projects
- `GET /api/projects` - Get all projects
- `GET /api/projects/{codigo}` - Get project by code
- `GET /api/projects/search?q={query}` - Search projects
- `GET /api/projects/active` - Get active projects
- `GET /api/projects/stats` - Get project statistics

### Catalogs
- `GET /api/catalogs/{type}` - Get catalog items by type
- `GET /api/catalogs/{type}/{id}` - Get specific catalog item
- `POST /api/catalogs/{type}` - Create catalog item
- `PUT /api/catalogs/{type}/{id}` - Update catalog item
- `DELETE /api/catalogs/{type}/{id}` - Delete catalog item

### Journals
- `GET /api/journals` - Get all journals
- `GET /api/journals/{id}` - Get journal by ID

### Cache Management
- `POST /api/admin/cache/catalogos/evict` - Invalidate catalog cache
- `POST /api/admin/cache/textos/evict` - Invalidate texts cache
- `POST /api/admin/cache/evictAll` - Invalidate all caches
- `GET /api/admin/cache/status` - Get cache status

## Cache Configuration

The application uses Caffeine Cache for in-memory caching of frequently accessed data:

- **Catalog Cache**: Caches catalog queries (journals, participation types, product types, etc.)
- **Texts Cache**: Caches text/translation lookups by code and language

Cache settings are configured in `application.yml` and can be adjusted based on your needs:
```yaml
cache:
  catalogos:
    max-size: 1000        # Maximum entries in catalog cache
    ttl-minutes: 30      # Time to live in minutes
  textos:
    max-size: 5000       # Maximum entries in texts cache
    ttl-minutes: 60      # Time to live in minutes
```

The cache automatically evicts entries after the configured TTL, and can be manually invalidated via the admin endpoints. The cache is designed to work alongside an older administration application that shares the same database. When the older app modifies catalog or text data, it should call the cache invalidation endpoints to maintain data consistency.

## Database

The application uses H2 in-memory database for development. You can access the H2 console at:

**H2 Console Access:**
- **URL:** `http://localhost:8081/sigic2.0/h2-console`
- **JDBC URL:** `jdbc:h2:mem:testdb`
- **Username:** `sa`
- **Password:** `password`

> **Note:** The H2 console runs on the same port as the backend (8081) with the context path `/sigic2.0/h2-console`. Make sure the backend is running before accessing the console.

**Alternative Database Tools:**
If you encounter iframe issues with H2 Console, you can use external tools like:
- **DBeaver** (Free): Connect using H2 (Embedded) with path `mem:testdb`
- **IntelliJ IDEA**: Database tool window with H2 connection
- **VS Code**: SQLTools extension with H2 support

## Project Structure

```
├── src/                          # Angular frontend
│   ├── app/
│   │   ├── core/                 # Core services, guards, interceptors
│   │   │   ├── guards/           # Route guards
│   │   │   ├── interceptors/     # HTTP interceptors
│   │   │   ├── models/           # TypeScript interfaces and DTOs
│   │   │   └── services/         # Core services
│   │   ├── features/             # Feature modules
│   │   │   ├── auth/             # Authentication (login, register)
│   │   │   └── dashboard/        # Dashboard
│   │   ├── modules/              # Main application modules
│   │   │   ├── publications/     # Publication management
│   │   │   ├── conferences/      # Conference management
│   │   │   ├── technology-transfer/ # Technology transfer
│   │   │   ├── intellectual-property/ # IP management
│   │   │   ├── thesis-students/  # Thesis student management
│   │   │   ├── projects/         # Project management
│   │   │   ├── researchers/      # Researcher management
│   │   │   └── catalogs/         # Catalog management
│   │   └── shared/               # Shared components
│   │       └── components/       # Reusable components
│   └── environments/             # Environment configurations
├── backend/                      # Spring Boot backend
│   ├── src/main/java/com/sisgic/
│   │   ├── config/               # Configuration classes (including CacheConfig)
│   │   ├── controller/           # REST controllers
│   │   │   └── admin/            # Admin controllers (cache management)
│   │   ├── dto/                  # Data Transfer Objects
│   │   ├── entity/               # JPA entities
│   │   ├── repository/           # Data repositories
│   │   ├── security/             # Security configuration
│   │   └── service/              # Business logic services (with caching)
│   └── src/main/resources/       # Application properties
└── README.md
```

## Key Features

### Publication Management
- Full CRUD operations for scientific publications
- DOI integration for automatic metadata fetching
- Impact factor tracking
- Author management with corresponding author designation
- Journal integration with ISSN and abbreviation support

### Project Management
- Project tracking with funding information
- Start and end date management
- External code integration (ANID codes)
- Financing type categorization
- Collaboration tracking
- Product association counting

### Catalog System
- Dynamic catalog management for various types
- Internationalization support with database-driven translations
- Read-only catalogs (Journals, Participation Types, Product Types, Researcher Types)
- Full CRUD catalogs for other types
- In-memory caching for improved performance
- Cache invalidation endpoints for external applications

### Cache Management
The application implements an in-memory caching system using Caffeine Cache to optimize access to catalog and text data. This is particularly useful when the application shares a database with an older administration application.

**Features:**
- Automatic caching of catalog queries (journals, participation types, etc.)
- Automatic caching of text/translation lookups
- Configurable TTL (Time To Live) and maximum cache size
- Cache invalidation endpoints for external applications
- Automatic cache eviction on data modifications

**Configuration:**
Cache settings can be configured in `application.yml`:
```yaml
cache:
  catalogos:
    max-size: 1000        # Maximum entries in catalog cache
    ttl-minutes: 30      # Time to live in minutes
  textos:
    max-size: 5000       # Maximum entries in texts cache
    ttl-minutes: 60      # Time to live in minutes
```

**Integration with External Applications:**
When the older administration application modifies catalog or text tables, it should call the cache invalidation endpoints to ensure data consistency:
- After modifying catalogs: `POST /sigic2.0/api/admin/cache/catalogos/evict`
- After modifying texts: `POST /sigic2.0/api/admin/cache/textos/evict`
- For bulk changes: `POST /sigic2.0/api/admin/cache/evictAll`

### User Interface
- Material Design components
- Responsive design for desktop and mobile
- Card and list view modes
- Search and filtering capabilities
- Real-time data updates

## Development

### Adding New Features

1. Create feature module in `src/app/modules/`
2. Add routes in the feature's routing file
3. Create components using Angular CLI: `ng generate component modules/feature-name/component-name`
4. Implement backend endpoints in appropriate controller
5. Add corresponding service methods
6. Update navigation in `app.routes.ts`

### Code Style

- Follow Angular style guide
- Use TypeScript strict mode
- Implement proper error handling
- Add unit tests for critical functionality
- Use consistent naming conventions

## Security

- JWT tokens for authentication
- Role-based access control
- CORS configuration for cross-origin requests
- Input validation on both frontend and backend
- SQL injection protection through JPA
- Password visibility toggle in login form

## Deployment

### Production Build

**Frontend:**
```bash
ng build --configuration production
```

**Backend:**
```bash
cd backend
mvn clean package
java -jar target/scientific-products-platform-0.0.1-SNAPSHOT.jar
```

### Environment Configuration

- **Development**: `src/environments/environment.ts`
- **Production**: `src/environments/environment.prod.ts`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For support and questions, please contact the development team or create an issue in the repository.