CREATE OR REPLACE TRIGGER TRG_AUTO_COMPLETE_PROJECT
AFTER INSERT OR UPDATE OF STATUS ON TASKS
FOR EACH ROW
DECLARE
    v_total_tasks NUMBER;
    v_completed_tasks NUMBER;
    v_current_status VARCHAR2(20);
BEGIN
    IF INSERTING OR (:OLD.STATUS != :NEW.STATUS) THEN

        SELECT STATUS INTO v_current_status
        FROM PROJECTS
        WHERE PROJECT_ID = :NEW.PROJECT_ID;

        SELECT COUNT(*),
               SUM(CASE WHEN STATUS = 'DONE' THEN 1 ELSE 0 END)
        INTO v_total_tasks, v_completed_tasks
        FROM TASKS
        WHERE PROJECT_ID = :NEW.PROJECT_ID;

        -- Auto-complete project - ALWAYS set progress to 100%
        IF v_total_tasks > 0 AND v_completed_tasks = v_total_tasks
           AND v_current_status IN ('ACTIVE', 'PLANNING', 'ON_HOLD') THEN

            UPDATE PROJECTS
            SET STATUS = 'COMPLETED',
                PROGRESS_PCT = 100
            WHERE PROJECT_ID = :NEW.PROJECT_ID;

        -- For all other cases, calculate progress normally
        ELSE
            UPDATE PROJECTS
            SET PROGRESS_PCT = CASE
                                 WHEN v_total_tasks = 0 THEN 0
                                 ELSE ROUND((v_completed_tasks * 100.0) / v_total_tasks, 1)
                               END
            WHERE PROJECT_ID = :NEW.PROJECT_ID;
        END IF;
    END IF;

EXCEPTION
    WHEN OTHERS THEN NULL;
END;
/

COMMIT;

-- Test procedure to manually check project completion
CREATE OR REPLACE PROCEDURE TEST_AUTO_COMPLETE(p_project_id IN NUMBER) AS
    v_total NUMBER;
    v_done NUMBER;
    v_status VARCHAR2(20);
BEGIN
    SELECT STATUS INTO v_status FROM PROJECTS WHERE PROJECT_ID = p_project_id;
    SELECT COUNT(*), SUM(CASE WHEN STATUS = 'DONE' THEN 1 ELSE 0 END)
    INTO v_total, v_done
    FROM TASKS WHERE PROJECT_ID = p_project_id;

    DBMS_OUTPUT.PUT_LINE('Project ' || p_project_id || ':');
    DBMS_OUTPUT.PUT_LINE('  Current Status: ' || v_status);
    DBMS_OUTPUT.PUT_LINE('  Total Tasks: ' || v_total);
    DBMS_OUTPUT.PUT_LINE('  Completed Tasks: ' || v_done);
    DBMS_OUTPUT.PUT_LINE('  Should Auto-Complete: ' ||
        CASE WHEN v_total > 0 AND v_done = v_total THEN 'YES' ELSE 'NO' END);
END;
/
