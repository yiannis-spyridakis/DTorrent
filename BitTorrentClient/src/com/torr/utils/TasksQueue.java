package com.torr.utils;

import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;

public class TasksQueue {
	// Queue of tasks to be performed on the thread
	protected LinkedBlockingQueue<FutureTask<Void>> tasks 
		= new LinkedBlockingQueue<FutureTask<Void>>();
	
	protected void addTask(FutureTask<Void> task)
	{
		tasks.add(task);
	}
	
	protected void processOutstandingTasks() throws Exception
	{
		while(!tasks.isEmpty())
		{
			tasks.take().get();
		}
	}
	
}
