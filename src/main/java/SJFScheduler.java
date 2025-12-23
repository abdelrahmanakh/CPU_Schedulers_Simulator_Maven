import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class SJFScheduler {
    List<String> executionOrder = new ArrayList<>();
    boolean is_unit_test;
    SJFScheduler(boolean is_unit_test) {
        this.is_unit_test = is_unit_test;
    }
    public void schedule(List<Process> processes, int contextSwitching) {
        for (Process p : processes) {
            p.reset();
        }

        int currentTime = 0;
        int completedProcesses = 0;
        int n = processes.size();
        Process lastProcess = null;

        List<Process> readyQueue = new ArrayList<>();

        while (completedProcesses < n) {

            for (Process p : processes) {
                if (p.getArrivalTime() <= currentTime && p.getRemainingTime() > 0 && !readyQueue.contains(p)) {
                    readyQueue.add(p);
                }
            }

            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            Process currentProcess = readyQueue.stream()
                    .min(Comparator.comparingInt(Process::getRemainingTime)
                            .thenComparingInt(Process::getArrivalTime))
                    .orElse(null);

            if (currentProcess != lastProcess) {
                if (lastProcess != null) {
                    currentTime += contextSwitching;
                }
                executionOrder.add(currentProcess.getName());
            }

            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - 1);
            currentTime++;
            lastProcess = currentProcess;

            if (currentProcess.getRemainingTime() == 0) {
                completedProcesses++;
                readyQueue.remove(currentProcess);

                currentProcess.setCompletionTime(currentTime);
                currentProcess.setTurnaroundTime(currentProcess.getCompletionTime() - currentProcess.getArrivalTime());
                currentProcess.setWaitingTime(currentProcess.getTurnaroundTime() - currentProcess.getBurstTime());
            }
        }

        if (!is_unit_test) {
            printResults(processes, executionOrder);
        }
    }

    List<String> getExecutionOrder() {
        return executionOrder;
    }

    private void printResults(List<Process> processes, List<String> executionOrder) {
        System.out.println("\n========== SJF (Preemptive) Results ==========");
        System.out.println("Execution Order: " + executionOrder);

        System.out.println("---------------------------------------------------------------");
        System.out.printf("%-10s %-15s %-15s\n", "Process", "Waiting Time", "Turnaround Time");
        System.out.println("---------------------------------------------------------------");

        double totalWaiting = 0;
        double totalTurnaround = 0;

        processes.sort(Comparator.comparing(Process::getName));

        for (Process p : processes) {
            System.out.printf("%-10s %-15d %-15d\n",
                    p.getName(), p.getWaitingTime(), p.getTurnaroundTime());
            totalWaiting += p.getWaitingTime();
            totalTurnaround += p.getTurnaroundTime();
        }

        System.out.println("---------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", (totalWaiting / processes.size()));
        System.out.printf("Average Turnaround Time: %.2f\n", (totalTurnaround / processes.size()));
        System.out.println("===============================================================");
    }
}