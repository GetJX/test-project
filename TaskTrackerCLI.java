package com.jxncqyl.abstractFactoryPattern02;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Task Tracker CLI - 命令行任务管理工具
 * 无外部依赖，纯Java实现
 */
public class TaskTrackerCLI {

    private static final String TASKS_FILE = "tasks.json";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== 主程序入口 ====================

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "add":
                    handleAdd(args);
                    break;
                case "update":
                    handleUpdate(args);
                    break;
                case "delete":
                    handleDelete(args);
                    break;
                case "mark-in-progress":
                    handleMark(args, "in-progress");
                    break;
                case "mark-done":
                    handleMark(args, "done");
                    break;
                case "list":
                    handleList(args);
                    break;
                case "help":
                    printHelp();
                    break;
                default:
                    System.out.println("Error: Unknown command '" + command + "'");
                    System.out.println("Use 'task-cli help' to see available commands.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid ID format. ID must be a number.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // ==================== 命令处理器 ====================

    private static void handleAdd(String[] args) {
        if (args.length < 2) {
            System.out.println("Error: Missing task description.");
            System.out.println("Usage: task-cli add \"Task description\"");
            return;
        }

        String description = args[1];
        List<Task> tasks = loadTasks();

        // 生成新ID
        int newId = tasks.stream()
                .mapToInt(t -> t.id)
                .max()
                .orElse(0) + 1;

        // 创建新任务
        String now = LocalDateTime.now().format(FORMATTER);
        Task task = new Task(newId, description, "todo", now, now);
        tasks.add(task);

        saveTasks(tasks);
        System.out.println("Task added successfully (ID: " + newId + ")");
    }

    private static void handleUpdate(String[] args) {
        if (args.length < 3) {
            System.out.println("Error: Missing ID or description.");
            System.out.println("Usage: task-cli update <id> \"New description\"");
            return;
        }

        int id = Integer.parseInt(args[1]);
        String newDescription = args[2];
        List<Task> tasks = loadTasks();

        Task task = findTaskById(tasks, id);
        if (task == null) {
            System.out.println("Error: Task with ID " + id + " not found.");
            return;
        }

        task.description = newDescription;
        task.updatedAt = LocalDateTime.now().format(FORMATTER);

        saveTasks(tasks);
        System.out.println("Task updated successfully (ID: " + id + ")");
    }

    private static void handleDelete(String[] args) {
        if (args.length < 2) {
            System.out.println("Error: Missing task ID.");
            System.out.println("Usage: task-cli delete <id>");
            return;
        }

        int id = Integer.parseInt(args[1]);
        List<Task> tasks = loadTasks();

        boolean removed = tasks.removeIf(t -> t.id == id);

        if (removed) {
            saveTasks(tasks);
            System.out.println("Task deleted successfully (ID: " + id + ")");
        } else {
            System.out.println("Error: Task with ID " + id + " not found.");
        }
    }

    private static void handleMark(String[] args, String status) {
        if (args.length < 2) {
            System.out.println("Error: Missing task ID.");
            System.out.println("Usage: task-cli " + args[0] + " <id>");
            return;
        }

        int id = Integer.parseInt(args[1]);
        List<Task> tasks = loadTasks();

        Task task = findTaskById(tasks, id);
        if (task == null) {
            System.out.println("Error: Task with ID " + id + " not found.");
            return;
        }

        task.status = status;
        task.updatedAt = LocalDateTime.now().format(FORMATTER);

        saveTasks(tasks);
        System.out.println("Task marked as " + status + " (ID: " + id + ")");
    }

    private static void handleList(String[] args) {
        List<Task> tasks = loadTasks();

        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        // 筛选
        String filter = args.length > 1 ? args[1].toLowerCase() : null;
        List<Task> filtered = new ArrayList<>();

        for (Task task : tasks) {
            if (filter == null || task.status.equals(filter)) {
                filtered.add(task);
            }
        }

        if (filtered.isEmpty()) {
            System.out.println("No " + (filter != null ? filter + " " : "") + "tasks found.");
            return;
        }

        // 输出
        String title = filter != null ?
                capitalize(filter) + " Tasks" : "All Tasks";
        System.out.println("\n" + title + " (" + filtered.size() + ")");
        System.out.println("─".repeat(70));
        System.out.printf("%-4s %-12s %-40s %s%n", "ID", "STATUS", "DESCRIPTION", "UPDATED");
        System.out.println("─".repeat(70));

        for (Task task : filtered) {
            String desc = task.description.length() > 38 ?
                    task.description.substring(0, 35) + "..." : task.description;
            System.out.printf("%-4d %-12s %-40s %s%n",
                    task.id,
                    "[" + task.status + "]",
                    desc,
                    task.updatedAt);
        }
        System.out.println();
    }

    // ==================== 任务类 ====================

    static class Task {
        int id;
        String description;
        String status;
        String createdAt;
        String updatedAt;

        Task(int id, String description, String status,
             String createdAt, String updatedAt) {
            this.id = id;
            this.description = description;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    // ==================== JSON 处理（无外部依赖）====================

    private static List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();

        try {
            Path path = Paths.get(TASKS_FILE);
            if (!Files.exists(path)) {
                return tasks;
            }

            String content = new String(Files.readAllBytes(path));
            content = content.trim();

            if (content.isEmpty() || content.equals("[]")) {
                return tasks;
            }

            // 解析 JSON 数组
            tasks = parseJsonArray(content);

        } catch (IOException e) {
            System.err.println("Warning: Could not load tasks file. Starting fresh.");
        }

        return tasks;
    }

    private static void saveTasks(List<Task> tasks) {
        try {
            StringBuilder json = new StringBuilder("[\n");

            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                json.append("  {\n");
                json.append("    \"id\": ").append(t.id).append(",\n");
                json.append("    \"description\": \"").append(escapeJson(t.description)).append("\",\n");
                json.append("    \"status\": \"").append(t.status).append("\",\n");
                json.append("    \"createdAt\": \"").append(t.createdAt).append("\",\n");
                json.append("    \"updatedAt\": \"").append(t.updatedAt).append("\"\n");
                json.append("  }");
                if (i < tasks.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("]");

            Files.write(Paths.get(TASKS_FILE), json.toString().getBytes());

        } catch (IOException e) {
            System.err.println("Error: Could not save tasks. " + e.getMessage());
        }
    }

    private static List<Task> parseJsonArray(String json) {
        List<Task> tasks = new ArrayList<>();

        // 移除外层方括号
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        // 分割对象
        int braceCount = 0;
        int start = -1;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '{') {
                if (braceCount == 0) start = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && start != -1) {
                    String objStr = json.substring(start, i + 1);
                    Task task = parseJsonObject(objStr);
                    if (task != null) {
                        tasks.add(task);
                    }
                    start = -1;
                }
            }
        }

        return tasks;
    }

