package exp.svc;

import exp.FreshDb;
import exp.entity.Node;
import exp.entity.Value;
import exp.entity.node.JqNode;
import exp.entity.node.RootNode;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ValueServiceTest extends FreshDb {

    @Inject
    ValueService valueService;

    @Inject
    TransactionManager tm;

    @Test
    public void persist_fixes_source_order() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        tm.begin();
        Node root = new RootNode();
        root.persist();
        Node a = new JqNode("a");
        a.sources=List.of(root);
        Node ab = new JqNode("ab");
        ab.sources=List.of(a);
        ab.persist();
        Node ac = new JqNode("ac");
        ac.sources=List.of(a);
        Node abc = new JqNode("abc");
        abc.sources=List.of(ab,ac,a,root);
        abc.persist();

        Value vr = new Value();
        vr.path="vr";
        vr.persist();
        Value va = new Value();
        va.path="va";
        va.sources=List.of(vr);
        va.persist();
        Value vab = new Value();
        vab.path="vab";
        vab.sources=List.of(va);
        vab.persist();
        Value vac = new Value();
        vac.path="vac";
        vac.sources=List.of(va);
        vac.persist();
        Value vabc =  new Value();
        vabc.path="vabc";
        vabc.sources=List.of(vab,vac,va,vr);
        vabc.persist();
        tm.commit();

        tm.begin();
        try{
            Value found = valueService.byId(vabc.id);
            List<Value> sources = found.sources;
            assertNotNull(sources);
            assertEquals(4, sources.size());

            assertTrue(sources.indexOf(va) < sources.indexOf(vab),"va should come before vab");
            assertTrue(sources.indexOf(va) < sources.indexOf(vac),"va should come before vac");
            assertTrue(sources.indexOf(vr) < sources.indexOf(va),"vr should come before va");
        }finally {
            tm.commit();
        }

    }

    @Test
    public void getValues() throws HeuristicRollbackException, SystemException, HeuristicMixedException, RollbackException, NotSupportedException {
        tm.begin();
        Node root = new RootNode();
        root.persist();
        Node alpha = new JqNode("alpha");
        alpha.sources=List.of(root);
        alpha.persist();
        Node bravo = new JqNode("bravo");
        bravo.sources=List.of(root);
        bravo.persist();
        Node bravobravo = new JqNode("bravobravo");
        bravobravo.sources=List.of(bravo);
        bravobravo.persist();

        Value rootValue = new Value();
        rootValue.node = root;
        rootValue.path="./";
        rootValue.persist();

        Value alphaValue = new Value();
        alphaValue.node = alpha;
        alphaValue.sources = List.of(rootValue);
        alphaValue.persist();

        Value bravoValue = new Value();
        bravoValue.node = bravo;
        bravoValue.sources = List.of(rootValue);
        bravoValue.persist();

        Value bravobravoValue = new Value();
        bravobravoValue.node = bravobravo;
        bravobravoValue.sources = List.of(bravoValue);
        bravobravoValue.persist();
        tm.commit();

        List<Value> values = valueService.getValues(root);
        assertNotNull(values,"result should not be null");
        assertEquals(1,values.size(),"result should contain 1 value: "+values);
        assertTrue(values.contains(rootValue),"result should contain root value: "+values);

    }
    @Test
    public void getDescendantValues_node() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        tm.begin();
        Node root = new JqNode("root");
        root.persist();
        Node alpha = new JqNode("alpha");
        alpha.sources=List.of(root);
        alpha.persist();
        Node bravo = new JqNode("bravo");
        bravo.sources=List.of(root);
        bravo.persist();
        Node bravobravo = new JqNode("bravobravo");
        bravobravo.sources=List.of(bravo);
        bravobravo.persist();

        Value rootValue = new Value();
        rootValue.node = root;
        rootValue.path="./root";
        rootValue.persist();

        Value alphaValue = new Value();
        alphaValue.node = alpha;
        alphaValue.sources = List.of(rootValue);
        alphaValue.path="./alpha";
        alphaValue.persist();

        Value bravoValue = new Value();
        bravoValue.node = bravo;
        bravoValue.sources = List.of(rootValue);
        bravoValue.path="./bravo";
        bravoValue.persist();

        Value bravobravoValue = new Value();
        bravobravoValue.node = bravobravo;
        bravobravoValue.sources = List.of(bravoValue);
        bravobravoValue.path="./bravobravo";
        bravobravoValue.persist();

        tm.commit();

        List<Value> found = valueService.getDescendantValues(root);

        assertEquals(3,found.size(),"expect to see three entries");
        assertTrue(found.contains(bravobravoValue),"missing bravobravo["+bravobravoValue.id+"]'s value: "+found);
    }

    @Test
    public void getDescendantValues_two_generations() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        tm.begin();
        Node root = new JqNode("root");
        root.persist();
        Node alpha = new JqNode("alpha");
        alpha.sources=List.of(root);
        alpha.persist();
        Node bravo = new JqNode("bravo");
        bravo.sources=List.of(root);
        bravo.persist();
        Node bravobravo = new JqNode("bravobravo");
        bravobravo.sources=List.of(root,bravo);
        bravobravo.persist();

        Value rootValue = new Value();
        rootValue.node = root;
        rootValue.path="./";
        rootValue.persist();

        Value alphaValue = new Value();
        alphaValue.node = alpha;
        alphaValue.sources = List.of(rootValue);
        alphaValue.persist();

        Value bravoValue = new Value();
        bravoValue.node = bravo;
        bravoValue.sources = List.of(rootValue);
        bravoValue.persist();

        Value bravobravoValue = new Value();
        bravobravoValue.node = bravobravo;
        bravobravoValue.sources = List.of(bravoValue);
        bravobravoValue.persist();

        tm.commit();

        List<Value> found = valueService.getDescendantValues(rootValue);
        assertEquals(3,found.size(),"expect to see three entries");
        assertTrue(found.contains(bravobravoValue),"missing bravobravo["+bravobravoValue.id+"]'s value: "+found);
    }
    @Test
    public void getDirectDescendantValues() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        tm.begin();
        Node root = new JqNode("root");
        root.persist();
        Node alpha = new JqNode("alpha");
        alpha.persist();
        Node bravo = new JqNode("bravo");
        bravo.persist();
        Node bravobravo = new JqNode("bravobravo");
        bravobravo.persist();

        Value rootValue = new Value();
        rootValue.node = root;
        rootValue.path="./";
        rootValue.persist();

        Value alphaValue = new Value();
        alphaValue.node = alpha;
        alphaValue.sources = List.of(rootValue);
        alphaValue.persist();

        Value bravoValue1 = new Value();
        bravoValue1.node = bravo;
        bravoValue1.sources = List.of(rootValue);
        bravoValue1.persist();

        Value bravoValue2 = new Value();
        bravoValue2.node = bravo;
        bravoValue2.sources = List.of(rootValue);
        bravoValue2.persist();

        Value bravobravoValue = new Value();
        bravobravoValue.node = bravobravo;
        bravobravoValue.sources = List.of(bravoValue1);
        bravobravoValue.persist();

        tm.commit();

        List<Value> found = valueService.getDirectDescendantValues(rootValue,bravo);
        assertEquals(2,found.size(),"expect to see 2 entry: "+found);
        assertFalse(found.contains(bravobravoValue),"should not contain bravobravo["+bravobravoValue.id+"]'s value: "+found);
    }
}
