package com.example;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.deployments.environments.Environment;
import com.atlassian.bamboo.deployments.execution.service.DeploymentExecutionService;
import com.atlassian.bamboo.deployments.projects.DeploymentProject;
import com.atlassian.bamboo.deployments.projects.service.DeploymentProjectService;
import com.atlassian.bamboo.deployments.versions.DeploymentVersion;
import com.atlassian.bamboo.deployments.versions.service.DeploymentVersionService;
import com.atlassian.bamboo.task.*;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

@Scanned
public class TaskExample implements CommonTaskType {
	@ComponentImport
	private final DeploymentVersionService deploymentVersionService;

	@ComponentImport
	private final DeploymentProjectService deploymentProjectService;

	@ComponentImport
	private final DeploymentExecutionService deploymentExecutionService;

	public TaskExample(DeploymentVersionService deploymentVersionService,
			DeploymentProjectService deploymentProjectService, DeploymentExecutionService deploymentExecutionService) {
		this.deploymentVersionService = deploymentVersionService;
		this.deploymentProjectService = deploymentProjectService;
		this.deploymentExecutionService = deploymentExecutionService;
	}
	
	private boolean performUpdate(final CommonTaskContext taskContext) {
		boolean result = false;
		final BuildLogger buildLogger = taskContext.getBuildLogger();
	     try {
			buildLogger.addBuildLogEntry("*****************  Navigator Version Updater Plugin *****************");
			final String uid = taskContext.getConfigurationMap().get("uid");
			buildLogger.addBuildLogEntry("use login : " + uid);
			final String pwd = taskContext.getConfigurationMap().get("pwd");
			//buildLogger.addBuildLogEntry("use pwd : " + pwd);
			// working dir
			File workingDir = taskContext.getWorkingDirectory();
			buildLogger.addBuildLogEntry("workingDir directory is " + workingDir.getAbsolutePath());

			// get jar dir
			File jarDir = new File(workingDir.getAbsolutePath() + File.separator + "target");
			buildLogger.addBuildLogEntry("jarDir directory is " + jarDir.getAbsolutePath());

			// get jarfile
			File[] jarFiles = jarDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			});

			// should only have 1
			buildLogger.addBuildLogEntry("number of jar files is " + jarFiles.length);
			String buildFile = "";
			for (File f : jarFiles) {
				buildLogger.addBuildLogEntry("jar file name is " + f.getName());
				buildFile = f.getName();
			}
			
	        final String url = "https://imgcmd01.dev.flagstar.com:9443/navigator/";
	        final String filePath = "/nas/webpatches/navigator/dev/case/plugins/" + buildFile;	        
	        
	        buildLogger.addBuildLogEntry("updating navigator plugin version to " + buildFile);
	        UpdatePluginVersion upv = new UpdatePluginVersion(url, uid, pwd, filePath, buildLogger);	        
			result = upv.perform();			

			return result;
		} catch (Exception exception) {
			return result;			
		}
     
	}	

	@Override
	public TaskResult execute(final CommonTaskContext taskContext) throws TaskException {

		final TaskResultBuilder builder = TaskResultBuilder.newBuilder(taskContext).failed(); //Initially set to Failed.

		if (performUpdate(taskContext)){
		   builder.success();
		}

		final TaskResult result = builder.build();
		return result;
	}

	private DeploymentProject getMatchingDeploymentProject(String name) {

		List<DeploymentProject> allDeploymentProjects = deploymentProjectService.getAllDeploymentProjects();

		for (DeploymentProject deploymentProject : allDeploymentProjects) {
			if (deploymentProject.getName().equals(name))
				return deploymentProject;
		}

		throw new RuntimeException("Unable to find deployment project: " + name);
	}

	private Environment getMatchingEnvironment(DeploymentProject deploymentProject, String name) {

		for (Environment environment : deploymentProject.getEnvironments()) {
			if (environment.getName().equals(name))
				return environment;
		}

		throw new RuntimeException("Unable to find environment: " + name);
	}

}
