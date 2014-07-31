package com.reneponette.comicbox.manager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PausableThreadPoolExecutor extends ThreadPoolExecutor {

	private boolean isPaused;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	
	public enum Type {
		DEFAULT, DISK, NETWORK;
	}
	
	private static PausableThreadPoolExecutor instanceForDefault; 
	private static PausableThreadPoolExecutor instanceForDiskIO; 
	private static PausableThreadPoolExecutor instanceForNetworkIO; 
	public static PausableThreadPoolExecutor instance(Type type) {
		PausableThreadPoolExecutor instance;
		
		switch (type) {
		case DISK:
			instanceForDiskIO = new PausableThreadPoolExecutor(5, 20, 10000, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
			instance = instanceForDiskIO;			
			break;
		case NETWORK:
			instanceForNetworkIO = new PausableThreadPoolExecutor(5, 20, 10000, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
			instance = instanceForNetworkIO;
			break;
		default:
			instanceForDefault = new PausableThreadPoolExecutor(5, 20, 10000, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
			instance = instanceForDefault;			
			break;
		}
		
		return instance;
	}
	
	private PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		pauseLock.lock();
		try {
			while (isPaused)
				unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}

	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}
}
