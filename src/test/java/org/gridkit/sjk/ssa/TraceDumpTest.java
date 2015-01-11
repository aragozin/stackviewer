package org.gridkit.sjk.ssa;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

import org.junit.Test;

public class TraceDumpTest {

    public Thread pickThread() {
        int tc = Thread.activeCount();
        Thread[] threads = new Thread[tc];
        tc = Thread.enumerate(threads);
        return threads[7 % tc];
    }
    
    @Test
    public void test_thread_stack_trace() throws InterruptedException {
        Thread t = pickThread();
        while(true) {
            Thread.sleep(5000);
            t.getStackTrace();
        }
    }
    
    @Test
    public void test_all_stack_trace() throws InterruptedException {
        while(true) {
            Thread.sleep(5000);
            Thread.getAllStackTraces();
        }
    }   

    @Test
    public void test_jmx_thread_info() throws InterruptedException {
        long id = Thread.currentThread().getId();
        while(true) {
            Thread.sleep(5000);
            ThreadInfo ti = ManagementFactory.getThreadMXBean().getThreadInfo(id, 1000);
            System.out.println(ti.getThreadName() + " - " + ti.getStackTrace().length);
        }
    }   
}
