
--------------------------------------------------------------------------------
-- 2) (as schema owner) - DDL changes
-------------------------------------------------------------------------------
-- 2.1 Add TYPE to NOTIFICATIONS (if not present)
DROP TABLE NOTIFICATIONS CASCADE CONSTRAINTS;

CREATE TABLE NOTIFICATIONS (
  NOTIF_ID          NUMBER(12)        PRIMARY KEY,
  USER_ID           NUMBER(10)        NOT NULL,
  TASK_ID           NUMBER(10),
  MESSAGE           VARCHAR2(4000)    NOT NULL,
  IS_READ           CHAR(1)           DEFAULT 'N' CHECK (IS_READ IN ('Y','N')),
  CREATED_AT        TIMESTAMP         DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT FK_NOTIF_USER FOREIGN KEY (USER_ID) REFERENCES USERS(USER_ID),
  CONSTRAINT FK_NOTIF_TASK FOREIGN KEY (TASK_ID) REFERENCES TASKS(TASK_ID) ON DELETE CASCADE
);

ALTER TABLE NOTIFICATIONS ADD TYPE VARCHAR2(30);
ALTER TABLE NOTIFICATIONS ADD CONSTRAINT CK_NOTIF_TYPE
  CHECK (TYPE IN ('DUE_SOON_3D','DEADLINE_PASSED','OVERDUE'));




-- Optional: prevent duplicates
-- CREATE UNIQUE INDEX UX_NOTIF_UNIQ ON NOTIFICATIONS (TASK_ID, USER_ID, TYPE);

--------------------------------------------------------------------------------
-- 3) Procedures
--------------------------------------------------------------------------------

-- 3.1 Due soon (3 days left)
CREATE OR REPLACE PROCEDURE PRC_RAISE_DUE_SOON_NOTIF AS
BEGIN
  INSERT INTO NOTIFICATIONS (NOTIF_ID, USER_ID, TASK_ID, MESSAGE, TYPE, IS_READ, CREATED_AT)
  SELECT NOTIFICATIONS_SEQ.NEXTVAL,
         P.OWNER_ID,  -- Use project owner instead of ASSIGNEE_ID
         T.TASK_ID,
         'Task "' || T.TITLE || '" is due in 3 days (' || TO_CHAR(T.DUE_DATE, 'YYYY-MM-DD') || ')',
         'DUE_SOON_3D',
         'N',
         SYSTIMESTAMP
  FROM TASKS T
  JOIN PROJECTS P ON P.PROJECT_ID = T.PROJECT_ID
  WHERE T.DUE_DATE IS NOT NULL
    AND T.STATUS NOT IN ('DONE', 'CANCELLED')
    AND TRUNC(T.DUE_DATE) = TRUNC(SYSDATE) + 3
    AND NOT EXISTS (
      SELECT 1 
      FROM NOTIFICATIONS N
      WHERE N.TASK_ID = T.TASK_ID
        AND N.TYPE = 'DUE_SOON_3D'
    );
END;
/

-- 3.2 Deadline passed

CREATE OR REPLACE PROCEDURE PRC_RAISE_DEADLINE_NOTIF AS
BEGIN
  INSERT INTO NOTIFICATIONS (NOTIF_ID, USER_ID, TASK_ID, MESSAGE, TYPE, IS_READ, CREATED_AT)
  SELECT NOTIFICATIONS_SEQ.NEXTVAL,
       P.OWNER_ID, 
       T.TASK_ID,
       ' Task "' || T.TITLE || '" deadline has passed (' || TO_CHAR(T.DUE_DATE, 'YYYY-MM-DD') || ')',
       'DEADLINE_PASSED',
       'N',
       SYSTIMESTAMP
FROM TASKS T
JOIN PROJECTS P ON P.PROJECT_ID = T.PROJECT_ID
WHERE T.DUE_DATE IS NOT NULL
  AND T.STATUS NOT IN ('DONE', 'CANCELLED')
  AND TRUNC(T.DUE_DATE) = TRUNC(SYSDATE)
  AND NOT EXISTS (
    SELECT 1 FROM NOTIFICATIONS N
    WHERE N.TASK_ID = T.TASK_ID
      AND N.TYPE = 'DEADLINE_PASSED'
  );
