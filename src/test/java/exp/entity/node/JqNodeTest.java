package exp.entity.node;

import exp.entity.Node;
import exp.entity.NodeTest;
import exp.entity.Value;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class JqNodeTest {

    @Test
    public void isNullInput(){
        assertTrue(JqNode.isNullInput("[inputs]"),"treating all inputs as an array should be a null input");
        assertFalse(JqNode.isNullInput(".inputs"),"accessing the inputs key should not trigger null input");
    }




}
