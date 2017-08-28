package com.example;

import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskConfiguratorHelper;
import java.util.*;  

public class ExampleTaskConfigurator extends AbstractTaskConfigurator {
    //TODO (save / validation logic)
@NotNull
@Override
public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition)
    {
	final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
	TaskConfiguratorHelper taskConfiguratorHelper = (TaskConfiguratorHelper) ContainerManager.getInstance().getContainerContext().getComponent("taskConfiguratorHelper");
	taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(config, params, FIELDS);
	return config;
    }
    
}
