import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class PriorityScheduler {
    private List<String> executionOrder = new ArrayList<>();

    private void applyAging(PriorityQueue<Process> queue, int time, int agingInterval) {
        if (agingInterval <= 0 || queue.isEmpty())
            return;
        List<Process> temp = new ArrayList<>(queue);
        queue.clear();
        for (Process p : temp) {
            if ((time - p.getArrivalTime()) % agingInterval == 0) {
                p.setPriority(Math.max(1, p.getPriority() - 1));
            }
            queue.add(p);
        }
    }

    boolean is_unit_test;

    PriorityScheduler(boolean is_unit_test) {
        this.is_unit_test = is_unit_test;
    }

    // get for print
    List<String> getExecutionOrder() {
        return executionOrder;
    }

    public void schedule(List<Process> input, int contextSwitching, int ageInterval) {
        List<Process> processes = new ArrayList<>();
        for (Process p : input) {
            p.reset(); 
            p.setRemainingTime(p.getBurstTime());
            processes.add(p);
        }

        PriorityQueue<Process> readyQueue = new PriorityQueue<>((p1, p2) -> {
            if (p1.getPriority() != p2.getPriority())
                return Integer.compare(p1.getPriority(), p2.getPriority());
            if (p1.getArrivalTime() != p2.getArrivalTime())
                return Integer.compare(p1.getArrivalTime(), p2.getArrivalTime());
            return p1.getName().compareTo(p2.getName());
        });

        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        int currentTime = processes.get(0).getArrivalTime();
        int i = 0;

        while (i < processes.size() && processes.get(i).getArrivalTime() <= currentTime) {
            readyQueue.add(processes.get(i));
            i++;
        }

        String lastProcessName = "";
        String lastExecutedName = "";
        Map<String, Integer> finalEffectivePriorities = new HashMap<>();

        while (!readyQueue.isEmpty() || i < processes.size()) {

            Process current = null;
            String currentName = "Null";

            if (!readyQueue.isEmpty()) {
                current = readyQueue.poll();
                currentName = current.getName();

                if (!currentName.equals(lastExecutedName)) {
                    executionOrder.add(currentName);
                    lastExecutedName = currentName;
                }
            }

            //  Context Switch 
            if (!lastProcessName.isEmpty() && !lastProcessName.equals(currentName) && !lastProcessName.equals("Null")) {
                if (current != null) readyQueue.add(current);

                for (int c = 0; c < contextSwitching; c++) {
                    currentTime++;
                    applyAging(readyQueue, currentTime, ageInterval);
                    
                    while (i < processes.size() && processes.get(i).getArrivalTime() == currentTime) {
                        readyQueue.add(processes.get(i));
                        i++;
                    }
                }
                lastProcessName = currentName;
                continue; 
            }

            lastProcessName = currentName;

            currentTime++;
            if (current != null) {
                current.setRemainingTime(current.getRemainingTime() - 1);
            }

            applyAging(readyQueue, currentTime, ageInterval);

            while (i < processes.size() && processes.get(i).getArrivalTime() == currentTime) {
                readyQueue.add(processes.get(i));
                i++;
            }

            if (current == null) continue;

            if (current.getRemainingTime() > 0) {
                readyQueue.add(current);
            } else {
                current.setCompletionTime(currentTime);
                current.setTurnaroundTime(current.getCompletionTime() - current.getArrivalTime());
                current.setWaitingTime(current.getTurnaroundTime() - current.getBurstTime());
                finalEffectivePriorities.put(current.getName(), current.getPriority());
            }
        }

        for(Process p : processes) {
            if(!finalEffectivePriorities.containsKey(p.getName())) {
                finalEffectivePriorities.put(p.getName(), p.getPriority());
            }
        }

        if (!is_unit_test) {
            printPriority(processes, executionOrder, finalEffectivePriorities);
        }
    }

    private void printPriority(List<Process> processes, List<String> executionOrder,
            java.util.Map<String, Integer> effectivePriority) {
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
