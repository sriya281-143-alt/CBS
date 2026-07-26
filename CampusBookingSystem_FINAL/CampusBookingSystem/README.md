# Campus Resource & Study Space Booking System (CRSSBS)

JavaFX desktop application for ITS66704 Advanced Programming — Part B (Development).
Simplified, student-friendly architecture: flat model classes, plain CSV files, and
direct Manager classes (no service layer, no serialization, no logging framework).

## How to run

```bash
mvn clean javafx:run
```

On first launch the app seeds demo data automatically and creates a `data/` folder
next to the project with `users.csv`, `resources.csv`, and `bookings.csv`. Delete the
`data/` folder to reset to a fresh demo state. Every change (booking, approval,
resource edit, password reset) is written straight back to the relevant CSV file
immediately, so nothing is lost even if the app is closed unexpectedly.

## Demo login credentials (login is by EMAIL)

| Role    | Email             | Password    |
|---------|-------------------|-------------|
| Student | aisha@student.edu | student123  |
| Student | james@student.edu | student123  |
| Staff   | rahul@staff.edu   | staff123    |
| Admin   | safir@admin.edu   | admin123    |

Role is detected automatically after login — Students/Staff land on the Resource
Listing page, Admins land on the Admin Dashboard. "Forgot Password?" on the login
page looks the account up by email and shows the password back (plain-text CSV,
no hashing — kept intentionally simple for a student project).

## Project structure

```
model/       User, Resource, Booking        - plain flat classes, no inheritance
manager/     FileManager                    - generic CSV line read/write
             UserManager                    - users.csv + login/reset
             ResourceManager                - resources.csv + search/filter
             BookingManager                 - bookings.csv + validation rules
exception/   BookingException               - booking conflicts/invalid duration/unavailable
             UnauthorizedAccessException    - role/permission checks
util/        Session                        - one static field holding the logged-in user
controller/  LoginController, ResourcesController, BookingController, AdminDashboardController
```

Each Manager loads its CSV file in its constructor and saves immediately after every
change, so there's no shared "app context" object to keep in sync — each screen just
creates its own Manager instances and they all read/write the same files.

## UI notes

- The window is resizable (maximize/minimize/drag-resize); resource and stat cards
  are wrapped in a `FlowPane` so they reflow instead of overflowing.
- Top bar on every page: page title on the left, logged-in user's name on the right
  (matches the prototype in the Part A report). Sidebar keeps the "Campus" branding.

## OOP concept mapping (for your Part B report)

| Concept | Where |
|---|---|
| Encapsulation | All model fields are `private` with getters/setters |
| Custom exceptions | `BookingException`, `UnauthorizedAccessException` |
| Collections | `ArrayList`/`List` in every Manager class |
| File I/O | `FileManager` — `Files.readAllLines` / `Files.write` for CSV |
| GUI/Event handling | JavaFX FXML + `@FXML` handlers across all four controllers |

Note: this simplified version intentionally does not use inheritance/polymorphism or
generics in the model layer, since the goal was a flatter, easier-to-explain codebase.
If your rubric rewards those concepts, mention them as a deliberate design trade-off
in your report, or ask for the inheritance-based version back.

## Still worth doing before submission

- Add a couple of JUnit tests for `BookingManager` (duration/double-booking rules).
- Take screenshots of each screen + a booking/approval flow for the report.
