package com;

import java.io.IOException;
import java.nio.charset.Charset;

import com.atlassian.bamboo.specs.api.BambooSpec;
import com.atlassian.bamboo.specs.api.builders.BambooKey;
import com.atlassian.bamboo.specs.api.builders.Variable;
import com.atlassian.bamboo.specs.api.builders.deployment.Deployment;
import com.atlassian.bamboo.specs.api.builders.deployment.Environment;
import com.atlassian.bamboo.specs.api.builders.deployment.ReleaseNaming;
import com.atlassian.bamboo.specs.api.builders.notification.Notification;
import com.atlassian.bamboo.specs.api.builders.permission.PermissionType;
import com.atlassian.bamboo.specs.api.builders.permission.Permissions;
import com.atlassian.bamboo.specs.api.builders.permission.PlanPermissions;
import com.atlassian.bamboo.specs.api.builders.plan.Job;
import com.atlassian.bamboo.specs.api.builders.plan.Plan;
import com.atlassian.bamboo.specs.api.builders.plan.PlanIdentifier;
import com.atlassian.bamboo.specs.api.builders.plan.Stage;
import com.atlassian.bamboo.specs.api.builders.plan.artifact.Artifact;
import com.atlassian.bamboo.specs.api.builders.plan.branches.BranchCleanup;
import com.atlassian.bamboo.specs.api.builders.plan.branches.PlanBranchManagement;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.AllOtherPluginsConfiguration;
import com.atlassian.bamboo.specs.api.builders.plan.configuration.ConcurrentBuilds;
import com.atlassian.bamboo.specs.api.builders.project.Project;
import com.atlassian.bamboo.specs.builders.notification.DeploymentFinishedNotification;
import com.atlassian.bamboo.specs.builders.notification.EmailRecipient;
import com.atlassian.bamboo.specs.builders.notification.PlanStatusChangedNotification;
import com.atlassian.bamboo.specs.builders.notification.XFailedChainsNotification;
import com.atlassian.bamboo.specs.builders.task.ArtifactDownloaderTask;
import com.atlassian.bamboo.specs.builders.task.CheckoutItem;
import com.atlassian.bamboo.specs.builders.task.CleanWorkingDirectoryTask;
import com.atlassian.bamboo.specs.builders.task.CommandTask;
import com.atlassian.bamboo.specs.builders.task.DockerBuildImageTask;
import com.atlassian.bamboo.specs.builders.task.DockerPushImageTask;
import com.atlassian.bamboo.specs.builders.task.DownloadItem;
import com.atlassian.bamboo.specs.builders.task.MavenTask;
import com.atlassian.bamboo.specs.builders.task.ScriptTask;
import com.atlassian.bamboo.specs.builders.task.VcsCheckoutTask;
import com.atlassian.bamboo.specs.builders.trigger.RemoteTrigger;
import com.atlassian.bamboo.specs.model.task.ScriptTaskProperties;
import com.atlassian.bamboo.specs.util.BambooServer;
import com.atlassian.bamboo.specs.util.MapBuilder;

/**
 * Plan configuration for Bamboo.
 * Learn more on: <a href="https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs">https://confluence.atlassian.com/display/BAMBOO/Bamboo+Specs</a>
 */
@BambooSpec
public class PlanSpec {
	
	public final String PROJECTKEY = "DOT";
    public final String PROJECTNAME = "DevOps";

    public final String PLANKEY = "TPEX";
    public final String PLANNAME = "Template Example1";
    public final String DEPLOYMENTPLANNAME = PROJECTNAME + " - " + PLANNAME;
    public final String LINKED_REPOSITORY_NAME = "Hello-world";

    public final String ADMINNAME = "Abhay";
    public final String NOTIFICATIONEMAIL = "abhgu40@in.ibm.com";

    public final boolean ENABLE_BITBUCKET_SSH_KEY = false;
    public final boolean ENABLE_RSYNC_SSH_KEY = false;

    public final String HIPCHAT_ROOM = "";
    public final String HIPCHAT_TOKEN = "";

