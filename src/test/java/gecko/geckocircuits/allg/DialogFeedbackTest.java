package gecko.geckocircuits.allg;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DialogFeedbackTest {

    @Test
    public void issuesUrlPointsToGitHubIssueChooser() {
        assertEquals("https://github.com/tinix84/GeckoCIRCUITS/issues/new/choose",
                DialogFeedback.ISSUES_URL);
    }

    @Test
    public void labelShowsGitHubUrlAndUsesItAsLinkTarget() {
        assertEquals("<html><a href=\"https://github.com/tinix84/GeckoCIRCUITS/issues/new/choose\">"
                + "https://github.com/tinix84/GeckoCIRCUITS/issues/new/choose</a></html>",
                DialogFeedback.labelHtml());
    }
}
