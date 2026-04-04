package org.maldroid;
import java.util.Map;

public class AnalyzeResult {
    boolean hasActiveBody;
    private Map<String, String> constants;
    private Map<String, String> variables;

    public AnalyzeResult(Map<String, String> constants, Map<String, String> variables, boolean hasActiveBody) {
        this.constants = constants;
        this.variables = variables;
        this.hasActiveBody = hasActiveBody;
    }

    public Map<String, String> getConstants() { return constants; }
    public Map<String, String> getVariables() { return variables; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Constants:\n");
        for (Map.Entry<String, String> e : constants.entrySet())
            sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
        sb.append("Variables:\n");
        for (Map.Entry<String, String> e : variables.entrySet())
            sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
        return sb.toString();
    }
}
