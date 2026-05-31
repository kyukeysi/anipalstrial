# **AniPals Backend Development Roadmap**

## **Backend Goal**
Build a scalable multiplayer backend that supports:
- Authentication
- Farming automation
- Gacha systems
- Multiplayer trading
- Real-time updates
- Offline progression
- Weather events
- Raids

---

# **Recommended Backend Architecture**

```text
src/main/java/com/anipals
│
├── auth/
├── user/
├── anipal/
├── farm/
├── inventory/
├── gacha/
├── trade/
├── social/
├── weather/
├── raid/
├── common/
├── config/
└── websocket/
```

Each module should contain:

```text
module/
├── controller/
├── service/
├── repository/
├── dto/
├── entity/
├── mapper/
└── enums/
```

---

# **Backend Tech Stack**

## **Backend Framework**
- Java 21
- Spring Boot

## **Database**
- PostgreSQL

## **Authentication & Security**
- Spring Security
- JWT Authentication
- BCrypt Password Hashing

## **Libraries**
- Spring Data JPA
- Lombok
- Validation
- WebSocket

---

# **Development Priority Order**

## **Phase 1 — Foundation**
Build these first before gameplay systems.

### **1. Project Setup**
Dependencies:
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- Validation
- JWT Library
- WebSocket

### **Core Setup**
- `application.yml`
- Environment variables
- Database connection
- Global exception handler
- API response wrapper

---

## **Phase 2 — Authentication System**

### **Features**
- Register
- Login
- JWT Authentication
- Password Encryption
- Username Setup

### **Recommended Structure**

```text
auth/
├── controller/
│   └── AuthController
├── service/
│   ├── AuthService
│   └── JwtService
├── dto/
│   ├── LoginRequest
│   ├── RegisterRequest
│   └── AuthResponse
└── security/
    ├── JwtFilter
    └── SecurityConfig
```

### **Endpoints**

```http
POST /api/auth/register
POST /api/auth/login
```

---

# **Database Design**

## **User Entity**

### **Fields**
- id
- email
- password
- username
- uid
- createdAt
- tutorialCompleted

---

## **AniPal Entity**

### **Fields**
- id
- name
- rarity
- type
- baseEfficiency

---

## **PlayerAniPal Entity**

### **Fields**
- id
- playerId
- anipalId
- level
- stamina
- hunger

---

## **Farm Entity**

### **Fields**
- id
- playerId

---

## **FarmPlot Entity**

### **Fields**
- id
- farmId
- cropId
- state
- plantedAt
- readyAt

---

## **InventoryItem Entity**

### **Fields**
- id
- playerId
- itemId
- quantity

---

# **Core Gameplay Systems**

## **Tutorial System**

### **Tutorial States**
```text
INTRO
GACHA_PULL
FARMING
COMPLETE
```

### **Recommended Pattern**
- State Pattern

---

## **Farm System**

### **Core Features**
- Crop planting
- Crop timers
- Harvesting
- Offline progression
- AniPal automation

### **Recommended Services**
```text
FarmService
CropService
OfflineProgressService
```

### **Important Logic**
- Elapsed time calculation
- Offline farming simulation
- Reward generation

---

## **Inventory System**

### **Requirements**
- Stackable items
- Sorting
- Item validation
- Inventory capacity

---

# **Gacha System**

## **Core Mechanics**
- 5★ rate: **0.006%**
- Guaranteed 5★ at **75 pulls**
- Guaranteed 4★ every **10 pulls**
- 50/50 Premium System
- Separate AniPal and Tool banners

---

## **Recommended Services**

```text
GachaService
PityService
RewardService
BannerService
```

---

## **Example Pity Logic**

```java
pityCounter++;

if (pityCounter >= 75) {
    giveFiveStar();
}

if (lost5050) {
    guaranteedPremium = true;
}
```

---

## **Important Edge Cases**
Must handle:
- pity overflow
- invalid currency
- duplicate requests
- disconnected requests

---

# **Multiplayer Systems**

## **Friend System**

### **Features**
- Add via UID
- Accept/reject requests
- View profiles

---

## **Trading System**

### **Trade Flow**
```text
Player A Request
→ Player B Accept
→ Open Trade Room
→ Add Items
→ Both Confirm
→ Validate Ownership
→ Execute Trade
```

---

## **Critical Trading Rules**
Prevent:
- duplicated items
- race conditions
- invalid trades
- inventory desync

---

## **Transactional Safety**

```java
@Transactional
public void executeTrade() {
    // validate
    // remove items
    // transfer items
    // commit
}
```

---

# **Weather System**

## **Design Pattern**
Strategy Pattern

```java
public interface WeatherEffect {
    void applyEffect(Player player);
}
```

### **Implementations**
- RainEffect
- StormEffect
- HeatwaveEffect
- LuckyDayEffect

---

## **Responsibilities**
- Rotate weather globally
- Apply gameplay modifiers
- Synchronize clients
- Trigger UI updates

---

# **Raid System**

## **Raid Outcome Factors**
- AniPal stats
- Weather
- Randomness
- Cooldowns

---

## **Must Prevent**
- raid abuse
- spam attacks
- exploit farming

---

# **WebSocket Systems**

Use WebSocket for:
- Trading updates
- Notifications
- Messages
- Weather synchronization
- Live multiplayer interactions

---

# **Security Rules**

## **Authentication**
- JWT validation
- BCrypt password hashing
- Session verification

---

## **Validation**
Always validate:
- inventory ownership
- trade state
- raid cooldowns
- currency balance
- request authenticity

---

# **Recommended Backend Principles**

## **SOLID Principles**

### **Single Responsibility Principle**
Each service handles one domain only.

### **Open/Closed Principle**
Systems should be extendable without modifying existing logic.

### **Dependency Inversion Principle**
Controllers depend on interfaces instead of implementations.

---

## **GRASP Principles**
- Controller
- Information Expert
- Low Coupling
- High Cohesion
- Protected Variations

---

# **Design Patterns Used**

| Pattern | Usage |
|---|---|
| Strategy Pattern | Weather + Gacha behaviors |
| State Pattern | Tutorial + Crop growth |
| Factory Pattern | AniPal generation |
| Observer Pattern | Notifications + Real-time updates |
| Transaction Script | Trading system |

---

# **Recommended Development Milestones**

## **Milestone 1**
- Spring Boot setup
- PostgreSQL setup
- Authentication
- JWT implementation

---

## **Milestone 2**
- User profiles
- Tutorial system
- Farm system

---

## **Milestone 3**
- Inventory system
- AniPal system
- Offline rewards

---

## **Milestone 4**
- Gacha system

---

## **Milestone 5**
- Friends system
- Trading system
- Messaging system

---

## **Milestone 6**
- Weather system
- Mini-games
- Raid system
- WebSocket polish

---

# **Backend MVP Completion Definition**

AniPals backend MVP is complete when:
- Users can register/login
- JWT authentication works
- Tutorial flow functions correctly
- Farming loop works
- Offline progression functions
- Inventory persists properly
- Gacha system works correctly
- Trading system validates safely
- Multiplayer interaction exists
- Weather system rotates globally

---

# **Final Backend Philosophy**

The AniPals backend focuses on:
- modular architecture
- transactional safety
- multiplayer synchronization
- scalability
- maintainability
- clean separation of systems

The backend is designed to support:
> “An idle multiplayer ecosystem that always feels alive.”