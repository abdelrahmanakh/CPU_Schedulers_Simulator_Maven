import java.util.*;

class AGScheduler {
    private List<String> executionOrder = new ArrayList<>();
    boolean is_unit_test;
    AGScheduler(boolean is_unit_test){
        this.is_unit_test = is_unit_test;
    }
    // Default entry used by testAG (context switching = 0 by default)
    public void schedule(List<Process> processes) {
        schedule(processes, 0);
    }

    // Full AG scheduler with context switching support
    public void schedule(List<Process> processes, int contextSwitching) {
        // Reset all processes and record their initial quantum as history
        for (Process p : processes) {
            p.reset();
            // quantum is already stored in the process (input JSON),
            // here we only push the initial value into the history list.
            p.setQuantum(p.getQuantum());
            // record initial quantum at process arrival time
            p.recordQuantum(p.getQuantum(), p.getArrivalTime());
        }

        Queue<Process> readyQueue = new LinkedList<>();
        executionOrder = new ArrayList<>();

        int completed = 0;
        int n = processes.size();
        int ptr = 0;
        int currentTime = 0;
        Process lastProcess = null;

        // Sort by arrival time once (use ptr to enqueue in order)
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        while (completed < n) {
            // Enqueue newly arrived processes
            while (ptr < n && processes.get(ptr).getArrivalTime() <= currentTime) {
                readyQueue.add(processes.get(ptr++));
            }

            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            Process cur = readyQueue.poll();

            // Context switching when changing the running process
            if (lastProcess != null && cur != lastProcess) {
                currentTime += contextSwitching;
            }

            int q = cur.getQuantum();
            int used = 0;                    // total time units used from this quantum
            int c25 = cur.getCeil25();       // ceil(25% of quantum)
            int c50 = cur.getCeil50();       // ceil(50% of quantum)

            boolean preemptedInPriority = false;
            boolean preemptedInSJF = false;

            // ---------------- Phase 1: FCFS (first ceil(25% of quantum)) -------------
            int phase1Limit = Math.min(c25, cur.getRemainingTime());
            for (int i = 0; i < phase1Limit; i++) {
                // Run current for one time unit (no preemption in FCFS phase)
                cur.setRemainingTime(cur.getRemainingTime() - 1);
                currentTime++;
                executionOrder.add(cur.getName());
                used++;

                // Enqueue any new arrivals during execution
                while (ptr < n && processes.get(ptr).getArrivalTime() <= currentTime) {
                    readyQueue.add(processes.get(ptr++));
                }

                if (cur.getRemainingTime() == 0) {
                    break;
                }
            }

            if (cur.getRemainingTime() == 0) {
                // Scenario (iv): finished before quantum is fully consumed
                completed++;
                cur.setCompletionTime(currentTime);
                cur.setTurnaroundTime(cur.getCompletionTime() - cur.getArrivalTime());
                cur.setWaitingTime(cur.getTurnaroundTime() - cur.getBurstTime());
                cur.setQuantum(0); // final quantum becomes 0
                cur.recordQuantum(0, currentTime);
                lastProcess = cur;
                continue;
            }

            // ---------------- Phase 2: Priority (next ceil(25% of quantum)) ---------
            int phase2Target = Math.min(c50, q); // absolute quantum boundary for 50%
            int phase2Limit = Math.min(phase2Target - used, cur.getRemainingTime());
            for (int i = 0; i < phase2Limit; i++) {
                // Before running this unit, check if any READY process has higher priority
                boolean higherPriorityExists = false;
                for (Process p : readyQueue) {
                    if (p.getPriority() < cur.getPriority()) { // smaller value => higher priority
                        higherPriorityExists = true;
                        break;
                    }
                }
                if (higherPriorityExists) {
                    // Scenario (ii): preempted while executing as non-preemptive Priority
                    preemptedInPriority = true;
                    break;
                }

                // Run one time unit under Priority
                cur.setRemainingTime(cur.getRemainingTime() - 1);
                currentTime++;
                executionOrder.add(cur.getName());
                used++;

                while (ptr < n && processes.get(ptr).getArrivalTime() <= currentTime) {
                    readyQueue.add(processes.get(ptr++));
                }

                if (cur.getRemainingTime() == 0) {
                    break;
                }
            }

            if (cur.getRemainingTime() == 0) {
                // Scenario (iv): finished during Priority phase
                completed++;
                cur.setCompletionTime(currentTime);
                cur.setTurnaroundTime(cur.getCompletionTime() - cur.getArrivalTime());
                cur.setWaitingTime(cur.getTurnaroundTime() - cur.getBurstTime());
                cur.setQuantum(0);
                cur.recordQuantum(0, currentTime);
                lastProcess = cur;
                continue;
            }

            // ---------------- Phase 3: Preemptive SJF (rest of quantum) -------------
            if (!preemptedInPriority) {
                int remainingSlice = q - used;
                for (int i = 0; i < remainingSlice; i++) {
                    // Check if any READY process has smaller remaining time -> preempt
                    Process sjfCandidate = cur;
                    for (Process p : readyQueue) {
                        if (p.getRemainingTime() < sjfCandidate.getRemainingTime()) {
                            sjfCandidate = p;
                        }
                    }

                    if (sjfCandidate != cur) {
                        // Scenario (iii): preempted while executing as preemptive SJF
                        preemptedInSJF = true;
                        break;
                    }

                    // Run one unit under SJF
                    cur.setRemainingTime(cur.getRemainingTime() - 1);
                    currentTime++;
                    executionOrder.add(cur.getName());
                    used++;

                    while (ptr < n && processes.get(ptr).getArrivalTime() <= currentTime) {
                        readyQueue.add(processes.get(ptr++));
                    }

                    if (cur.getRemainingTime() == 0) {
                        break;
                    }
                }
            }

            // Check for completion after SJF phase
            if (cur.getRemainingTime() == 0) {
                // Scenario (iv): finished before using full quantum
                completed++;
                cur.setCompletionTime(currentTime);
                cur.setTurnaroundTime(cur.getCompletionTime() - cur.getArrivalTime());
                cur.setWaitingTime(cur.getTurnaroundTime() - cur.getBurstTime());
                cur.setQuantum(0);
                cur.recordQuantum(0, currentTime);
                lastProcess = cur;
                continue;
            }

            // ---------------- Quantum update scenarios (i)–(iii) --------------------
            int remainingQuantum = q - used;

            if (used >= q && cur.getRemainingTime() > 0) {
                // (i) Used all quantum and still has work -> quantum += 2
                cur.setQuantum(q + 2);
                cur.recordQuantum(cur.getQuantum(), currentTime);
                readyQueue.add(cur);
            } else if (preemptedInPriority) {
                // (ii) Preempted in Priority phase -> quantum += ceil(remaining/2)
                int inc = (int) Math.ceil(remainingQuantum / 2.0);
                cur.setQuantum(q + inc);
                cur.recordQuantum(cur.getQuantum(), currentTime);
                readyQueue.add(cur);
            } else if (preemptedInSJF) {
                // (iii) Preempted in SJF phase -> quantum += remaining
                cur.setQuantum(q + remainingQuantum);
                cur.recordQuantum(cur.getQuantum(), currentTime);
                readyQueue.add(cur);
            } else {
                // Safety net: if not finished and no explicit scenario matched
                if (cur.getRemainingTime() > 0) {
                    cur.setQuantum(q);
                    cur.recordQuantum(cur.getQuantum(), currentTime);
                    readyQueue.add(cur);
                } else {
                    cur.setQuantum(0);
                }
            }

            lastProcess = cur;
        }

        if (!is_unit_test){
            // Printing results (for manual debugging / visualization)
            System.out.println("\n========== AG Scheduling Results ==========");
            System.out.println("Execution Order: " + executionOrder);

            System.out.println("---------------------------------------------------------------");
            System.out.printf("%-10s %-15s %-15s %-25s %-25s\n", "Process", "Waiting Time", "Turnaround Time", "Quantum History", "Quantum Timeline");
            System.out.println("---------------------------------------------------------------");

            double totalWaiting = 0;
            double totalTurnaround = 0;

            processes.sort(Comparator.comparing(Process::getName));
            for (Process p : processes) {
                System.out.printf("%-10s %-15d %-15d %-25s %-25s\n",
                        p.getName(), p.getWaitingTime(), p.getTurnaroundTime(), p.getQuantumHistory(), p.getQuantumTimeline());
                totalWaiting += p.getWaitingTime();
                totalTurnaround += p.getTurnaroundTime();
            }

            System.out.println("---------------------------------------------------------------");
            System.out.printf("Average Waiting Time: %.2f\n", (totalWaiting / processes.size()));
            System.out.printf("Average Turnaround Time: %.2f\n", (totalTurnaround / processes.size()));
            System.out.println("===============================================================");
        }
    }

    List<String> getExecutionOrder() {
        return executionOrder;
    }
}
