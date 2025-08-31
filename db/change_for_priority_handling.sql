CREATE OR REPLACE FUNCTION PRIORITY_VALUE(priority_str IN VARCHAR2) RETURN NUMBER IS
BEGIN
    RETURN CASE UPPER(priority_str)
        WHEN 'CRITICAL' THEN 4
        WHEN 'HIGH'     THEN 3
        WHEN 'MEDIUM'   THEN 2
        WHEN 'LOW'      THEN 1
        ELSE 0
    END;
END PRIORITY_VALUE;
/

CREATE OR REPLACE TRIGGER TRG_CHECK_PRIORITY_DEPS
BEFORE UPDATE OF STATUS ON TASKS
FOR EACH ROW
DECLARE
    v_higher_priority_unfinished NUMBER;
    v_task_priority_value NUMBER;
BEGIN
    -- Only check when changing to IN_PROGRESS
    IF :NEW.STATUS = 'IN_PROGRESS' AND :OLD.STATUS != 'IN_PROGRESS' THEN
        -- Get current task's priority value
        v_task_priority_value := PRIORITY_VALUE(:NEW.PRIORITY);
        
        -- Check if there are any unfinished tasks with higher priority under the same parent
        SELECT COUNT(*)
        INTO v_higher_priority_unfinished
        FROM TASKS
        WHERE PARENT_TASK_ID = :NEW.PARENT_TASK_ID
          AND TASK_ID != :NEW.TASK_ID
          AND STATUS NOT IN ('DONE', 'CANCELLED')
          AND PRIORITY_VALUE(PRIORITY) > v_task_priority_value;
        
        -- If higher priority tasks exist and are unfinished, prevent the update
        IF v_higher_priority_unfinished > 0 THEN
            RAISE_APPLICATION_ERROR(-20002, 
                'Cannot start lower priority task. There are ' || v_higher_priority_unfinished || 
                ' higher priority task(s) that must be completed first.');
        END IF;
    END IF;
END;
/


CREATE OR REPLACE TRIGGER TRG_CHECK_PRIORITY_DEPS
FOR UPDATE OF STATUS ON TASKS
COMPOUND TRIGGER

    TYPE task_rec IS RECORD (
        task_id NUMBER,
        parent_task_id NUMBER,
        priority VARCHAR2(10),
        new_status VARCHAR2(20),
        old_status VARCHAR2(20)
    );
    
    TYPE task_table IS TABLE OF task_rec;
    g_tasks task_table := task_table();
    
BEFORE EACH ROW IS
BEGIN
    IF :NEW.STATUS = 'IN_PROGRESS' AND :OLD.STATUS != 'IN_PROGRESS' THEN
        g_tasks.EXTEND;
        g_tasks(g_tasks.LAST) := task_rec(
            :NEW.TASK_ID,
            :NEW.PARENT_TASK_ID,
            :NEW.PRIORITY,
            :NEW.STATUS,
            :OLD.STATUS
        );
    END IF;
END BEFORE EACH ROW;

AFTER STATEMENT IS
    v_higher_priority_unfinished NUMBER;
    v_task_priority_value NUMBER;
BEGIN
    FOR i IN 1..g_tasks.COUNT LOOP
        v_task_priority_value := PRIORITY_VALUE(g_tasks(i).priority);
        
        SELECT COUNT(*)
        INTO v_higher_priority_unfinished
        FROM TASKS
        WHERE PARENT_TASK_ID = g_tasks(i).parent_task_id
          AND TASK_ID != g_tasks(i).task_id
          AND STATUS NOT IN ('DONE', 'CANCELLED')
          AND PRIORITY_VALUE(PRIORITY) > v_task_priority_value;
        
        IF v_higher_priority_unfinished > 0 THEN
            RAISE_APPLICATION_ERROR(-20002, 
                'Cannot start lower priority task. There are ' || v_higher_priority_unfinished || 
                ' higher priority task(s) that must be completed first.');
        END IF;
    END LOOP;
END AFTER STATEMENT;

END TRG_CHECK_PRIORITY_DEPS;
/



