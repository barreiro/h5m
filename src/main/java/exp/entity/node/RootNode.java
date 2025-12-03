package exp.entity.node;

import exp.entity.Node;
import exp.entity.Value;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@DiscriminatorValue("root")
public class RootNode extends Node {

    public RootNode() {
        super();
        this.type="root";
    }
    //The root node does not shallow copy
    @Override
    protected Node shallowCopy() {
        return this;
    }
}
