package com.chenjy.testThread;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableFutureTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        CallableFutureTest test = new CallableFutureTest();
        
        // ����һ���̳߳�
        ExecutorService pool = Executors.newFixedThreadPool(3);
        
        long startTime = System.currentTimeMillis();//��ȡ��ǰʱ��
        
        // ���������з���ֵ������
        Callable c1 = test.new MyCallable(0,5000000);
        Callable c2 = test.new MyCallable(5000000,5000000);
//        Callable c3 = test.new MyCallable(500000,250000);
        
        
        
        // ִ�����񲢻�ȡFuture����
        Future f1 = pool.submit(c1);
        Future f2 = pool.submit(c2);
//        Future f3 = pool.submit(c3);
        
  //      long cnt = (long)(f1.get()) + (long)f2.get();
        
        long endTime = System.currentTimeMillis();
		System.out.println("��������ʱ�䣺"+(endTime-startTime)+"ms");
        // ��Future�����ϻ�ȡ����ķ���ֵ�������������̨
        System.out.println(">>>" + f1.get());
        System.out.println(">>>" + f2.get().toString());
//        System.out.println(">>>" + f3.get().toString());
        // �ر��̳߳�
        pool.shutdown();
    }
    
    public class MyCallable implements Callable {
        private long oid=0;
//        private int start;
//        private int length;
        
        MyCallable(int start,int length) {
        	for(int i=start;i<start+length-1;i++)
        	{
        		this.oid +=i;
        	}
        	
        }
        public Object call() throws Exception {
            return oid;
        }
    }
}