    private static Task parseJsonObject(String json) {
        try {
            Map<String, String> map = new HashMap<>();

            // 移除花括号
            json = json.trim();
            if (json.startsWith("{")) json = json.substring(1);
            if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

            // 解析键值对
            String[] parts = json.split(",(?=\\s*\")");  // 在逗号处分割，但只在引号前的逗号

            for (String part : parts) {
                int colonIdx = part.indexOf(':');
                if (colonIdx == -1) continue;

                String key = part.substring(0, colonIdx).trim();
                String value = part.substring(colonIdx + 1).trim();

                // 移除引号
                key = removeQuotes(key);
                value = removeQuotes(value);
                value = unescapeJson(value);

                map.put(key, value);
            }

            return new Task(
                    Integer.parseInt(map.getOrDefault("id", "0")),
                    map.getOrDefault("description", ""),
                    map.getOrDefault("status", "todo"),
                    map.getOrDefault("createdAt", ""),
                    map.getOrDefault("updatedAt", "")
            );

        } catch (Exception e) {
            return null;
        }
    }

    private static String removeQuotes(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    // ==================== 工具方法 ====================

    private static Task findTaskById(List<Task> tasks, int id) {
        for (Task task : tasks) {
            if (task.id == id) {
                return task;
            }
        }
        return null;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static void printHelp() {
        System.out.println("""
            
            ╔══════════════════════════════════════════════════════════════╗
            ║              Task Tracker CLI - 任务跟踪工具                  ║
            ╚══════════════════════════════════════════════════════════════╝
            
            Usage: java TaskTrackerCLI <command> [arguments]
            
            Commands:
              add <description>           添加新任务
              update <id> <description>   更新任务描述
              delete <id>                 删除任务
              mark-in-progress <id>       标记任务为"进行中"
              mark-done <id>              标记任务为"已完成"
              list                        列出所有任务
              list todo                   列出待办任务
              list in-progress            列出进行中的任务
              list done                   列出已完成的任务
              help                        显示帮助信息
            
            Examples:
              java TaskTrackerCLI add "Buy groceries"
              java TaskTrackerCLI update 1 "Buy groceries and cook dinner"
              java TaskTrackerCLI mark-in-progress 1
              java TaskTrackerCLI mark-done 1
              java TaskTrackerCLI list
              java TaskTrackerCLI list done
              java TaskTrackerCLI delete 1
            
            """);
    }
}
