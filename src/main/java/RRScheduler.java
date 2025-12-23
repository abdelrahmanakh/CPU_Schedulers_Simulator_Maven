import java.util.*;

class RRScheduler {
    List<String> executionOrder = new ArrayList<>();
    boolean is_unit_test;
    RRScheduler(boolean is_unit_test){
        this.is_unit_test = is_unit_test;
    }
    public void schedule(List<Process> processes, int quantum, int contextSwitching) {
        for (Process p : processes) {
            p.reset();
        }

        Queue<Process> readyQueue = new LinkedList<>();
        int completedProcesses = 0, n = processes.size();
        int ptr = 0, currentTime = 0;
        Process lstProcess = null;

        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        while (completedProcesses < n) {
            while (ptr < n && processes.get(ptr).getArrivalTime() <= currentTime) {
                readyQueue.add(processes.get(ptr++));
            }
            if (readyQueue.isEmpty()) {
                ++currentTime;
                continue;
            }

            Process currentProcess = readyQueue.poll();
            if (lstProcess != null && lstProcess != currentProcess) {
                currentTime += contextSwitching;
            }

            int requiredTime = Math.min(quantum, currentProcess.getRemainingTime());
            currentTime += requiredTime;
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - requiredTime);
            executionOrder.add(currentProcess.getName());

            while (ptr < n && processes.get(ptr).getArrivalTime() <= currentTime) {
                readyQueue.add(processes.get(ptr++));
            }

            if (currentProcess.getRemainingTime() == 0) {
                ++completedProcesses;
                currentProcess.setCompletionTime(currentTime);
                currentProcess.setTurnaroundTime(currentProcess.getCompletionTime() - currentProcess.getArrivalTime());
                currentProcess.setWaitingTime(currentProcess.getTurnaroundTime() - currentProcess.getBurstTime());
            } else {
                readyQueue.add(currentProcess);
            }
            lstProcess = currentProcess;
        }

        if (!is_unit_test) {
            printResults(processes, executionOrder);
        }
    }
    List<String> getExecutionOrder() {
        return executionOrder;
    }

    private void printResults(List<Process> processes, List<String> executionOrder) {
        System.out.println("\n========== Round Robin Scheduler Results ==========");
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
