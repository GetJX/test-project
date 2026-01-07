import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.json.*;

public class TaskTrackerCLI {
    
    private static final String TASKS_FILE = "tasks.json";
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 任务状态枚举
    public enum TaskStatus {
        TODO("todo"),
        IN_PROGRESS("in-progress"),
        DONE("done");
        
        private final String value;
        
        TaskStatus(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static TaskStatus fromString(String text) {
            for (TaskStatus status : TaskStatus.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return TODO;
        }
    }
    
    // 任务类
    public static class Task {
        private int id;
        private String description;
        private TaskStatus status;
        private String createdAt;
        private String updatedAt;
        
        public Task() {}
        
        public Task(int id, String description, TaskStatus status, 
                   String createdAt, String updatedAt) {
            this.id = id;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("description", description);
            json.put("status", status.getValue());
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            return json;
        }
        
        public static Task fromJSON(JSONObject json) {
            Task task = new Task();
            task.setId(json.getInt("id"));
            task.setDescription(json.getString("description"));
            task.setStatus(TaskStatus.fromString(json.getString("status")));
            task.setCreatedAt(json.getString("createdAt"));
            task.setUpdatedAt(json.getString("updatedAt"));
            return task;
        }
        
        @Override
        public String toString() {
            return String.format("[%d] %s - Status: %s (Created: %s, Updated: %s)",
                id, description, status.getValue(), createdAt, updatedAt);
        }
    }
    
    // JSON处理类
    public static class JSONHandler {
        
        public static List<Task> loadTasks() {
            List<Task> tasks = new ArrayList<>();
            
            try {
                if (!Files.exists(Paths.get(TASKS_FILE))) {
                    return tasks;
                }
                
                String content = new String(Files.readAllBytes(Paths.get(TASKS_FILE)));
                if (content.trim().isEmpty()) {
                    return tasks;
                }
                
                JSONArray jsonArray = new JSONArray(content);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject json = jsonArray.getJSONObject(i);
                    tasks.add(Task.fromJSON(json));
                }
                
            } catch (Exception e) {
                System.err.println("Error loading tasks: " + e.getMessage());
            }
            
            return tasks;
        }
        
        public static void saveTasks(List<Task> tasks) {
            try {
                JSONArray jsonArray = new JSONArray();
                for (Task task : tasks) {
                    jsonArray.put(task.toJSON());
                }
                
                Files.write(Paths.get(TASKS_FILE), 
                           jsonArray.toString(2).getBytes(),
                           StandardOpenOption.CREATE,
                           StandardOpenOption.TRUNCATE_EXISTING);
                
            } catch (Exception e) {
                System.err.println("Error saving tasks: " + e.getMessage());
            }
        }
    }
    
    // 任务管理器
    public static class TaskManager {
        
        public static String getCurrentDateTime() {
            return LocalDateTime.now().format(DATE_FORMATTER);
        }
        
        public static void addTask(String description) {
            List<Task> tasks = JSONHandler.loadTasks();
            
            // 生成新ID
            int newId = 1;
            if (!tasks.isEmpty()) {
                newId = tasks.stream()
                           .mapToInt(Task::getId)
                           .max()
                           .orElse(0) + 1;
            }
            
            // 创建新任务
            String now = getCurrentDateTime();
            Task task = new Task(newId, description, TaskStatus.TODO, now, now);
            tasks.add(task);
            
            // 保存
            JSONHandler.saveTasks(tasks);
            System.out.println("Task added successfully (ID: " + newId + ")");
        }
        
        public static void updateTask(int id, String newDescription) {
            List<Task> tasks = JSONHandler.loadTasks();
            boolean found = false;
            
            for (Task task : tasks) {
                if (task.getId() == id) {
                    task.setDescription(newDescription);
                    task.setUpdatedAt(getCurrentDateTime());
                    found = true;
                    break;
                }
            }
            
            if (found) {
                JSONHandler.saveTasks(tasks);
                System.out.println("Task updated successfully (ID: " + id + ")");
            } else {
                System.out.println("Task not found (ID: " + id + ")");
            }
        }
        
