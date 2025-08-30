package org.example.upnext.tools;

import org.example.upnext.dao.impl.*;
import org.example.upnext.model.*;
import org.example.upnext.service.AuthService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/** Run once to seed demo data. */
public class SetupData {

    public static void main(String[] args) throws Exception {
        var userDAO = new UserDAOImpl();
        var projectDAO = new ProjectDAOImpl();
        var taskDAO = new TaskDAOImpl();
        var depDAO = new TaskDependencyDAOImpl();

        // 1) Ensure demo user
        var auth = new AuthService(userDAO);
        long demoUserId = ensureUser(auth, "admin", "admin@example.com", "admin123", "ADMIN");

        // 2) Ensure demo project
        long projectId = ensureProject(projectDAO, demoUserId, "UpNext Demo Project",
                "This is a seeded project to try UpNext features.");

        // 3) Seed a couple of tasks + hierarchy
        long taskPlanId = ensureTask(taskDAO, projectId, null, "Plan MVP",
                "Plan features, schema, and UI", "HIGH", "IN_PROGRESS", 40, LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), demoUserId);

        long taskBuildId = ensureTask(taskDAO, projectId, null, "Build MVP",
                "Implement DAO, services, UI", "CRITICAL", "TODO", 0, LocalDate.now(), LocalDate.now().plusDays(10), demoUserId);

        long subtaskDaoId = ensureTask(taskDAO, projectId, taskBuildId, "DAO Layer",
                "JDBC DAOs for all entities", "HIGH", "TODO", 0, LocalDate.now(), LocalDate.now().plusDays(4), demoUserId);

        long subtaskUiId = ensureTask(taskDAO, projectId, taskBuildId, "JavaFX UI",
                "FXML and controllers", "HIGH", "TODO", 0, LocalDate.now(), LocalDate.now().plusDays(7), demoUserId);

        // 4) Add dependency: Build MVP depends on Plan MVP
        ensureDependency(depDAO, taskPlanId, taskBuildId);

        System.out.println("Seed complete.");
        System.out.println("Login with username: admin   password: admin123");
    }

    private static long ensureUser(AuthService auth, String username, String email, String passwordPlain, String role) throws SQLException, IllegalAccessException {
        var uDao = ((UserDAOImpl)((java.lang.reflect.Field)(getField(auth, "userDAO"))).get(auth)); // little reflection trick to reuse dao
        Optional<User> existing = uDao.findByUsername(username);
        if (existing.isPresent()) return existing.get().getUserId();

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setGlobalRole(role);
        u.setStatus("ACTIVE");
        return auth.register(u, passwordPlain);
    }

    private static long ensureProject(ProjectDAOImpl projectDAO, long ownerId, String name, String desc) throws SQLException {
        // try by owner and name
        for (Project p : projectDAO.findByOwner(ownerId)) {
            if (name.equalsIgnoreCase(p.getName())) return p.getProjectId();
        }
        Project p = new Project();
        p.setName(name);
        p.setDescription(desc);
        p.setOwnerId(ownerId);
        p.setStartDate(LocalDate.now().minusDays(3));
        p.setEndDate(LocalDate.now().plusDays(14));
        p.setStatus("ACTIVE");
        return projectDAO.create(p);
    }

    private static long ensureTask(TaskDAOImpl taskDAO, long projectId, Long parentId, String title, String desc,
                                   String priority, String status, double progress, LocalDate start, LocalDate due, Long assigneeId) throws SQLException {
        // naive check: if a task with same title exists in project, return it
        for (Task t : taskDAO.findByProject(projectId)) {
            if (title.equalsIgnoreCase(t.getTitle())) return t.getTaskId();
        }
        Task t = new Task(projectId, title);
        t.setDescription(desc);
        t.setPriority(priority);
        t.setStatus(status);
        t.setProgressPct(progress);
        t.setStartDate(start);
        t.setDueDate(due);
        t.setAssigneeId(assigneeId);
        t.setParentTaskId(parentId);
        return taskDAO.create(t);
    }

    private static void ensureDependency(TaskDependencyDAOImpl depDAO, long pred, long succ) throws SQLException {
        for (TaskDependency d : depDAO.findForSuccessor(succ)) {
            if (d.getPredecessorTaskId() == pred) return;
        }
        TaskDependency d = new TaskDependency(pred, succ);
        depDAO.create(d);
    }

    private static java.lang.reflect.Field getField(Object obj, String name) {
        try {
            var f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
