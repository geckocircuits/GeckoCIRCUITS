package gecko.geckocircuits.newscope;

import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class GeckoDialogIconTest {

    @Test
    public void resolvesIconInsideJar() throws Exception {
        assertEquals("jar:file:/tmp/x.jar!/gecko/pics/gecko.gif",
                GeckoDialog.iconUrl(new URL("jar:file:/tmp/x.jar!/gecko/pics/")).toExternalForm());
    }

    @Test
    public void resolvesIconInClassesDirectory() throws Exception {
        assertEquals("file:/tmp/classes/gecko/pics/gecko.gif",
                GeckoDialog.iconUrl(new URL("file:/tmp/classes/gecko/pics/")).toExternalForm());
    }

    @Test
    public void preservesEncodedSpacesInJarPath() throws Exception {
        assertEquals("jar:file:/C:/Program%20Files/GeckoCIRCUITS.jar!/gecko/pics/gecko.gif",
                GeckoDialog.iconUrl(new URL(
                        "jar:file:/C:/Program%20Files/GeckoCIRCUITS.jar!/gecko/pics/")).toExternalForm());
    }
}
