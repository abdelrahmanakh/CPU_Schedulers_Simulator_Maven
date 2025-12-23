import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

// This class matches the structure of your "test_1.json" file
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonStructure {

    public String name;
    public InputData input;

    // This Map will hold "SJF", "RR", "Priority" automatically
    public Map<String, AlgorithmResult> expectedOutput;

    // Input Matcher

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InputData {
        public int contextSwitch;
        public int rrQuantum;
        public int agingInterval;
        public List<ProcessData> processes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessData {
        public String name;
        public int arrival;
        public int burst;
        public int priority;
        public int quantum;
    }

    // Output Matcher
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlgorithmResult {
        public List<String> executionOrder;
        public List<ProcessResult> processResults;
        public double averageWaitingTime;
        public double averageTurnaroundTime;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProcessResult {
        public String name;
        public int waitingTime;
        public int turnaroundTime;
        public List<Integer> quantumHistory;
    }
}