package com.slackworks.naether.maven;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Java SE
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Apache Maven
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

// Codehause Plexus
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;

import com.slackworks.naether.Notation;

/**
 * Maven Project Model
 * 
 * @author Michael Guymon
 * 
 */
public class Project {

	private static Logger log = LoggerFactory.getLogger(Project.class);

	private Model mavenModel;
	private Pattern propertyPattern = Pattern.compile("^\\$\\{(.+)\\}$");

	/**
	 * New Instance
	 */
	public Project() {
		Model model = new Model();
		model.setModelVersion("4.0.0");
		model.setPackaging("jar");
		setMavenModel(model);

	}

	/**
	 * New Instance loading Maven pom
	 * 
	 * @param pomPath String path
	 * @throws ProjectException 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	public Project(String pomPath) throws ProjectException {
		loadPOM(pomPath);
	}

	/**
	 * Load Maven pom
	 * 
	 * @param pomPath String path
	 * @throws ProjectException 
	 */
	public void loadPOM(String pomPath) throws ProjectException {
		log.debug("Loading pom {}", pomPath);
		MavenXpp3Reader reader = new MavenXpp3Reader();
		try {
			setMavenModel(reader.read(new BufferedReader(new FileReader(new File(pomPath)))));
		} catch (FileNotFoundException e) {
			log.error( "Failed to access pom", e);
			throw new ProjectException( "Failed to access pom", e );
			
		} catch (IOException e) {
			log.error( "Failed to read pom", e);
			throw new ProjectException("Failed to read pom", e );
			
		} catch (XmlPullParserException e) {
			log.error( "Failed to parse pom", e);
			throw new ProjectException( "Failed to parse pom", e );
		}
	}

	public String getModelVersion() {
		return getMavenModel().getModelVersion();
	}

	public void setModelVersion(String version) {
		getMavenModel().setModelVersion(version);
	}

	public String getVersion() {
		return getMavenModel().getVersion();
	}

	public void setVersion(String version) {
		getMavenModel().setVersion(version);
	}

	public String getArtifactId() {
		return getMavenModel().getArtifactId();
	}

	public void setArtifactId(String artifactId) {
		getMavenModel().setArtifactId(artifactId);
	}

	public String getGroupId() {
		return getMavenModel().getGroupId();
	}

	public void setGroupId(String groupId) {
		getMavenModel().setGroupId(groupId);
	}

	public String getType() {
		return getMavenModel().getPackaging();
	}

	public void setType(String type) {
		getMavenModel().setPackaging(type);
	}

	/**
	 * Set Maven Project from String notation
	 * 
	 * @param notation String
	 */
	public void setProjectNotation(String notation) {
		Map<String, String> notationMap = Notation.parse(notation);
		this.setGroupId(notationMap.get("groupId"));
		this.setArtifactId(notationMap.get("artifactId"));
		this.setType(notationMap.get("type"));
		this.setVersion(notationMap.get("version"));
	}

	/**
	 * Get List of {@link Dependency} for Maven Project
	 * 
	 * @return List<Dependency>
	 */
	public List<Dependency> getDependencies() {
		return getDependencies(true);
	}

	/**
	 * Get List of {@link Depedencies} for the Maven Project, with boolean to
	 * substitute Project Properties.
	 * 
	 * @param substituteProperties boolean
	 * @return List<Dependency>
	 */
	public List<Dependency> getDependencies(boolean substituteProperties) {
		List<Dependency> dependencies = new ArrayList<Dependency>();

		// Substitute Properties
		if (substituteProperties) {
			// XXX: There has to be a way maven handles this automatically
			for (Dependency dependency : getMavenModel().getDependencies()) {

				String artifactId = substituteProperty(dependency.getArtifactId());
				String groupId = substituteProperty(dependency.getGroupId());
				String version = substituteProperty(dependency.getVersion());

				dependency.setArtifactId(artifactId);
				dependency.setGroupId(groupId);
				dependency.setVersion(version);
				dependencies.add(dependency);

			}

		// Keep vals
		} else {
			for (Dependency dependency : getMavenModel().getDependencies()) {
				dependencies.add(dependency);
			}
		}

		return dependencies;
	}

	/**
	 * Return {@link List<String>} of dependencies in the format of
	 * {@code groupId:artifactId:packageType:version}
	 * 
	 * @return List<String>
	 */
	public List<String> getDependenciesNotation() {
		return getDependenciesNotation(true);
	}

	/**
	 * Get List<String> of dependencies in format of
	 * groupId:artifactId:packageType:version
	 * 
	 * @param substituteProperties boolean
	 * @return List<String>
	 */
	public List<String> getDependenciesNotation(boolean substituteProperties) {
		List<String> notations = new ArrayList<String>();

		for (Dependency dependency : getDependencies(substituteProperties)) {
			notations.add(Notation.generate(dependency));
		}

		return notations;
	}

	public void addDependency(Dependency dependency) {
		mavenModel.addDependency(dependency);
	}

	public void addDependency(String notation ) {
		addDependency( notation, "compile" );
	}
	
	public void addDependency(String notation, String scope ) {
		Map<String, String> notationMap = Notation.parse(notation);
		Dependency dependency = new Dependency();
		dependency.setGroupId(notationMap.get("groupId"));
		dependency.setArtifactId(notationMap.get("artifactId"));
		dependency.setType(notationMap.get("type"));
		dependency.setVersion(notationMap.get("version"));
		dependency.setScope( scope );
		addDependency(dependency);
	}
	
	public void addDependency(org.sonatype.aether.graph.Dependency aetherDep) {
		Artifact artifact = aetherDep.getArtifact();
		
		Dependency dependency = new Dependency();
		dependency.setGroupId( artifact.getGroupId() );
		dependency.setArtifactId( artifact.getArtifactId() );
		dependency.setType( artifact.getExtension() );
		dependency.setVersion( artifact.getVersion() );
		dependency.setScope( aetherDep.getScope() );
		addDependency( dependency );
	}

	/**
	 * Set the Maven {@link Model}
	 * 
	 * @param mavenModel {@link Model}
	 */
	public void setMavenModel(Model mavenModel) {
		this.mavenModel = mavenModel;
	}

	/**
	 * Get the Maven {@link Model}
	 * 
	 * @return {@link Model}
	 */
	public Model getMavenModel() {
		return mavenModel;
	}

	/**
	 * Substitute a Maven Property expression, i.e. ${aetherVersion}, to its
	 * corresponding Maven pom definition, i.e. 1.11 from
	 * {@code <properties><aetherVersion>1.11</aetherVersion></properties>}
	 * 
	 * @param field
	 * @return
	 */
	private String substituteProperty(String field) {
		String property = null;
		Matcher matcher = propertyPattern.matcher(field);
		while (matcher.find()) {
			property = matcher.group(1);
		}

		if (property != null) {
			log.debug("Set property {} to {}", property, this.getMavenModel().getProperties().getProperty(property));
			return this.getMavenModel().getProperties().getProperty(property);
		} else {
			return field;
		}
	}

	public void writePom(String filePath) throws ProjectException {
		log.debug("Writing pom: {}", filePath);
		Writer writer;
		try {
			writer = new BufferedWriter(new FileWriter(filePath));
		} catch (IOException e) {
			throw new ProjectException(e);
		}
		
		MavenXpp3Writer pomWriter = new MavenXpp3Writer();
		try {
			pomWriter.write(writer, this.mavenModel);
		} catch (IOException e) {
			throw new ProjectException("Failed to write pom", e);
		}
	}
}