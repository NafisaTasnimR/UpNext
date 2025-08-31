ALTER TABLE ACTIVITY_LOGS DROP CONSTRAINT FK_LOG_PROJ;
ALTER TABLE ACTIVITY_LOGS
  ADD CONSTRAINT FK_LOG_PROJ
  FOREIGN KEY (PROJECT_ID) REFERENCES PROJECTS(PROJECT_ID)
  ON DELETE SET NULL;

ALTER TABLE ACTIVITY_LOGS DROP CONSTRAINT FK_LOG_TASK;
ALTER TABLE ACTIVITY_LOGS
  ADD CONSTRAINT FK_LOG_TASK
  FOREIGN KEY (TASK_ID) REFERENCES TASKS(TASK_ID)
  ON DELETE SET NULL;



-- Drop old row-level audit trigger if present
BEGIN
  EXECUTE IMMEDIATE 'DROP TRIGGER TRG_TASKS_AUD';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE <> -4080 THEN RAISE; END IF;  -- ignore "does not exist"
END;
/

-- Create compound trigger that logs AFTER STATEMENT (no mutating)
CREATE OR REPLACE TRIGGER TRG_TASKS_AUD
FOR INSERT OR UPDATE OR DELETE ON TASKS
COMPOUND TRIGGER
  TYPE t_row IS RECORD (
    action      VARCHAR2(6),
    project_id  PROJECTS.PROJECT_ID%TYPE,
    task_id     TASKS.TASK_ID%TYPE
  );
  TYPE t_tab IS TABLE OF t_row INDEX BY PLS_INTEGER;

  g_rows t_tab;
  g_idx  PLS_INTEGER := 0;

  BEFORE STATEMENT IS
  BEGIN
    g_rows.DELETE;
    g_idx := 0;
  END BEFORE STATEMENT;

  AFTER EACH ROW IS
  BEGIN
    g_idx := g_idx + 1;
    IF INSERTING THEN
      g_rows(g_idx).action     := 'INSERT';
      g_rows(g_idx).project_id := :NEW.PROJECT_ID;
      g_rows(g_idx).task_id    := :NEW.TASK_ID;
    ELSIF UPDATING THEN
      g_rows(g_idx).action     := 'UPDATE';
      g_rows(g_idx).project_id := :NEW.PROJECT_ID;
      g_rows(g_idx).task_id    := :NEW.TASK_ID;
    ELSIF DELETING THEN
      g_rows(g_idx).action     := 'DELETE';
      g_rows(g_idx).project_id := :OLD.PROJECT_ID;
      g_rows(g_idx).task_id    := :OLD.TASK_ID;
    END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
  BEGIN
    FOR i IN 1..g_idx LOOP
      INSERT INTO ACTIVITY_LOGS
        (ENTITY_TYPE, ENTITY_ID, ACTION, PROJECT_ID, TASK_ID, DETAILS, PERFORMED_BY, OCCURRED_AT)
      VALUES
        ('TASK',
         g_rows(i).task_id,
         g_rows(i).action,
         g_rows(i).project_id,
         g_rows(i).task_id,
         NULL,
         /* if you set CLIENT_IDENTIFIER per request, it’ll appear here; else fall back to schema user */
         NVL(SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER'), USER),
         SYSTIMESTAMP);
    END LOOP;
  END AFTER STATEMENT;
END;
/

---------------------------- hello
-------------------------------------------------------------------------------
-- 0) Make ACTIVITY_LOGS FKs tolerant of deletes (keep history)
-------------------------------------------------------------------------------
ALTER TABLE ACTIVITY_LOGS DROP CONSTRAINT FK_LOG_PROJ;
ALTER TABLE ACTIVITY_LOGS
  ADD CONSTRAINT FK_LOG_PROJ
  FOREIGN KEY (PROJECT_ID) REFERENCES PROJECTS(PROJECT_ID)
  ON DELETE SET NULL;

ALTER TABLE ACTIVITY_LOGS DROP CONSTRAINT FK_LOG_TASK;
ALTER TABLE ACTIVITY_LOGS
  ADD CONSTRAINT FK_LOG_TASK
  FOREIGN KEY (TASK_ID) REFERENCES TASKS(TASK_ID)
  ON DELETE SET NULL;

-------------------------------------------------------------------------------
-- 1) PROJECTS audit -> COMPOUND trigger, log AFTER STATEMENT
-------------------------------------------------------------------------------
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_PROJECTS_AUD';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -4080 THEN RAISE; END IF; END;
/

