import java.util.ArrayList;
import java.util.List;

public class Process {
    private String name;
    private int arrivalTime;
    private int burstTime;
    private int priority;

    // Execution state
    private int remainingTime;
    private int quantum;
    private int waitingTime;
    private int turnaroundTime;
    private int completionTime;

    // History for AG/Analysis
    private List<Integer> quantumHistory;
    private List<String> quantumTimeline;

    public Process(int arrivalTime, int burstTime, String name, int priority, int quantum) {
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.name = name;
        this.priority = priority;
        this.quantum = quantum;
        this.waitingTime = 0;
        this.quantumHistory = new ArrayList<>();
        this.quantumTimeline = new ArrayList<>();
        this.remainingTime = burstTime;
    }

    public void reset() {
        this.remainingTime = this.burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.completionTime = 0;
        this.quantumHistory = new ArrayList<>();
        this.quantumTimeline = new ArrayList<>();
    }

    // --- Logic Methods ---

    public void setQuantum(int quantum) {
        this.quantum = quantum;
        this.quantumHistory.add(quantum);
    }

    public void recordQuantum(int quantum, int time) {
        if (this.quantumTimeline == null) this.quantumTimeline = new ArrayList<>();
        this.quantumTimeline.add("t=" + time + ":q=" + quantum);
    }

    public int getCeil25() {
        return (int) Math.ceil(0.25 * quantum);
    }

    public int getCeil50() {
        return (int) Math.ceil(0.50 * quantum);
    }

    // --- Getters and Setters ---
    public String getName() { return name; }
    public int getArrivalTime() { return arrivalTime; }
    public int getBurstTime() { return burstTime; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getRemainingTime() { return remainingTime; }
    public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }

    public int getQuantum() { return quantum; }

    public int getWaitingTime() { return waitingTime; }
    public void setWaitingTime(int waitingTime) { this.waitingTime = waitingTime; }

    public int getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(int turnaroundTime) { this.turnaroundTime = turnaroundTime; }

    public int getCompletionTime() { return completionTime; }
    public void setCompletionTime(int completionTime) { this.completionTime = completionTime; }

    public List<Integer> getQuantumHistory() { return quantumHistory; }
    public List<String> getQuantumTimeline() { return quantumTimeline; }
}