    public static final int NUMBER_OF_STAGES = 1;
    public final String RSYNC_ENCRYPTED_PRIVATE_SSH_KEY = "";


    /**
     * Run main to publish plan on Bamboo
     */
    public static void main(final String[] args) throws Exception {
        //By default credentials are read from the '.credentials' file.
        BambooServer bambooServer = new BambooServer("http://localhost:8085");

        Plan plan = new PlanSpec().createPlan();

        bambooServer.publish(plan);

        PlanPermissions planPermission = new PlanSpec().createPlanPermission(plan.getIdentifier());

        bambooServer.publish(planPermission);
        
        Deployment deployment = new PlanSpec().createDeployment();
        bambooServer.publish(deployment);
    }

    PlanPermissions createPlanPermission(PlanIdentifier planIdentifier) {
        Permissions permission = new Permissions()
                .userPermissions("abhaygupta", PermissionType.ADMIN, PermissionType.CLONE, PermissionType.EDIT)
                .groupPermissions("bamboo-admin", PermissionType.ADMIN)
                .loggedInUserPermissions(PermissionType.VIEW)
                .anonymousUserPermissionView();
        return new PlanPermissions(planIdentifier.getProjectKey(), planIdentifier.getPlanKey()).permissions(permission);
    }

    Project project() {
        return new Project()
                .name("Project Name")
                .key("PRJ");
    }

	Plan createPlan() {

		return new Plan(
                new Project().key(new BambooKey(PROJECTKEY)).name(PROJECTNAME),
                PLANNAME,
                new BambooKey(PLANKEY))
            .description("Managed by BambooSPECS")
            .pluginConfigurations(new ConcurrentBuilds()
                    .useSystemWideDefault(false),
                new AllOtherPluginsConfiguration()
                    .configuration(new MapBuilder()
                            .put("custom.buildExpiryConfig", new MapBuilder()
                                .put("duration", "20")
                                .put("period", "days")
                                .put("labelsToKeep", "")
                                .put("buildsToKeep", "3")
                                .put("enabled", "false")
                                .put("expiryTypeArtifact", "true")
                                .build())
                            .build()))
            .stages(new Stage("Default Stage")
                    .jobs(new Job("Make Build artifact",
                            new BambooKey("BA"))
                            .description("Managed by BambooSPECS")
                            .pluginConfigurations(new AllOtherPluginsConfiguration()
                                    .configuration(new MapBuilder()
                                            .put("custom", new MapBuilder()
                                                .put("auto", new MapBuilder()
                                                    .put("regex", "")
                                                    .put("label", "")
                                                    .build())
                                                .put("buildHangingConfig.enabled", "false")
                                                .build())
                                            .build()))
                            .artifacts(new Artifact()
                                    .name("Deploymentconfig")
                                    .location("Dockerfile-K8s-Co")
                                    .copyPattern("sources.tar.zst")
                                    .shared(true),
                                new Artifact()
                                    .name("Makefile")
                                    .copyPattern("Makefile*")
                                    .shared(true))
                            .tasks(
                        		new VcsCheckoutTask()
			                         .description("Checkout Default Repository")
			                         .checkoutItems(new CheckoutItem().defaultRepository()),
					            new MavenTask() 
					                 .goal("clean install -DskipTests")                                     
					                 .hasTests(false)
					                 .version3()
					                 .jdk("JDK 1.8")
					                 .executableLabel("Maven-3.0.1"),
                                new DockerBuildImageTask()
		                               .description("build")
		                               .imageName("abhayibm/hello-world:0.0.1")
		                               .dockerfileInWorkingDir(),   
	                            new DockerPushImageTask()
	                                   .dockerHubImage("abhayibm/hello-world:0.0.1")
	                                   .authentication("abhayibm", "abhay@1995"), 
	                            new ScriptTask()
	                                   .description("Any Script")
	                                   .interpreterShell()
	                                   .inlineBody("echo 'Helloworld'"))
                            .finalTasks()
                            .cleanWorkingDirectory(true)))
            .linkedRepositories(LINKED_REPOSITORY_NAME)
            .triggers(new RemoteTrigger())
            .planBranchManagement(new PlanBranchManagement()
                    .createManually()
                    .delete(new BranchCleanup()
                        .whenRemovedFromRepositoryAfterDays(3)
                        .whenInactiveInRepositoryAfterDays(10))
                    .notificationLikeParentPlan())
            .notifications(new Notification()
                    .type(new XFailedChainsNotification()
                            .numberOfFailures(3))
                    .recipients(new EmailRecipient(NOTIFICATIONEMAIL)),
                new Notification()
                    .type(new PlanStatusChangedNotification())
                    .recipients(new EmailRecipient(NOTIFICATIONEMAIL)));

	}
	