CREATE OR REPLACE TRIGGER TRG_PROJECTS_AUD
FOR INSERT OR UPDATE OR DELETE ON PROJECTS
COMPOUND TRIGGER
  TYPE t_row IS RECORD (
    action     VARCHAR2(6),
    project_id PROJECTS.PROJECT_ID%TYPE
  );
  TYPE t_tab IS TABLE OF t_row INDEX BY PLS_INTEGER;
  g t_tab; n PLS_INTEGER := 0;

  BEFORE STATEMENT IS
  BEGIN n := 0; g.DELETE; END BEFORE STATEMENT;

  AFTER EACH ROW IS
  BEGIN
    n := n + 1;
    IF INSERTING THEN
      g(n).action     := 'INSERT';
      g(n).project_id := :NEW.PROJECT_ID;
    ELSIF UPDATING THEN
      g(n).action     := 'UPDATE';
      g(n).project_id := :NEW.PROJECT_ID;
    ELSIF DELETING THEN
      g(n).action     := 'DELETE';
      g(n).project_id := :OLD.PROJECT_ID;
    END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
  BEGIN
    FOR i IN 1..n LOOP
      INSERT INTO ACTIVITY_LOGS
        (ENTITY_TYPE, ENTITY_ID, ACTION,
         PROJECT_ID, TASK_ID,
         DETAILS, PERFORMED_BY, OCCURRED_AT)
      VALUES
        ('PROJECT',
         g(i).project_id,
         g(i).action,
         CASE WHEN g(i).action = 'DELETE' THEN NULL ELSE g(i).project_id END,
         NULL,
         NULL,
         NVL(SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER'), USER),
         SYSTIMESTAMP);
    END LOOP;
  END AFTER STATEMENT;
END;
/

-------------------------------------------------------------------------------
-- 2) TASKS audit -> COMPOUND trigger, log AFTER STATEMENT
-------------------------------------------------------------------------------
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_TASKS_AUD';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -4080 THEN RAISE; END IF; END;
/

CREATE OR REPLACE TRIGGER TRG_TASKS_AUD
FOR INSERT OR UPDATE OR DELETE ON TASKS
COMPOUND TRIGGER
  TYPE t_row IS RECORD (
    action     VARCHAR2(6),
    project_id PROJECTS.PROJECT_ID%TYPE,
    task_id    TASKS.TASK_ID%TYPE
  );
  TYPE t_tab IS TABLE OF t_row INDEX BY PLS_INTEGER;
  g t_tab; n PLS_INTEGER := 0;

  BEFORE STATEMENT IS
  BEGIN n := 0; g.DELETE; END BEFORE STATEMENT;

  AFTER EACH ROW IS
  BEGIN
    n := n + 1;
    IF INSERTING THEN
      g(n).action     := 'INSERT';
      g(n).project_id := :NEW.PROJECT_ID;
      g(n).task_id    := :NEW.TASK_ID;
    ELSIF UPDATING THEN
      g(n).action     := 'UPDATE';
      g(n).project_id := :NEW.PROJECT_ID;
      g(n).task_id    := :NEW.TASK_ID;
    ELSIF DELETING THEN
      g(n).action     := 'DELETE';
      g(n).project_id := :OLD.PROJECT_ID;
      g(n).task_id    := :OLD.TASK_ID;
    END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
  BEGIN
    FOR i IN 1..n LOOP
      INSERT INTO ACTIVITY_LOGS
        (ENTITY_TYPE, ENTITY_ID, ACTION,
         PROJECT_ID, TASK_ID,
         DETAILS, PERFORMED_BY, OCCURRED_AT)
      VALUES
        ('TASK',
         g(i).task_id,
         g(i).action,
         CASE WHEN g(i).action = 'DELETE' THEN NULL ELSE g(i).project_id END,
         CASE WHEN g(i).action = 'DELETE' THEN NULL ELSE g(i).task_id    END,
         NULL,
         NVL(SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER'), USER),
         SYSTIMESTAMP);
    END LOOP;
  END AFTER STATEMENT;
END;
/

-------------------------------------------------------------------------------
-- 3) PROJECT_MEMBERS audit -> COMPOUND trigger, log AFTER STATEMENT
-------------------------------------------------------------------------------
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_PM_AUD';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -4080 THEN RAISE; END IF; END;
/

