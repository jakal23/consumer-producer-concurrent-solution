package com.atcc.test.java.concurrent;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    private static final int mPoolCount = 1;

    private static final ExecutorService sConsumerExecutorService =
            Executors.newFixedThreadPool(mPoolCount, new DefaultThreadFactory("Consumer"));

    private static final ExecutorService sProducerExecutorService =
            Executors.newFixedThreadPool(mPoolCount, new DefaultThreadFactory("Producer"));

    private static final Object LOCK = new Object();


    public static void main(String[] args) {
        int taskCount = 100;

        List<Future<Pair<Integer, String>>> tasks = new ArrayList<>(taskCount);
        for (int index = 0; index < taskCount; index++ ){
            Future<Pair<Integer, String>> task = sConsumerExecutorService.submit(new ConsumerCallable(index));
            tasks.add(task);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sProducerExecutorService.submit(new ProducerReader(tasks));

        sConsumerExecutorService.shutdown();
    }


    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = name + "-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    static class ConsumerCallable implements Callable<Pair<Integer, String>>{

        private final int index;

         ConsumerCallable(int index) {
            this.index = index;
        }

        @Override
        public Pair<Integer, String> call(){
            Pair<Integer, String> res =
                    new Pair<>(this.index,Thread.currentThread().getName());

            synchronized (LOCK){
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println(res + " print");
            return res;
        }

    }

    static class ProducerReader implements Runnable{

        private final List<Future<Pair<Integer, String>>> tasks;

        public ProducerReader(List<Future<Pair<Integer, String>>> tasks) {
            this.tasks = tasks;
        }

        @Override
        public void run() {

            synchronized (LOCK){
                LOCK.notifyAll();
            }

            for ( Future<Pair<Integer, String>> task : tasks){
                Pair<Integer, String> res = null;
                try {
                    res = task.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }finally {
                    if (res != null){
                        System.out.println(res.getKey() + " Read " + Thread.currentThread().getName());
                    }

                    synchronized (LOCK){
                        LOCK.notifyAll();
                    }
                }
            }
        }
    }
}
