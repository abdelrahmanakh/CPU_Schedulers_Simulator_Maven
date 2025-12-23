import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class PriorityScheduler {
    private List<String> executionOrder = new ArrayList<>();
    private boolean verbose;
    private int agingInterval;

    PriorityScheduler(boolean verbose) {
        this.verbose = verbose;
    }

    List<String> getExecutionOrder() {
        return executionOrder;
    }

    public void schedule(List<Process> processes, int contextSwitchTime, int agingInterval) {
        executionOrder.clear();
        this.agingInterval = agingInterval;

        // Reset all processes and track input order
        for (int i = 0; i < processes.size(); i++) {
            Process p = processes.get(i);
            p.reset();
            p.setRemainingTime(p.getBurstTime());
        }

        // Store original priorities and track last aging time
        int[] lastAgeTime = new int[processes.size()];
        int[] effectivePriority = new int[processes.size()];
        for (int i = 0; i < processes.size(); i++) {
            effectivePriority[i] = processes.get(i).getPriority();
            lastAgeTime[i] = processes.get(i).getArrivalTime();
        }

        int time = 0;
        Process current = null;
        String lastRecorded = null;

        while (true) {
            // Check if all processes are done
            boolean allDone = true;
            for (Process p : processes) {
                if (p.getRemainingTime() > 0) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) break;

            // Apply aging to waiting processes
            for (int i = 0; i < processes.size(); i++) {
                Process p = processes.get(i);
                if (p.getArrivalTime() <= time && p.getRemainingTime() > 0) {
                    if (time > lastAgeTime[i] && agingInterval > 0 && (time - lastAgeTime[i]) % agingInterval == 0) {
                        effectivePriority[i] = Math.max(1, effectivePriority[i] - 1);
                        lastAgeTime[i] = time;
                    }
                }
            }

            // Find highest priority process (lowest priority number)
            Process next = null;
            int nextIndex = -1;
            for (int i = 0; i < processes.size(); i++) {
                Process p = processes.get(i);
                if (p.getRemainingTime() > 0 && p.getArrivalTime() <= time) {
                    if (next == null ||
                        effectivePriority[i] < effectivePriority[nextIndex] ||
                        (effectivePriority[i] == effectivePriority[nextIndex] && p.getArrivalTime() < next.getArrivalTime()) ||
                        (effectivePriority[i] == effectivePriority[nextIndex] && p.getArrivalTime() == next.getArrivalTime() && i < nextIndex)) {
                        next = p;
                        nextIndex = i;
                    }
                }
            }

            // No process ready, advance time
            if (next == null) {
                time++;
                continue;
            }

            // Context switch if process changed
            if (current != next) {
                if (current != null) {
                    time += contextSwitchTime;
                }
                current = next;
                // Add to execution order if different from last recorded
                if (!current.getName().equals(lastRecorded)) {
                    executionOrder.add(current.getName());
                    lastRecorded = current.getName();
                }
                continue;
            }

            // Execute one unit of time
            current.setRemainingTime(current.getRemainingTime() - 1);
            time++;
            lastAgeTime[nextIndex] = time;

            // If process completed
            if (current.getRemainingTime() == 0) {
                current.setCompletionTime(time);
                current.setTurnaroundTime(time - current.getArrivalTime());
                current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
            }
        }

        if (verbose) {
            printResults(processes, effectivePriority);
        }
    }

    // Overload for backward compatibility (without agingInterval parameter)
    public void schedule(List<Process> processes, int contextSwitchTime) {
        schedule(processes, contextSwitchTime, 5); // Default aging interval
    }

    private void printResults(List<Process> processes, int[] effectivePriority) {
        System.out.println("\n========== Priority Scheduling (Preemptive + Aging) ==========");
        System.out.println("Execution Order: " + executionOrder);

        System.out.println("\nPriority Changes");
        System.out.printf("%-10s %-18s %-18s%n", "Process", "Original Priority", "Final Priority");
        System.out.println("---------------------------------------------------------------");

        for (int i = 0; i < processes.size(); i++) {
            Process p = processes.get(i);
            System.out.printf("%-10s %-18d %-18d%n",
                    p.getName(), p.getPriority(), effectivePriority[i]);
        }

        System.out.println("\nProcess Execution Results");
        System.out.println("---------------------------------------------------------------");
        System.out.printf("%-10s %-15s %-15s%n", "Process", "Waiting Time", "Turnaround Time");
        System.out.println("---------------------------------------------------------------");

        double totalWait = 0;
        double totalTurnaround = 0;

        for (Process p : processes) {
            System.out.printf("%-10s %-15d %-15d%n",
                    p.getName(), p.getWaitingTime(), p.getTurnaroundTime());
            totalWait += p.getWaitingTime();
            totalTurnaround += p.getTurnaroundTime();
        }

        System.out.println("---------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f%n", totalWait / processes.size());
        System.out.printf("Average Turnaround Time: %.2f%n", totalTurnaround / processes.size());
    }
}