# ARTS database design

ARTS stores its data in Cloud Firestore (project `arts-477f0`). The Android app and the website read and write the same collections. Each collection below behaves like a table: every document has a unique id (its primary key), and documents point to each other by id (a foreign key).

## Relationships

```
users (id) 1 ----< incidents (reporterId)
users (id) 1 ----< alerts      (stored under users/{id}/alerts)
users (id) 1 ---- 1 security   (stored under users/{id}/private/security)
users (id) 1 ---- 0..1 admins  (admins/{id}, created in the console only)

incidents (id) 1 ----< dispatchLog (incidentId)
contractedServices (id) 1 ----< dispatchLog (serviceId)
incidents (forwardedTo) >----< contractedServices (many-to-many, list of service ids)
incidents (id) 1 ----< notifications (incidentId)
```

## Collections

### users  (primary key: Firebase Authentication uid)

| Field | Type | Notes |
|---|---|---|
| fullName | string | |
| email | string | locked, comes from sign-in |
| phone | string | |
| createdAt | string | ISO-8601 UTC timestamp |
| safetyScore | number | |
| avatarDataUrl | string or null | profile photo, compressed data URL |
| settings | map | theme and language |
| idNumber, age, gender, bloodType, weightKg, heightCm | string or null | emergency medical profile |
| allergiesOrConditions, chronicConditions, currentMedications | string or null | |
| medicalAidProvider, medicalAidNumber, preferredHospital | string or null | |
| emergencyContactName, emergencyContactPhone | string or null | |

Subcollections: `alerts` (messages for the app's Alerts screen: title, message, type, read, createdAt) and `private/security` (totpEnabled, totpSecret). Only the owner can read `private`.

### incidents  (primary key: document id)

| Field | Type | Notes |
|---|---|---|
| title, type, description | string | type is Collision, Pedestrian Incident, Breakdown or Emergency |
| priority | string | High, Medium or Low |
| status | string | Pending, Forwarded or Resolved |
| location | string | text address |
| latitude, longitude | number or null | pinned location |
| photoUrl | string | compressed data URL, empty if none |
| reporterId | string | foreign key to users |
| reporterName | string | |
| createdAt, forwardedAt, resolvedAt | string or null | ISO-8601 UTC |
| forwardedTo | array of string | foreign keys to contractedServices |
| autoForwarded, panic | boolean | set by triage and the panic button |
| resolvedBy | string or null | |

### contractedServices  (primary key: document id)

name, serviceType (Ambulance, Police, Fire, Traffic, Towing, Hospital and so on), phone, email, address, notes, active (boolean), createdAt.

### dispatchLog  (primary key: document id)

incidentId (foreign key), serviceId (foreign key), serviceName, forwardedBy (an administrator uid, or `ai_triage`), forwardedAt, notes.

### notifications  (primary key: document id)

The administrators' inbox. type, incidentId (foreign key), message, read (boolean), createdAt.

### admins  (primary key: uid)

A document here makes that account an administrator. It can only be created in the Firebase console, never by the app.

## Who can read and write what

The rules are in `firestore.rules`. In short: everyone must be signed in; a person reads only their own profile and incidents; administrators read everything and manage incidents, services and users; two-factor secrets are readable only by their owner.

## Sample data

Administrators can press **Load demo data** on the website dashboard to fill `users`, `contractedServices`, `incidents`, `dispatchLog` and `notifications` with ten records each.