CREATE OR REPLACE TRIGGER TRG_PM_AUD
FOR INSERT OR UPDATE OR DELETE ON PROJECT_MEMBERS
COMPOUND TRIGGER
  TYPE t_row IS RECORD (
    action     VARCHAR2(6),
    pm_id      PROJECT_MEMBERS.PROJECT_MEMBER_ID%TYPE,
    project_id PROJECTS.PROJECT_ID%TYPE
  );
  TYPE t_tab IS TABLE OF t_row INDEX BY PLS_INTEGER;
  g t_tab; n PLS_INTEGER := 0;

  BEFORE STATEMENT IS
  BEGIN n := 0; g.DELETE; END BEFORE STATEMENT;

  AFTER EACH ROW IS
  BEGIN
    n := n + 1;
    IF INSERTING THEN
      g(n).action     := 'INSERT';
      g(n).pm_id      := :NEW.PROJECT_MEMBER_ID;
      g(n).project_id := :NEW.PROJECT_ID;
    ELSIF UPDATING THEN
      g(n).action     := 'UPDATE';
      g(n).pm_id      := :NEW.PROJECT_MEMBER_ID;
      g(n).project_id := :NEW.PROJECT_ID;
    ELSIF DELETING THEN
      g(n).action     := 'DELETE';
      g(n).pm_id      := :OLD.PROJECT_MEMBER_ID;
      g(n).project_id := :OLD.PROJECT_ID;
    END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
  BEGIN
    FOR i IN 1..n LOOP
      INSERT INTO ACTIVITY_LOGS
        (ENTITY_TYPE, ENTITY_ID, ACTION,
         PROJECT_ID, TASK_ID,
         DETAILS, PERFORMED_BY, OCCURRED_AT)
      VALUES
        ('PROJECT_MEMBER',
         g(i).pm_id,
         g(i).action,
         CASE WHEN g(i).action = 'DELETE' THEN NULL ELSE g(i).project_id END,
         NULL,
         NULL,
         NVL(SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER'), USER),
         SYSTIMESTAMP);
    END LOOP;
  END AFTER STATEMENT;
END;
/

-------------------------------------------------------------------------------
-- 4) TASK_DEPENDENCIES audit -> COMPOUND trigger, log AFTER STATEMENT
-------------------------------------------------------------------------------
BEGIN EXECUTE IMMEDIATE 'DROP TRIGGER TRG_DEP_AUD';
EXCEPTION WHEN OTHERS THEN IF SQLCODE <> -4080 THEN RAISE; END IF; END;
/

CREATE OR REPLACE TRIGGER TRG_DEP_AUD
FOR INSERT OR UPDATE OR DELETE ON TASK_DEPENDENCIES
COMPOUND TRIGGER
  TYPE t_row IS RECORD (
    action   VARCHAR2(6),
    dep_id   TASK_DEPENDENCIES.DEP_ID%TYPE,
    succ_id  TASKS.TASK_ID%TYPE
  );
  TYPE t_tab IS TABLE OF t_row INDEX BY PLS_INTEGER;
  g t_tab; n PLS_INTEGER := 0;

  BEFORE STATEMENT IS
  BEGIN n := 0; g.DELETE; END BEFORE STATEMENT;

  AFTER EACH ROW IS
  BEGIN
    n := n + 1;
    IF INSERTING THEN
      g(n).action  := 'INSERT';
      g(n).dep_id  := :NEW.DEP_ID;
      g(n).succ_id := :NEW.SUCCESSOR_TASK_ID;
    ELSIF UPDATING THEN
      g(n).action  := 'UPDATE';
      g(n).dep_id  := :NEW.DEP_ID;
      g(n).succ_id := :NEW.SUCCESSOR_TASK_ID;
    ELSIF DELETING THEN
      g(n).action  := 'DELETE';
      g(n).dep_id  := :OLD.DEP_ID;
      g(n).succ_id := :OLD.SUCCESSOR_TASK_ID;
    END IF;
  END AFTER EACH ROW;

  AFTER STATEMENT IS
  BEGIN
    FOR i IN 1..n LOOP
      INSERT INTO ACTIVITY_LOGS
        (ENTITY_TYPE, ENTITY_ID, ACTION,
         PROJECT_ID, TASK_ID,
         DETAILS, PERFORMED_BY, OCCURRED_AT)
      VALUES
        ('TASK_DEPENDENCY',
         g(i).dep_id,
         g(i).action,
         NULL,
         CASE WHEN g(i).action = 'DELETE' THEN NULL ELSE g(i).succ_id END,
         NULL,
         NVL(SYS_CONTEXT('USERENV','CLIENT_IDENTIFIER'), USER),
         SYSTIMESTAMP);
    END LOOP;
  END AFTER STATEMENT;
END;
/

-- helps to see the trigger status
SELECT trigger_name, table_name, triggering_event, status
FROM user_triggers
ORDER BY table_name, trigger_name;




