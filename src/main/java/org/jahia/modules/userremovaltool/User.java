package org.jahia.modules.userremovaltool;

public class User {
    private String name;
    private String path;
    private String type;

    public User(String name, String path, String type) {
        this.name = name;
        this.path = path;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }
}