        public static void deleteTask(int id) {
            List<Task> tasks = JSONHandler.loadTasks();
            boolean removed = tasks.removeIf(task -> task.getId() == id);
            
            if (removed) {
                JSONHandler.saveTasks(tasks);
                System.out.println("Task deleted successfully (ID: " + id + ")");
            } else {
                System.out.println("Task not found (ID: " + id + ")");
            }
        }
        
        public static void markTask(int id, TaskStatus status) {
            List<Task> tasks = JSONHandler.loadTasks();
            boolean found = false;
            
            for (Task task : tasks) {
                if (task.getId() == id) {
                    task.setStatus(status);
                    task.setUpdatedAt(getCurrentDateTime());
                    found = true;
                    break;
                }
            }
            
            if (found) {
                JSONHandler.saveTasks(tasks);
                String statusText = status == TaskStatus.DONE ? "done" : 
                                   status == TaskStatus.IN_PROGRESS ? "in-progress" : "todo";
                System.out.println("Task marked as " + statusText + " (ID: " + id + ")");
            } else {
                System.out.println("Task not found (ID: " + id + ")");
            }
        }
        
        public static void listAllTasks() {
            List<Task> tasks = JSONHandler.loadTasks();
            
            if (tasks.isEmpty()) {
                System.out.println("No tasks found.");
                return;
            }
            
            System.out.println("All Tasks (" + tasks.size() + "):");
            System.out.println("=".repeat(80));
            for (Task task : tasks) {
                System.out.println(task);
            }
        }
        
        public static void listTasksByStatus(TaskStatus status) {
            List<Task> tasks = JSONHandler.loadTasks();
            List<Task> filteredTasks = new ArrayList<>();
            
            for (Task task : tasks) {
                if (task.getStatus() == status) {
                    filteredTasks.add(task);
                }
            }
            
            if (filteredTasks.isEmpty()) {
                String statusText = status == TaskStatus.DONE ? "done" : 
                                   status == TaskStatus.IN_PROGRESS ? "in-progress" : "todo";
                System.out.println("No " + statusText + " tasks found.");
                return;
            }
            
            String statusText = status == TaskStatus.DONE ? "Done" : 
                               status == TaskStatus.IN_PROGRESS ? "In Progress" : "Todo";
            System.out.println(statusText + " Tasks (" + filteredTasks.size() + "):");
            System.out.println("=".repeat(80));
            for (Task task : filteredTasks) {
                System.out.println(task);
            }
        }
    }
    
    // 主程序
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0].toLowerCase();
        