	public Deployment createDeployment() throws IOException {
        final String DEPLOYMENTSCRIPT =  "deployment script";
        final String NOTIFICATIONSCRIPT = "notification script";

        Deployment rootObject = new Deployment(new PlanIdentifier(PROJECTKEY, PLANKEY), DEPLOYMENTPLANNAME)
            .releaseNaming(new ReleaseNaming("release-1")
                    .autoIncrement(true));

        for (int x = 1; x <= NUMBER_OF_STAGES; x++)
        {
            rootObject.environments(new Environment("Developement")
                    .tasks(
                        new ScriptTask()
                            .description("Create tracking file")
                            .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                            .inlineBody("Echo Hello world"),
                        new ArtifactDownloaderTask()
                            .description("Download release contents")
                            .artifacts(new DownloadItem().allArtifacts(true)),
                        new CommandTask()
                            .executable("kubectl")
                            .argument("apply -f Deployment.yaml -n namespace1"),    
                        new ScriptTask()
                            .description("Any Script")
                            .interpreterShell() 
                            .inlineBody("#!/usr/bin/env bash\n\nset -x;\nrm -rf tracking.tmp"))
                    .finalTasks(new ScriptTask()
                            .description("Send HipChat Notification")
                            .enabled(true)
                            .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                            .inlineBody(NOTIFICATIONSCRIPT),
                        new CleanWorkingDirectoryTask())
                    .variables(
                        new Variable("RSYNC_PASSWORD", RSYNC_ENCRYPTED_PRIVATE_SSH_KEY),
                        new Variable("work_env", "stage"),
                        new Variable("stage_number", "" + x),
                        new Variable("hipchat_channel", HIPCHAT_ROOM),
                        new Variable("hipchat_token", HIPCHAT_TOKEN)
                    )
                    .notifications(new Notification()
                            .type(new DeploymentFinishedNotification())
                            .recipients(new EmailRecipient(NOTIFICATIONEMAIL))));
        }

        rootObject.environments(new Environment("Production")
                    .tasks(new CleanWorkingDirectoryTask(),
                        new ScriptTask()
                            .description("Create tracking file")
                            .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                            .inlineBody("set -x;\ntouch tracking.tmp;"),
                        new ArtifactDownloaderTask()
                            .description("Download release contents")
                            .artifacts(new DownloadItem().allArtifacts(true)),
                        new ScriptTask()
                            .description("Deployment")
                            .inlineBody(DEPLOYMENTSCRIPT),
                        new ScriptTask()
                            .description("Remove tracking file")
                            .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                            .inlineBody("#!/usr/bin/env bash\n\nset -x;\nrm -rf tracking.tmp"))
                    .finalTasks(new ScriptTask()
                            .description("Send HipChat Notification")
                            .enabled(true)
                            .interpreter(ScriptTaskProperties.Interpreter.BINSH_OR_CMDEXE)
                            .inlineBody(NOTIFICATIONSCRIPT),
                        new CleanWorkingDirectoryTask())
                    .variables(new Variable("RSYNC_PASSWORD", RSYNC_ENCRYPTED_PRIVATE_SSH_KEY),
                        new Variable("work_env", "prod"),
                        new Variable("hipchat_channel", HIPCHAT_ROOM),
                        new Variable("hipchat_token", HIPCHAT_TOKEN))
                    .notifications(new Notification()
                            .type(new DeploymentFinishedNotification())
                            .recipients(new EmailRecipient(NOTIFICATIONEMAIL))));

        return rootObject;
    }



}
