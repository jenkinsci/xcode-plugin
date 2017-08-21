package au.com.rayh;

import com.sun.mail.iap.ByteArray;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by lrossett on 7/13/17.
 */
public class LauncherUtility {
    private Launcher launcher;
    private TaskListener listener;
    private EnvVars env;
    FilePath pwd;

    public LauncherUtility() {}

    public LauncherUtility(Launcher launcher, TaskListener listener, EnvVars env) {
        this.launcher = launcher;
        this.listener = listener;
        this.env = env;
    }

    public LauncherUtility(Launcher launcher, TaskListener listener) {
        this.launcher = launcher;
        this.listener = listener;
    }

    public LauncherUtility(Launcher launcher) {
        this.launcher = launcher;
    }

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    public void setListener(TaskListener listener) {
        this.listener = listener;
    }

    public void setEnv(EnvVars env) {
        this.env = env;
    }

    public FilePath getPwd() {
        return pwd;
    }

    public void setPwd(FilePath pwd) {
        this.pwd = pwd;
    }

    public void bootstrap(Launcher launcher, TaskListener listener) {
        this.launcher = launcher;
        this.listener = listener;
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public LauncherResponse run(ArgumentListBuilder args, boolean streamLog) throws IOException, InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        LauncherResponse response = new LauncherResponse(stdout, stderr);
        Launcher.ProcStarter proc = launcher.launch();

        if (this.env != null) {
            proc = proc.envs(this.env);
        }

        if (this.pwd != null) {
            proc = proc.pwd(this.pwd);
        }

        int code = proc
                .cmds(args)
                .stdout(response.getStdout())
                .stderr(response.getStderr())
                .join();
        response.setCode(code);

        if (this.listener != null && streamLog) {
            this.listener.getLogger().write(response.toByteArray());
        }

        return response;
    }

    public LauncherResponse run(ArgumentListBuilder args) throws IOException, InterruptedException {
        return run(args, true);
    }

    public static class LauncherResponse {
        private int code;
        private ByteArrayOutputStream stdout;
        private ByteArrayOutputStream stderr;

        public LauncherResponse(ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public void setStdout(ByteArrayOutputStream stdout) {
            this.stdout = stdout;
        }

        public void setStderr(ByteArrayOutputStream stderr) {
            this.stderr = stderr;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public ByteArrayOutputStream getStdout() {
            return stdout;
        }

        public ByteArrayOutputStream getStderr() {
            return stderr;
        }

        @SuppressFBWarnings("DM_DEFAULT_ENCODING")
        public String toString() {
            if (this.isSuccess()) {
                return this.getStdout().toString();
            }
            return this.getStderr().toString();
        }

        @SuppressFBWarnings("DM_DEFAULT_ENCODING")
        public byte[] toByteArray() {
            if (this.isSuccess()) {
                return this.getStdout().toByteArray();
            }
            return this.getStderr().toByteArray();
        }

        public boolean isSuccess() {
            return this.getCode() == 0;
        }
    }
}
