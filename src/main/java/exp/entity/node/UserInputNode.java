package exp.entity.node;

import exp.entity.Node;
import exp.entity.Value;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Entity
@DiscriminatorValue("user")
public class UserInputNode extends Node {

    public UserInputNode(){}
    public UserInputNode(String name,String operation){
        super(name,operation);
    }

    @Override
    protected Node shallowCopy() {
        return new UserInputNode(name,operation);
    }
}
