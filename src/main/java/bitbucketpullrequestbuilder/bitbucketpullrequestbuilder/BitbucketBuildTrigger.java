package bitbucketpullrequestbuilder.bitbucketpullrequestbuilder;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.RevisionParameterAction;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nishio
 */
public class BitbucketBuildTrigger extends Trigger<AbstractProject<?, ?>> {
    private static final Logger logger = Logger.getLogger(BitbucketBuildTrigger.class.getName());
    private final String projectPath;
    private final String username;
    private final String password;
    private final String repositoryOwner;
    private final String repositoryName;
    private final String ciSkipPhrases;
    private final boolean checkDestinationCommit;
    private final boolean approveIfSuccess;
    
    transient private BitbucketPullRequestsBuilder bitbucketPullRequestsBuilder;

    @Extension
    public static final BitbucketBuildTriggerDescriptor descriptor = new BitbucketBuildTriggerDescriptor();

    @DataBoundConstructor
    public BitbucketBuildTrigger(
            String projectPath,
            String username,
            String password,
            String repositoryOwner,
            String repositoryName,
            String ciSkipPhrases,
            boolean checkDestinationCommit,
            boolean approveIfSuccess
            ) {
        this.projectPath = projectPath;
        this.username = username;
        this.password = password;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.ciSkipPhrases = ciSkipPhrases;
        this.checkDestinationCommit = checkDestinationCommit;
        this.approveIfSuccess = approveIfSuccess;
    }

    public String getProjectPath() {
        return this.projectPath;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getCiSkipPhrases() {
        return ciSkipPhrases;
    }
    
    public boolean getCheckDestinationCommit() {
    	return checkDestinationCommit;
    }

    public boolean getApproveIfSuccess() {
        return approveIfSuccess;
    }

    @Override
    public void start(AbstractProject<?, ?> project, boolean newInstance) {
        try {
            this.bitbucketPullRequestsBuilder = BitbucketPullRequestsBuilder.getBuilder();
            this.bitbucketPullRequestsBuilder.setProject(project);
            this.bitbucketPullRequestsBuilder.setTrigger(this);
            this.bitbucketPullRequestsBuilder.setupBuilder();
        } catch(IllegalStateException e) {
            logger.log(Level.SEVERE, "Can't start trigger", e);
            return;
        }
        super.start(project, newInstance);
    }

    public static BitbucketBuildTrigger getTrigger(AbstractProject project) {
        Trigger trigger = project.getTrigger(BitbucketBuildTrigger.class);
        return (BitbucketBuildTrigger)trigger;
    }

    public BitbucketPullRequestsBuilder getBuilder() {
        return this.bitbucketPullRequestsBuilder;
    }

    public QueueTaskFuture<?> startJob(BitbucketCause cause) {
        Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
        values.put("sourceBranch", new StringParameterValue("sourceBranch", cause.getSourceBranch()));
        values.put("targetBranch", new StringParameterValue("targetBranch", cause.getTargetBranch()));
        values.put("repositoryOwner", new StringParameterValue("repositoryOwner", cause.getRepositoryOwner()));
        values.put("repositoryName", new StringParameterValue("repositoryName", cause.getRepositoryName()));
        values.put("pullRequestId", new StringParameterValue("pullRequestId", cause.getPullRequestId()));
        values.put("destinationRepositoryOwner", new StringParameterValue("destinationRepositoryOwner", cause.getDestinationRepositoryOwner()));
        values.put("destinationRepositoryName", new StringParameterValue("destinationRepositoryName", cause.getDestinationRepositoryName()));
        values.put("pullRequestTitle", new StringParameterValue("pullRequestTitle", cause.getPullRequestTitle()));
        return this.job.scheduleBuild2(0, cause, new ParametersAction(new ArrayList(values.values())), new RevisionParameterAction(cause.getSourceCommitHash()));
    }

    private Map<String, ParameterValue> getDefaultParameters() {
        Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
        ParametersDefinitionProperty definitionProperty = this.job.getProperty(ParametersDefinitionProperty.class);

        if (definitionProperty != null) {
            for (ParameterDefinition definition : definitionProperty.getParameterDefinitions()) {
                values.put(definition.getName(), definition.getDefaultParameterValue());
            }
        }
        return values;
    }

    public static final class BitbucketBuildTriggerDescriptor extends TriggerDescriptor {
        public BitbucketBuildTriggerDescriptor() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Bitbucket Pull Requests Builder";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }
    }
}
