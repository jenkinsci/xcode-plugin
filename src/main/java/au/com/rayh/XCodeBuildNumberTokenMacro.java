package au.com.rayh;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.tokenmacro.DataBoundTokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;


@Extension
public class XCodeBuildNumberTokenMacro extends DataBoundTokenMacro {
    @Override
    public String evaluate(AbstractBuild<?, ?> context, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
            XCodeAction a = context.getAction(XCodeAction.class);
            if(a == null){
                return "";
            }
			return a.getBuildDescription();
    }

    @Override
    public boolean acceptsMacroName(String macroName) {
        return macroName.equals("XCODE_BUILD_NUMBER");
    }
}