/*
 * #%L
 * GraphWalker Command Line Interface
 * %%
 * Copyright (C) 2011 - 2014 GraphWalker
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package org.graphwalker.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import org.apache.commons.lang3.StringUtils;
import org.graphwalker.cli.antlr.GeneratorFactory;
import org.graphwalker.cli.commands.Offline;
import org.graphwalker.core.generator.PathGenerator;
import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.core.machine.Machine;
import org.graphwalker.core.machine.MachineException;
import org.graphwalker.core.machine.SimpleMachine;
import org.graphwalker.core.model.Model;
import org.graphwalker.core.model.Vertex;
import org.graphwalker.io.factory.GraphMLModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class CLI {
  private static final Logger logger = LoggerFactory.getLogger(CLI.class);
  JCommander jc;
  Options options;
  Offline offline;

  public static void main(String[] args) {
    CLI cli = new CLI();
    try {
      cli.run(args);
    } catch (Exception e) {
      // We should have caught all exceptions up until here, but there
      // might have been problems with the command parser for instance...
      System.err.println(e);
      logger.error("Un error occurred when running command: " + StringUtils.join(args, " "), e);
    }
  }

  /**
   * Parses the command line.
   *
   * @param args
   */
  private void run(String[] args) {
    options = new Options();
    jc = new JCommander(options);
    jc.setProgramName("java -jar graphwalker.jar");

    offline = new Offline();
    jc.addCommand("offline", offline);

    try {
      jc.parse(args);

      // Parse for commands
      if (jc.getParsedCommand() != null) {
        if (jc.getParsedCommand().equals("offline")) {
          RunCommandOffline();
        }
      }

      // Parse for arguments
      else if (options.version) {
        System.out.println(printVersionInformation());
      }

      // No commands or options were found
      else {
        jc.usage();
      }

    } catch (MissingCommandException e) {
      System.err.println("I did not see a valid command.");
      System.err.println("");
      jc.usage();
    } catch (Exception e) {
      System.err.println("Un error occurred when running command: " + StringUtils.join(args, " "));
      System.err.println(e.getMessage());
      logger.error("Un error occurred when running command: " + StringUtils.join(args, " "), e);
    }
  }

  private void RunCommandOffline()  throws Exception {
    GraphMLModelFactory factory = new GraphMLModelFactory();

    Iterator itr = offline.model.iterator();
    while(itr.hasNext()) {
      Model model = factory.create((String)itr.next());

      PathGenerator pathGenerator = GeneratorFactory.parse((String) itr.next());
      ExecutionContext context = new ExecutionContext(model, pathGenerator);
      SetStartVertex(context);

      Machine machine = new SimpleMachine(context);
      while (machine.hasNextStep()) {
        try {
          machine.getNextStep();
        } catch ( MachineException e) {
          ;
        } finally {
          System.out.println(context.getCurrentElement().getName());
        }
      }
    }
  }

  private void SetStartVertex( ExecutionContext context ) {
    // Backward compatibility with GW2, which expects a Start node to exists
    List<Vertex.RuntimeVertex> list = context.getModel().findVertices("Start");
    if (!list.isEmpty() && list.get(0) != null) {
      context.setCurrentElement(context.getModel().findVertices("Start").get(0));

    // New GW3 syntax, which defines the Start as the vertex with no in-edges
    } else {
      for (Vertex.RuntimeVertex vertex : context.getModel().getVertices() ) {
        if (context.getModel().getInEdges(vertex).size()==0){
          context.setNextElement(vertex);
          return;
        }
      }
    }
  }

  private String printVersionInformation() {
    String version = "org.graphwalker version: " + getVersionString() + System.getProperty("line.separator");
    version += System.getProperty("line.separator");

    version += "org.graphwalker is open source software licensed under MIT license" + System.getProperty("line.separator");
    version += "The software (and it's source) can be downloaded from http://graphwalker.org" + System.getProperty("line.separator");
    version += "For a complete list of this package software dependencies, see TO BE DEFINED" + System.getProperty("line.separator");

    return version;
  }

  private String getVersionString() {
    Properties properties = new Properties();
    InputStream inputStream = null;
    try {
      inputStream = getClass().getResourceAsStream("/org/graphwalker/resources/version.properties");
      properties.load(inputStream);
      inputStream.close();
    } catch (IOException e) {
      ;
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (Exception e) {
          logger.error("Un error occurred when trying to get the version string", e);
        }
      }
    }
    StringBuilder stringBuilder = new StringBuilder();
    if (properties.containsKey("version.major")) {
      stringBuilder.append(properties.getProperty("version.major"));
    }
    if (properties.containsKey("version.minor")) {
      stringBuilder.append(".");
      stringBuilder.append(properties.getProperty("version.minor"));
    }
    if (properties.containsKey("version.fix")) {
      stringBuilder.append(".");
      stringBuilder.append(properties.getProperty("version.fix"));
    }
    if (properties.containsKey("version.git.commit")) {
      stringBuilder.append(", git commit ");
      stringBuilder.append(properties.getProperty("version.git.commit"));
    }
    return stringBuilder.toString();
  }

  private String generateListOfValidGenerators() {
    return "";
  }

  private String generateListOfValidStopConditions() {
    return "";
  }

  private boolean helpNeeded(String module, boolean condition, String message) {
    if (condition) {
      System.out.println(message);
      System.out.println("Type 'java -jar graphwalker.jar help " + module + "' for help.");
    }
    return condition;
  }
}
