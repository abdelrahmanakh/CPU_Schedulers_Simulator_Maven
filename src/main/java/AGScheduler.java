import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AGScheduler {
    private List<String> executionOrder;
    private boolean is_unit_tes;

    public AGScheduler(boolean is_unit_tes) {
        this.executionOrder = new ArrayList<>();
        this.is_unit_tes = is_unit_tes;
    }

    public List<String> getExecutionOrder() {
        return executionOrder;
    }

    public void schedule(List<Process> processes) {
        executionOrder.clear();

        // Reset all processes and initialize quantum history
        for (Process p : processes) {
            p.reset();
            p.setRemainingTime(p.getBurstTime());
            p.getQuantumHistory().add(p.getQuantum());
        }

        // Sort by arrival time
        List<Process> sortedProcesses = new ArrayList<>(processes);
        sortedProcesses.sort(Comparator.comparingInt(Process::getArrivalTime));

        Queue<Process> readyQueue = new LinkedList<>();
        int currentTime = 0;
        int nextProcessIndex = 0;
        Process currentProcess = null;

        while (true) {
            // Add arrived processes
            while (nextProcessIndex < sortedProcesses.size() &&
                    sortedProcesses.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                Process p = sortedProcesses.get(nextProcessIndex++);
                readyQueue.add(p);
            }

            if (readyQueue.isEmpty() && currentProcess == null) {
                if (nextProcessIndex == sortedProcesses.size())
                    break;
                currentTime = sortedProcesses.get(nextProcessIndex).getArrivalTime();
                continue;
            }

            if (currentProcess == null) {
                currentProcess = readyQueue.poll();
            }

            // Add to execution order
            executionOrder.add(currentProcess.getName());

            // Phase 1: FCFS for ceil(25%) of quantum
            int quantum = currentProcess.getQuantum();
            int time = (quantum + 3) / 4;  // ceil(25%)
            time = Math.min(time, currentProcess.getRemainingTime());
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - time);
            int rem = quantum - time;
            currentTime += time;

            // Check if process finished
            if (currentProcess.getRemainingTime() == 0) {
                calculateMetrics(currentProcess, currentTime);
                currentProcess.setQuantum(0);  // scenario iv
                currentProcess = null;
                continue;
            }

            // Add newly arrived processes
            while (nextProcessIndex < sortedProcesses.size() &&
                    sortedProcesses.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                Process p = sortedProcesses.get(nextProcessIndex++);
                readyQueue.add(p);
            }

            // Check for priority preemption (go to Phase 2)
            Process nextProcess = null;
            if (!readyQueue.isEmpty())
                nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getPriority));

            if (nextProcess == null || currentProcess.getPriority() <= nextProcess.getPriority())
                nextProcess = currentProcess;

            if (nextProcess != currentProcess) {
                // Scenario ii: Priority preemption - add ceil(remaining/2)
                currentProcess.setQuantum(currentProcess.getQuantum() + (rem + 1) / 2);
                readyQueue.remove(nextProcess);
                readyQueue.add(currentProcess);
                currentProcess = nextProcess;
                continue;
            }

            // Phase 2: Non-preemptive Priority for another ceil(25%)
            time = (quantum + 3) / 4;  // another ceil(25%)
            time = Math.min(time, currentProcess.getRemainingTime());
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - time);
            rem -= time;
            currentTime += time;

            // Check if process finished
            if (currentProcess.getRemainingTime() == 0) {
                calculateMetrics(currentProcess, currentTime);
                currentProcess.setQuantum(0);  // scenario iv
                currentProcess = null;
                continue;
            }

            // Add newly arrived processes
            while (nextProcessIndex < sortedProcesses.size() &&
                    sortedProcesses.get(nextProcessIndex).getArrivalTime() <= currentTime) {
                Process p = sortedProcesses.get(nextProcessIndex++);
                readyQueue.add(p);
            }

            // Check for SJF preemption (go to Phase 3)
            nextProcess = null;
            if (!readyQueue.isEmpty())
                nextProcess = Collections.min(readyQueue, Comparator.comparingInt(Process::getRemainingTime));

            if (nextProcess == null || currentProcess.getRemainingTime() <= nextProcess.getRemainingTime())
                nextProcess = currentProcess;

            if (nextProcess != currentProcess) {
                // Scenario iii: SJF preemption - add full remaining quantum
                currentProcess.setQuantum(currentProcess.getQuantum() + rem);
                readyQueue.remove(nextProcess);
                readyQueue.add(currentProcess);
                currentProcess = nextProcess;
                continue;
            }

            // Phase 3: Execute remaining quantum
            time = rem;
            time = Math.min(time, currentProcess.getRemainingTime());
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - time);
            currentTime += time;

            // Check if process finished
            if (currentProcess.getRemainingTime() == 0) {
                calculateMetrics(currentProcess, currentTime);
                currentProcess.setQuantum(0);  // scenario iv
                currentProcess = null;
                continue;
            }

            // Scenario i: Quantum exhausted - add 2
            currentProcess.setQuantum(currentProcess.getQuantum() + 2);
            readyQueue.add(currentProcess);
            currentProcess = null;
        }

        // Remove consecutive duplicates from execution order
        List<String> cleanedOrder = new ArrayList<>();
        for (String name : executionOrder) {
            if (cleanedOrder.isEmpty() || !cleanedOrder.get(cleanedOrder.size() - 1).equals(name)) {
                cleanedOrder.add(name);
            }
        }
        executionOrder.clear();
        executionOrder.addAll(cleanedOrder);

        if (!is_unit_tes) {
            printResults(processes);
        }
    }

    private void calculateMetrics(Process p, int currentTime) {
        p.setCompletionTime(currentTime);
        p.setTurnaroundTime(currentTime - p.getArrivalTime());
        p.setWaitingTime(p.getTurnaroundTime() - p.getBurstTime());
    }

    private void printResults(List<Process> processes) {
        System.out.println("\n=== AG Scheduling Results ===");
        System.out.println("Execution Order: " + executionOrder);
        System.out.println("\nProcess Details:");
        System.out.println("---------------------------------------------------------------");
        System.out.printf("%-10s %-12s %-15s %-20s%n", "Process", "Wait Time", "Turnaround", "Quantum History");
        System.out.println("---------------------------------------------------------------");

        double totalWait = 0, totalTurnaround = 0;
        for (Process p : processes) {
            System.out.printf("%-10s %-12d %-15d %s%n",
                    p.getName(), p.getWaitingTime(), p.getTurnaroundTime(), p.getQuantumHistory());
            totalWait += p.getWaitingTime();
            totalTurnaround += p.getTurnaroundTime();
        }
        System.out.println("---------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f%n", totalWait / processes.size());
        System.out.printf("Average Turnaround Time: %.2f%n", totalTurnaround / processes.size());
    }
}
