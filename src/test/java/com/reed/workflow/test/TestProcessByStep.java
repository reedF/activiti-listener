package com.reed.workflow.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.FormService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.reed.workflow.service.WorkflowDeployService;

/**
 * test the whole process step by step to see how did the listener work
 * 
 * @author reed
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:test.xml")
public class TestProcessByStep {

	private static final String key = "test1";
	private static final String bus_key = key + "_" + 2;
	private static ProcessInstance p;
	private static Task t;

	@Autowired
	private TaskService taskService;

	@Autowired
	private FormService formService;

	@Autowired
	private RuntimeService runTimeService;

	@Autowired
	private WorkflowDeployService deployService;

	@Before
	public final void setUp() {
		p = runTimeService.createProcessInstanceQuery()
				.processDefinitionKey(key).processInstanceBusinessKey(bus_key)
				.active().singleResult();
		if (p != null) {
			t = taskService.createTaskQuery().processInstanceId(p.getId())
					.active().singleResult();
		}
	}

	@After
	public final void clear() {
		t = null;
		p = null;
	}

	// 1
	@Test
	public void startProcess() {
		int r = deployService
				.deployProcess(key, "classpath:diagrams/test.bpmn");
		p = runTimeService.startProcessInstanceByKey(key, bus_key);
		Assert.assertNotNull(p);
	}

	// 2
	@Test
	public void rejectTask1() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("pass", "false");
		taskService.complete(t.getId(), m);
	}

	// 3
	@Test
	public void passTask1() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("pass", "true");
		taskService.complete(t.getId(), m);
	}

	// 4
	@Test
	public void rejectTask2() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("pass", "false");
		taskService.complete(t.getId(), m);
	}

	// 5
	@Test
	public void passTask2() {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("pass", "true");
		taskService.complete(t.getId(), m);
	}

}
