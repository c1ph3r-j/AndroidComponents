package online.c1ph3rj.androidcomponents.model;

public class GridItem {
    private final String name;
    private final Class<?> activityClass;

    public GridItem(String name, Class<?> activityClass) {
        this.name = name;
        this.activityClass = activityClass;
    }

    public String getName() {
        return name;
    }

    public Class<?> getActivityClass() {
        return activityClass;
    }
}

