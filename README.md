# Subscription Tracker For Managing Online Services

A full-stack application to track, manage, and control auto-pay subscriptions from multiple payment platforms using AI-powered detection.

## Features

- 🔐 **Secure Authentication** - JWT-based login/signup
- 💳 **Payment Integration** - Connect PhonePe, Google Pay, and UPI accounts (simulated OAuth2)
- 🤖 **AI Subscription Detection** - Automatically detect recurring subscriptions from transaction history
- 📊 **Dashboard** - View all subscriptions, monthly spending, and upcoming renewals
- 🔔 **Notifications** - Get reminders before subscription renewals
- 🔒 **Security** - Encrypted sensitive data, OAuth2 for third-party integrations
- 🎨 **Modern UI** - Responsive design with Tailwind CSS, similar to fintech apps

## Tech Stack

- **Backend**: Spring Boot 3.2, MySQL, Spring Security, JWT
- **Frontend**: React 18, Vite, Tailwind CSS, Axios
- **AI Service**: Python FastAPI for subscription pattern detection
- **Database**: MySQL

## Prerequisites

- Java 17+
- Node.js 18+
- Python 3.9+
- MySQL 8.0+

## Setup Instructions

### 1. Database Setup

Create MySQL database:

```sql
CREATE DATABASE subscription_tracker;
```

Update `backend/src/main/resources/application.properties` with your MySQL credentials:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 2. Backend Setup

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

Backend runs on `http://localhost:8081`

### 3. AI Service Setup (Optional)

```bash
cd ai-service
pip install -r requirements.txt
python main.py
```

AI service runs on `http://localhost:8000`

**Note**: The app works without the AI service, but subscription detection from transactions won't be available.

### 4. Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`

## Quick Start Scripts

### Windows

**Backend:**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Frontend:**
```powershell
cd frontend
npm install
npm run dev
```

**AI Service:**
```powershell
cd ai-service
pip install -r requirements.txt
python main.py
```

## Usage

1. **Sign Up**: Create an account at `http://localhost:5173/signup`
2. **Connect Payment Account**: Click "Connect Payment" and select your provider (simulated)
3. **Add Subscriptions**: Manually add subscriptions or let AI detect them from transactions
4. **View Dashboard**: See all subscriptions, monthly expenses, and upcoming renewals
5. **Notifications**: Get reminders 3 days before renewals

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Login

### Subscriptions
- `GET /api/subscriptions` - Get all subscriptions
- `POST /api/subscriptions` - Create subscription
- `PUT /api/subscriptions/{id}` - Update subscription
- `DELETE /api/subscriptions/{id}` - Delete subscription
- `GET /api/subscriptions/dashboard` - Get dashboard data

### Payment Integration
- `POST /api/payments/connect` - Connect payment account
- `GET /api/payments/accounts` - Get connected accounts
- `POST /api/payments/sync/{accountId}` - Sync transactions
- `POST /api/payments/import` - Import transactions manually

### Notifications
- `GET /api/notifications` - Get notifications
- `GET /api/notifications/unread-count` - Get unread count
- `PUT /api/notifications/{id}/read` - Mark as read

## Environment Variables

### Backend
Set `ENCRYPTION_KEY` environment variable for production (defaults to dev key).

### Frontend
Create `frontend/.env`:
```
VITE_API_URL=http://localhost:8081/api
```

## Security Notes

- **Development**: Uses default encryption key. Change in production!
- **OAuth2**: Payment integrations are simulated. In production, implement real OAuth2 flows.
- **JWT**: Tokens expire after 24 hours (configurable).
- **CORS**: Configured for localhost. Update for production.

## Project Structure

```
Subscription Tracker/
├── backend/              # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/subscriptiontracker/
│   │   │   │       ├── config/      # Security, CORS config
│   │   │   │       ├── controller/ # REST controllers
│   │   │   │       ├── dto/        # Data transfer objects
│   │   │   │       ├── entity/     # JPA entities
│   │   │   │       ├── repository/ # JPA repositories
│   │   │   │       ├── security/   # JWT, UserDetails
│   │   │   │       ├── service/    # Business logic
│   │   │   │       └── util/       # Utilities (encryption)
│   │   │   └── resources/
│   │   │       └── application.properties
│   └── pom.xml
├── frontend/             # React frontend
│   ├── src/
│   │   ├── components/  # React components
│   │   ├── context/     # Auth context
│   │   ├── pages/       # Page components
│   │   ├── services/    # API service
│   │   └── index.css    # Tailwind CSS
│   └── package.json
└── ai-service/          # Python FastAPI AI service
    ├── main.py          # AI detection logic
    └── requirements.txt
```

## Troubleshooting

**Backend won't start:**
- Check MySQL is running
- Verify database credentials in `application.properties`
- Check port 8081 is available

**Frontend can't connect:**
- Ensure backend is running on port 8081
- Check `frontend/.env` has correct API URL
- Verify CORS settings in backend

**AI detection not working:**
- Ensure AI service is running on port 8000
- Check backend logs for connection errors
- AI service is optional - app works without it


