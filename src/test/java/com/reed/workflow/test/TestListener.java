package com.reed.workflow.test;

import org.activiti.engine.runtime.Execution;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.reed.workflow.listener.BaseListener;

@Component
public class TestListener extends BaseListener {

	/** Logger */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(TestListener.class);

	public void start() {
		LOGGER.info("===========>start");
	}

	public void end() {
		LOGGER.info("===========>end");
	}

	public void t1Create(Task t) {
		LOGGER.info("===========>create t1:taskId:{},procId:{}", t.getId(),
				t.getProcessInstanceId());
		// claim(task,who)
		super.getTaskService().claim(t.getId(), t.getId());
	}

	public void t1Assign(Task t) {
		LOGGER.info("===========>assign t1:taskId:{},procId:{}", t.getId(),
				t.getProcessInstanceId());
	}

	public void t1Complate(Task t) {
		LOGGER.info("===========>complate t1:taskId:{},procId:{}", t.getId(),
				t.getProcessInstanceId());

	}

	public void t2Create(Task t) {
		LOGGER.info("===========>create t2:taskId:{},procId:{}", t.getId(),
				t.getProcessInstanceId());
		// claim(task,who)
		super.getTaskService().claim(t.getId(), t.getId());
	}

	public void t2Assign(Task t) {
		LOGGER.info("===========>assign t2:taskId:{},procId:{}", t.getId(),
				t.getProcessInstanceId());
	}

	public void t2Complate(Task t) {
		LOGGER.info("===========>complate t2:taskId:{},procId:{}", t.getId(),
				t.getProcessInstanceId());

	}

	public void t1All(Task t) {
		LOGGER.info("===========>All t1:taskId:{},procId:{}", t.getId(),
				t.getProcessInstanceId());
	}

	public void t1Pass(Execution t) {
		LOGGER.info("===========>pass t1:procId:{}", t.getProcessInstanceId());
	}

	public void t1Reject(Execution t) {
		LOGGER.info("===========>reject t1:procId:{}", t.getProcessInstanceId());
	}

	public void t2Pass(Execution t) {
		LOGGER.info("===========>pass t2:procId:{}", t.getProcessInstanceId());
	}

	public void t2Reject(Execution t) {
		LOGGER.info("===========>reject t2:procId:{}", t.getProcessInstanceId());
	}
}
