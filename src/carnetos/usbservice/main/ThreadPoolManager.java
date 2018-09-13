package carnetos.usbservice.main;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

public final class ThreadPoolManager {

	private static ThreadPoolManager instance;
	private static final int POOL_SIZE = 5;
	private ExecutorService fixedThreadPool;

	private ThreadPoolManager() {
		fixedThreadPool = Executors.newFixedThreadPool(POOL_SIZE);
	}

	public static ThreadPoolManager getInstance() {
		if (instance == null) {
			synchronized (ThreadPoolManager.class) {
				if (instance == null) {
					instance = new ThreadPoolManager();
				}
			}
		}
		return instance;
	}

	/**
	 * 从线程池获取线程，执行任务
	 * 
	 * @param workTask
	 * @return
	 */
	public Future<?> execute(Runnable workTask) {
		Future<?> future = fixedThreadPool.submit(workTask);
		return future;
	}
	
	/**
	 * 获取当前活动的线程数
	 * @return
	 */
	public int getActiveCount() {
		return ((ThreadPoolExecutor)fixedThreadPool).getActiveCount();
	}

}