        try {
            switch (command) {
                case "add":
                    if (args.length < 2) {
                        System.out.println("Error: Missing task description");
                        System.out.println("Usage: task-cli add \"Task description\"");
                        return;
                    }
                    TaskManager.addTask(args[1]);
                    break;
                    
                case "update":
                    if (args.length < 3) {
                        System.out.println("Error: Missing task ID or description");
                        System.out.println("Usage: task-cli update <id> \"New description\"");
                        return;
                    }
                    int updateId = Integer.parseInt(args[1]);
                    TaskManager.updateTask(updateId, args[2]);
                    break;
                    
                case "delete":
                    if (args.length < 2) {
                        System.out.println("Error: Missing task ID");
                        System.out.println("Usage: task-cli delete <id>");
                        return;
                    }
                    int deleteId = Integer.parseInt(args[1]);
                    TaskManager.deleteTask(deleteId);
                    break;
                    
                case "mark-in-progress":
                    if (args.length < 2) {
                        System.out.println("Error: Missing task ID");
                        System.out.println("Usage: task-cli mark-in-progress <id>");
                        return;
                    }
                    int progressId = Integer.parseInt(args[1]);
                    TaskManager.markTask(progressId, TaskStatus.IN_PROGRESS);
                    break;
                    
                case "mark-done":
                    if (args.length < 2) {
                        System.out.println("Error: Missing task ID");
                        System.out.println("Usage: task-cli mark-done <id>");
                        return;
                    }
                    int doneId = Integer.parseInt(args[1]);
                    TaskManager.markTask(doneId, TaskStatus.DONE);
                    break;
                    
                case "list":
                    if (args.length == 1) {
                        TaskManager.listAllTasks();
                    } else {
                        String status = args[1].toLowerCase();
                        switch (status) {
                            case "done":
                                TaskManager.listTasksByStatus(TaskStatus.DONE);
                                break;
                            case "todo":
                                TaskManager.listTasksByStatus(TaskStatus.TODO);
                                break;
                            case "in-progress":
                                TaskManager.listTasksByStatus(TaskStatus.IN_PROGRESS);
                                break;
                            default:
                                System.out.println("Error: Invalid status. Use: done, todo, or in-progress");
                                System.out.println("Usage: task-cli list [done|todo|in-progress]");
                        }
                    }
                    break;
                    
                default:
                    System.out.println("Error: Unknown command '" + command + "'");
                    printUsage();
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid task ID. ID must be a number.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void printUsage() {
        System.out.println("Task Tracker CLI - A simple command-line task management tool");
        System.out.println("=".repeat(60));
        System.out.println("Usage:");
        System.out.println("  task-cli add \"Task description\"          - Add a new task");
        System.out.println("  task-cli update <id> \"New description\"   - Update a task");
        System.out.println("  task-cli delete <id>                     - Delete a task");
        System.out.println("  task-cli mark-in-progress <id>           - Mark task as in-progress");
        System.out.println("  task-cli mark-done <id>                  - Mark task as done");
        System.out.println("  task-cli list                            - List all tasks");
        System.out.println("  task-cli list done                       - List completed tasks");
        System.out.println("  task-cli list todo                       - List todo tasks");
        System.out.println("  task-cli list in-progress                - List in-progress tasks");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  task-cli add \"Buy groceries\"");
        System.out.println("  task-cli update 1 \"Buy groceries and cook dinner\"");
        System.out.println("  task-cli list");
        System.out.println("  task-cli mark-done 1");
    }
}

// JSON.java - 一个简单的JSON处理类（为了不依赖外部库）
class JSONObject {
    private Map<String, Object> map = new HashMap<>();
    
    public JSONObject() {}
    
    public JSONObject put(String key, Object value) {
        map.put(key, value);
        return this;
    }
    
    public int getInt(String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return 0;
    }
    
    public String getString(String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
    
    public boolean getBoolean(String key) {
        Object value = map.get(key);
        return value != null && Boolean.parseBoolean(value.toString());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            Object value = entry.getValue();
            
            if (value instanceof String) {
                sb.append("\"").append(escape(value.toString())).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof JSONObject) {
                sb.append(value.toString());
            } else if (value instanceof JSONArray) {
                sb.append(value.toString());
            } else {
                sb.append("\"").append(escape(value.toString())).append("\"");
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    public String toString(int indent) {
        return toString(); // 简单实现，不处理缩进
    }
    
    private String escape(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}

class JSONArray {
    private List<Object> list = new ArrayList<>();
    
    public JSONArray() {}
    
    public JSONArray put(Object value) {
        list.add(value);
        return this;
    }
    
    public int length() {
        return list.size();
    }
    
    public JSONObject getJSONObject(int index) {
        Object value = list.get(index);
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        }
        return new JSONObject();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        
        for (Object value : list) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            if (value instanceof String) {
                sb.append("\"").append(escape(value.toString())).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof JSONObject) {
                sb.append(value.toString());
            } else if (value instanceof JSONArray) {
                sb.append(value.toString());
            } else {
                sb.append("\"").append(escape(value.toString())).append("\"");
            }
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    public String toString(int indent) {
        return toString(); // 简单实现，不处理缩进
    }
    
    private String escape(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
}
