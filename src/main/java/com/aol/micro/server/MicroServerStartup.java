package com.aol.micro.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.aol.micro.server.config.Config;
import com.aol.micro.server.config.Microserver;
import com.aol.micro.server.config.MicroserverConfigurer;
import com.aol.micro.server.module.Module;
import com.aol.micro.server.servers.ApplicationRegister;
import com.aol.micro.server.servers.ServerApplication;
import com.aol.micro.server.servers.ServerRunner;
import com.aol.micro.server.servers.model.GrizzlyApplicationFactory;
import com.aol.micro.server.spring.SpringContextFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nurkiewicz.lazyseq.LazySeq;

public class MicroServerStartup {
	
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final List<Module> modules;
	private final CompletableFuture end = new CompletableFuture();

	@Getter
	private final AnnotationConfigWebApplicationContext springContext;

	
	public MicroServerStartup(Module...modules){
		this.modules = Lists.newArrayList(modules);
		springContext = new SpringContextFactory(new MicroserverConfigurer().buildConfig(extractClass()),extractClass(),
				modules[0].getSpringConfigurationClasses()).createSpringContext();
		
	}
	private Class extractClass() {
		try {
			return Class.forName(new Exception().getStackTrace()[2].getClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
	}
	
	public MicroServerStartup(Class c, Module... modules) {
		
		this.modules = Lists.newArrayList(modules);
		springContext = new SpringContextFactory(Config.get(),c,modules[0].getSpringConfigurationClasses()).createSpringContext();

	}

	
	public MicroServerStartup(Config config, Module... modules) {
		
	this.modules = Lists.newArrayList(modules);
	config.set();
		springContext = new SpringContextFactory(config,config.getClasses(),
				modules[0].getSpringConfigurationClasses()).createSpringContext();
	
	}

	

	public void stop(){
		
		end.complete(true);
		
		
	}
	public void run() {
		start().forEach(thread -> join(thread));
	}


	public List<Thread> start() {
	
		List<ServerApplication> apps = modules.stream().map(module -> 
						new GrizzlyApplicationFactory(springContext,module).createApp()).collect(Collectors.toList());

		ServerRunner runner;
		try{
			
			runner = new ServerRunner(springContext.getBean(ApplicationRegister.class), apps,end);
		}catch(BeansException e){
			runner = new ServerRunner(apps,end);
		}
		
		return runner.run();
	}
	private void join(Thread thread) {
		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}
	



	
}