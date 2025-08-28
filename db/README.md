# DB Setup (Oracle) for This Project

This folder contains versioned SQL scripts so every teammate can reproduce the **same tables and seed data**.

## What you need
- Oracle Database (e.g., XE 21c) running and reachable
- A user/schema to work in (example below uses user `UPNEXT`)
- A SQL client (SQL*Plus or SQL Developer)

## 1) (Optional) Create the schema user `UPNEXT`
If you don't already have a working schema, ask your DBA or, on a local XE:

```sql
-- As a privileged account (e.g., SYS as SYSDBA), adjust tablespace names as needed:
CREATE USER UPNEXT IDENTIFIED BY myPass QUOTA UNLIMITED ON users;
GRANT CREATE SESSION, CREATE TABLE TO UPNEXT;
```

> If you already have a schema, skip this and use your own username.

## 2) Run the migration scripts
Connect as your application user (e.g., `UPNEXT`) and run:

**SQL*Plus**
```sql
sqlplus EX/myPass@//localhost:1521/XEPDB1
@db/V1__reset_and_create_flower.sql
@db/V2__seed_flower.sql
SELECT * FROM flower;
```

**SQL Developer**
- Open a worksheet connected as `UPNEXT`
- Run `@db/V1_the_file_name_you_want_to_run.sql`
- Run `@db/V2_the_file_name_you_want_to_run.sql`
- Check with: `SELECT * FROM flower;`

## 3) Configure the Java app
In your Java app, set the same connection details:
- Host: `localhost`
- Port: `1522`
- Service: `XEPDB1`
- Username: `UPNEXT`
- Password: `myPass`

If your schema or service differs, update the values accordingly.

## Notes
- sql file description will be here.
- Scripts are deliberately split into **create** and **seed** steps.
- `V1__...` safely drops and recreates the table (idempotent for local dev).
- `V2__...` inserts seed rows and commits them.
- You can add more scripts as the schema grows: `V3__add_column_x.sql`, etc.
