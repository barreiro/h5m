package exp.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name="folder"
)
public class Folder extends PanacheEntity {

    @Column(unique = true)
    public String name;

    @Column(unique = true)
    public String path;

    @OneToOne(cascade = {CascadeType.ALL})
    public NodeGroup group;

    public Folder(){

    }
    public Folder(String name,String path){
        this.name = name;
        this.path = path;
        //TODO do we auto-create a nodeGroup?
        this.group = new NodeGroup(name);
    }
    public Folder(String name,String path,NodeGroup group){
        this.name = name;
        this.path = path;
        this.group = group;
    }

    @Override
    public String toString() {
        return "Folder<"+id+">[ name="+name+" path="+path+" ]";

    }
}
