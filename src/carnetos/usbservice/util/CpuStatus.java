package carnetos.usbservice.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.internal.os.ProcessCpuTracker;

public class CpuStatus {

	public static String getCPURateDesc() {

		Runtime.getRuntime().availableProcessors();

		ProcessCpuTracker stats = new ProcessCpuTracker(false);

		final int userTime = stats.getLastUserTime();
		final int systemTime = stats.getLastSystemTime();
		final int iowaitTime = stats.getLastIoWaitTime();
		final int irqTime = stats.getLastIrqTime();
		final int softIrqTime = stats.getLastSoftIrqTime();
		final int idleTime = stats.getLastIdleTime();

		final int totalTime = userTime + systemTime + iowaitTime + irqTime + softIrqTime + idleTime;
		if (totalTime == 0) {
			return null;
		}
		int userW = (userTime) / totalTime;
		int systemW = (systemTime) / totalTime;
		int irqW = ((iowaitTime + irqTime + softIrqTime)) / totalTime;
		String path = "/proc/stat";// 系统CPU信息文件
		/**
		 * CUP总消耗时间
		 */
		long totalJiffies[] = new long[2];
		/**
		 * 总空闲CPU时间
		 */
		long totalIdle[] = new long[2];
		int firstCPUNum = 0;// 设置这个参数，这要是防止两次读取文件获知的CPU数量不同，导致不能计算。这里统一以第一次的CPU数量为基准
		FileReader fileReader = null;
		BufferedReader bufferedReader = null;
		Pattern pattern = Pattern.compile(" [0-9]+");
		for (int i = 0; i < 2; i++) {
			totalJiffies[i] = 0;
			totalIdle[i] = 0;
			try {
				fileReader = new FileReader(path);
				bufferedReader = new BufferedReader(fileReader, 8192);
				int currentCPUNum = 0;
				String str;
				while ((str = bufferedReader.readLine()) != null && (i == 0 || currentCPUNum < firstCPUNum)) {
					if (str.toLowerCase().startsWith("cpu")) {
						currentCPUNum++;
						int index = 0;
						Matcher matcher = pattern.matcher(str);
						while (matcher.find()) {
							try {
								long tempJiffies = Long.parseLong(matcher.group(0).trim());
								totalJiffies[i] += tempJiffies;
								if (index == 3) {// 空闲时间为该行第4条栏目
									totalIdle[i] += tempJiffies;
								}
								index++;
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
						}
					}
					if (i == 0) {
						firstCPUNum = currentCPUNum;
						try {// 暂停50毫秒，等待系统更新信息。
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		double rate = -1;
		if (totalJiffies[0] > 0 && totalJiffies[1] > 0 && totalJiffies[0] != totalJiffies[1]) {
			rate = 1.0 * ((totalJiffies[1] - totalIdle[1]) - (totalJiffies[0] - totalIdle[0])) / (totalJiffies[1] - totalJiffies[0]);
		}

		return String.format("cpu:%.2f", rate);
	}

	/**
	 * 获取当前空闲的CPU占有率
	 * @param mLastSystemTime
	 * @param mLastIdleTime
	 * @return
	 */
	public static float getCpuIdleTime(float mLastSystemTime, float mLastIdleTime) { // 获取系统总CPU使用时间
//		String[] cpuInfos = null;
//		try {
//			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/stat")), 1000);
//			String load = reader.readLine();
//			reader.close();
//			cpuInfos = load.split("\\s+");
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
//		int offSet = 1 ;
//		// Total user time is user + nice time.
//		long user = Long.parseLong(cpuInfos[1]);
//		long nice = Long.parseLong(cpuInfos[1+offSet]);// 从系统启动开始累计到当前时刻，nice值为负的进程所占用的CPU时间
//		long systemtime = Long.parseLong(cpuInfos[2+offSet]);
//		long idle = Long.parseLong(cpuInfos[3+offSet]);
//		long iowait = Long.parseLong(cpuInfos[4+offSet]);
//		long irq = Long.parseLong(cpuInfos[5+offSet]);// 从系统启动开始累计到当前时刻，硬中断时间（单位：jiffies）
//		long softirq = Long.parseLong(cpuInfos[6+offSet]);// 从系统启动开始累计到当前时刻，软中断时间
//
//		long totalCpuTime = user + nice + systemtime + idle + iowait + irq + softirq;
//		if (totalCpuTime - mLastSystemTime != 0) {
//			float cpuIdleStatus = ((float) ((float) idle - (float) mLastIdleTime)) / ((float) totalCpuTime - (float) mLastSystemTime);
//			mLastSystemTime = totalCpuTime;
//			mLastIdleTime = idle;
//			return cpuIdleStatus;
//		}
		return -1f;
	}

}
