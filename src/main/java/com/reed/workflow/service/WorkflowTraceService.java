package com.reed.workflow.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.history.HistoricVariableInstanceQuery;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.bpmn.diagram.ProcessDiagramGenerator;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @ClassName: WorkflowTraceService
 * @author reed
 */
@Service
public class WorkflowTraceService {
	protected Logger logger = LoggerFactory
			.getLogger(WorkflowTraceService.class);

	/** 工作流运行状态 Service */
	@Autowired
	protected RuntimeService runtimeService;
	/** 任务 service */
	@Autowired
	protected TaskService taskService;
	/** 工作流库 Service */
	@Autowired
	protected RepositoryService repositoryService;
	/** 工作流 Service */
	@Autowired
	protected IdentityService identityService;
	/** 工作流历史 Service */
	@Autowired
	protected HistoryService historyService;
	/** 工作流 Engine */
	@Autowired
	ProcessEngineFactoryBean processEngine;

	/**
	 * 根据流程实例ID查询流程定义对象{@link ProcessDefinition}
	 * 
	 * @param processInstanceId
	 *            流程实例ID
	 * @return 流程定义对象{@link ProcessDefinition}
	 */
	public ProcessDefinition findProcessDefinitionByPid(String processInstanceId) {
		HistoricProcessInstance historicProcessInstance = historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		String processDefinitionId = historicProcessInstance
				.getProcessDefinitionId();
		ProcessDefinition processDefinition = findProcessDefinition(processDefinitionId);
		return processDefinition;
	}

	/**
	 * 根据流程定义ID查询流程定义对象{@link ProcessDefinition}
	 * 
	 * @param processDefinitionId
	 *            流程定义对象ID
	 * @return 流程定义对象{@link ProcessDefinition}
	 */
	public ProcessDefinition findProcessDefinition(String processDefinitionId) {
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
		return processDefinition;
	}

	/**
	 * 获取发起者Id,相应的需要在流程启动是要显示的给starter进行赋值
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public String findStarter(String processInstanceId) {
		List<IdentityLink> idLink = runtimeService
				.getIdentityLinksForProcessInstance(processInstanceId);
		if (idLink != null) {
			for (IdentityLink indentityLink : idLink) {
				if (indentityLink.getType().equalsIgnoreCase("starter")) {
					return indentityLink.getUserId();
				}
			}
		}
		return null;
	}

	/**
	 * 流程跟踪图，该方法可以配合js生成每个节点的信息。
	 * 
	 * @param processInstanceId
	 *            流程实例ID
	 * @return 封装了各种节点信息
	 */
	public List<Map<String, Object>> traceProcess(String processInstanceId)
			throws Exception {
		Execution execution = runtimeService.createExecutionQuery()
				.executionId(processInstanceId).singleResult();// 执行实例

		// Object property = PropertyUtils.getProperty(execution, "activityId");
		// String activityId = "";
		// if (property != null) {
		// activityId = property.toString();
		// }
		String activityId = "";
		if (execution.getActivityId() != null) {
			activityId = execution.getActivityId();
		}
		ProcessInstance processInstance = runtimeService
				.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
				.getDeployedProcessDefinition(processInstance
						.getProcessDefinitionId());
		List<ActivityImpl> activitiList = processDefinition.getActivities();// 获得当前任务的所有节点

		List<Map<String, Object>> activityInfos = new ArrayList<Map<String, Object>>();
		for (ActivityImpl activity : activitiList) {

			boolean currentActiviti = false;
			String id = activity.getId();

			// 当前节点
			if (id.equals(activityId)) {
				currentActiviti = true;
			}

			Map<String, Object> activityImageInfo = packageSingleActivitiInfo(
					activity, processInstance, currentActiviti);

			activityInfos.add(activityImageInfo);
		}

		return activityInfos;
	}

