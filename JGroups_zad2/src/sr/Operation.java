package sr;

import java.io.Serializable;

enum OperationType{
    PUT,
    REMOVE
}

public class Operation implements Serializable {
    private final OperationType type;
    private final String key;
    private final String value;

    public OperationType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Operation(OperationType type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }
}
