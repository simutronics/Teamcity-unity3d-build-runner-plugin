/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unityRunner.agent;


import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;
import unityRunner.common.PluginConstants;
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration;
import java.io.File;

public class UnityRunnerBuildServiceFactory implements CommandLineBuildServiceFactory {

    public UnityRunnerBuildServiceFactory() {
    }

    @NotNull
    public CommandLineBuildService createService() {
        return new UnityRunnerBuildService();
    }

    @NotNull
    public AgentBuildRunnerInfo getBuildRunnerInfo() {
        return new AgentBuildRunnerInfo() {

            @NotNull
            public String getType() {
                return PluginConstants.RUN_TYPE;
            }

            public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
                setupConfigurationParameters(agentConfiguration);
                return agentConfiguration.getSystemInfo().isWindows() || agentConfiguration.getSystemInfo().isMac();
            }

            /**
             * Setup agent configuration parameters.
             * @param agentConfiguration build agent configuration.
             */
            public void setupConfigurationParameters(@NotNull BuildAgentConfiguration agentConfiguration) {
                String unityPath = null;
                String unityLog = null;

                // Get unity path and editor log path for supported platforms.
                if (agentConfiguration.getSystemInfo().isWindows()) {
                    unityPath = UnityRunnerConfiguration.getUnityPath(UnityRunnerConfiguration.Platform.Windows);
                    unityLog = UnityRunnerConfiguration.getUnityLogPath(UnityRunnerConfiguration.Platform.Windows);
                } else if (agentConfiguration.getSystemInfo().isMac()) {
                    unityPath = UnityRunnerConfiguration.getUnityPath(UnityRunnerConfiguration.Platform.Mac);
                    unityLog = UnityRunnerConfiguration.getUnityLogPath(UnityRunnerConfiguration.Platform.Mac);
                }

                if (unityPath != null && unityLog != null) {
                    File file = new File(unityPath);

                    if (file.exists()) {
                        agentConfiguration.addConfigurationParameter("unity.exe", unityPath);
                        agentConfiguration.addConfigurationParameter("unity.log", unityLog);

                        // don't hard-code the unity version
                        if (agentConfiguration.getSystemInfo().isMac()) {
                            readMacUnityVersion(agentConfiguration);
                        } else {
                            // find on windows - reading versionNumber from the exe ( Windows PE specification)
                            readWindowsUnityVersion(agentConfiguration);
                        }
                    }
                }
            }

            private void readMacUnityVersion(@NotNull BuildAgentConfiguration agentConfiguration) {
                try {
                    XMLPropertyListConfiguration config = new XMLPropertyListConfiguration("/Applications/Unity/Unity.app/Contents/Info.plist");
                    if (config != null) {
                        String version = config.getString("CFBundleVersion");   
                        if (version != null) {             
                            // strip off f-part
                            int indexOfF = version.indexOf("f");
                            if (indexOfF > -1) {
                                version = version.substring(0,indexOfF);
                            }
                            Loggers.AGENT.info("Found unity.version= " + version);
                            agentConfiguration.addConfigurationParameter("unity.version", version);
                        }

                        String buildNumber = config.getString("UnityBuildNumber");
                        if (buildNumber != null) {
                            Loggers.AGENT.info("Found unity.buildNumber= " + buildNumber);
                            agentConfiguration.addConfigurationParameter("unity.buildNumber", buildNumber);
                        }

                    }

                } catch (Exception e) {
                    // had trouble detecting version
                    Loggers.AGENT.error("Exception getting unity version :" + e.getMessage());
                }
            }

            private void readWindowsUnityVersion(@NotNull BuildAgentConfiguration agentConfiguration){
                String fileName = UnityRunnerConfiguration.getUnityPath(UnityRunnerConfiguration.Platform.Windows);
                Loggers.AGENT.info("Reading file to get buildNumber: " + fileName);
                try{
                    String buildNumber  =  FileVersionInfo.getShortVersionNumber(fileName);
                    Loggers.AGENT.info("Found unity buildNumber: " + buildNumber);
                    agentConfiguration.addConfigurationParameter("unity.buildNumber", buildNumber);
                } catch(Exception e){
                    Loggers.AGENT.error("Exception getting unity version :" + e.getMessage());
                }

            }
        };
    }
}





