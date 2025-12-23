import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {
    static class ProcessInput {
        String name;
        int arrival;
        int burst;
        int priority;
        int quantum; // We will default this to the global RR quantum if not asked

        public ProcessInput(String name, int arrival, int burst, int priority, int quantum) {
            this.name = name;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.quantum = quantum;
        }
    }
    public static void main(String[] args) {
        schedulersUnitTesting();
        agUnitTesting();
    }
    public static void agUnitTesting() {
        ObjectMapper mapper = new ObjectMapper();
        String testFolder = "test_cases_v5/AG/";
        System.out.println("\nStarting AG Scheduler Unit Tests...\n");

        for (int i = 1; i <= 6; i++) {
            String fileName = "AG_test" + i + ".json";
            File file = new File(testFolder + fileName);
            if (!file.exists()) {
                System.out.println("File " + fileName + " does not exist.");
                continue;
            }
            try {
                JsonAgStructure data = mapper.readValue(file, JsonAgStructure.class);
                List<Process> processes = convertToProcesses(data.input.processes);
                //test AG
                AGScheduler ag = new AGScheduler(false);
                ag.schedule(processes);
                runAndVerify("AG", fileName, processes, ag.getExecutionOrder(), data.expectedOutput);

            } catch (Exception e){
                System.out.println("Error while trying to process file " + fileName + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    public static void schedulersUnitTesting() {
        ObjectMapper mapper = new ObjectMapper();
        String testFolder = "test_cases_v5/Other_Schedulers/";
        System.out.println("Starting Unit Tests for All Schedulers...\n");
        for (int i = 1; i <= 6; i++) {
            String fileName = "test_" + i + ".json";
            File file = new File(testFolder + fileName);
            if (!file.exists()) {
                System.out.println("File " + fileName + " does not exist.");
                continue;
            }
            try {
                // Preparing Input for Schedulers
                JsonStructure data = mapper.readValue(file, JsonStructure.class);
                Map<String, JsonStructure.AlgorithmResult> expectedOutput = data.expectedOutput;
                // SJF Testing
                List<Process> sjfProcesses = convertToProcesses(data.input.processes);
                SJFScheduler sjf = new SJFScheduler(false);
                sjf.schedule(sjfProcesses, data.input.contextSwitch);
                runAndVerify("SJF", fileName, sjfProcesses, sjf.getExecutionOrder(), expectedOutput.get("SJF"));
                
                // RR Testing
                List<Process> rrProcesses = convertToProcesses(data.input.processes);
                RRScheduler rr = new RRScheduler(false);
                rr.schedule(rrProcesses, data.input.rrQuantum, data.input.contextSwitch);
                runAndVerify("RR", fileName, rrProcesses, rr.getExecutionOrder(), expectedOutput.get("RR"));
                
                // Priority Testing
//                int agingInterval = data.input.agingInterval;
//                List<Process> pProcesses = convertToProcesses(data.input.processes);
//                PriorityScheduler ps = new PriorityScheduler(false);
//                ps.schedule(pProcesses, data.input.contextSwitch);
//                runAndVerify("Priority", fileName, pProcesses, ps.getExecutionOrder(), expectedOutput.get("Priority"));

                System.out.println();

            } catch (Exception e){
                System.out.println("Error while trying to process file " + fileName + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    public static void runAndVerify(String schedulerName, String fileName, List<Process> outputProcesses, List<String> outputOrder, JsonStructure.AlgorithmResult expectedOutput){
        boolean isCorrectOrder = outputOrder.equals(expectedOutput.executionOrder);
        boolean isCorrectStats = verifyStats(outputProcesses, expectedOutput.processResults);
        if (isCorrectOrder && isCorrectStats) {
            System.out.println(schedulerName + " PASSED " + fileName);
        } else {
            System.out.println(schedulerName + " FAILED " + fileName);
            if (!isCorrectOrder) {
                System.out.println("Execution Order Mismatch");
                System.out.println("Expected: " + expectedOutput.executionOrder);
                System.out.println("Actual:   " + outputOrder);
            }
            if (!isCorrectStats) {
                System.out.println("Statistics (Wait/Turnaround) Mismatch");
            }
        }
    }
    private static boolean verifyStats(List<Process> actualProcs, List<JsonStructure.ProcessResult> expectedResults) {
        for (JsonStructure.ProcessResult expected : expectedResults) {
            // Find the corresponding actual process
            Process actual = actualProcs.stream()
                    .filter(p -> p.getName().equals(expected.name))
                    .findFirst()
                    .orElse(null);

            if (actual == null) return false;

            // Check values (Exact match required)
            if (actual.getWaitingTime() != expected.waitingTime) return false;
            if (actual.getTurnaroundTime() != expected.turnaroundTime) return false;
            if (expected.quantumHistory != null) {
                if (!expected.quantumHistory.equals(actual.getQuantumHistory())) {
                    System.out.println("Quantum History Mismatch for " + expected.name);
                    System.out.println("Expected: " + expected.quantumHistory);
                    System.out.println("Actual:   " + actual.getQuantumHistory());
                    return false;
                }
            }
        }
        return true;
    }
    public static List<Process> convertToProcesses(List<JsonStructure.ProcessData> input) {
        List<Process> processes = new ArrayList<>();
        for (JsonStructure.ProcessData d: input) {
            processes.add(new Process(d.arrival, d.burst, d.name, d.priority, d.quantum));
        }
        return processes;
    }
}