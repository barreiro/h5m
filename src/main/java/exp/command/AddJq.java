package exp.command;

import exp.entity.Node;
import exp.entity.NodeGroup;
import exp.entity.node.JqNode;
import exp.svc.NodeGroupService;
import exp.svc.NodeService;
import io.hyperfoil.tools.yaup.AsciiArt;
import jakarta.inject.Inject;
import picocli.CommandLine;

import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name="jq", separator = " ", description = "add jq node", mixinStandardHelpOptions = true)
public class AddJq implements Callable<Integer> {

    @CommandLine.Option(names = {"to"},description = "target group / test" ) String groupName;
    @CommandLine.Parameters(index="0",arity="0..1",description = "node name") String name;
    @CommandLine.Parameters(index="1",arity="0..1",description = "jq filter") String jq;

    @Inject
    NodeGroupService nodeGroupService;

    @Inject
    NodeService nodeService;

    @Override
    public Integer call() throws Exception {
        Scanner sc = new Scanner(System.in);
        if(name == null && H5m.consoleAttached()){
            System.out.printf("Enter name: ");
            name = sc.nextLine();
        }
        NodeGroup foundGroup = null;
        do{
            if(groupName == null && H5m.consoleAttached()){
                System.out.printf("Enter target group / folder name: ");
                groupName = sc.nextLine();
            }
            foundGroup =  nodeGroupService.byName(groupName);
            if(foundGroup == null){
                System.out.println("could not find "+groupName);
                groupName = null;
            }
        }while(groupName == null && H5m.consoleAttached());

        if(jq == null && H5m.consoleAttached()){
            System.out.printf("Enter jq filter: ");
            jq = sc.nextLine();
        }
        NodeGroup staticFoundGroup = foundGroup;
        Node node = JqNode.parse(name,jq, n->nodeService.findNodeByFqdn(n,staticFoundGroup.id));
        if(node == null){
            System.err.println("cannot create node from jq="+jq);
            return 1;
        }
        node.group=foundGroup;
        if(node.sources.isEmpty()){
            node.sources.add(foundGroup.root);
        }
        long response = nodeService.create(node);
        if(response < 0 ){
            return 1;
        }
        return 0;
    }
}
