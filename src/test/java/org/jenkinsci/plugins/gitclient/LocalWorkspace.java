package org.jenkinsci.plugins.gitclient;

import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.TaskListener;
import hudson.plugins.git.IGitAPI;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import static junit.framework.TestCase.assertEquals;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.objenesis.ObjenesisStd;

/*
 * Local workspace of a Git repository.
 */
public class LocalWorkspace {

    private final File repo;
    private final GitClient git;
    private final boolean bare;
    private final TaskListener listener;
    private final hudson.EnvVars env = new hudson.EnvVars();

    public LocalWorkspace(File directory, String implementation, TaskListener listener, boolean bare) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, IOException, InterruptedException {
        this.repo = directory;
        this.bare = bare;
        this.listener = listener;
        this.git = Git.with(listener, env).in(this.repo).using(implementation).getClient();
        setupProxy(this.git);
    }

    public LocalWorkspace(File directory, String implementation, TaskListener listener) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, IOException, InterruptedException {
        this(directory, implementation, listener, false);
    }

    public LocalWorkspace(File directory, String implementation) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, IOException, InterruptedException {
        this(directory, implementation, StreamTaskListener.fromStdout(), false);
    }

    public GitClient getGitClient() throws IOException, InterruptedException {
        return git;
    }

    String cmd(String args) throws IOException, InterruptedException {
        return launchCommand(args.split(" "));
    }

    String cmd(boolean ignoreError, String args) throws IOException, InterruptedException {
        return launchCommand(ignoreError, args.split(" "));
    }

    String launchCommand(String... args) throws IOException, InterruptedException {
        return launchCommand(false, args);
    }

    String launchCommand(boolean ignoreError, String... args) throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int st = new Launcher.LocalLauncher(listener).launch().pwd(repo).cmds(args).
                envs(env).stdout(out).join();
        String s = out.toString();
        if (!ignoreError) {
            if (s == null || s.isEmpty()) {
                s = StringUtils.join(args, ' ');
            }
            /* Report full output of failing commands */
            assertEquals(s, 0, st);
        }
        return s;
    }

    String repoPath() {
        return repo.getAbsolutePath();
    }

    LocalWorkspace init() throws IOException, InterruptedException {
        git.init();
        return this;
    }

    LocalWorkspace init(boolean bare) throws IOException, InterruptedException {
        git.init_().workspace(repoPath()).bare(bare).execute();
        return this;
    }

    void tag(String tag) throws IOException, InterruptedException {
        cmd("git tag " + tag);
    }

    void commitEmpty(String msg) throws IOException, InterruptedException {
        cmd("git commit --allow-empty -m " + msg);
    }

    File file(String path) {
        return new File(repo, path);
    }

    boolean exists(String path) {
        return file(path).exists();
    }

    void touch(String path) throws IOException {
        file(path).createNewFile();
    }

    File touch(String path, String content) throws IOException {
        File f = file(path);
        FileUtils.writeStringToFile(f, content, "UTF-8");
        return f;
    }

    public void rm(String path) {
        file(path).delete();
    }

    public String contentOf(String path) throws IOException {
        return FileUtils.readFileToString(file(path), "UTF-8");
    }

    protected CliGitAPIImpl cgit() throws Exception {
        if (git instanceof CliGitAPIImpl) {
            return (CliGitAPIImpl) git;
        }
        return (CliGitAPIImpl) Git.with(listener, env).in(this.repo).using("git").getClient();
    }

    protected FileRepository repo() throws IOException {
        return bare ? new FileRepository(repo) : new FileRepository(new File(repo, ".git"));
    }

    ObjectId head() throws IOException, InterruptedException {
        return git.revParse("HEAD");
    }

    public IGitAPI igit() {
        return (IGitAPI) git;
    }

    public void cloneMirror() throws IOException, InterruptedException {
        /* Should use specific implementation rather than command line git */
        launchCommand("git", "clone", localMirror(), repoPath());
    }

    private String getSystemProperty(String... keyVariants) {
        for (String key : keyVariants) {
            String value = System.getProperty(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /* HEAD ref of local mirror - all read access should use getMirrorHead */
    private static ObjectId mirrorHead = null;

    private ObjectId getMirrorHead() throws IOException, InterruptedException {
        if (mirrorHead == null) {
            final String mirrorPath = new File(localMirror()).getAbsolutePath();
            mirrorHead = ObjectId.fromString(launchCommand("git", "--git-dir=" + mirrorPath, "rev-parse", "HEAD").substring(0, 40));
        }
        return mirrorHead;
    }

    private final String remoteMirrorURL = "https://github.com/jenkinsci/git-client-plugin.git";
    private final String remoteSshURL = "git@github.com:jenkinsci/git-client-plugin.git";

    public String localMirror() throws IOException, InterruptedException {
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File mirror = new File(targetDir, "mirror.git");
        if (!mirror.exists()) {  // TODO: perhaps some kind of quick timestamp-based up-to-date check?
            launchCommand("git", "clone", "--mirror", remoteMirrorURL, mirror.getAbsolutePath());
        }
        return mirror.getAbsolutePath();
    }

    private void setupProxy(GitClient gitClient)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final String proxyHost = getSystemProperty("proxyHost", "http.proxyHost", "https.proxyHost");
        final String proxyPort = getSystemProperty("proxyPort", "http.proxyPort", "https.proxyPort");
        final String proxyUser = getSystemProperty("proxyUser", "http.proxyUser", "https.proxyUser");
        //final String proxyPassword = getSystemProperty("proxyPassword", "http.proxyPassword", "https.proxyPassword");
        final String noProxyHosts = getSystemProperty("noProxyHosts", "http.noProxyHosts", "https.noProxyHosts");
        if (isBlank(proxyHost) || isBlank(proxyPort)) {
            return;
        }
        ProxyConfiguration proxyConfig = new ObjenesisStd().newInstance(ProxyConfiguration.class);
        setField(ProxyConfiguration.class, "name", proxyConfig, proxyHost);
        setField(ProxyConfiguration.class, "port", proxyConfig, Integer.parseInt(proxyPort));
        setField(ProxyConfiguration.class, "userName", proxyConfig, proxyUser);
        setField(ProxyConfiguration.class, "noProxyHost", proxyConfig, noProxyHosts);
        //Password does not work since a set password results in a "Secret" call which expects a running Jenkins
        setField(ProxyConfiguration.class, "password", proxyConfig, null);
        setField(ProxyConfiguration.class, "secretPassword", proxyConfig, null);
        gitClient.setProxy(proxyConfig);
    }

    private void setField(Class<?> clazz, String fieldName, Object object, Object value)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(fieldName);
        declaredField.setAccessible(true);
        declaredField.set(object, value);
    }
}
