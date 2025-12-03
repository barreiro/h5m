package exp.entity;

import exp.queue.WorkQueue;
import exp.svc.NodeGroupService;
import exp.svc.NodeService;
import exp.svc.ValueService;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.util.*;

@Entity
@Table(
        name = "work"
)
@DiscriminatorColumn(name = "type")
@Immutable
public class Work  extends PanacheEntity implements Comparable<Work>{

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name="work_values",
            joinColumns = @JoinColumn(name = "work_id"),
            inverseJoinColumns = @JoinColumn(name = "value_id")
    )
    public List<Value> sourceValues;//multiple values could happen for cross test comparisons and

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(
            name="work_nodes",
            joinColumns = @JoinColumn(name = "work_id"),
            inverseJoinColumns = @JoinColumn(name = "node_id")
    )
    public List<Node> sourceNodes; //what is going to use a list of sources that are not already listed for the activeNode?

    @ManyToOne
    @JoinColumn(name = "active_node_id")
    public Node activeNode; //is there any work that would not have a node associated?


    public Work(){}
    public Work(Node activeNode,List<Node> sourceNodes,List<Value> sourceValues){
        this.activeNode = activeNode;
        this.sourceValues = sourceValues == null ? Collections.emptyList() : new ArrayList(sourceValues);
        this.sourceNodes = sourceNodes == null ? Collections.emptyList() : new ArrayList(sourceNodes);
    }

    public Node getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(Node activeNode) {
        this.activeNode = activeNode;
    }

    //work A depends on work B if A.activeNode depends on B.activeNode or
    public boolean dependsOn(Work work){

        if(work == null || work.activeNode == null){
            return false;
        }
        boolean activeValue = activeNode!=null && activeNode.dependsOn(work.activeNode);
        boolean foundValue = sourceValues.stream().anyMatch(sourceValue ->
                sourceValue.node.dependsOn(work.activeNode));
        boolean foundNode = sourceNodes.stream().anyMatch(sourceNode ->
                sourceNode.dependsOn(work.activeNode));

        return foundValue || foundNode || activeValue;
    }

    public List<Work> getDependentWorks(List<Work> works){
        List<Work> rtrn = works.stream().filter(this::dependsOn).toList();
        return rtrn;
    }

    @Override
    public int compareTo(Work o) {
        if(this.dependsOn(o)){
            return 1;
        } else if (o.dependsOn(this)){
            return -1;
        } else {
            return 0;
        }
    }
}
