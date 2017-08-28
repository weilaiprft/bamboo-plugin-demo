package com.example;

import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskConfiguratorHelper;
import java.util.*;  

public class ExampleTaskConfigurator extends AbstractTaskConfigurator {

	private String[] fields = new String[]{
			"uid",
			"pwd"
	};

	//TODO (save / validation logic)
	@Override
	public Map<String, String> generateTaskConfigMap(final ActionParametersMap params, final TaskDefinition previousTaskDefinition) {
		final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

		for (String field : fields) {
			config.put(field, params.getString(field));
		}

		return config;
	}
}