	/**
	 * 获取历史工作流实例流转过程中的变量参数
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public List<HistoricVariableInstance> findHistoryVarinst(
			String processInstanceId) {
		List<HistoricVariableInstance> list = historyService
				.createHistoricVariableInstanceQuery()
				.processInstanceId(processInstanceId).list();
		// for (HistoricVariableInstance variable : list) {
		// System.out.println("variable: " + variable.getVariableName()
		// + " = " + variable.getValue());
		// }
		return list;
	}

	/**
	 * 获取历史工作流实例流程过程中的表单数据
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public List<HistoricDetail> findHistoryFormData(String processInstanceId) {
		List<HistoricDetail> formProperties = historyService
				.createHistoricDetailQuery()
				.processInstanceId(processInstanceId).formProperties().list();
		// for (HistoricDetail historicDetail : formProperties) {
		// HistoricFormProperty field = (HistoricFormProperty) historicDetail;
		// System.out.println("field id: " + field.getPropertyId()
		// + ", value: " + field.getPropertyValue());
		// }

		return formProperties;
	}

	/**
	 * 通过 processInstanceId 获取 当前运行节点的 CandidateGroupIdExpression 表达式
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public String getCandGrpIdExpByProcInstId(String processInstanceId) {
		Execution execution = runtimeService.createExecutionQuery()
				.executionId(processInstanceId).singleResult();// 执行实例
		String activityId = "";
		if (execution != null) {
			activityId = execution.getActivityId();

			ProcessInstance processInstance = runtimeService
					.createProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();
			// ProcessDefinitionEntity processDefinition =
			// (ProcessDefinitionEntity)
			// ((RepositoryServiceImpl)
			// repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());
			ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) ((RepositoryServiceImpl) processEngine
					.getProcessEngineConfiguration().getRepositoryService())
					.getDeployedProcessDefinition(processInstance
							.getProcessDefinitionId());
			List<ActivityImpl> activitiList = processDefinition.getActivities();// 获得当前任务的所有节点

			for (ActivityImpl activity : activitiList) {
				String id = activity.getId();
				// 当前节点
				if (id.equals(activityId)) {
					ActivityBehavior activityBehavior = activity
							.getActivityBehavior();
					if (activityBehavior instanceof UserTaskActivityBehavior) {

						/*
						 * 当前任务的分配角色
						 */
						UserTaskActivityBehavior userTaskActivityBehavior = (UserTaskActivityBehavior) activityBehavior;
						TaskDefinition taskDefinition = userTaskActivityBehavior
								.getTaskDefinition();
						Set<Expression> candidateGroupIdExpressions = taskDefinition
								.getCandidateGroupIdExpressions();
						if (candidateGroupIdExpressions != null) {
							for (Expression expression : candidateGroupIdExpressions) {
								return expression.getExpressionText();
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * 根据流程id,获取当前taskId
	 * 
	 * @param processInstanceId
	 * @return
	 */
	public String getTaskId(String processInstanceId) {
		TaskQuery todoQuery = taskService.createTaskQuery().processInstanceId(
				processInstanceId);
		List<Task> taskList = todoQuery.list();
		if (taskList != null && taskList.size() > 0) {
			return taskList.get(0).getId();
		} else {
			return null;
		}
	}

	public String getProcessInstanceIdByTaskId(String taskId) {
		TaskQuery todoQuery = taskService.createTaskQuery().taskId(taskId);
		List<Task> taskList = todoQuery.list();
		if (taskList != null && taskList.size() > 0) {
			return taskList.get(0).getProcessInstanceId();
		} else {
			return null;
		}
	}

	/**
	 * 转换流程节点类型为中文说明
	 * 
	 * 
	 * @param type
	 * @param type
	 *            英文名称
	 * @return 翻译后的中文名称
	 */
	public static String parseToZhType(String type) {
		Map<String, String> types = new HashMap<String, String>();
		types.put("userTask", "用户任务");
		types.put("serviceTask", "系统任务");
		types.put("startEvent", "开始节点");
		types.put("endEvent", "结束节点");
		types.put("exclusiveGateway", "条件判断节点(系统自动根据条件处理)");
		types.put("inclusiveGateway", "并行处理任务");
		types.put("callActivity", "子流程");
		return types.get(type) == null ? type : types.get(type);
	}

	/**
	 * 封装输出信息，包括：当前节点的X、Y坐标、变量信息、任务类型、任务描述
	 * 
	 * @param activity
	 * @param processInstance
	 * @param currentActiviti
	 * @return
	 */
	private Map<String, Object> packageSingleActivitiInfo(
			ActivityImpl activity, ProcessInstance processInstance,
			boolean currentActiviti) throws Exception {
		Map<String, Object> vars = new HashMap<String, Object>();
		Map<String, Object> activityInfo = new HashMap<String, Object>();
		activityInfo.put("currentActiviti", currentActiviti);
		setPosition(activity, activityInfo);
		setWidthAndHeight(activity, activityInfo);

		Map<String, Object> properties = activity.getProperties();
		vars.put("任务类型", parseToZhType(properties.get("type").toString()));
		ActivityBehavior activityBehavior = activity.getActivityBehavior();
		logger.debug("activityBehavior={}", activityBehavior);
		if (activityBehavior instanceof UserTaskActivityBehavior) {

			Task currentTask = null;

			/*
			 * 当前节点的task
			 */
			if (currentActiviti) {
				currentTask = getCurrentTaskInfo(processInstance);
			}

			/*
			 * 当前任务的分配角色
			 */
			UserTaskActivityBehavior userTaskActivityBehavior = (UserTaskActivityBehavior) activityBehavior;
			TaskDefinition taskDefinition = userTaskActivityBehavior
					.getTaskDefinition();
			Set<Expression> candidateGroupIdExpressions = taskDefinition
					.getCandidateGroupIdExpressions();
			if (!candidateGroupIdExpressions.isEmpty()) {

				// 任务的处理角色
				setTaskGroup(vars, candidateGroupIdExpressions);

				// 当前处理人
				if (currentTask != null) {
					setCurrentTaskAssignee(vars, currentTask);
				}
			}
		}

		vars.put("节点说明", properties.get("documentation"));

		String description = activity.getProcessDefinition().getDescription();
		vars.put("描述", description);

		logger.debug("trace variables: {}", vars);
		activityInfo.put("vars", vars);
		return activityInfo;
	}

	private void setTaskGroup(Map<String, Object> vars,
			Set<Expression> candidateGroupIdExpressions) {
		String roles = "";
		for (Expression expression : candidateGroupIdExpressions) {
			String expressionText = expression.getExpressionText();
			String roleName = identityService.createGroupQuery()
					.groupId(expressionText).singleResult().getName();
			roles += roleName;
		}
		vars.put("任务所属角色", roles);
	}

	/**
	 * 设置当前处理人信息
	 * 
	 * @param vars
	 * @param currentTask
	 */
	private void setCurrentTaskAssignee(Map<String, Object> vars,
			Task currentTask) {
		String assignee = currentTask.getAssignee();
		if (assignee != null) {
			User assigneeUser = identityService.createUserQuery()
					.userId(assignee).singleResult();
			String userInfo = assigneeUser.getFirstName() + " "
					+ assigneeUser.getLastName();
			vars.put("当前处理人", userInfo);
		}
	}

	/**
	 * 获取当前节点信息
	 * 
	 * @param processInstance
	 * @return
	 */
	public Task getCurrentTaskInfo(ProcessInstance processInstance) {
		Task currentTask = null;
		try {
			// String activitiId = (String)
			// PropertyUtils.getProperty(processInstance, "activityId");
			String activitiId = processInstance.getActivityId();
			logger.debug("current activity id: {}", activitiId);

			currentTask = taskService.createTaskQuery()
					.processInstanceId(processInstance.getId())
					.taskDefinitionKey(activitiId).singleResult();
			logger.debug("current task for processInstance: {}",
					ToStringBuilder.reflectionToString(currentTask));

		} catch (Exception e) {
			logger.error(
					"can not get property activityId from processInstance: {}",
					processInstance);
		}
		return currentTask;
	}

	/**
	 * 根据流程定义key,业务实体ID查询当前运行task
	 * 
	 * @param businessKey
	 * @param processDefKey
	 * @return
	 */
	public List<Task> findTasksByBusinessKey(String businessKey,
			String processDefKey) {
		List<Task> tasks = null;
		if (StringUtils.isNotBlank(businessKey)
				&& StringUtils.isNotBlank(processDefKey)) {
			tasks = new ArrayList<Task>();
			TaskQuery todoQuery = taskService.createTaskQuery()
					.processDefinitionKey(processDefKey)
					.processInstanceBusinessKey(businessKey).active()
					.orderByTaskId().desc().orderByTaskCreateTime().desc();
			List<Task> todoList = todoQuery.list();
			tasks.addAll(todoList);
		}
		return tasks;
	}

	/**
	 * 设置宽度、高度属性
	 * 
	 * @param activity
	 * @param activityInfo
	 */
	private void setWidthAndHeight(ActivityImpl activity,
			Map<String, Object> activityInfo) {
		activityInfo.put("width", activity.getWidth());
		activityInfo.put("height", activity.getHeight());
	}

	/**
	 * 设置坐标位置
	 * 
	 * @param activity
	 * @param activityInfo
	 */
	private void setPosition(ActivityImpl activity,
			Map<String, Object> activityInfo) {
		activityInfo.put("x", activity.getX());
		activityInfo.put("y", activity.getY());
	}

	/**
	 * 读取资源图片
	 * 
	 * @param processInstanceId
	 *            流程Id
	 * @return
	 */
	public InputStream getResourceStream(String processInstanceId,
			String resourceType) {
		InputStream resourceAsStream = null;
		ProcessInstance processInstance = runtimeService
				.createProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(processInstance.getProcessDefinitionId())
				.singleResult();

		String resourceName = "";
		if (resourceType.equals("image")) {
			resourceName = processDefinition.getDiagramResourceName();
		} else if (resourceType.equals("xml")) {
			resourceName = processDefinition.getResourceName();
		}
		resourceAsStream = repositoryService.getResourceAsStream(
				processDefinition.getDeploymentId(), resourceName);
		return resourceAsStream;
	}

	// /*********************** new method in use
	// *************************************************/
	//
	// /**
	// * 查询流程实例Id,该方法包含了从历史表中取流程Id的功能
	// *
	// * @param bussinessId
	// * 业务Id
	// * @param key
	// * 业务对应的流程模版key
	// * @return
	// */
	// public String findProcInstIdByBussinessKeyAndKey(String bussinessId,
	// String key) {
	// ProcessDefinition pd = findProcessDefinitionByKey(key);
	// if (pd != null) {
	// Execution execution = runtimeService.createExecutionQuery()
	// .processInstanceBusinessKey(bussinessId)
	// .processDefinitionId(pd.getId()).singleResult();
	// if (execution != null) {
	// return execution.getProcessInstanceId();
	// } else {
	// HistoricProcessInstance history = historyService
	// .createHistoricProcessInstanceQuery()
	// .processInstanceBusinessKey(bussinessId)
	// .processDefinitionId(pd.getId()).singleResult();
	// if (history != null) {
	// return history.getId();
	// } else
	// return null;
	// }
	// } else {
	// return null;
	// }
	// }

	/**
	 * @param processInstanceId
	 */
	public String getProcessStarter(String processInstanceId) {
		HistoricVariableInstance historyVariable = historyService
				.createHistoricVariableInstanceQuery().variableName("starter")
				.processInstanceId(processInstanceId).singleResult();
		return historyVariable.getValue().toString();

	}

	/**
	 * 获取传入类别的工作流定义
	 * 
	 * @param processInstanceType
	 */
	public ProcessDefinition getAllPocessInstanceDefined(
			String processInstanceType) {
		List<ProcessDefinition> proDefinList = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionKeyLike(processInstanceType)
				.orderByProcessDefinitionVersion().desc().list();

		if (proDefinList != null && proDefinList.size() > 0) {
			return proDefinList.get(0);
		} else {
			return null;
		}
	}

	public List<HistoricTaskInstance> findHisTasksByProInstIdDesc(
			String procInstId) {
		return historyService.createHistoricTaskInstanceQuery()
				.orderByHistoricTaskInstanceStartTime().desc()
				.processInstanceId(procInstId).list();
	}

	public List<HistoricTaskInstance> findHisTasksByProInstIdAndTaskKeyDesc(
			String procInstId, String taskKey) {
		return historyService.createHistoricTaskInstanceQuery()
				.taskDefinitionKey(taskKey)
				.orderByHistoricTaskInstanceStartTime().desc()
				.processInstanceId(procInstId).list();
	}

	public List<HistoricVariableInstance> findHisVarsByProInstIdAndTaskId(
			String taskId, String procInstId) {
		return historyService.createHistoricVariableInstanceQuery()
				.taskId(taskId).processInstanceId(procInstId).list();
	}

	/********************* 20130905修改 ********************************/
	/**
	 * 获取流程历史表中的流程实例对象
	 * 
	 * @param bussinessKey
	 *            业务ID
	 * @param key
	 *            流程模版Key
	 * @return
	 */
	public HistoricProcessInstance getHisPorcInstance(String bussinessKey,
			String key) {
		List<HistoricProcessInstance> inst = historyService
				.createHistoricProcessInstanceQuery()
				.processInstanceBusinessKey(bussinessKey).list();
		if (inst != null) {
			for (HistoricProcessInstance h : inst) {
				if (h.getProcessDefinitionId().startsWith(key)) {
					return h;
				}
			}
		}
		return null;
	}

	/**
	 * 获取流程历史表ActivityInstance实例List
	 * 
	 * @param bussKey
	 *            业务ID
	 * @param page
	 *            分页对象
	 * @param key
	 *            流程模版Key
	 * @return
	 */
	public List<HistoricActivityInstance> getHisActInstList(String bussKey,
			String key) {
		List<HistoricActivityInstance> hisActivityIns = null;
		HistoricProcessInstance hisProcIns = getHisPorcInstance(bussKey, key);
		if (hisProcIns != null) {
			String procInsId = hisProcIns.getId();
			hisActivityIns = historyService
					.createHistoricActivityInstanceQuery()
					.processInstanceId(procInsId)
					.orderByHistoricActivityInstanceStartTime().asc()
					.orderByHistoricActivityInstanceEndTime().asc().list();
		}
		return hisActivityIns;
	}

	/**
	 * 获取流程历史表VariableInstance实例List
	 * 
	 * @param bussKey
	 *            业务ID
	 * @param page
	 *            分页对象
	 * @param key
	 *            流程模版Key
	 * @return
	 */
	public List<HistoricVariableInstance> getHisVariableInstList(
			String bussKey, String key) {
		List<HistoricVariableInstance> hisVarInsList = null;
		HistoricProcessInstance hisProcIns = getHisPorcInstance(bussKey, key);

		if (hisProcIns != null) {
			String procInsId = hisProcIns.getId();
			HistoricVariableInstanceQuery hisVarInsQuery = historyService
					.createHistoricVariableInstanceQuery();
			hisVarInsList = hisVarInsQuery.processInstanceId(procInsId).list();

		}
		return hisVarInsList;
	}

	/**
	 * 读取带跟踪的图片 需要通过历史数据进行读取
	 * 
	 * @param businessKey
	 * @param executionId
	 * @return
	 */
	public InputStream traceImg(String businessKey, String processKey) {

		HistoricProcessInstance hisProIns = getHisPorcInstance(businessKey,
				processKey);

		if (hisProIns != null) {
			BpmnModel bpmnModel = repositoryService.getBpmnModel(hisProIns
					.getProcessDefinitionId());
			List<HistoricActivityInstance> actList = historyService
					.createHistoricActivityInstanceQuery()
					.processInstanceId(hisProIns.getId())
					.orderByHistoricActivityInstanceStartTime().desc()
					.orderByHistoricActivityInstanceEndTime().desc().list();
			// 展示红圈的List
			List<String> activeActivityIds = new ArrayList<String>();
			if (actList != null && actList.size() > 0) {
				// for(HistoricActivityInstance act:actList){
				// if(act.getEndTime()==null){
				// activeActivityIds.add(act.getActivityId());
				// }
				// }
				if (actList.get(0).getActivityName().endsWith("Gateway")) {
					activeActivityIds.add(actList.get(1).getActivityId());
				} else {
					activeActivityIds.add(actList.get(0).getActivityId());
				}
			}
			// 使用spring注入引擎请使用下面的这行代码
			Context.setProcessEngineConfiguration(processEngine
					.getProcessEngineConfiguration());

			InputStream imageStream = ProcessDiagramGenerator.generateDiagram(
					bpmnModel, "png", activeActivityIds);

			return imageStream;
		} else
			return null;
	}

	/**
	 * 读取带跟踪的图片,针对并行任务,获取当前多个task定位显示
	 * 
	 * @param businessKey
	 * @param processKey
	 * @return
	 */
	public InputStream traceImgByParallel(String businessKey, String processKey) {

		HistoricProcessInstance hisProIns = getHisPorcInstance(businessKey,
				processKey);

		if (hisProIns != null) {
			BpmnModel bpmnModel = repositoryService.getBpmnModel(hisProIns
					.getProcessDefinitionId());
			List<Task> t = taskService.createTaskQuery()
					.processInstanceId(hisProIns.getId())
					.processInstanceBusinessKey(businessKey).list();
			// List<HistoricActivityInstance> actList = null;
			List<String> activeActivityIds = new ArrayList<String>();
			// 如果是并行任务，则需要同时列出红框
			if (t != null && t.size() > 0) {
				// actList = new ArrayList<HistoricActivityInstance>();
				for (Task task : t) {
					if (task != null) {
						List<String> act = runtimeService
								.getActiveActivityIds(task.getExecutionId());
						if (act != null) {
							activeActivityIds.addAll(act);
						}
					}
				}
			}
			// 使用spring注入引擎请使用下面的这行代码
			Context.setProcessEngineConfiguration(processEngine
					.getProcessEngineConfiguration());
			InputStream imageStream = null;
			if (activeActivityIds.size() > 0) {
				imageStream = ProcessDiagramGenerator.generateDiagram(
						bpmnModel, "png", activeActivityIds);
			} else {
				// end
				imageStream = traceImg(businessKey, processKey);
			}
			return imageStream;
		} else
			return null;
	}

}
