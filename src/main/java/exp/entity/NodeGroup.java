package exp.entity;

import exp.entity.node.RootNode;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(
        name = "nodegroup"
)
public class NodeGroup extends PanacheEntity {

    public String name;

    @OneToOne(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
    public Node root;

    @OneToMany(cascade = { CascadeType.PERSIST,
            CascadeType.MERGE }, fetch = FetchType.LAZY, orphanRemoval = false, mappedBy = "group")
    public List<Node> sources;

    public NodeGroup(){
        this.sources = new ArrayList<>();
        this.root = new RootNode();

    }
    public NodeGroup(String name){
        this.name = name;
        this.sources = new ArrayList<>();
        this.root = new RootNode();
    }

    public boolean canLoad(NodeGroup group){
        Set<String> fqdn = sources.stream().map(n->n.getFqdn()).collect(Collectors.toSet());
        return group.sources.stream().noneMatch(n->fqdn.contains(n.getFqdn()));
    }
    public void loadGroup(NodeGroup group){
        //TODO do we track the sourceGroup when loading a group

    }

    @PreUpdate
    @PrePersist
    public void checkNodes(){

        //ensure all nodes reference the root for this group
        sources.forEach(n->{
            if(!n.sources.isEmpty()){
                for(int i=0; i<n.sources.size(); i++){
                    if(n.sources.get(i) instanceof RootNode && !n.sources.get(i).equals(root)){
                        n.sources.remove(i);
                        n.sources.add(i,root);
                    }
                }
            }
        });
        boolean hasSources = sources.stream().anyMatch(n->!n.sources.isEmpty());
        if(hasSources){
            this.sources = Node.kahnDagSort(sources);
        }
    }


}
