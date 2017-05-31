package au.com.rayh;

import org.kohsuke.stapler.DataBoundConstructor;

public class Team {

    private String teamName;
    private String teamID;

    public Team() {
    }

    @DataBoundConstructor
    public Team(String teamName, String teamID) {
        this.teamName = teamName;
        this.teamID = teamID;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamID() {
        return teamID;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }
    
}