END;
/

SHOW ERRORS;

-- 3.3 Overdue (modify existing to add TYPE)
drop procedure PRC_RAISE_OVERDUE_NOTIFICATIONS

CREATE OR REPLACE PROCEDURE PRC_RAISE_OVERDUE_NOTIFICATIONS AS
BEGIN
  INSERT INTO NOTIFICATIONS (NOTIF_ID, USER_ID, TASK_ID, MESSAGE, TYPE, IS_READ, CREATED_AT)
  SELECT NOTIFICATIONS_SEQ.NEXTVAL,
         P.OWNER_ID,  -- Always use the project owner instead of ASSIGNEE_ID
         T.TASK_ID,
         'Task "' || T.TITLE || '" is overdue by ' || (TRUNC(SYSDATE) - TRUNC(T.DUE_DATE)) || ' day(s).',
         'OVERDUE',
         'N',
         SYSTIMESTAMP
  FROM TASKS T
  JOIN PROJECTS P ON P.PROJECT_ID = T.PROJECT_ID
  WHERE T.DUE_DATE IS NOT NULL
    AND T.STATUS NOT IN ('DONE', 'CANCELLED')
    AND T.DUE_DATE < SYSDATE
    AND NOT EXISTS (
      SELECT 1 FROM NOTIFICATIONS N
      WHERE N.TASK_ID = T.TASK_ID
        AND N.TYPE = 'OVERDUE'
    );
END;
/


SHOW ERRORS;

--------------------------------------------------------------------------------
-- 4) Scheduler (run all procs daily at 09:00)
--------------------------------------------------------------------------------
BEGIN
  DBMS_SCHEDULER.CREATE_JOB (
    job_name        => 'JOB_GEN_NOTIFS_DAILY',
    job_type        => 'PLSQL_BLOCK',
    job_action      => 'BEGIN PRC_RAISE_DUE_SOON_NOTIF; PRC_RAISE_DEADLINE_NOTIF; PRC_RAISE_OVERDUE_NOTIFICATIONS; END;',
    start_date      => SYSTIMESTAMP,
    repeat_interval => 'FREQ=DAILY;BYHOUR=9;BYMINUTE=0;BYSECOND=0',
    enabled         => TRUE
  );
END;
/

--------------------------------------------------------------------------------
-- 5) Backfill CREATED_BY for existing tasks
--------------------------------------------------------------------------------
UPDATE TASKS T
SET T.PROJECT_ID = (SELECT P.OWNER_ID FROM PROJECTS P WHERE P.PROJECT_ID = T.PROJECT_ID)
WHERE T.PROJECT_ID IS NULL
  AND T.STATUS NOT IN ('DONE', 'CANCELLED');
COMMIT;

drop procedure PRC_RAISE_OVERDUE_NOTIFICATIONS

CREATE OR REPLACE PROCEDURE PRC_RAISE_OVERDUE_NOTIFICATIONS AS
BEGIN
  INSERT INTO NOTIFICATIONS (NOTIF_ID, USER_ID, TASK_ID, MESSAGE, TYPE, IS_READ, CREATED_AT)
  SELECT NOTIFICATIONS_SEQ.NEXTVAL,
         P.OWNER_ID,  -- Always use the project owner instead of ASSIGNEE_ID
         T.TASK_ID,
         'Task "' || T.TITLE || '" is overdue by ' || (TRUNC(SYSDATE) - TRUNC(T.DUE_DATE)) || ' day(s).',
         'OVERDUE',
         'N',
         SYSTIMESTAMP
  FROM TASKS T
  JOIN PROJECTS P ON P.PROJECT_ID = T.PROJECT_ID
  WHERE T.DUE_DATE IS NOT NULL
    AND T.STATUS NOT IN ('DONE', 'CANCELLED')
    AND T.DUE_DATE < SYSDATE
    AND NOT EXISTS (
      SELECT 1 FROM NOTIFICATIONS N
      WHERE N.TASK_ID = T.TASK_ID
        AND N.TYPE = 'OVERDUE'
    );
END;
/

