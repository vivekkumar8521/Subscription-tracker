# Quick Start Guide

## Prerequisites Check

✅ Java 17+ installed (`java -version`)
✅ Node.js 18+ installed (`node -v`)
✅ Python 3.9+ installed (`python --version`)
✅ MySQL 8.0+ running

## Step-by-Step Setup

### 1. Database Setup (One-time)

```sql
CREATE DATABASE subscription_tracker;
```

Update `backend/src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### 2. Start Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Wait for: `Started SubscriptionTrackerApplication`

### 3. Start AI Service (Optional)

```powershell
cd ai-service
pip install -r requirements.txt
python main.py
```

### 4. Start Frontend

```powershell
cd frontend
npm install
npm run dev
```

### 5. Open Browser

Go to: `http://localhost:5173`

## First Steps

1. **Sign Up** - Create your account
2. **Add Subscription** - Click "+ Add Subscription"
3. **Connect Payment** - Click "💳 Connect Payment" (simulated)
4. **View Dashboard** - See your subscriptions and expenses

## Troubleshooting

**Port 8081 already in use:**
- Change port in `backend/src/main/resources/application.properties`
- Update `frontend/.env` with new port

**MySQL connection error:**
- Check MySQL is running: `mysql -u root -p`
- Verify credentials in `application.properties`

**Frontend can't connect:**
- Ensure backend is running
- Check browser console for errors
- Verify `frontend/.env` exists

## Production Notes

- Set `ENCRYPTION_KEY` environment variable
- Use real OAuth2 for payment integrations
- Configure production CORS origins
- Use HTTPS
- Set strong JWT secret
