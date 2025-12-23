import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class PriorityScheduler {
    private List<String> executionOrder = new ArrayList<>();
    boolean is_unit_test;
    PriorityScheduler(boolean is_unit_test){
        this.is_unit_test = is_unit_test;
    }
    // get for print
    List<String> getExecutionOrder() {
        return executionOrder;
    }
    public void schedule(List<Process> processes, int contextSwitching) {
        // Reset all processes
        for(Process pr : processes){
            pr.reset();
        }

        int currentTime = 0;
        int completedProcesses = 0;
        int size = processes.size();
        Process lastProcess = null;

        final int ageIntreval = 2;
        final int ageVlaue = 1;

        // Track for effective priority and wait time for age
        java.util.Map<String, Integer> effectivePriority = new java.util.HashMap<>();
        java.util.Map<String, Integer> waitCounter = new java.util.HashMap<>();

        // intialize them
        for (Process pr : processes) {
            effectivePriority.put(pr.getName(), pr.getPriority());
            waitCounter.put(pr.getName(), 0);
        }

        while(completedProcesses < size){
            // arrived process and not complete
            List<Process> readyQueue = new ArrayList<>();
            for(Process pr : processes){
                if(pr.getArrivalTime() <= currentTime && pr.getRemainingTime() > 0){
                    readyQueue.add(pr);
                }
            }

            // no process ready
            if (readyQueue.isEmpty()) {
                currentTime++;
                continue;
            }

            // raise age of processes
            for (Process p : readyQueue) {
                if (p != lastProcess && p.getRemainingTime() > 0) {
                    int waiting = waitCounter.get(p.getName());
                    waitCounter.put(p.getName(), waiting + 1);
                    // reduce age and increase priority
                    if ((waiting + 1) % ageIntreval == 0) {
                        int currentPriority = effectivePriority.get(p.getName());
                        int newPriority = Math.max(0, currentPriority - ageVlaue);
                        effectivePriority.put(p.getName(), newPriority);
                    }
                }
            }

            // highest process
            Process highProcess = readyQueue.get(0);
            for(Process pr : processes){
                int anyPriot = effectivePriority.get(pr.getName());
                int hPriot = effectivePriority.get(highProcess.getName());

                if(anyPriot < hPriot){
                    highProcess = pr;
                } else if (anyPriot == hPriot){
                    // choose by arrival time
                    if(pr.getArrivalTime() < highProcess.getArrivalTime()){
                        highProcess = pr;
                    }
                    // by name order
                    else if(pr.getArrivalTime() == highProcess.getArrivalTime()){
                        if(pr.getName().compareTo(highProcess.getName()) > 0){
                            highProcess = pr;
                        }
                    }
                }
            }

            // contezt switch if process changed
            if(lastProcess != null && highProcess != lastProcess){
                currentTime += contextSwitching;
            }

            // reset waiting counter for run process
            waitCounter.put(highProcess.getName(), 0);

            // extract one of processes
            highProcess.setRemainingTime(highProcess.getRemainingTime() - 1);
            currentTime++;
            executionOrder.add(highProcess.getName());
            lastProcess = highProcess;

            // if process complete
            if(highProcess.getRemainingTime() == 0){
                completedProcesses++;

                highProcess.setCompletionTime(currentTime);
                highProcess.setTurnaroundTime(
                        highProcess.getCompletionTime() - highProcess.getArrivalTime()
                );
                highProcess.setWaitingTime(
                        highProcess.getTurnaroundTime() - highProcess.getBurstTime()
                );
            }
        }
        if (!is_unit_test) {
            printPriority(processes,executionOrder,effectivePriority);
        }
    }

    private void printPriority(List<Process> processes, List<String> executionOrder, java.util.Map<String, Integer> effectivePriority){
        System.out.println("\n========== Priority Scheduling (Preemptive + Aging) ==========");
        System.out.println("Execution Order: " + executionOrder);

        System.out.println("\nPriority Changes");
        System.out.printf("%-10s %-18s %-18s\n", "Process", "Original Priority", "Final Priority");
        System.out.println("---------------------------------------------------------------");

        processes.sort(Comparator.comparing(Process::getName));


        for (Process pr : processes) {
            System.out.printf("%-10s %-18d %-18d\n",
                    pr.getName(), pr.getPriority(), effectivePriority.get(pr.getName()));
        }

        System.out.println("\nProcess Execution Results");
        System.out.println("---------------------------------------------------------------");
        System.out.printf("%-10s %-15s %-15s\n", "Process", "Waiting Time", "Turnaround Time");
        System.out.println("---------------------------------------------------------------");

        double totWait = 0;
        double totTurnaround = 0;

        for (Process pr : processes) {
            System.out.printf("%-10s %-15d %-15d\n",
                    pr.getName(), pr.getWaitingTime(), pr.getTurnaroundTime());
            totWait += pr.getWaitingTime();
            totTurnaround += pr.getTurnaroundTime();
        }

        System.out.println("---------------------------------------------------------------");
        System.out.printf("Average Waiting Time: %.2f\n", (totWait / processes.size()));
        System.out.printf("Average Turnaround Time: %.2f\n", (totTurnaround / processes.size()));
    }
}
