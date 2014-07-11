/**
 * WorkflowDeployService.java
 * Copyright (c) 2013 by lashou.com
 */
package com.reed.workflow.service;

import org.activiti.engine.RepositoryService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * 工作流发布Service
 * 
 * @author reed
 */
@Service
public class WorkflowDeployService {

	protected Logger logger = LoggerFactory
			.getLogger(WorkflowDeployService.class);

	/** 工作流库 Service */
	@Autowired
	protected RepositoryService repositoryService;

	/**
	 * 发布工作流模版
	 * 
	 * @param processkey
	 * @param fileName
	 * @return 0：失败，1成功
	 */
	public int deployProcess(String processkey, String fileName) {
		if (StringUtils.isBlank(processkey) || StringUtils.isBlank(fileName)) {
			return 0;
		}
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		try {
			Resource resource = resourceLoader.getResource(fileName);
			InputStream inputStream = resource.getInputStream();
			if (fileName.endsWith(".bpmn")) {// use bpmn to deploy
				repositoryService
						.createDeployment()
						.addInputStream(processkey + ".bpmn20.xml", inputStream)
						.deploy();
			} else if (fileName.endsWith(".bar")) {// using bar to deploy
				logger.debug("read workflow from: {}", fileName);
				if (inputStream == null) {
					logger.warn("ignore deploy workflow module: {}", fileName);
				} else {
					logger.debug("finded workflow module: {}, deploy it!",
							fileName);
					ZipInputStream zis = new ZipInputStream(inputStream);
					repositoryService.createDeployment().addZipInputStream(zis)
							.deploy();
				}
			}
			return 1;
		} catch (FileNotFoundException e) {
			logger.error("deploy failed:=====>can not find file:" + fileName);
			return 0;
		} catch (IOException e) {
			logger.error("deploy failed:=====>" + e.getMessage());
			e.printStackTrace();
			return 0;
		}
	}
}
